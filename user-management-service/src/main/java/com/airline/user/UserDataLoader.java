package com.airline.user;

import com.airline.user.entity.User;
import com.airline.user.entity.Booking;
import com.airline.user.entity.BookingStatus;
import com.airline.user.repository.UserRepository;
import com.airline.user.repository.BookingRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * ============================================
 * USER DATA LOADER
 * ============================================
 * 
 * Loads sample data on application startup.
 * This helps with testing and demonstration.
 * 
 * SAMPLE DATA:
 * - Admin user (admin/admin123)
 * - Passenger users (passenger/pass123, john/john123)
 * - Sample bookings in various states
 */
@Component
public class UserDataLoader implements CommandLineRunner {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) throws Exception {
        // Load sample users if database is empty
        if (userRepository.count() == 0) {
            System.out.println("====================================");
            System.out.println("Loading sample users into MySQL...");
            System.out.println("====================================");

            // Create admin user (password is BCrypt encoded)
            User admin = new User("admin", passwordEncoder.encode("admin123"), "ADMIN");
            userRepository.save(admin);
            System.out.println("Created admin user: admin/admin123");

            // Create passenger users (passwords are BCrypt encoded)
            User passenger1 = new User("passenger", passwordEncoder.encode("pass123"), "PASSENGER");
            userRepository.save(passenger1);
            System.out.println("Created passenger user: passenger/pass123");

            User passenger2 = new User("john", passwordEncoder.encode("john123"), "PASSENGER");
            userRepository.save(passenger2);
            System.out.println("Created passenger user: john/john123");

            User passenger3 = new User("jane", passwordEncoder.encode("jane123"), "PASSENGER");
            userRepository.save(passenger3);
            System.out.println("Created passenger user: jane/jane123");

            System.out.println("Sample users loaded successfully!");
        }

        // Load sample bookings if database is empty
        // Note: These are demonstration bookings to show different states
        if (bookingRepository.count() == 0) {
            System.out.println("====================================");
            System.out.println("Loading sample bookings...");
            System.out.println("====================================");

            // Get user IDs (assuming they exist from above)
            User passenger = userRepository.findByUsername("passenger");
            User john = userRepository.findByUsername("john");

            if (passenger != null) {
                // Sample CONFIRMED booking
                Booking confirmedBooking = new Booking(
                        passenger.getId(), 1L, "F101",
                        "New York", "Los Angeles", "2026-01-15");
                confirmedBooking.setSeatNumber("3A");
                confirmedBooking.setBookingStatus(BookingStatus.CONFIRMED);
                bookingRepository.save(confirmedBooking);
                System.out.println("Created CONFIRMED booking #" + confirmedBooking.getId());

                // Sample CANCELLED booking
                Booking cancelledBooking = new Booking(
                        passenger.getId(), 2L, "F102",
                        "Chicago", "Miami", "2026-01-18");
                cancelledBooking.setSeatNumber("5B");
                cancelledBooking.setBookingStatus(BookingStatus.CANCELLED);
                bookingRepository.save(cancelledBooking);
                System.out.println("Created CANCELLED booking #" + cancelledBooking.getId());
            }

            if (john != null) {
                // Sample CONFIRMED booking (Business class)
                Booking businessBooking = new Booking(
                        john.getId(), 1L, "F101",
                        "New York", "Los Angeles", "2026-01-15");
                businessBooking.setSeatNumber("1A");
                businessBooking.setSeatClass("Business");
                businessBooking.setBookingStatus(BookingStatus.CONFIRMED);
                bookingRepository.save(businessBooking);
                System.out.println("Created Business class CONFIRMED booking #" + businessBooking.getId());

                // Sample EXPIRED booking (to demonstrate expiry)
                Booking expiredBooking = new Booking(
                        john.getId(), 3L, "F103",
                        "Seattle", "Denver", "2026-01-20");
                expiredBooking.setSeatNumber("7C");
                expiredBooking.setBookingStatus(BookingStatus.EXPIRED);
                bookingRepository.save(expiredBooking);
                System.out.println("Created EXPIRED booking #" + expiredBooking.getId());
            }

            System.out.println("====================================");
            System.out.println("Sample data loaded successfully!");
            System.out.println("====================================");
            System.out.println("");
            System.out.println("TEST ACCOUNTS:");
            System.out.println("  Admin:     admin / admin123");
            System.out.println("  Passenger: passenger / pass123");
            System.out.println("  Passenger: john / john123");
            System.out.println("  Passenger: jane / jane123");
            System.out.println("");
        }
    }
}
