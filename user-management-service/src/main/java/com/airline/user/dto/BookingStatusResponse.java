package com.airline.user.dto;

public class BookingStatusResponse {
    private BookingResponse booking;
    private boolean expiryChecked;
    private boolean seatLockActive;
    private String message;

    public BookingStatusResponse() {
    }

    public BookingStatusResponse(BookingResponse booking, boolean expiryChecked, boolean seatLockActive, String message) {
        this.booking = booking;
        this.expiryChecked = expiryChecked;
        this.seatLockActive = seatLockActive;
        this.message = message;
    }

    public BookingResponse getBooking() {
        return booking;
    }

    public void setBooking(BookingResponse booking) {
        this.booking = booking;
    }

    public boolean isExpiryChecked() {
        return expiryChecked;
    }

    public void setExpiryChecked(boolean expiryChecked) {
        this.expiryChecked = expiryChecked;
    }

    public boolean isSeatLockActive() {
        return seatLockActive;
    }

    public void setSeatLockActive(boolean seatLockActive) {
        this.seatLockActive = seatLockActive;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
