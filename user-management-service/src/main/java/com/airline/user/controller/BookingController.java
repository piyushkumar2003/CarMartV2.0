package com.airline.user.controller;

import java.util.List;
import java.util.Map;
import java.util.HashMap;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import com.airline.user.entity.User;
import com.airline.user.entity.Booking;
import com.airline.user.entity.BookingStatus;
import com.airline.user.dto.FlightDTO;
import com.airline.user.service.BookingService;
import com.airline.user.service.UserService;

/**
 * ============================================
 * BOOKING CONTROLLER
 * ============================================
 * 
 * Handles all booking-related HTTP requests.
 * Implements the complete booking lifecycle flow.
 * 
 * FLOW OVERVIEW:
 * 1. User selects flight -> POST /initiate-booking
 * 2. User selects seat -> POST /lock-seat-and-pending
 * 3. User confirms -> POST /confirm-booking
 * OR
 * 3. User cancels -> POST /cancel-booking
 * 
 * PAGES:
 * - booking-initiate.html: Show flight details, start booking
 * - booking-pending.html: Show seat locked, waiting for confirmation
 * - booking-status.html: Show booking status (all states)
 * - admin-bookings.html: Admin view of all bookings
 */
@Controller
public class BookingController {

    @Autowired
    private BookingService bookingService;

    @Autowired
    private UserService userService;

    // ===========================================
    // STEP 1: INITIATE BOOKING
    // ===========================================

    /**
     * Show the booking initiation page.
     * Displays flight details and "Start Booking" button.
     */
    @GetMapping("/initiate-booking")
    public String showInitiateBooking(@RequestParam Long flightId,
            HttpSession session, Model model) {
        User user = (User) session.getAttribute("user");
        if (user == null)
            return "redirect:/login";

        // Fetch flight details from Flight Service
        FlightDTO flight = userService.getFlightById(flightId);
        if (flight == null) {
            model.addAttribute("error", "Flight not found!");
            return "error";
        }

        model.addAttribute("flight", flight);
        return "booking-initiate";
    }

    /**
     * POST /initiate-booking
     * Creates a new booking in INITIATED state.
     * Redirects to seat selection.
     */
    @PostMapping("/initiate-booking")
    public String initiateBooking(@RequestParam Long flightId,
            HttpSession session, Model model) {
        User user = (User) session.getAttribute("user");
        if (user == null)
            return "redirect:/login";

        try {
            // Create booking in INITIATED state
            Booking booking = bookingService.initiateBooking(user.getId(), flightId);
            System.out.println("Created booking #" + booking.getId() + " in INITIATED state");

            // Redirect to seat selection page with booking ID
            return "redirect:/select-seat?bookingId=" + booking.getId();
        } catch (Exception e) {
            model.addAttribute("error", "Failed to initiate booking: " + e.getMessage());
            return "error";
        }
    }

    // ===========================================
    // STEP 2: SELECT SEAT AND LOCK
    // ===========================================

    /**
     * GET /select-seat
     * Shows seat map for the flight.
     * User picks a seat from available options.
     */
    @GetMapping("/select-seat")
    public String selectSeatPage(@RequestParam Long bookingId,
            HttpSession session, Model model) {
        User user = (User) session.getAttribute("user");
        if (user == null)
            return "redirect:/login";

        // Get booking and verify ownership
        Booking booking = bookingService.getBookingById(bookingId);
        if (booking == null || !booking.getUserId().equals(user.getId())) {
            model.addAttribute("error", "Booking not found!");
            return "error";
        }

        // Only INITIATED bookings can proceed to seat selection
        if (booking.getBookingStatus() != BookingStatus.INITIATED) {
            // Already selected seat or in other state, redirect appropriately
            if (booking.getBookingStatus() == BookingStatus.PENDING
                    || booking.getBookingStatus() == BookingStatus.PAYMENT_PENDING) {
                return "redirect:/booking-pending?bookingId=" + bookingId;
            }
            return "redirect:/booking-status/" + bookingId;
        }

        // Get seat map from Flight Service
        List<com.airline.user.dto.SeatDTO> seats = userService.getSeatMap(booking.getFlightNumber());

        model.addAttribute("seats", seats);
        model.addAttribute("booking", booking);
        model.addAttribute("bookingId", bookingId);
        model.addAttribute("flightNumber", booking.getFlightNumber());

        return "seat-selection";
    }

    /**
     * POST /lock-seat-and-pending
     * Attempts to lock the selected seat in Redis.
     * If successful, moves booking to PENDING state.
     */
    @PostMapping("/lock-seat-and-pending")
    public String lockSeatAndPending(@RequestParam Long bookingId,
            @RequestParam String seatNumber,
            HttpSession session, Model model) {
        User user = (User) session.getAttribute("user");
        if (user == null)
            return "redirect:/login";

        // Verify booking ownership
        Booking booking = bookingService.getBookingById(bookingId);
        if (booking == null || !booking.getUserId().equals(user.getId())) {
            model.addAttribute("error", "Booking not found!");
            return "error";
        }

        // Check if we (this booking) already have the seat locked from the previous
        // step (REST call)
        if (booking.getSeatNumber() != null && booking.getSeatNumber().equals(seatNumber)) {
            // Verify lock validity just in case
            boolean stillLocked = userService.isSeatLocked(booking.getFlightNumber(), seatNumber);
            if (stillLocked) {
                // Already locked by us! Just allow proceed to PENDING status
                booking.setBookingStatus(BookingStatus.PENDING);
                bookingService.saveBooking(booking);
                return "redirect:/booking-pending?bookingId=" + bookingId;
            }
        }

        // Attempt to lock seat
        boolean success = bookingService.lockSeatAndSetPending(bookingId, seatNumber);

        if (success) {
            // SUCCESS! Redirect to pending confirmation page
            return "redirect:/booking-pending?bookingId=" + bookingId;
        } else {
            // FAILED! Seat already taken, go back to seat selection
            model.addAttribute("error", "Seat " + seatNumber + " is already taken or locked. Please choose another.");
            return selectSeatPage(bookingId, session, model);
        }
    }

    /**
     * POST /lock-seat (REST API)
     * 
     * AJAX endpoint to lock a seat without page reload.
     * Returns JSON response for client-side handling.
     * 
     * Used by seat-selection.html JavaScript to:
     * 1. Lock the seat via API
     * 2. Show success/error message
     * 3. Enable/disable "Proceed Booking" button
     */
    @PostMapping("/lock-seat")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> lockSeat(
            @RequestParam Long bookingId,
            @RequestParam String seatNumber,
            HttpSession session) {

        Map<String, Object> response = new HashMap<>();

        User user = (User) session.getAttribute("user");
        if (user == null) {
            response.put("success", false);
            response.put("message", "Please login to continue.");
            return ResponseEntity.status(401).body(response);
        }

        // Verify booking ownership
        Booking booking = bookingService.getBookingById(bookingId);
        if (booking == null || !booking.getUserId().equals(user.getId())) {
            response.put("success", false);
            response.put("message", "Booking not found!");
            return ResponseEntity.badRequest().body(response);
        }

        // Attempt to lock seat via Flight Service (but don't change to PENDING yet)
        boolean success = userService.lockSeat(booking.getFlightNumber(), seatNumber);

        if (success) {
            // Update booking with seat number
            booking.setSeatNumber(seatNumber);
            bookingService.saveBooking(booking);

            response.put("success", true);
            response.put("message", "Seat " + seatNumber + " locked successfully!");
            response.put("seatNumber", seatNumber);
            response.put("bookingId", bookingId);
            return ResponseEntity.ok(response);
        } else {
            response.put("success", false);
            response.put("message", "Seat " + seatNumber + " is already taken or locked. Please choose another.");
            return ResponseEntity.ok(response);
        }
    }

    /**
     * GET /booking-confirmation
     * 
     * Shows intermediate confirmation page after seat is locked.
     * Displays booking details and allows user to proceed or change seat.
     */
    @GetMapping("/booking-confirmation")
    public String showBookingConfirmation(
            @RequestParam Long bookingId,
            @RequestParam String seatNumber,
            HttpSession session, Model model) {

        User user = (User) session.getAttribute("user");
        if (user == null)
            return "redirect:/login";

        // Get booking
        Booking booking = bookingService.getBookingById(bookingId);
        if (booking == null || !booking.getUserId().equals(user.getId())) {
            model.addAttribute("error", "Booking not found!");
            return "error";
        }

        model.addAttribute("booking", booking);
        model.addAttribute("bookingId", bookingId);
        model.addAttribute("seatNumber", seatNumber);

        return "booking-confirmation";
    }

    // ===========================================
    // STEP 3: PENDING CONFIRMATION
    // ===========================================

    /**
     * GET /booking-pending
     * Shows the booking in PENDING state.
     * Displays countdown message and Confirm/Cancel buttons.
     */
    @GetMapping("/booking-pending")
    public String showBookingPending(@RequestParam Long bookingId,
            HttpSession session, Model model) {
        User user = (User) session.getAttribute("user");
        if (user == null)
            return "redirect:/login";

        // Get booking with expiry check
        Booking booking = bookingService.getBookingWithExpiryCheck(bookingId);

        if (booking == null || !booking.getUserId().equals(user.getId())) {
            model.addAttribute("error", "Booking not found!");
            return "error";
        }

        // If expired, show appropriate message
        if (booking.getBookingStatus() == BookingStatus.EXPIRED) {
            model.addAttribute("booking", booking);
            model.addAttribute("message", "Your booking has expired. The seat lock timed out.");
            return "booking-expired";
        }

        // If payment is pending, show the detail page with retry actions.
        if (booking.getBookingStatus() == BookingStatus.PAYMENT_PENDING) {
            return "redirect:/booking-status/" + bookingId + "?paymentPending=true";
        }

        // If not PENDING anymore, redirect to status page
        if (booking.getBookingStatus() != BookingStatus.PENDING) {
            return "redirect:/booking-status/" + bookingId;
        }

        model.addAttribute("booking", booking);
        return "booking-pending";
    }

    // ===========================================
    // STEP 4: CONFIRM BOOKING
    // ===========================================

    /**
     * POST /confirm-booking
     * User confirms the booking.
     * Moves from PENDING to CONFIRMED.
     */
    @PostMapping("/confirm-booking")
    public String confirmBooking(@RequestParam Long bookingId,
            HttpSession session, Model model) {
        User user = (User) session.getAttribute("user");
        if (user == null)
            return "redirect:/login";

        // Verify booking ownership
        Booking booking = bookingService.getBookingById(bookingId);
        if (booking == null || !booking.getUserId().equals(user.getId())) {
            model.addAttribute("error", "Booking not found!");
            return "error";
        }

        // Attempt to confirm booking
        String result = bookingService.confirmBooking(bookingId);

        switch (result) {
            case "SUCCESS":
                // Booking confirmed! Redirect to success view
                return "redirect:/booking-status/" + bookingId + "?confirmed=true";

            case "EXPIRED":
                model.addAttribute("error", "Sorry, your seat reservation has expired. Please try booking again.");
                model.addAttribute("booking", bookingService.getBookingById(bookingId));
                return "booking-expired";

            case "INVALID_STATE":
                model.addAttribute("error", "This booking cannot be confirmed (invalid state).");
                return "redirect:/booking-status/" + bookingId;

            case "PAYMENT_PENDING":
                // Payment failed or service unavailable
                return "redirect:/booking-status/" + bookingId + "?paymentPending=true";

            default:
                model.addAttribute("error", "Booking not found!");
                return "error";
        }
    }

    /**
     * POST /retry-payment
     * Retries payment for a PAYMENT_PENDING booking.
     */
    @PostMapping("/retry-payment")
    public String retryPayment(@RequestParam Long bookingId,
            HttpSession session, Model model) {
        User user = (User) session.getAttribute("user");
        if (user == null)
            return "redirect:/login";

        Booking booking = bookingService.getBookingById(bookingId);
        if (booking == null || !booking.getUserId().equals(user.getId())) {
            model.addAttribute("error", "Booking not found!");
            return "error";
        }

        String result = bookingService.retryPayment(bookingId);

        switch (result) {
            case "SUCCESS":
                return "redirect:/booking-status/" + bookingId + "?confirmed=true";

            case "PAYMENT_PENDING":
                return "redirect:/booking-status/" + bookingId + "?paymentPending=true";

            case "EXPIRED":
                model.addAttribute("error", "Sorry, your seat reservation has expired. Please start again.");
                model.addAttribute("booking", bookingService.getBookingById(bookingId));
                return "booking-expired";

            default:
                model.addAttribute("error", "Payment cannot be retried for this booking.");
                return "redirect:/booking-status/" + bookingId;
        }
    }

    // ===========================================
    // CANCEL BOOKING
    // ===========================================

    /**
     * POST /cancel-booking
     * Cancels an INITIATED or PENDING booking.
     * CONFIRMED bookings cannot be cancelled here.
     */
    @PostMapping("/cancel-booking")
    public String cancelBooking(@RequestParam Long bookingId,
            HttpSession session, Model model) {
        User user = (User) session.getAttribute("user");
        if (user == null)
            return "redirect:/login";

        // Verify booking ownership
        Booking booking = bookingService.getBookingById(bookingId);
        if (booking == null || !booking.getUserId().equals(user.getId())) {
            model.addAttribute("error", "Booking not found!");
            return "error";
        }

        // Check if booking is CONFIRMED (cannot cancel)
        if (booking.getBookingStatus() == BookingStatus.CONFIRMED) {
            model.addAttribute("error", "Cannot cancel a confirmed booking. Please contact support for refunds.");
            return "redirect:/booking-status/" + bookingId;
        }

        // Cancel the booking
        boolean cancelled = bookingService.cancelBooking(bookingId);

        if (cancelled) {
            return "redirect:/my-bookings?cancelled=true";
        } else {
            model.addAttribute("error", "Failed to cancel booking.");
            return "redirect:/booking-status/" + bookingId;
        }
    }

    // ===========================================
    // VIEW BOOKING STATUS
    // ===========================================

    /**
     * GET /booking-status/{bookingId}
     * Shows the current status of a specific booking.
     */
    @GetMapping("/booking-status/{bookingId}")
    public String viewBookingStatus(@PathVariable Long bookingId,
            @RequestParam(required = false) Boolean confirmed,
            @RequestParam(required = false) Boolean paymentPending,
            HttpSession session, Model model) {
        User user = (User) session.getAttribute("user");
        if (user == null)
            return "redirect:/login";

        // Get booking with expiry check
        Booking booking = bookingService.getBookingWithExpiryCheck(bookingId);

        if (booking == null) {
            model.addAttribute("error", "Booking not found!");
            return "error";
        }

        // Check ownership (unless admin)
        if (!booking.getUserId().equals(user.getId()) && !"ADMIN".equals(user.getRole())) {
            model.addAttribute("error", "You don't have permission to view this booking.");
            return "error";
        }

        model.addAttribute("booking", booking);
        if (Boolean.TRUE.equals(confirmed)) {
            model.addAttribute("successMessage", "Your booking has been confirmed successfully!");
        }
        if (Boolean.TRUE.equals(paymentPending)) {
            model.addAttribute("error",
                    "Payment could not be completed. Your seat is still held for now; please retry payment before it expires.");
        }

        return "booking-status-detail";
    }

    /**
     * GET /my-bookings
     * List all bookings for the logged-in user.
     * Supports filtering by status: CONFIRMED, CANCELLED, EXPIRED
     */
    @GetMapping("/my-bookings")
    public String myBookings(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Boolean cancelled,
            HttpSession session, Model model) {
        try {
            User user = (User) session.getAttribute("user");
            if (user == null)
                return "redirect:/login";

            List<Booking> bookings;

            // Filter by status if provided
            if (status != null && !status.isEmpty()) {
                try {
                    BookingStatus bookingStatus = BookingStatus.valueOf(status.toUpperCase());
                    bookings = bookingService.getBookingsForUserByStatus(user.getId(), bookingStatus);
                    model.addAttribute("selectedStatus", status.toUpperCase());
                } catch (IllegalArgumentException e) {
                    // Invalid status, get all bookings
                    bookings = bookingService.getBookingsForUser(user.getId());
                }
            } else {
                // Get all bookings (with expiry check)
                bookings = bookingService.getBookingsForUser(user.getId());
            }

            model.addAttribute("bookings", bookings);
            model.addAttribute("statuses",
                    new String[] { "CONFIRMED", "CANCELLED", "EXPIRED", "PAYMENT_PENDING", "PENDING", "INITIATED" });

            if (Boolean.TRUE.equals(cancelled)) {
                model.addAttribute("message", "Booking has been cancelled.");
            }

            return "booking-status";
        } catch (Exception e) {
            e.printStackTrace();
            model.addAttribute("error", "Failed to load bookings: " + e.getMessage());
            return "error";
        }
    }

    /**
     * GET /pnr-search
     * Show PNR search page.
     */
    @GetMapping("/pnr-search")
    public String searchPnrPage() {
        return "search-pnr";
    }

    /**
     * GET /booking-by-pnr/{pnr}
     * Find booking by PNR and show details.
     */
    @GetMapping("/booking-by-pnr")
    public String searchByPnr(@RequestParam String pnr, Model model) {
        if (pnr == null || pnr.trim().isEmpty()) {
            model.addAttribute("error", "PNR is required");
            return "search-pnr";
        }

        Booking booking = bookingService.getBookingByPnr(pnr.trim().toUpperCase());
        if (booking == null) {
            model.addAttribute("error", "No booking found for PNR: " + pnr);
            return "search-pnr";
        }

        // Redirect to the existing status detail page which already knows how to show
        // booking details
        return "redirect:/booking-status/" + booking.getId();
    }

    // ===========================================
    // ADMIN: VIEW ALL BOOKINGS
    // ===========================================

    /**
     * GET /admin-bookings
     * Admin view of all bookings in the system.
     */
    @GetMapping("/admin-bookings")
    public String adminBookings(@RequestParam(required = false) String status,
            HttpSession session, Model model) {
        User user = (User) session.getAttribute("user");
        if (user == null || !"ADMIN".equals(user.getRole())) {
            return "redirect:/login";
        }

        List<Booking> bookings;

        // Filter by status if provided
        if (status != null && !status.isEmpty()) {
            try {
                BookingStatus bookingStatus = BookingStatus.valueOf(status);
                bookings = bookingService.getBookingsByStatus(bookingStatus);
                model.addAttribute("selectedStatus", status);
            } catch (IllegalArgumentException e) {
                bookings = bookingService.getAllBookings();
            }
        } else {
            bookings = bookingService.getAllBookings();
        }

        model.addAttribute("bookings", bookings);
        model.addAttribute("statuses", BookingStatus.values());

        return "admin-bookings";
    }

    // ===========================================
    // UPGRADE BOOKING
    // ===========================================

    /**
     * POST /upgrade-booking
     * Upgrade a CONFIRMED booking to Business class.
     */
    @PostMapping("/upgrade-booking")
    public String upgradeBooking(@RequestParam Long bookingId, HttpSession session) {
        User user = (User) session.getAttribute("user");
        if (user == null)
            return "redirect:/login";

        Booking booking = bookingService.getBookingById(bookingId);
        if (booking != null && booking.getUserId().equals(user.getId())) {
            bookingService.upgradeToBusinessClass(bookingId);
        }

        return "redirect:/my-bookings";
    }
}
