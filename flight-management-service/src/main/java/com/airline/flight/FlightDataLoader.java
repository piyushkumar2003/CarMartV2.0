package com.airline.flight;

import com.airline.flight.entity.Flight;
import com.airline.flight.repository.FlightRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class FlightDataLoader implements CommandLineRunner {

    @Autowired
    private FlightRepository flightRepository;

    @Override
    public void run(String... args) throws Exception {
        System.out.println("Checking flight data...");

        safeLoadFlight("AI-101", "Air India", "Delhi", "Mumbai", "2026-02-14", 150, 4500.0);
        safeLoadFlight("AI-202", "Air India", "Mumbai", "Delhi", "2026-02-14", 150, 4500.0);
        safeLoadFlight("IG-303", "IndiGo", "Delhi", "Bangalore", "2026-02-15", 180, 6000.0);
        safeLoadFlight("UK-404", "Vistara", "Bangalore", "Delhi", "2026-02-15", 160, 6500.0);
        safeLoadFlight("AI-999", "Air India", "Chennai", "Kolkata", "2026-02-16", 140, 5200.0);
        safeLoadFlight("6E-555", "IndiGo", "Goa", "Pune", "2026-02-16", 120, 3500.0);
        safeLoadFlight("DP-001", "Demo Air", "Pune", "Goa", "2026-02-14", 10, 2000.0);
        safeLoadFlight("6E-777", "IndiGo", "Delhi", "Mumbai", "2026-02-14", 180, 3800.0);
        safeLoadFlight("UK-888", "Vistara", "Delhi", "Mumbai", "2026-02-14", 160, 5500.0);

        System.out.println("Flight data check complete.");
    }

    private void safeLoadFlight(String flightNumber, String airlineName, String source, String destination, String date,
            int totalSeats, double basePrice) {
        if (flightRepository.findAll().stream().noneMatch(f -> f.getFlightNumber().equals(flightNumber))) {
            Flight flight = new Flight(flightNumber, airlineName, source, destination, date, totalSeats, basePrice);
            flightRepository.save(flight);
            System.out.println("Loaded flight: " + flightNumber);
        }
    }
}
