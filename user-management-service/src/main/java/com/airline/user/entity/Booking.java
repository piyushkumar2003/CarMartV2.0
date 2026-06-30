package com.airline.user.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * ============================================
 * BOOKING ENTITY - Represents a flight booking
 * ============================================
 * 
 * This entity stores all booking information in MySQL.
 * Each booking goes through a LIFECYCLE with defined states.
 * 
 * DATABASE TABLE: bookings
 * 
 * KEY FIELDS:
 * - bookingStatus: Current state (INITIATED, PENDING, CONFIRMED, CANCELLED,
 * EXPIRED)
 * - seatNumber: The selected seat (may be null in INITIATED state)
 * - createdAt: When booking was first created
 * - updatedAt: When booking was last modified
 * 
 * LIFECYCLE:
 * - Created in INITIATED state
 * - Moves to PENDING when seat is locked in Redis
 * - Moves to CONFIRMED when user confirms
 * - Moves to CANCELLED if user cancels
 * - Moves to EXPIRED if Redis lock expires
 */
@Entity
@Table(name = "bookings")
public class Booking {

    // ===========================================
    // PRIMARY KEY
    // ===========================================
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ===========================================
    // USER REFERENCE
    // ===========================================
    /** ID of the user who made this booking */
    private Long userId;

    // ===========================================
    // FLIGHT DETAILS (Snapshot at booking time)
    // ===========================================
    /** Reference to the flight in Flight Service */
    private Long flightId;

    /** Flight number (e.g., "F101") - stored for quick reference */
    private String flightNumber;

    /** Departure city */
    private String source;

    /** Arrival city */
    private String destination;

    /** Date of journey (format: YYYY-MM-DD) */
    private String dateOfJourney;

    // ===========================================
    // SEAT INFORMATION
    // ===========================================
    /** Selected seat number (e.g., "3A") */
    private String seatNumber;

    @Column(name = "seat_class")
    private String seatClass; // Economy, Business

    @Column(name = "price")
    private Double price;

    // ===========================================
    // PNR INFORMATION
    // ===========================================
    @Column(unique = true)
    private String pnr;

    // ===========================================
    // BOOKING STATUS - THE KEY FIELD!
    // ===========================================
    /**
     * Current booking status.
     * Uses @Enumerated(EnumType.STRING) to store as readable string in DB.
     * This makes debugging easier - you can read "PENDING" instead of "1"
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "booking_status", nullable = false)
    private BookingStatus bookingStatus;

    // ===========================================
    // TIMESTAMPS
    // ===========================================
    /** When the booking was first created */
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    /** When the booking was last updated */
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // ===========================================
    // CONSTRUCTORS
    // ===========================================

    /** Default constructor required by JPA */
    public Booking() {
    }

    /**
     * Full constructor for creating a new booking.
     * Note: Booking starts in INITIATED state by default.
     * 
     * @param userId        The user making the booking
     * @param flightId      The flight ID from Flight Service
     * @param flightNumber  The flight number (e.g., "F101")
     * @param source        Departure city
     * @param destination   Arrival city
     * @param dateOfJourney Journey date
     */
    public Booking(Long userId, Long flightId, String flightNumber,
            String source, String destination, String dateOfJourney) {
        this.userId = userId;
        this.flightId = flightId;
        this.flightNumber = flightNumber;
        this.source = source;
        this.destination = destination;
        this.dateOfJourney = dateOfJourney;

        // NEW: Booking always starts in INITIATED state
        this.bookingStatus = BookingStatus.INITIATED;
        this.seatClass = "Economy";

        // Set timestamps
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    // ===========================================
    // JPA LIFECYCLE CALLBACKS
    // ===========================================

    /**
     * Automatically set createdAt before first save.
     * This ensures we always have a creation timestamp.
     */
    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Automatically update updatedAt on every save.
     * This helps track when booking status changed.
     */
    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    // ===========================================
    // GETTERS AND SETTERS
    // ===========================================

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Long getFlightId() {
        return flightId;
    }

    public void setFlightId(Long flightId) {
        this.flightId = flightId;
    }

    /**
     * Get booking status as String for backward compatibility.
     * Some existing code may use getStatus() expecting a String.
     */
    public String getStatus() {
        return bookingStatus != null ? bookingStatus.name() : null;
    }

    /**
     * Set status from String for backward compatibility.
     * Converts string to BookingStatus enum.
     */
    public void setStatus(String status) {
        if (status != null) {
            this.bookingStatus = BookingStatus.valueOf(status);
        }
    }

    /**
     * Get the actual BookingStatus enum.
     * Prefer this method for new code.
     */
    public BookingStatus getBookingStatus() {
        return bookingStatus;
    }

    /**
     * Set the BookingStatus enum directly.
     * Prefer this method for new code.
     */
    public void setBookingStatus(BookingStatus bookingStatus) {
        this.bookingStatus = bookingStatus;
    }

    public String getFlightNumber() {
        return flightNumber;
    }

    public void setFlightNumber(String flightNumber) {
        this.flightNumber = flightNumber;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getDestination() {
        return destination;
    }

    public void setDestination(String destination) {
        this.destination = destination;
    }

    public String getDateOfJourney() {
        return dateOfJourney;
    }

    public void setDateOfJourney(String dateOfJourney) {
        this.dateOfJourney = dateOfJourney;
    }

    public String getSeatClass() {
        return seatClass;
    }

    public void setSeatClass(String seatClass) {
        this.seatClass = seatClass;
    }

    public String getSeatNumber() {
        return seatNumber;
    }

    public void setSeatNumber(String seatNumber) {
        this.seatNumber = seatNumber;
    }

    public Double getPrice() {
        return price;
    }

    public void setPrice(Double price) {
        this.price = price;
    }

    public String getPnr() {
        return pnr;
    }

    public void setPnr(String pnr) {
        this.pnr = pnr;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    // ===========================================
    // UTILITY METHODS
    // ===========================================

    /**
     * Check if booking can be cancelled.
     * Only INITIATED and PENDING bookings can be cancelled.
     */
    public boolean canBeCancelled() {
        return bookingStatus == BookingStatus.INITIATED
                || bookingStatus == BookingStatus.PENDING
                || bookingStatus == BookingStatus.PAYMENT_PENDING;
    }

    /**
     * Check if booking can be confirmed.
     * PENDING and PAYMENT_PENDING bookings can be confirmed if the seat lock is active.
     */
    public boolean canBeConfirmed() {
        return bookingStatus == BookingStatus.PENDING
                || bookingStatus == BookingStatus.PAYMENT_PENDING;
    }

    /**
     * Check if this is a terminal state (no more transitions allowed).
     */
    public boolean isTerminalState() {
        return bookingStatus == BookingStatus.CONFIRMED
                || bookingStatus == BookingStatus.CANCELLED
                || bookingStatus == BookingStatus.EXPIRED;
    }

    @Override
    public String toString() {
        return "Booking{" +
                "id=" + id +
                ", flightNumber='" + flightNumber + '\'' +
                ", seatNumber='" + seatNumber + '\'' +
                ", status=" + bookingStatus +
                '}';
    }
}
