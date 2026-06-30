package com.airline.user.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import com.airline.user.entity.Booking;
import com.airline.user.entity.BookingStatus;

import org.springframework.data.jpa.repository.Query;

/**
 * ============================================
 * BOOKING REPOSITORY
 * ============================================
 * 
 * Data access layer for Booking entities.
 * Spring Data JPA automatically implements these methods!
 * 
 * HOW IT WORKS:
 * - Spring parses method names and creates SQL queries
 * - findByUserId becomes: SELECT * FROM bookings WHERE user_id = ?
 * - findByBookingStatus becomes: SELECT * FROM bookings WHERE booking_status =
 * ?
 */
public interface BookingRepository extends JpaRepository<Booking, Long> {

    /**
     * Find all bookings for a specific user.
     * Used in "My Bookings" page.
     * 
     * @param userId The user's ID
     * @return List of all bookings by this user
     */
    List<Booking> findByUserId(Long userId);

    /**
     * Find all bookings with a specific status.
     * Used by admin to view all PENDING, CONFIRMED, etc. bookings.
     * 
     * @param bookingStatus The status to filter by
     * @return List of bookings with this status
     */
    List<Booking> findByBookingStatus(BookingStatus bookingStatus);

    /**
     * Find all bookings for a specific flight.
     * Useful for viewing passengers on a flight.
     * 
     * @param flightNumber The flight number
     * @return List of bookings for this flight
     */
    List<Booking> findByFlightNumber(String flightNumber);

    /**
     * Find bookings by user and status.
     * E.g., find all PENDING bookings for a user.
     * 
     * @param userId        The user's ID
     * @param bookingStatus The status to filter by
     * @return List of matching bookings
     */
    List<Booking> findByUserIdAndBookingStatus(Long userId, BookingStatus bookingStatus);

    /**
     * Find all bookings sorted by creation date (newest first).
     * Useful for admin view.
     * 
     * @return All bookings ordered by createdAt descending
     */
    List<Booking> findAllByOrderByCreatedAtDesc();

    /**
     * Find a booking by its unique PNR.
     * 
     * @param pnr The PNR string
     * @return Optional containing the booking if found
     */
    java.util.Optional<Booking> findByPnr(String pnr);

    // Reporting / Statistics

    @Query("SELECT b.dateOfJourney as date, COUNT(b) as count, SUM(b.price) as revenue " +
            "FROM Booking b WHERE b.bookingStatus = com.airline.user.entity.BookingStatus.CONFIRMED " +
            "GROUP BY b.dateOfJourney ORDER BY b.dateOfJourney DESC")
    List<DailyStats> findDailyBookingStats();

    @Query("SELECT b.destination as destination, COUNT(b) as count, SUM(b.price) as revenue " +
            "FROM Booking b WHERE b.bookingStatus = com.airline.user.entity.BookingStatus.CONFIRMED " +
            "GROUP BY b.destination ORDER BY revenue DESC")
    List<DestinationStats> findDestinationStats();

    @Query("SELECT b.flightNumber as flightNumber, COUNT(b) as count " +
            "FROM Booking b WHERE b.bookingStatus = com.airline.user.entity.BookingStatus.CONFIRMED " +
            "GROUP BY b.flightNumber")
    List<FlightStats> findFlightBookingCounts();

    // Projections for Reporting
    interface DailyStats {
        String getDate();

        Long getCount();

        Double getRevenue();
    }

    interface DestinationStats {
        String getDestination();

        Long getCount();

        Double getRevenue();
    }

    interface FlightStats {
        String getFlightNumber();

        Long getCount();
    }
}
