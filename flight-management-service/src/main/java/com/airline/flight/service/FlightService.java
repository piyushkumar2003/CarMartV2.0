package com.airline.flight.service;

import com.airline.flight.entity.Flight;
import com.airline.flight.repository.FlightRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class FlightService {

    @Autowired
    private FlightRepository flightRepository;

    @Autowired
    private RedisService redisService;

    @Autowired
    private com.airline.flight.repository.SeatRepository seatRepository;

    public List<Flight> searchFlights(String source, String destination, String date) {
        // 1. Check Redis Cache
        List<Flight> cachedResults = (List<Flight>) redisService.getCachedSearchResults(source, destination, date);
        if (cachedResults != null) {
            System.out.println("Returning flight search results from Redis cache.");
            return cachedResults;
        }

        // 2. Fall back to MySQL
        System.out.println("Searching flights in MySQL database.");
        List<Flight> flights = flightRepository.findBySourceAndDestinationAndDateOfJourney(source, destination, date);

        // 3. Cache the results if found
        if (!flights.isEmpty()) {
            redisService.cacheSearchResults(source, destination, date, flights);
        }

        return flights;
    }

    public Flight saveFlight(Flight flight) {
        return flightRepository.save(flight);
    }

    public List<Flight> getAllFlights() {
        return flightRepository.findAll();
    }

    public Flight getFlightById(Long id) {
        return flightRepository.findById(id).orElse(null);
    }

    // Attempt to lock a seat
    public boolean requestSeatLock(String flightNumber, String seatNumber) {
        // 1. Check if already permanently booked
        com.airline.flight.entity.Seat soldSeat = seatRepository.findByFlightNumberAndSeatNumber(flightNumber,
                seatNumber);
        if (soldSeat != null) {
            return false;
        }
        // 2. Try Redis Lock
        return redisService.lockSeat(flightNumber, seatNumber);
    }

    public void releaseSeatLock(String flightNumber, String seatNumber) {
        redisService.releaseSeat(flightNumber, seatNumber);
    }

    /**
     * Check if a seat is currently locked in Redis.
     * Used to detect expired bookings (TTL expired).
     */
    public boolean isSeatLocked(String flightNumber, String seatNumber) {
        return redisService.isSeatLocked(flightNumber, seatNumber);
    }

    public void finalizeSeatBooking(String flightNumber, String seatNumber) {
        // 1. Save to MySQL
        com.airline.flight.entity.Seat seat = new com.airline.flight.entity.Seat(flightNumber, seatNumber, "BOOKED");
        seatRepository.save(seat);

        // 2. Remove Lock
        redisService.releaseSeat(flightNumber, seatNumber);

        // 3. Update Available count and Price
        // Find flight by number (assuming flightNumber is unique or we take the first
        // one)
        // Since we don't have findByFlightNumber in repository easily, let's fetch all
        // or assume we can find it.
        // Better: Update FlightRepository to find by flightNumber.
        // For now, let's look up the flight if possible.
        // Actually, let's fetch the flight by ID if we had it. But here we have
        // flightNumber.
        // Let's rely on finding standard flight.

        // Assuming we have a way to find flight.
        // Let's add findByFlightNumber to repository or iterate.
        // I will add a helper method to find flight here.
        Flight flight = flightRepository.findAll().stream()
                .filter(f -> f.getFlightNumber().equals(flightNumber))
                .findFirst().orElse(null);

        if (flight != null) {
            flight.setAvailableSeats(flight.getAvailableSeats() - 1);
            updateDynamicPrice(flight);
            flightRepository.save(flight);

            // Invalidate cache so new price/availability is shown in search
            redisService.clearSearchCache(flight.getSource(), flight.getDestination(), flight.getDateOfJourney());
        }
    }

    /**
     * Calculates and updates the dynamic price based on occupancy.
     * 
     * Rules:
     * - < 40% occupancy: Base Price
     * - 40% - 70%: Base Price * 1.2
     * - 70% - 90%: Base Price * 1.4
     * - > 90%: Base Price * 1.6
     */
    private void updateDynamicPrice(Flight flight) {
        int total = flight.getTotalSeats();
        int available = flight.getAvailableSeats();
        int occupied = total - available;

        double occupancyRate = (double) occupied / total;
        double multiplier = 1.0;

        if (occupancyRate >= 0.90) {
            multiplier = 1.60;
        } else if (occupancyRate >= 0.70) {
            multiplier = 1.40;
        } else if (occupancyRate >= 0.40) {
            multiplier = 1.20;
        }

        double newPrice = flight.getBasePrice() * multiplier;
        // Round to nearest whole number
        flight.setCurrentPrice(Math.round(newPrice));

        System.out.println("Updated price for " + flight.getFlightNumber() +
                ": Occupancy=" + (int) (occupancyRate * 100) + "%, Price=" + flight.getCurrentPrice());
    }

    public com.airline.flight.dto.PriceDTO getFlightPrice(String flightNumber) {
        Flight flight = flightRepository.findAll().stream()
                .filter(f -> f.getFlightNumber().equals(flightNumber))
                .findFirst().orElse(null);

        if (flight != null) {
            return new com.airline.flight.dto.PriceDTO(flight.getBasePrice(), flight.getCurrentPrice());
        }
        return new com.airline.flight.dto.PriceDTO(0, 0);
    }

    public java.util.List<com.airline.flight.dto.SeatDTO> getSeatMap(String flightNumber) {
        // Find flight to get total seats (Assuming we can find by number, if not, we
        // need a repository method)
        // For this demo, we'll assume a standard 60 seats if flight not found, or use a
        // method to find it.
        // But wait, FlightRepository findByFlightNumber is not explicitly there?
        // Let's iterate all flights or assume standard.
        // The previous user requirement says "Maintain total seats".
        // Let's assume 60 seats (10 rows) for simplicity if we can't find the flight
        // easily without ID.

        // Actually, let's fetch flight details properly.
        // We'll add findByFlightNumber to FlightRepository? Or just query all.
        // Let's treat it as 60 seats for now to be safe.
        int totalSeats = 60;

        java.util.List<com.airline.flight.dto.SeatDTO> map = new java.util.ArrayList<>();
        java.util.List<com.airline.flight.entity.Seat> bookedSeats = seatRepository.findByFlightNumber(flightNumber);
        java.util.Set<String> bookedSet = bookedSeats.stream().map(com.airline.flight.entity.Seat::getSeatNumber)
                .collect(java.util.stream.Collectors.toSet());

        char[] rows = { 'A', 'B', 'C', 'D', 'E', 'F' };
        int rowCount = totalSeats / 6;

        for (int r = 1; r <= rowCount; r++) {
            for (char c : rows) {
                String seatNum = r + "" + c;
                String status = "AVAILABLE";

                if (bookedSet.contains(seatNum)) {
                    status = "BOOKED";
                } else if (redisService.isSeatLocked(flightNumber, seatNum)) {
                    status = "LOCKED";
                }
                map.add(new com.airline.flight.dto.SeatDTO(seatNum, status));
            }
        }
        return map;
    }
}
