package com.airline.flight.dto;

public class PriceDTO {
    private double basePrice;
    private double currentPrice;

    public PriceDTO() {
    }

    public PriceDTO(double basePrice, double currentPrice) {
        this.basePrice = basePrice;
        this.currentPrice = currentPrice;
    }

    public double getBasePrice() {
        return basePrice;
    }

    public void setBasePrice(double basePrice) {
        this.basePrice = basePrice;
    }

    public double getCurrentPrice() {
        return currentPrice;
    }

    public void setCurrentPrice(double currentPrice) {
        this.currentPrice = currentPrice;
    }
}
