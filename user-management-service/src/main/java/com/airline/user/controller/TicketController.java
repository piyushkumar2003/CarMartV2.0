package com.airline.user.controller;

import com.airline.user.entity.Booking;
import com.airline.user.entity.BookingStatus;
import com.airline.user.entity.Passenger;
import com.airline.user.entity.User;
import com.airline.user.service.BookingService;
import com.airline.user.service.PassengerService;
import com.airline.user.service.TicketService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

/**
 * Controller for E-Ticket PDF download.
 */
@Controller
public class TicketController {

    @Autowired
    private TicketService ticketService;

    @Autowired
    private BookingService bookingService;

    @Autowired
    private PassengerService passengerService;

    /**
     * GET /download-ticket/{bookingId}
     * 
     * Download E-Ticket PDF for a confirmed booking.
     * Only allows download if:
     * - User is logged in
     * - Booking belongs to the user
     * - Booking status is CONFIRMED
     */
    @GetMapping("/download-ticket/{bookingId}")
    public ResponseEntity<byte[]> downloadTicket(
            @PathVariable Long bookingId,
            HttpSession session) {

        // Check authentication
        User user = (User) session.getAttribute("user");
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        // Get booking
        Booking booking = bookingService.getBookingById(bookingId);

        if (booking == null) {
            return ResponseEntity.notFound().build();
        }

        // Verify ownership
        if (!booking.getUserId().equals(user.getId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        // Check if CONFIRMED
        if (booking.getBookingStatus() != BookingStatus.CONFIRMED) {
            return ResponseEntity.badRequest()
                    .body("Ticket can only be downloaded for confirmed bookings.".getBytes());
        }

        try {
            // Get passenger name
            Passenger passenger = passengerService.getPassengerByUserId(user.getId());
            String passengerName = passenger != null && passenger.getFullName() != null
                    ? passenger.getFullName()
                    : user.getUsername();

            // Generate PDF
            byte[] pdfBytes = ticketService.generateTicketPdf(booking, passengerName);

            // Create filename
            String filename = "E-Ticket_PNR" + String.format("%06d", bookingId) + ".pdf";

            // Return PDF response
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDispositionFormData("attachment", filename);
            headers.setContentLength(pdfBytes.length);

            return new ResponseEntity<>(pdfBytes, headers, HttpStatus.OK);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError()
                    .body(("Failed to generate ticket: " + e.getMessage()).getBytes());
        }
    }
}
