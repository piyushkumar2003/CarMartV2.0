package com.airline.user.dto;

public class BookingInitiateRequest {
    private Long flightId;

    public BookingInitiateRequest() {
    }

    public Long getFlightId() {
        return flightId;
    }

    public void setFlightId(Long flightId) {
        this.flightId = flightId;
    }
}
