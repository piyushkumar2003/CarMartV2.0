package com.airline.user.dto;

public class BookingSeatLockRequest {
    private String seatNumber;

    public BookingSeatLockRequest() {
    }

    public String getSeatNumber() {
        return seatNumber;
    }

    public void setSeatNumber(String seatNumber) {
        this.seatNumber = seatNumber;
    }
}
