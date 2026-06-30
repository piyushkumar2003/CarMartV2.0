# Booking Lifecycle & Status Management

## Overview

This document explains the **Booking Lifecycle** feature implemented in the Airline Ticket Booking System.

The feature introduces a realistic booking flow where each booking goes through well-defined states instead of being instantly marked as "booked".

---

## Booking States

Each booking has exactly one of the following states:

| Status | Description |
|--------|-------------|
| **INITIATED** | Booking created, seat NOT locked yet |
| **PENDING** | Seat locked in Redis, waiting for confirmation |
| **CONFIRMED** | Booking complete, seat permanently assigned |
| **CANCELLED** | User cancelled the booking |
| **EXPIRED** | Redis lock TTL expired, seat released |

---

## State Transition Diagram

```
┌─────────────┐
│  INITIATED  │ ← User starts booking process
└──────┬──────┘
       │ (seat locked in Redis)
       ↓
┌─────────────┐
│   PENDING   │ ← Seat held for 10 minutes
└──────┬──────┘
       │
       ├──────────────────┐──────────────────┐
       │ (user confirms)  │ (user cancels)   │ (TTL expires)
       ↓                  ↓                  ↓
┌─────────────┐    ┌─────────────┐    ┌─────────────┐
│  CONFIRMED  │    │  CANCELLED  │    │   EXPIRED   │
│ (TERMINAL)  │    │             │    │             │
└─────────────┘    └─────────────┘    └─────────────┘

Note: CONFIRMED is a TERMINAL state - no further transitions allowed!
```

---

## Business Rules

1. **A booking always starts in INITIATED state**
   - Created when user clicks "Book Now" on a flight

2. **INITIATED → PENDING requires seat lock**
   - Seat must be successfully locked in Redis
   - Lock has 10-minute TTL

3. **PENDING → CONFIRMED requires user confirmation**
   - User clicks "Confirm & Pay"
   - System verifies Redis lock still exists
   - Seat is permanently saved to MySQL

4. **PENDING → CANCELLED when user cancels**
   - Redis lock is released
   - Booking marked as cancelled

5. **PENDING → EXPIRED automatically**
   - Redis TTL expires (10 minutes)
   - Detected on next status check
   - Seat becomes available again

6. **CONFIRMED cannot be changed**
   - This is a terminal state
   - Requires support/refund process to undo

---

## API Endpoints

### User Management Service (Port 8081)

| Method | Endpoint | Description | Status Change |
|--------|----------|-------------|---------------|
| POST | `/initiate-booking` | Create booking | → INITIATED |
| POST | `/lock-seat-and-pending` | Lock seat in Redis | INITIATED → PENDING |
| POST | `/confirm-booking` | Confirm booking | PENDING → CONFIRMED |
| POST | `/cancel-booking` | Cancel booking | PENDING → CANCELLED |
| GET | `/booking-status/{id}` | View booking details | - |
| GET | `/my-bookings` | List user's bookings | - |
| GET | `/admin-bookings` | Admin view all bookings | - |

### Flight Management Service (Port 8082)

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/flights/lock-seat` | Lock seat in Redis |
| POST | `/api/flights/release-seat` | Release Redis lock |
| POST | `/api/flights/book-seat` | Permanent booking (MySQL) |
| GET | `/api/flights/is-seat-locked` | Check if seat is locked |

---

## User Flow

### Booking a Flight

1. **Search Flights** (`/book-flight`)
   - User searches for available flights
   - Clicks "Book Now" on desired flight

2. **Initiate Booking** (`/initiate-booking`)
   - Booking created in INITIATED state
   - Redirected to seat selection

3. **Select Seat** (`/select-seat`)
   - Visual seat map shown
   - Green = Available, Yellow = Locked, Red = Booked

4. **Lock Seat** (`/lock-seat-and-pending`)
   - Selected seat locked in Redis (10 min TTL)
   - Booking status → PENDING

5. **Confirm Booking** (`/booking-pending`)
   - User sees countdown warning
   - Clicks "Confirm & Pay"

6. **Booking Complete** (`/booking-status/{id}`)
   - Status → CONFIRMED
   - Seat permanently assigned

---

## Database Schema

### bookings table

| Column | Type | Description |
|--------|------|-------------|
| id | BIGINT (PK) | Auto-generated booking ID |
| user_id | BIGINT | Reference to user |
| flight_id | BIGINT | Reference to flight |
| flight_number | VARCHAR | Flight number (e.g., "F101") |
| source | VARCHAR | Departure city |
| destination | VARCHAR | Arrival city |
| date_of_journey | VARCHAR | Journey date |
| seat_number | VARCHAR | Selected seat (e.g., "3A") |
| seat_class | VARCHAR | "Economy" or "Business" |
| booking_status | VARCHAR | INITIATED/PENDING/CONFIRMED/CANCELLED/EXPIRED |
| created_at | DATETIME | When booking was created |
| updated_at | DATETIME | Last modification time |

---

## Redis Integration

### Seat Lock Key Format
```
seat:{flightNumber}:{seatNumber}
Example: seat:F101:3A
```

### TTL
- 10 minutes
- Using `SETNX` for atomic lock acquisition

### Expiry Detection
- No background scheduler needed
- Expiry detected on next status check
- If Redis key doesn't exist → Booking marked EXPIRED

---

## HTML Pages

| Page | Purpose |
|------|---------|
| `booking-initiate.html` | Show flight details, start booking |
| `seat-selection.html` | Visual seat map for selection |
| `booking-pending.html` | Show PENDING status, countdown |
| `booking-status.html` | List all user's bookings |
| `booking-status-detail.html` | Single booking details |
| `booking-expired.html` | Expired booking message |
| `admin-bookings.html` | Admin view of all bookings |

---

## Error Handling

| Scenario | Handling |
|----------|----------|
| Flight not found | Redirect to error page |
| Seat already taken | Show error, return to seat selection |
| Booking expired | Show expired message, option to retry |
| Cannot cancel CONFIRMED | Block action, show message |
| Booking not found | 404 error page |

---

## Sample Accounts

| Username | Password | Role |
|----------|----------|------|
| admin | admin123 | ADMIN |
| passenger | pass123 | PASSENGER |
| john | john123 | PASSENGER |
| jane | jane123 | PASSENGER |

---

## Key Files

### Entities
- `BookingStatus.java` - Enum with all states
- `Booking.java` - JPA entity with status management

### Services
- `BookingService.java` - Core booking lifecycle logic
- `UserService.java` - Inter-service communication

### Controllers
- `BookingController.java` - Booking API endpoints
- `UserController.java` - Authentication & navigation

### Repository
- `BookingRepository.java` - Database queries

---

## Testing the Feature

1. **Start all services:**
   ```bash
   # Terminal 1: Redis
   redis-server
   
   # Terminal 2: Eureka
   cd eureka-server && mvn spring-boot:run
   
   # Terminal 3: Flight Service
   cd flight-management-service && mvn spring-boot:run
   
   # Terminal 4: User Service
   cd user-management-service && mvn spring-boot:run
   ```

2. **Access the application:**
   - User Service: http://localhost:8081
   - Flight Service: http://localhost:8082
   - Eureka: http://localhost:8761

3. **Test booking flow:**
   - Login as `passenger/pass123`
   - Go to "Book Flight"
   - Select a flight → Select seat → Confirm

4. **Test expiry (optional):**
   - Stop at PENDING state
   - Wait 10 minutes
   - Booking will show as EXPIRED

5. **Admin view:**
   - Login as `admin/admin123`
   - View all bookings in "Booking Management"
