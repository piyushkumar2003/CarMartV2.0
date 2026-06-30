package com.airline.user.entity;

/**
 * ============================================
 * BOOKING STATUS ENUM
 * ============================================
 * 
 * This enum defines all possible states a booking can be in.
 * The booking lifecycle follows a well-defined state machine:
 * 
 * STATE TRANSITIONS:
 * ------------------
 * INITIATED --> PENDING : When seat is successfully locked in Redis
 * PENDING --> CONFIRMED : When user confirms and payment succeeds
 * PENDING --> PAYMENT_PENDING : When payment fails or payment service is down
 * PAYMENT_PENDING --> CONFIRMED : When retry payment succeeds
 * PAYMENT_PENDING --> CANCELLED : When user cancels the booking
 * PAYMENT_PENDING --> EXPIRED : When Redis lock TTL expires
 * PENDING --> CANCELLED : When user cancels the booking
 * PENDING --> EXPIRED : When Redis lock TTL expires (seat lock times out)
 * 
 * IMPORTANT: CONFIRMED is a TERMINAL state - no further transitions allowed!
 * 
 * WHY THESE STATES?
 * -----------------
 * - INITIATED: Booking just started, seat not yet secured
 * - PENDING: Seat locked in Redis, waiting for user to confirm/pay
 * - CONFIRMED: Booking complete, seat permanently assigned
 * - CANCELLED: User decided not to proceed
 * - EXPIRED: User took too long to confirm (Redis TTL expired)
 */
public enum BookingStatus {

    /**
     * Booking has been created but seat is NOT locked yet.
     * This is the initial state when user clicks "Book" on a flight.
     */
    INITIATED,

    /**
     * Seat is locked in Redis (temporary hold).
     * Waiting for user to confirm/pay within the time limit.
     * TTL: 10 minutes (defined in Redis seat lock)
     */
    PENDING,

    /**
     * Seat is still temporarily held, but payment did not complete.
     * User can retry payment while the Redis seat lock is active.
     */
    PAYMENT_PENDING,

    /**
     * Booking is successfully completed!
     * Seat is permanently assigned to this booking.
     * THIS IS A TERMINAL STATE - cannot be changed.
     */
    CONFIRMED,

    /**
     * User cancelled the booking manually.
     * Seat is released back to available pool.
     */
    CANCELLED,

    /**
     * Booking expired because user didn't confirm in time.
     * Redis lock TTL expired, seat automatically released.
     */
    EXPIRED
}
