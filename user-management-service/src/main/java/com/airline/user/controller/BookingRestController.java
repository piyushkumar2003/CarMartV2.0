package com.airline.user.controller;

import java.util.List;
import java.util.Map;
import java.util.HashMap;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import com.airline.user.dto.ApiMessageResponse;
import com.airline.user.dto.BookingInitiateRequest;
import com.airline.user.dto.BookingResponse;
import com.airline.user.dto.BookingSeatLockRequest;
import com.airline.user.dto.BookingStatusResponse;
import com.airline.user.dto.SeatMapResponse;
import com.airline.user.entity.Booking;
import com.airline.user.entity.BookingStatus;
import com.airline.user.entity.User;
import com.airline.user.service.BookingService;
import com.airline.user.service.UserService;
import com.airline.user.dto.SeatDTO;

@RestController
@RequestMapping("/api/bookings")
public class BookingRestController {

    @Autowired
    private BookingService bookingService;

    @Autowired
    private UserService userService;

    @PostMapping("/initiate")
    public ResponseEntity<?> initiateBooking(@RequestBody BookingInitiateRequest request) {
        User user = getCurrentUser();
        if (user == null) {
            return unauthorized();
        }

        if (request == null || request.getFlightId() == null) {
            return ResponseEntity.badRequest().body(new ApiMessageResponse(false, "flightId is required"));
        }

        try {
            Booking booking = bookingService.initiateBooking(user.getId(), request.getFlightId());
            return ResponseEntity.ok(BookingResponse.from(booking));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(new ApiMessageResponse(false, "Failed to initiate booking: " + e.getMessage()));
        }
    }

    @GetMapping
    public ResponseEntity<?> listMyBookings(@RequestParam(required = false) String status) {
        User user = getCurrentUser();
        if (user == null) {
            return unauthorized();
        }

        try {
            List<Booking> bookings;
            if (status != null && !status.isBlank()) {
                BookingStatus bookingStatus = BookingStatus.valueOf(status.trim().toUpperCase());
                bookings = bookingService.getBookingsForUserByStatus(user.getId(), bookingStatus);
            } else {
                bookings = bookingService.getBookingsForUser(user.getId());
            }

            return ResponseEntity.ok(bookings.stream().map(BookingResponse::from).toList());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(new ApiMessageResponse(false, "Invalid booking status"));
        }
    }

    @GetMapping("/{bookingId}/seat-map")
    public ResponseEntity<?> getSeatMap(@PathVariable Long bookingId) {
        User user = getCurrentUser();
        if (user == null) {
            return unauthorized();
        }

        Booking booking = bookingService.getBookingById(bookingId);
        if (booking == null) {
            return ResponseEntity.notFound().build();
        }
        if (!canAccessBooking(user, booking)) {
            return forbidden();
        }

        List<SeatDTO> seats = userService.getSeatMap(booking.getFlightNumber());
        return ResponseEntity.ok(new SeatMapResponse(booking.getId(), booking.getFlightNumber(), seats));
    }

    @PostMapping("/{bookingId}/lock-seat")
    public ResponseEntity<?> lockSelectedSeat(@PathVariable Long bookingId,
            @RequestBody BookingSeatLockRequest request) {
        User user = getCurrentUser();
        if (user == null) {
            return unauthorized();
        }

        if (request == null || request.getSeatNumber() == null || request.getSeatNumber().isBlank()) {
            return ResponseEntity.badRequest().body(new ApiMessageResponse(false, "seatNumber is required"));
        }

        Booking booking = bookingService.getBookingById(bookingId);
        if (booking == null) {
            return ResponseEntity.notFound().build();
        }
        if (!canAccessBooking(user, booking)) {
            return forbidden();
        }

        String seatNumber = request.getSeatNumber().trim().toUpperCase();
        boolean locked = false;

        if (booking.getBookingStatus() == BookingStatus.INITIATED
                && seatNumber.equalsIgnoreCase(booking.getSeatNumber())
                && userService.isSeatLocked(booking.getFlightNumber(), seatNumber)) {
            booking.setSeatNumber(seatNumber);
            booking.setBookingStatus(BookingStatus.PENDING);
            booking = bookingService.saveBooking(booking);
            locked = true;
        } else {
            locked = bookingService.lockSeatAndSetPending(bookingId, seatNumber);
            booking = bookingService.getBookingById(bookingId);
        }

        if (!locked) {
            return ResponseEntity.badRequest()
                    .body(new ApiMessageResponse(false, "Seat is already taken, locked, or booking state is invalid"));
        }

        return ResponseEntity.ok(BookingResponse.from(booking));
    }

    @PostMapping("/{bookingId}/cancel")
    public ResponseEntity<?> cancelBookingApi(@PathVariable Long bookingId) {
        User user = getCurrentUser();
        if (user == null) {
            return unauthorized();
        }

        Booking booking = bookingService.getBookingById(bookingId);
        if (booking == null) {
            return ResponseEntity.notFound().build();
        }
        if (!canAccessBooking(user, booking)) {
            return forbidden();
        }

        boolean cancelled = bookingService.cancelBooking(bookingId);
        if (!cancelled) {
            return ResponseEntity.badRequest()
                    .body(new ApiMessageResponse(false, "Booking cannot be cancelled in its current state"));
        }

        return ResponseEntity.ok(BookingResponse.from(bookingService.getBookingById(bookingId)));
    }

    @GetMapping("/{bookingId}/status")
    public ResponseEntity<?> getBookingStatus(@PathVariable Long bookingId) {
        User user = getCurrentUser();
        if (user == null) {
            return unauthorized();
        }

        Booking booking = bookingService.getBookingWithExpiryCheck(bookingId);
        if (booking == null) {
            return ResponseEntity.notFound().build();
        }
        if (!canAccessBooking(user, booking)) {
            return forbidden();
        }

        boolean seatLockActive = isSeatLockDependent(booking)
                && booking.getSeatNumber() != null
                && userService.isSeatLocked(booking.getFlightNumber(), booking.getSeatNumber());
        String message = booking.getBookingStatus() == BookingStatus.EXPIRED
                ? "Booking has expired because the seat lock timed out"
                : "Booking status checked";

        return ResponseEntity.ok(new BookingStatusResponse(
                BookingResponse.from(booking),
                true,
                seatLockActive,
                message));
    }

    @GetMapping("/search")
    public ResponseEntity<?> searchByPnr(@RequestParam String pnr) {
        User user = getCurrentUser();
        if (user == null) {
            return unauthorized();
        }

        if (pnr == null || pnr.isBlank()) {
            return ResponseEntity.badRequest().body(new ApiMessageResponse(false, "pnr is required"));
        }

        Booking booking = bookingService.getBookingByPnr(pnr.trim().toUpperCase());
        if (booking == null) {
            return ResponseEntity.notFound().build();
        }
        if (!canAccessBooking(user, booking)) {
            return forbidden();
        }

        Booking checkedBooking = bookingService.getBookingWithExpiryCheck(booking.getId());
        return ResponseEntity.ok(BookingResponse.from(checkedBooking));
    }

    @PostMapping("/book")
    public ResponseEntity<?> createBooking(@RequestBody Map<String, Object> payload) {
        try {
            String username = getCurrentUsername();
            if (username == null) {
                return ResponseEntity.status(401).body("User not authenticated");
            }

            User user = userService.findByUsername(username);
            if (user == null) {
                return ResponseEntity.status(404).body("User not found");
            }

            Long flightId = Long.valueOf(payload.get("flightId").toString());

            // 1. Initiate Booking
            Booking booking = bookingService.initiateBooking(user.getId(), flightId);

            // 2. Auto-select Seat (Simplification for Single-Click Booking)
            List<SeatDTO> seats = userService.getSeatMap(booking.getFlightNumber());
            String textSeat = null;

            // Find first available seat (not booked)
            for (SeatDTO seat : seats) {
                if ("AVAILABLE".equalsIgnoreCase(seat.getStatus())) {
                    textSeat = seat.getSeatNumber();
                    break;
                }
            }

            if (textSeat != null) {
                boolean locked = bookingService.lockSeatAndSetPending(booking.getId(), textSeat);
                if (!locked) {
                    return ResponseEntity.badRequest().body("Failed to lock seat " + textSeat);
                }
            } else {
                return ResponseEntity.badRequest().body("No seats available on this flight");
            }

            return ResponseEntity.ok(booking);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body("Booking failed: " + e.getMessage());
        }
    }

    @PostMapping("/confirm/{bookingId}")
    public ResponseEntity<?> confirmBooking(@PathVariable Long bookingId) {
        try {
            User user = getCurrentUser();
            if (user == null) {
                return unauthorized();
            }

            Booking booking = bookingService.getBookingById(bookingId);
            if (booking == null) {
                return ResponseEntity.notFound().build();
            }
            if (!canAccessBooking(user, booking)) {
                return forbidden();
            }

            String result = bookingService.confirmBooking(bookingId);
            if ("SUCCESS".equals(result)) {
                return ResponseEntity.ok(BookingResponse.from(bookingService.getBookingById(bookingId)));
            } else if ("PAYMENT_PENDING".equals(result)) {
                return ResponseEntity.status(202).body(BookingResponse.from(bookingService.getBookingById(bookingId)));
            } else if ("EXPIRED".equals(result)) {
                return ResponseEntity.status(409).body(BookingResponse.from(bookingService.getBookingById(bookingId)));
            } else {
                return ResponseEntity.badRequest().body(new ApiMessageResponse(false, "Confirmation failed: " + result));
            }
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(new ApiMessageResponse(false, "Confirmation error: " + e.getMessage()));
        }
    }

    @PostMapping("/{bookingId}/retry-payment")
    public ResponseEntity<?> retryPayment(@PathVariable Long bookingId) {
        try {
            User user = getCurrentUser();
            if (user == null) {
                return unauthorized();
            }

            Booking booking = bookingService.getBookingById(bookingId);
            if (booking == null) {
                return ResponseEntity.notFound().build();
            }
            if (!canAccessBooking(user, booking)) {
                return forbidden();
            }

            String result = bookingService.retryPayment(bookingId);
            if ("SUCCESS".equals(result)) {
                return ResponseEntity.ok(BookingResponse.from(bookingService.getBookingById(bookingId)));
            } else if ("PAYMENT_PENDING".equals(result)) {
                return ResponseEntity.status(202).body(BookingResponse.from(bookingService.getBookingById(bookingId)));
            } else if ("EXPIRED".equals(result)) {
                return ResponseEntity.status(409).body(BookingResponse.from(bookingService.getBookingById(bookingId)));
            }

            return ResponseEntity.badRequest().body(new ApiMessageResponse(false, "Payment retry failed: " + result));
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(new ApiMessageResponse(false, "Payment retry error: " + e.getMessage()));
        }
    }

    @GetMapping("/user/{username}")
    public ResponseEntity<?> getUserBookings(@PathVariable String username) {
        User user = userService.findByUsername(username);
        if (user == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(bookingService.getBookingsForUser(user.getId()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getBooking(@PathVariable Long id) {
        User user = getCurrentUser();
        if (user == null) {
            return unauthorized();
        }

        Booking booking = bookingService.getBookingById(id);
        if (booking == null)
            return ResponseEntity.notFound().build();
        if (!canAccessBooking(user, booking)) {
            return forbidden();
        }
        return ResponseEntity.ok(BookingResponse.from(booking));
    }

    private String getCurrentUsername() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getName())) {
            return auth.getName();
        }
        return null;
    }

    private User getCurrentUser() {
        String username = getCurrentUsername();
        if (username == null) {
            return null;
        }
        return userService.findByUsername(username);
    }

    private boolean canAccessBooking(User user, Booking booking) {
        return booking.getUserId().equals(user.getId()) || "ADMIN".equals(user.getRole());
    }

    private boolean isSeatLockDependent(Booking booking) {
        return booking.getBookingStatus() == BookingStatus.PENDING
                || booking.getBookingStatus() == BookingStatus.PAYMENT_PENDING;
    }

    private ResponseEntity<ApiMessageResponse> unauthorized() {
        return ResponseEntity.status(401).body(new ApiMessageResponse(false, "User not authenticated"));
    }

    private ResponseEntity<ApiMessageResponse> forbidden() {
        return ResponseEntity.status(403).body(new ApiMessageResponse(false, "You do not have access to this booking"));
    }
}
