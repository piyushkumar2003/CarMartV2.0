package com.airline.user.dto;

import java.util.List;

public class SeatMapResponse {
    private Long bookingId;
    private String flightNumber;
    private List<SeatDTO> seats;

    public SeatMapResponse() {
    }

    public SeatMapResponse(Long bookingId, String flightNumber, List<SeatDTO> seats) {
        this.bookingId = bookingId;
        this.flightNumber = flightNumber;
        this.seats = seats;
    }

    public Long getBookingId() {
        return bookingId;
    }

    public void setBookingId(Long bookingId) {
        this.bookingId = bookingId;
    }

    public String getFlightNumber() {
        return flightNumber;
    }

    public void setFlightNumber(String flightNumber) {
        this.flightNumber = flightNumber;
    }

    public List<SeatDTO> getSeats() {
        return seats;
    }

    public void setSeats(List<SeatDTO> seats) {
        this.seats = seats;
    }
}
