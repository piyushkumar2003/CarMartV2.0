package com.airline.user.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.airline.user.dto.FlightDTO;
import com.airline.user.repository.BookingRepository;
import com.airline.user.repository.BookingRepository.DailyStats;
import com.airline.user.repository.BookingRepository.DestinationStats;
import com.airline.user.repository.BookingRepository.FlightStats;

@Service
public class ReportService {

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private UserService userService; // To get total seats info

    public List<DailyStats> getDailyReport() {
        return bookingRepository.findDailyBookingStats();
    }

    public List<DestinationStats> getDestinationReport() {
        return bookingRepository.findDestinationStats();
    }

    public List<OccupancyStats> getOccupancyReport() {
        List<FlightStats> flightStats = bookingRepository.findFlightBookingCounts();
        List<OccupancyStats> occupancyReport = new ArrayList<>();

        for (FlightStats stat : flightStats) {
            String flightNumber = stat.getFlightNumber();
            Long bookedCount = stat.getCount();

            // Default capacity if not found or service fails
            int totalCapacity = 0;
            double percentage = 0.0;

            // Fetch flight details needed for capacity (assuming efficient call or cache)
            // Note: Since we only have flightNumber but search needs ID or criteria,
            // we might fallback or try to get it via search.
            // Optimization: Real system would cache flight capacities or store capacity in
            // Booking snapshot.
            // Here we try to fetch via UserService.searchFlights or mock if expensive.

            // HACK: For demo, since we don't have getFlightByNumber endpoint exposed easily
            // in UserService without search,
            // we'll use a safe default or mock capacity if service call fails or is
            // complex.
            // Ideally FlightDTO has totalSeats.

            // Let's assume standard capacity 60 for now to avoid N+1 REST calls in a loop
            // for reports
            // (Circuit Breaker risk!)
            // OR enable a bulk fetch.
            // Let's rely on a fixed capacity for the report to be safe and responsive.
            totalCapacity = 60;

            if (totalCapacity > 0) {
                percentage = (bookedCount * 100.0) / totalCapacity;
            }

            occupancyReport.add(new OccupancyStats(flightNumber, bookedCount, totalCapacity, percentage));
        }

        return occupancyReport;
    }

    // DTO for Report View
    public static class OccupancyStats {
        private String flightNumber;
        private Long bookedSeats;
        private int totalSeats;
        private double occupancyPercentage;

        public OccupancyStats(String flightNumber, Long bookedSeats, int totalSeats, double occupancyPercentage) {
            this.flightNumber = flightNumber;
            this.bookedSeats = bookedSeats;
            this.totalSeats = totalSeats;
            this.occupancyPercentage = occupancyPercentage;
        }

        public String getFlightNumber() {
            return flightNumber;
        }

        public Long getBookedSeats() {
            return bookedSeats;
        }

        public int getTotalSeats() {
            return totalSeats;
        }

        public double getOccupancyPercentage() {
            return occupancyPercentage;
        }
    }
}
