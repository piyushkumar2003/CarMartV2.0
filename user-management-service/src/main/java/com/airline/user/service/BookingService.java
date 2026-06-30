package com.airline.user.service;

import com.airline.user.dto.FlightDTO;
import com.airline.user.entity.Booking;
import com.airline.user.entity.BookingStatus;
import com.airline.user.repository.BookingRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * ============================================
 * BOOKING SERVICE
 * ============================================
 * 
 * Handles all booking lifecycle operations.
 * This is the BRAIN of the booking system!
 * 
 * KEY RESPONSIBILITIES:
 * 1. Create bookings (INITIATED state)
 * 2. Lock seats and move to PENDING
 * 3. Confirm bookings (PENDING/PAYMENT_PENDING -> CONFIRMED)
 * 4. Cancel bookings (PENDING/PAYMENT_PENDING/INITIATED -> CANCELLED)
 * 5. Handle expired bookings (check Redis lock status)
 * 6. Validate state transitions (enforce business rules)
 * 
 * STATE MACHINE:
 * ──────────────
 * INITIATED ──(seat locked)──> PENDING
 * │
 * ┌──────────────┼──────────────┐
 * ▼ ▼ ▼
 * CONFIRMED CANCELLED EXPIRED
 * (terminal) (terminal) (terminal)
 */
@Service
public class BookingService {

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private UserService userService;

    // ===========================================
    // STEP 1: INITIATE BOOKING
    // ===========================================

    /**
     * Creates a new booking in INITIATED state.
     * At this point, no seat is locked yet.
     * User just expressed intent to book a flight.
     * 
     * FLOW:
     * 1. Fetch flight details from Flight Service
     * 2. Create booking with INITIATED status
     * 3. Save to database
     * 4. Return the created booking
     * 
     * @param userId   The user making the booking
     * @param flightId The flight to book
     * @return The created booking in INITIATED state
     */
    public Booking initiateBooking(Long userId, Long flightId) {
        // Fetch flight details from Flight Management Service
        FlightDTO flight = userService.getFlightById(flightId);

        if (flight == null) {
            throw new RuntimeException("Flight not found with ID: " + flightId);
        }

        Booking booking = new Booking();
        booking.setUserId(userId);
        booking.setFlightId(flightId);
        booking.setFlightNumber(flight.getFlightNumber());
        booking.setSource(flight.getSource());
        booking.setDestination(flight.getDestination());
        booking.setDateOfJourney(flight.getDateOfJourney());
        booking.setBookingStatus(BookingStatus.INITIATED);

        // Lock the price at initiation
        booking.setPrice(flight.getPrice());

        return bookingRepository.save(booking);
    }

    // ===========================================
    // STEP 2: LOCK SEAT AND MOVE TO PENDING
    // ===========================================

    /**
     * Locks a seat in Redis and updates booking to PENDING.
     * 
     * BUSINESS RULES:
     * - Booking must be in INITIATED state
     * - Seat must not be already booked or locked
     * - On success: status -> PENDING, seatNumber is set
     * - On failure: return false (seat unavailable)
     * 
     * WHY SEPARATE FROM INITIATE?
     * - User might change their mind about seat selection
     * - Allows seat selection UI to be shown first
     * - Seat lock has TTL, so we lock only when ready
     * 
     * @param bookingId  The booking to update
     * @param seatNumber The seat to lock
     * @return true if seat locked successfully, false otherwise
     */
    public boolean lockSeatAndSetPending(Long bookingId, String seatNumber) {
        Booking booking = bookingRepository.findById(bookingId).orElse(null);

        if (booking == null) {
            System.out.println("ERROR: Booking not found: " + bookingId);
            return false;
        }

        // RULE: Can only lock seat for INITIATED bookings
        if (booking.getBookingStatus() != BookingStatus.INITIATED) {
            System.out.println("ERROR: Cannot lock seat - booking is not in INITIATED state. Current: "
                    + booking.getBookingStatus());
            return false;
        }

        // Attempt to lock seat in Redis via Flight Service
        boolean success = userService.lockSeat(booking.getFlightNumber(), seatNumber);

        if (success) {
            // SUCCESS! Update booking to PENDING
            booking.setSeatNumber(seatNumber);
            booking.setBookingStatus(BookingStatus.PENDING);
            bookingRepository.save(booking);

            System.out.println("SUCCESS: Seat " + seatNumber + " locked. Booking "
                    + bookingId + " is now PENDING.");
            return true;
        } else {
            System.out.println("FAILED: Could not lock seat " + seatNumber
                    + " (already taken or locked by another user)");
            return false;
        }
    }

    // ===========================================
    // STEP 3: CONFIRM BOOKING
    // ===========================================

    /**
     * Confirms a PENDING or PAYMENT_PENDING booking.
     * This is the final step - makes the booking permanent!
     * 
     * ACTIONS:
     * 1. Verify booking is in PENDING or PAYMENT_PENDING state
     * 2. Check if Redis lock is still valid (not expired)
     * 3. If valid: finalize seat in Flight Service (permanent booking)
     * 4. Update status to CONFIRMED
     * 5. Remove Redis lock (seat is now in MySQL permanently)
     * 
     * RETURNS:
     * - "SUCCESS" if confirmed
     * - "EXPIRED" if lock expired (user took too long)
     * - "PAYMENT_PENDING" if payment failed or payment service is unavailable
     * - "INVALID_STATE" if not in a confirmable state
     * - "NOT_FOUND" if booking doesn't exist
     * 
     * @param bookingId The booking to confirm
     * @return Status message indicating result
     */
    public String confirmBooking(Long bookingId) {
        Booking booking = bookingRepository.findById(bookingId).orElse(null);

        if (booking == null) {
            return "NOT_FOUND";
        }

        // RULE: Only held bookings can be confirmed.
        if (!booking.canBeConfirmed()) {
            System.out.println("Cannot confirm - booking is " + booking.getBookingStatus());
            return "INVALID_STATE";
        }

        // Check if Redis lock is still valid (hasn't expired)
        boolean seatStillLocked = userService.isSeatLocked(
                booking.getFlightNumber(),
                booking.getSeatNumber());

        if (!seatStillLocked) {
            // Redis lock expired! Mark booking as EXPIRED
            booking.setBookingStatus(BookingStatus.EXPIRED);
            bookingRepository.save(booking);

            System.out.println("EXPIRED: Seat lock for booking " + bookingId
                    + " has expired. User took too long to confirm.");
            return "EXPIRED";
        }

        // Lock is still valid - proceed with confirmation!

        // 1. Process Payment (Protected by Circuit Breaker)
        // If payment service is down, this returns false (fallback)
        boolean paymentSuccess = userService.processPayment(booking.getId(), booking.getPrice());

        if (!paymentSuccess) {
            booking.setBookingStatus(BookingStatus.PAYMENT_PENDING);
            bookingRepository.save(booking);

            System.out.println("PAYMENT_PENDING: Payment failed or service unavailable. Booking "
                    + bookingId + " requires retry.");
            return "PAYMENT_PENDING";
        }

        // 2. Finalize seat in Flight Service (saves to MySQL, removes Redis lock)
        userService.finalizeSeat(booking.getFlightNumber(), booking.getSeatNumber());

        // 3. Generate PNR if not already present
        if (booking.getPnr() == null || booking.getPnr().isEmpty()) {
            String generatedPnr = generatePnrForFlight(booking.getFlightNumber());
            booking.setPnr(generatedPnr);
        }

        // 4. Update booking status to CONFIRMED
        booking.setBookingStatus(BookingStatus.CONFIRMED);
        bookingRepository.save(booking);

        System.out.println("SUCCESS: Booking " + bookingId + " is now CONFIRMED! PNR: " + booking.getPnr());
        return "SUCCESS";
    }

    /**
     * Retries payment for a booking that previously moved to PAYMENT_PENDING.
     */
    public String retryPayment(Long bookingId) {
        Booking booking = bookingRepository.findById(bookingId).orElse(null);

        if (booking == null) {
            return "NOT_FOUND";
        }

        if (booking.getBookingStatus() != BookingStatus.PAYMENT_PENDING) {
            return "INVALID_STATE";
        }

        return confirmBooking(bookingId);
    }

    /**
     * Generates a unique PNR in format AIR-{FLIGHT}-{RANDOM4}.
     */
    private String generatePnrForFlight(String flightNumber) {
        java.util.Random random = new java.util.Random();
        String pnr;
        boolean exists;

        do {
            int randomNum = 1000 + random.nextInt(9000); // 4 digit random number
            pnr = "AIR-" + flightNumber + "-" + randomNum;
            exists = bookingRepository.findByPnr(pnr).isPresent();
        } while (exists);

        return pnr;
    }

    /**
     * Find a booking by its unique PNR.
     */
    public Booking getBookingByPnr(String pnr) {
        return bookingRepository.findByPnr(pnr).orElse(null);
    }

    // ===========================================
    // STEP 4: CANCEL BOOKING
    // ===========================================

    /**
     * Cancels a booking.
     * 
     * CAN CANCEL:
     * - INITIATED: Just delete/cancel, no cleanup needed
     * - PENDING/PAYMENT_PENDING: Release Redis lock, cancel booking
     * 
     * CANNOT CANCEL:
     * - CONFIRMED: Booking is complete (would need refund logic)
     * - EXPIRED: Already expired
     * - CANCELLED: Already cancelled
     * 
     * @param bookingId The booking to cancel
     * @return true if cancelled, false if not allowed
     */
    public boolean cancelBooking(Long bookingId) {
        Booking booking = bookingRepository.findById(bookingId).orElse(null);

        if (booking == null) {
            System.out.println("ERROR: Booking not found: " + bookingId);
            return false;
        }

        // Check if cancellation is allowed
        if (!booking.canBeCancelled()) {
            System.out.println("ERROR: Cannot cancel booking in state: " + booking.getBookingStatus());
            return false;
        }

        // If seat is held, release the Redis lock
        if (isSeatLockDependent(booking)) {
            userService.releaseSeat(booking.getFlightNumber(), booking.getSeatNumber());
            System.out.println("Released Redis lock for seat " + booking.getSeatNumber());
        }

        // Update status to CANCELLED
        booking.setBookingStatus(BookingStatus.CANCELLED);
        bookingRepository.save(booking);

        System.out.println("SUCCESS: Booking " + bookingId + " is now CANCELLED.");
        return true;
    }

    // ===========================================
    // CHECK BOOKING STATUS (WITH EXPIRY CHECK)
    // ===========================================

    /**
     * Gets booking and checks if it should be marked as EXPIRED.
     * 
     * WHY THIS METHOD?
     * - Redis TTL may expire while booking is in PENDING state
     * - We need to detect this and update our database
     * - This method checks Redis lock and updates if expired
     * 
     * @param bookingId The booking to check
     * @return The booking (possibly updated to EXPIRED)
     */
    public Booking getBookingWithExpiryCheck(Long bookingId) {
        Booking booking = bookingRepository.findById(bookingId).orElse(null);

        if (booking == null) {
            return null;
        }

        expireIfSeatLockGone(booking);

        return booking;
    }

    // ===========================================
    // QUERY METHODS
    // ===========================================

    /**
     * Get all bookings for a specific user.
     * Checks for expired PENDING bookings while fetching.
     */
    public List<Booking> getBookingsForUser(Long userId) {
        List<Booking> bookings = bookingRepository.findByUserId(userId);

        // Check each held booking for expiry
        for (Booking booking : bookings) {
            expireIfSeatLockGone(booking);
        }

        return bookings;
    }

    /**
     * Get a booking by ID (simple lookup, no expiry check).
     * Use getBookingWithExpiryCheck() if you need expiry validation.
     */
    public Booking getBookingById(Long bookingId) {
        return bookingRepository.findById(bookingId).orElse(null);
    }

    /**
     * Get all bookings (for admin view).
     * Sorted by creation date, newest first.
     */
    public List<Booking> getAllBookings() {
        return bookingRepository.findAllByOrderByCreatedAtDesc();
    }

    /**
     * Get bookings by status (for admin filtering).
     */
    public List<Booking> getBookingsByStatus(BookingStatus status) {
        List<Booking> bookings = bookingRepository.findByBookingStatus(status);
        for (Booking booking : bookings) {
            expireIfSeatLockGone(booking);
        }
        return bookings;
    }

    /**
     * Get bookings for a specific user filtered by status.
     * Used for the booking history filter feature.
     */
    public List<Booking> getBookingsForUserByStatus(Long userId, BookingStatus status) {
        List<Booking> bookings = bookingRepository.findByUserIdAndBookingStatus(userId, status);
        for (Booking booking : bookings) {
            expireIfSeatLockGone(booking);
        }
        return bookings;
    }

    /**
     * Upgrade booking to Business class.
     * Can only upgrade CONFIRMED bookings.
     */
    public boolean upgradeToBusinessClass(Long bookingId) {
        Booking booking = bookingRepository.findById(bookingId).orElse(null);

        if (booking == null) {
            return false;
        }

        // Only CONFIRMED bookings can be upgraded
        if (booking.getBookingStatus() != BookingStatus.CONFIRMED) {
            return false;
        }

        booking.setSeatClass("Business");
        bookingRepository.save(booking);
        return true;
    }

    /**
     * Save a booking (for backward compatibility).
     */
    public Booking saveBooking(Booking booking) {
        return bookingRepository.save(booking);
    }

    private boolean isSeatLockDependent(Booking booking) {
        return booking.getBookingStatus() == BookingStatus.PENDING
                || booking.getBookingStatus() == BookingStatus.PAYMENT_PENDING;
    }

    private void expireIfSeatLockGone(Booking booking) {
        if (!isSeatLockDependent(booking)) {
            return;
        }

        boolean seatStillLocked = userService.isSeatLocked(
                booking.getFlightNumber(),
                booking.getSeatNumber());

        if (!seatStillLocked) {
            booking.setBookingStatus(BookingStatus.EXPIRED);
            bookingRepository.save(booking);
            System.out.println("Detected expired booking: " + booking.getId());
        }
    }
}
