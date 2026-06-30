package com.airline.user.service;

import com.airline.user.dto.FlightDTO;
import com.airline.user.entity.Booking;
import com.airline.user.entity.User;
import com.airline.user.repository.BookingRepository;
import com.airline.user.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;
import java.util.List;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;

    private static final String FLIGHT_SERVICE_URL = "http://flight-management-service";

    // Authentication & User Mgmt
    public User registerUser(User user) {
        if (userRepository.findByUsername(user.getUsername()) != null) {
            throw new RuntimeException("Username already exists!");
        }
        // Hash the password before saving
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        return userRepository.save(user);
    }

    public User findByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    // Bookings
    public List<Booking> getUserBookings(Long userId) {
        return bookingRepository.findByUserId(userId);
    }

    public Booking getBookingById(Long bookingId) {
        return bookingRepository.findById(bookingId).orElse(null);
    }

    public Booking saveBooking(Booking booking) {
        return bookingRepository.save(booking);
    }

    // Flight Integration
    @io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker(name = "flight-service", fallbackMethod = "searchFlightsFallback")
    public List<FlightDTO> searchFlights(String source, String destination, String date) {
        String url;
        if (source != null && !source.isEmpty() && destination != null && !destination.isEmpty() && date != null) {
            url = FLIGHT_SERVICE_URL + "/api/flights/search?source=" + source + "&destination=" + destination + "&date="
                    + date;
        } else {
            url = FLIGHT_SERVICE_URL + "/api/flights";
        }

        try {
            ResponseEntity<List<FlightDTO>> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<List<FlightDTO>>() {
                    });
            return response.getBody();
        } catch (Exception e) {
            throw e; // Let Circuit Breaker handle it or fallback
        }
    }

    public List<FlightDTO> searchFlightsFallback(String source, String destination, String date, Throwable t) {
        System.out.println("FALLBACK: Flight Service unavailable for search. Reason: " + t.getMessage());
        return Collections.emptyList();
    }

    @io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker(name = "pricing-service", fallbackMethod = "pricingFallback")
    public FlightDTO getFlightById(Long flightId) {
        return restTemplate.getForObject(FLIGHT_SERVICE_URL + "/api/flights/" + flightId, FlightDTO.class);
    }

    public FlightDTO pricingFallback(Long flightId, Throwable t) {
        System.out.println(
                "FALLBACK: Pricing/Flight Service unavailable for ID " + flightId + ". Reason: " + t.getMessage());
        return null; // Handle null gracefully in caller
    }

    public boolean bookFlightSeat(Long flightId) {
        // Deprecated simple booking
        try {
            Boolean success = restTemplate.postForObject(FLIGHT_SERVICE_URL + "/api/flights/" + flightId + "/book",
                    null, Boolean.class);
            return Boolean.TRUE.equals(success);
        } catch (Exception e) {
            return false;
        }
    }

    // New Seat Logic
    @io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker(name = "seat-service", fallbackMethod = "getSeatMapFallback")
    public List<com.airline.user.dto.SeatDTO> getSeatMap(String flightNumber) {
        ResponseEntity<List<com.airline.user.dto.SeatDTO>> response = restTemplate.exchange(
                FLIGHT_SERVICE_URL + "/seat-map/" + flightNumber,
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<List<com.airline.user.dto.SeatDTO>>() {
                });
        return response.getBody();
    }

    public List<com.airline.user.dto.SeatDTO> getSeatMapFallback(String flightNumber, Throwable t) {
        System.out.println("FALLBACK: Seat Service unavailable for map. Reason: " + t.getMessage());
        return Collections.emptyList();
    }

    @io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker(name = "seat-service", fallbackMethod = "lockSeatFallback")
    public boolean lockSeat(String flightNumber, String seatNumber) {
        // POST /api/flights/lock-seat?flightNumber=...
        Boolean success = restTemplate.postForObject(
                FLIGHT_SERVICE_URL + "/api/flights/lock-seat?flightNumber=" + flightNumber + "&seatNumber="
                        + seatNumber,
                null, Boolean.class);
        return Boolean.TRUE.equals(success);
    }

    public boolean lockSeatFallback(String flightNumber, String seatNumber, Throwable t) {
        System.out.println("FALLBACK: Seat Service unavailable for locking. Reason: " + t.getMessage());
        return false;
    }

    public void finalizeSeat(String flightNumber, String seatNumber) {
        try {
            restTemplate.postForObject(
                    FLIGHT_SERVICE_URL + "/api/flights/book-seat?flightNumber=" + flightNumber + "&seatNumber="
                            + seatNumber,
                    null, Boolean.class);
        } catch (Exception e) {
            // ignore
        }
    }

    public void releaseSeat(String flightNumber, String seatNumber) {
        try {
            restTemplate.postForObject(
                    FLIGHT_SERVICE_URL + "/api/flights/release-seat?flightNumber=" + flightNumber + "&seatNumber="
                            + seatNumber,
                    null, Boolean.class);
        } catch (Exception e) {
            // ignore
        }
    }

    /**
     * Check if a seat is currently locked in Redis.
     * Used by BookingService to detect EXPIRED bookings.
     * 
     * HOW IT WORKS:
     * - Calls Flight Service API to check Redis lock status
     * - Returns true if lock exists (seat is being held)
     * - Returns false if lock expired or doesn't exist
     * 
     * @param flightNumber The flight number
     * @param seatNumber   The seat to check
     * @return true if seat is locked, false otherwise
     */
    public boolean isSeatLocked(String flightNumber, String seatNumber) {
        try {
            Boolean locked = restTemplate.getForObject(
                    FLIGHT_SERVICE_URL + "/api/flights/is-seat-locked?flightNumber=" + flightNumber
                            + "&seatNumber=" + seatNumber,
                    Boolean.class);
            return Boolean.TRUE.equals(locked);
        } catch (Exception e) {
            // If we can't check, assume not locked (safer default)
            System.out.println("Warning: Could not check seat lock status: " + e.getMessage());
            return false;
        }
    }

    // ===========================================
    // PAYMENT PROCESSING (Simulated)
    // ===========================================

    private static final String PAYMENT_SERVICE_URL = "http://payment-service";

    /**
     * Calls the separated Payment Microservice.
     * Protected by Circuit Breaker "payment-service".
     */
    @io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker(name = "payment-service", fallbackMethod = "paymentFallback")
    public boolean processPayment(Long bookingId, Double amount) {
        System.out.println("Initiating payment for booking " + bookingId + " via Payment Service...");

        try {
            // Call POST /api/payments/process?bookingId=...
            String url = PAYMENT_SERVICE_URL + "/api/payments/process?bookingId=" + bookingId
                    + "&amount=" + amount + "&mode=CARD"; // Default to CARD for now

            Boolean success = restTemplate.postForObject(url, null, Boolean.class);
            return Boolean.TRUE.equals(success);
        } catch (Exception e) {
            System.out.println("Payment Service Call Failed: " + e.getMessage());
            throw e; // Let Circuit Breaker handle it
        }
    }

    public boolean paymentFallback(Long bookingId, Double amount, Throwable t) {
        System.out.println("FALLBACK: Payment Service unavailable. Marking as PENDING. Reason: " + t.getMessage());
        return false;
    }

    /**
     * Get all users (for admin features).
     */
    public java.util.List<User> getAllUsers() {
        return userRepository.findAll();
    }
}
