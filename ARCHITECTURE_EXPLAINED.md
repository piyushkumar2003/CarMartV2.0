# Flight Management System: Architecture Enhancements

## 1. Why MySQL? (vs In-Memory H2)

In our initial version, we used H2 (In-Memory Database). While great for quick testing, it has a major flaw: **Data Loss**. Every time you restart the application, valid users, bookings, and flight data disappear.

**We switched to MySQL because:**
*   **Persistence**: Data survives application restarts. Essential for a real airline system where bookings must be permanent.
*   **Scalability**: MySQL can handle millions of records (flights, bookings) which in-memory lists cannot.
*   **Concurrency**: MySQL (with InnoDB engine) handles multiple users trying to book the same flight much better than simple Java Lists.
*   **Microservices Best Practice**: We gave each service its own Database (`airline_flight_db`, `airline_user_db`). This ensures loose coupling—changes in the Flight DB schema don't break the User Service.

## 2. Why Redis?

Redis is an in-memory data store, used here as a **cache** and a **distributed lock manager**. It is incredibly fast because it runs in RAM.

### Use Case A: Caching Flight Search (Speed)
**Problem**: Flight search is the most frequent operation. Querying MySQL every time thousands of users search for "Delhi to Mumbai" is slow and puts load on the database.
**Solution**:
1.  Check Redis first for the search key (e.g., `search:Delhi:Mumbai:2024-01-20`).
2.  If found (Cache Hit), return data immediately. (Speed: <1ms)
3.  If not found (Cache Miss), query MySQL, store result in Redis, and return.
**TTL (Time To Live)**: We set it to 5 minutes so users don't see stale data for too long.

### Use Case B: Seat Locking (Concurrency)
**Problem**: Two users click "Book" for the last seat at the exact same time.
**Solution**: We use Redis to "Lock" the seat for a temporary period (e.g., 10 minutes) while the user completes payment.
*   **Key**: `seat:{flightNumber}:{seatNumber}` (e.g., `seat:AI-101:1A`).
*   **Mechanism**: We use `SETNX` (Set if Not Exists). Only **one** request can successfully set this key. The second request fails immediately, telling the user "Seat is currently being booked by someone else."
*   **Automatic Expiry**: If the user closes the browser, the key automatically expires after 10 minutes, releasing the seat.

## 3. Summary of Benefits

| Feature | Before (In-Memory/H2) | After (MySQL + Redis) | Benefit |
| :--- | :--- | :--- | :--- |
| **Data Storage** | Lost on restart | Saved to Disk (MySQL) | **Reliability** |
| **Search Speed** | Fast (Local List) | Blazing Fast (Redis Cache) | **Performance at Scale** |
| **Booking Safety**| Race Conditions Possible | Redis Atomic Locks | **Data Integrity** |
| **Architecture** | Monolithic State | Decoupled/Distributed | **Scalability** |

This architecture simulates a real-world production environment where **Reliability (MySQL)** and **Performance (Redis)** work together.
