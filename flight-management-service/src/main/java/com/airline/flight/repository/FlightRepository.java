package com.airline.flight.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import com.airline.flight.entity.Flight;

public interface FlightRepository extends JpaRepository<Flight, Long> {
    List<Flight> findBySourceAndDestinationAndDateOfJourney(String source, String destination, String dateOfJourney);
}
