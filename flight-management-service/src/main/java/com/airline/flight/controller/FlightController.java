package com.airline.flight.controller;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import com.airline.flight.entity.Flight;
import com.airline.flight.service.FlightService;

@Controller
public class FlightController {

    @Autowired
    private FlightService flightService;

    // ----- UI Endpoints -----

    @GetMapping("/")
    public String home() {
        return "flight-list"; // redirects to list
    }

    @GetMapping("/all-flights")
    public String getAllFlights(Model model) {
        List<Flight> flights = flightService.getAllFlights();
        model.addAttribute("flights", flights);
        return "flight-list";
    }

    @GetMapping("/add-flight")
    public String showAddFlightForm(Model model) {
        model.addAttribute("flight", new Flight());
        return "add-flight";
    }

    @PostMapping("/add-flight")
    public String addFlight(@ModelAttribute Flight flight) {
        flight.setAvailableSeats(flight.getTotalSeats());
        flightService.saveFlight(flight);
        return "redirect:/all-flights";
    }

    @GetMapping("/search-flights")
    public String searchFlights(@RequestParam(required = false) String source,
            @RequestParam(required = false) String destination,
            @RequestParam(required = false) String date,
            Model model) {
        if (source != null && destination != null && date != null) {
            // Uses Redis Cache if available
            List<Flight> flights = flightService.searchFlights(source, destination, date);
            model.addAttribute("flights", flights);
        }
        return "search-flights";
    }

    @GetMapping("/report")
    public String showReport(Model model) {
        List<Flight> flights = flightService.getAllFlights();
        model.addAttribute("flights", flights);
        return "report";
    }

    @GetMapping("/seat-map/{flightNumber}")
    @ResponseBody
    public List<com.airline.flight.dto.SeatDTO> getSeatMap(@PathVariable String flightNumber) {
        return flightService.getSeatMap(flightNumber);
    }

    // ----- REST API Endpoints for Inter-Service -----

    @ResponseBody
    @GetMapping("/api/flights/{id}")
    public Flight getFlightById(@PathVariable Long id) {
        return flightService.getFlightById(id);
    }

    // Existing "final booking" logic (decrements count)
    @ResponseBody
    @PostMapping("/api/flights/{id}/book")
    public boolean bookSeat(@PathVariable Long id) {
        Flight flight = flightService.getFlightById(id);
        if (flight != null && flight.getAvailableSeats() > 0) {
            flight.setAvailableSeats(flight.getAvailableSeats() - 1);
            flightService.saveFlight(flight); // Persistence
            return true;
        }
        return false;
    }

    /*
     * NEW: Redis Seat Locking Endpoint
     * Usage: POST /api/flights/lock-seat?flightNumber=F101&seatNumber=1A
     */
    @ResponseBody
    @PostMapping("/api/flights/lock-seat")
    public boolean lockSeat(@RequestParam String flightNumber, @RequestParam String seatNumber) {
        return flightService.requestSeatLock(flightNumber, seatNumber);
    }

    @ResponseBody
    @PostMapping("/api/flights/release-seat")
    public boolean releaseSeat(@RequestParam String flightNumber, @RequestParam String seatNumber) {
        flightService.releaseSeatLock(flightNumber, seatNumber);
        return true;
    }

    @ResponseBody
    @PostMapping("/api/flights/book-seat")
    public boolean bookSeatPermanent(@RequestParam String flightNumber, @RequestParam String seatNumber) {
        flightService.finalizeSeatBooking(flightNumber, seatNumber);
        return true;
    }

    /**
     * NEW: Check if a seat is currently locked in Redis.
     * Used by User Service to detect expired bookings.
     * 
     * @param flightNumber The flight number
     * @param seatNumber   The seat to check
     * @return true if locked, false otherwise
     */
    @ResponseBody
    @GetMapping("/api/flights/is-seat-locked")
    public boolean isSeatLocked(@RequestParam String flightNumber, @RequestParam String seatNumber) {
        return flightService.isSeatLocked(flightNumber, seatNumber);
    }

    @ResponseBody
    @GetMapping("/api/flights/flight-price/{flightNumber}")
    public com.airline.flight.dto.PriceDTO getFlightPrice(@PathVariable String flightNumber) {
        return flightService.getFlightPrice(flightNumber);
    }

    @ResponseBody
    @GetMapping("/api/flights")
    public List<Flight> getAllFlightsApi() {
        return flightService.getAllFlights();
    }

    @ResponseBody
    @GetMapping("/api/flights/search")
    public List<Flight> searchFlightsApi(@RequestParam String source, @RequestParam String destination,
            @RequestParam String date) {
        // Uses Redis Cache
        return flightService.searchFlights(source, destination, date);
    }
}
