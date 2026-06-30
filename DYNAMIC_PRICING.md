# Dynamic Pricing Implementation

## Overview
Dynamic pricing automatically adjusts flight ticket prices based on seat occupancy. This simulates a real-world demand-based pricing model.

## Pricing Rules
The base price is stored for each flight. The current price is calculated as follows:

| Occupancy Rate (Booked/Total) | Price Multiplier |
|-------------------------------|------------------|
| < 40%                         | 1.0x (Base Price)|
| 40% - 70%                     | 1.2x             |
| 70% - 90%                     | 1.4x             |
| > 90%                         | 1.6x             |

**Formula:**
`Occupancy = (Total Seats - Available Seats) / Total Seats`

## Implementation Details

### Flight Management Service
- **Entity**: `Flight` updated with `basePrice` and `currentPrice`.
- **Logic**: `FlightService.updateDynamicPrice()` calculates price on every booking.
- **Cache**: Search results are cached in Redis (5 min TTL). The cache is **invalidated** immediately upon price update to ensure users see real-time prices.
- **API**: warning
    - `POST /api/flights/book-seat` (Finalize booking -> Updates Price)
    - `GET /api/flights/flight-price/{flightId}` (Exposes price)

### User Management Service
- **DTO**: `FlightDTO` updated to carry price info.
- **Booking**: `Booking` entity stores the `price` at the time of booking (locking the price).
- **UI**: 
    - Search results show current dynamic price.
    - Booking Pending/Status pages show the locked price for that specific booking.

## Demo / Testing
A script `demo_pricing.py` is included to verify the logic.
1. It logs in.
2. Checks initial price for flight `DP-001`.
3. Books 4 seats (1A, 1B, 1C, 1D).
4. Verifies price increase after the 4th booking (reaching 40% occupancy).

## Usage
Run `python3 demo_pricing.py` to see the demo live.
