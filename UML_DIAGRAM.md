# UML Diagram - Airline Microservices Project

## System Architecture Overview

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                         AIRLINE BOOKING SYSTEM                              │
│                    Microservices Architecture                               │
└─────────────────────────────────────────────────────────────────────────────┘

                                  ┌──────────────┐
                                  │   Frontend   │
                                  │   (Angular)  │
                                  └──────┬───────┘
                                         │
                    ┌────────────────────┴────────────────────┐
                    │                                         │
            ┌───────▼────────┐                       ┌────────▼────────┐
            │  API Gateway   │                       │ Cache (Redis)   │
            │  (Port 8080)   │                       │ (Port 6379)     │
            └───────┬────────┘                       └────────┬────────┘
                    │                                         │
        ┌───────────┼───────────┬─────────────┐               │
        │           │           │             │               │
        │           │           │             └───────────────┤
   ┌────▼─┐   ┌────▼─┐   ┌─────▼────┐   ┌──────────────┐     │
   │User  │   │Flight│   │ Payment  │   │Config Server │     │
   │Mgmt  │   │Mgmt  │   │Service   │   │  (Port 8888) │     │
   │Svc   │   │Svc   │   │          │   │              │     │
   │8081  │   │8082  │   │8083      │   │              │     │
   └──────┘   └──────┘   └──────────┘   └──────────────┘     │
        │           │           │             │               │
        │           │           └─────────────────────────────┤
        │           │                         │               │
        └─────┬─────┴────┬────────────────┬───┴──────────────┘
              │          │                │
       ┌──────▼─────────▼────────────────▼────────┐
       │                                           │
       │      Eureka Service Discovery             │
       │         (Port 8761)                       │
       │                                           │
       └───────────────────────────────────────────┘
              │          │                │
       ┌──────▼──────────▼────────────────▼────────┐
       │                                           │
       │    Database Layer (MySQL + Redis)         │
       │                                           │
       │ • User DB (user_management_db)            │
       │ • Flight DB (airline_flight_db)           │
       │ • Payment DB (payment_db)                 │
       │ • Redis Cache (seat locks, pricing)       │
       │                                           │
       └───────────────────────────────────────────┘
```

---

## Detailed Entity Class Diagram

### 1. User Management Service (Port 8081)

```
┌─────────────────────────────────────────────────────────────┐
│                  USER MANAGEMENT SERVICE                    │
│                                                              │
│  ┌────────────────────────────────────────────────────────┐ │
│  │                     USER ENTITY                        │ │
│  ├────────────────────────────────────────────────────────┤ │
│  │ - id: Long (PK)                                       │ │
│  │ - username: String                                   │ │
│  │ - password: String (hashed)                         │ │
│  │ - role: String (PASSENGER, ADMIN)                  │ │
│  │                                                     │ │
│  │ Methods:                                           │ │
│  │ + getId(): Long                                    │ │
│  │ + getUsername(): String                           │ │
│  │ + getPassword(): String                           │ │
│  │ + getRole(): String                               │ │
│  └────────────────────────────────────────────────────┘ │
│             │                                             │
│             │ 1:N (User creates multiple bookings)       │
│             │                                             │
│  ┌──────────▼────────────────────────────────────────────┐ │
│  │                   BOOKING ENTITY                      │ │
│  ├────────────────────────────────────────────────────────┤ │
│  │ - id: Long (PK)                                      │ │
│  │ - userId: Long (FK)                                │ │
│  │ - flightId: Long (Reference to Flight Service)    │ │
│  │ - flightNumber: String                            │ │
│  │ - source: String                                  │ │
│  │ - destination: String                            │ │
│  │ - dateOfJourney: String (YYYY-MM-DD)            │ │
│  │ - seatNumber: String (e.g., "3A")               │ │
│  │ - seatClass: String (Economy, Business)         │ │
│  │ - price: Double                                  │ │
│  │ - pnr: String (Unique)                          │ │
│  │ - bookingStatus: BookingStatus (ENUM)           │ │
│  │ - createdAt: LocalDateTime                      │ │
│  │ - updatedAt: LocalDateTime                      │ │
│  │                                                  │ │
│  │ BookingStatus Enum:                             │ │
│  │ • INITIATED (Just created)                      │ │
│  │ • PENDING (Seat locked in Redis)               │ │
│  │ • CONFIRMED (User confirmed)                   │ │
│  │ • CANCELLED (User cancelled)                   │ │
│  │ • EXPIRED (Redis lock expired)                 │ │
│  │                                                  │ │
│  │ Methods:                                         │ │
│  │ + getId(): Long                                 │ │
│  │ + getBookingStatus(): BookingStatus            │ │
│  │ + setBookingStatus(status): void               │ │
│  │ + getSeatNumber(): String                      │ │
│  │ + getPNR(): String                             │ │
│  └────────────────────────────────────────────────┘ │
│             │                                         │
│             │ 1:N (User has multiple passengers)     │
│             │                                         │
│  ┌──────────▼────────────────────────────────────────┐ │
│  │                 PASSENGER ENTITY                 │ │
│  ├────────────────────────────────────────────────────┤ │
│  │ - id: Long (PK)                                  │ │
│  │ - userId: Long (FK)                            │ │
│  │ - firstName: String                            │ │
│  │ - lastName: String                             │ │
│  │ - email: String                                │ │
│  │ - phoneNumber: String                          │ │
│  │ - gender: String                               │ │
│  │                                                │ │
│  │ Methods:                                       │ │
│  │ + getFullName(): String                       │ │
│  │ + getContactInfo(): Map<String, String>      │ │
│  └────────────────────────────────────────────────┘ │
│             │                                         │
│             │ extends (Inheritance)                  │
│             │                                         │
│  ┌──────────▼────────────────────────────────────────┐ │
│  │            FREQUENT_TRAVELER ENTITY              │ │
│  ├────────────────────────────────────────────────────┤ │
│  │ - loyaltyPoints: Integer                          │ │
│  │ - tier: String (BRONZE, SILVER, GOLD, PLATINUM) │ │
│  │ - membershipDate: LocalDateTime                   │ │
│  │                                                    │ │
│  │ Methods:                                          │ │
│  │ + addLoyaltyPoints(points): void               │ │
│  │ + calculateDiscount(): Double                  │ │
│  │ + upgradeTier(): void                          │ │
│  └────────────────────────────────────────────────┘ │
│                                                      │
│  Services/Repositories:                             │
│  + UserService                                      │
│  + BookingService                                   │
│  + PassengerService                                 │
│  + TicketService                                    │
│  + ReportService                                    │
│  + UserRepository (JpaRepository)                  │
│  + BookingRepository (JpaRepository)               │
│  + PassengerRepository (JpaRepository)             │
│  + FrequentTravelerRepository (JpaRepository)      │
│                                                      │
│  Controllers:                                       │
│  + UserController (/api/users)                     │
│  + BookingController (/api/bookings)               │
│  + BookingRestController (/api/bookings/rest)      │
│  + TicketController (/api/tickets)                 │
│  + ReportController (/api/reports)                 │
│  + ProfileController (/api/profile)                │
│  + AuthController (/api/auth)                      │
│                                                      │
│  Security:                                          │
│  + SecurityConfig (JWT authentication)             │
│  + JwtService (Token generation/validation)        │
│  + JwtAuthenticationFilter (Request interceptor)   │
│  + CustomUserDetailsService                        │
│                                                      │
└─────────────────────────────────────────────────────┘
```

---

### 2. Flight Management Service (Port 8082)

```
┌─────────────────────────────────────────────────────────────┐
│                 FLIGHT MANAGEMENT SERVICE                   │
│                                                              │
│  ┌────────────────────────────────────────────────────────┐ │
│  │                    FLIGHT ENTITY                       │ │
│  ├────────────────────────────────────────────────────────┤ │
│  │ - id: Long (PK)                                       │ │
│  │ - flightNumber: String (e.g., "AI101")              │ │
│  │ - airlineName: String                               │ │
│  │ - source: String (Departure city)                   │ │
│  │ - destination: String (Arrival city)                │ │
│  │ - dateOfJourney: String (YYYY-MM-DD)               │ │
│  │ - totalSeats: int                                   │ │
│  │ - availableSeats: int                               │ │
│  │ - basePrice: double                                 │ │
│  │ - currentPrice: double (Dynamic pricing)            │ │
│  │                                                      │ │
│  │ Methods:                                            │ │
│  │ + getId(): Long                                    │ │
│  │ + getFlightNumber(): String                        │ │
│  │ + getAvailableSeats(): int                         │ │
│  │ + updatePrice(newPrice): void                      │ │
│  │ + updateAvailableSeats(count): void               │ │
│  └────────────────────────────────────────────────────┘ │
│             │                                             │
│             │ 1:N (Flight has many seats)                │
│             │                                             │
│  ┌──────────▼────────────────────────────────────────────┐ │
│  │                     SEAT ENTITY                       │ │
│  ├────────────────────────────────────────────────────────┤ │
│  │ - id: Long (PK)                                      │ │
│  │ - flightId: Long (FK)                              │ │
│  │ - seatNumber: String (e.g., "1A", "2B")          │ │
│  │ - seatClass: String (Economy, Business, First)    │ │
│  │ - isAvailable: boolean                             │ │
│  │ - basePrice: double                                │ │
│  │ - currentPrice: double                             │ │
│  │                                                     │ │
│  │ Methods:                                           │ │
│  │ + getSeatNumber(): String                         │ │
│  │ + isAvailable(): boolean                          │ │
│  │ + bookSeat(): void                                │ │
│  │ + cancelSeat(): void                              │ │
│  └────────────────────────────────────────────────────┘ │
│                                                          │
│  DTOs (Data Transfer Objects):                          │
│  + PriceDTO                                             │
│  + SeatDTO                                              │
│                                                          │
│  Services:                                              │
│  + FlightService                                        │
│  + RedisService (Seat locking, caching)               │
│                                                          │
│  Repositories:                                          │
│  + FlightRepository (JpaRepository)                    │
│  + SeatRepository (JpaRepository)                      │
│                                                          │
│  Controllers:                                           │
│  + FlightController (/api/flights)                     │
│                                                          │
│  Configuration:                                         │
│  + RedisConfig (Redis client setup)                    │
│                                                          │
│  Data Loader:                                           │
│  + FlightDataLoader (Initializes sample data)         │
│                                                          │
└─────────────────────────────────────────────────────────┘
```

---

### 3. Payment Service (Port 8083)

```
┌─────────────────────────────────────────────────────────────┐
│                    PAYMENT SERVICE                          │
│                                                              │
│  ┌────────────────────────────────────────────────────────┐ │
│  │                   PAYMENT ENTITY                       │ │
│  ├────────────────────────────────────────────────────────┤ │
│  │ - id: Long (PK)                                       │ │
│  │ - bookingId: Long (FK to User Management Service)   │ │
│  │ - amount: Double                                     │ │
│  │ - paymentMode: String                               │ │
│  │   • UPI                                              │ │
│  │   • CARD                                             │ │
│  │   • NETBANKING                                       │ │
│  │ - status: String                                     │ │
│  │   • SUCCESS                                          │ │
│  │   • FAILED                                           │ │
│  │   • PENDING                                          │ │
│  │ - transactionId: String (Unique)                    │ │
│  │ - timestamp: LocalDateTime                          │ │
│  │                                                      │ │
│  │ Methods:                                            │ │
│  │ + getId(): Long                                    │ │
│  │ + getBookingId(): Long                             │ │
│  │ + getStatus(): String                              │ │
│  │ + processPayment(): boolean                         │ │
│  │ + refundPayment(): boolean                          │ │
│  └────────────────────────────────────────────────────┘ │
│                                                          │
│  Services:                                              │
│  + PaymentService                                       │
│                                                          │
│  Repositories:                                          │
│  + PaymentRepository (JpaRepository)                   │
│                                                          │
│  Controllers:                                           │
│  + PaymentController (/api/payments)                   │
│                                                          │
└─────────────────────────────────────────────────────────┘
```

---

### 4. Support Services

```
┌──────────────────────────────────────────────────────────────┐
│                  EUREKA SERVICE DISCOVERY                    │
│                      (Port 8761)                             │
├──────────────────────────────────────────────────────────────┤
│ Service Registry:                                            │
│ + Registers all microservices                               │
│ + Provides service discovery                                │
│ + Health monitoring                                         │
│ + Load balancing support                                    │
└──────────────────────────────────────────────────────────────┘

┌──────────────────────────────────────────────────────────────┐
│                CONFIG SERVER                                │
│                   (Port 8888)                               │
├──────────────────────────────────────────────────────────────┤
│ Git Repository: /config-repo/                               │
│ • flight-management-service-dev.yml                         │
│ • payment-service-dev.yml                                   │
│ • user-management-service-dev.yml                           │
│                                                              │
│ Functions:                                                  │
│ + Centralized configuration management                      │
│ + Environment-specific configs (dev, prod)                 │
│ + Dynamic property refresh                                  │
└──────────────────────────────────────────────────────────────┘

┌──────────────────────────────────────────────────────────────┐
│                   API GATEWAY                               │
│                    (Port 8080)                              │
├──────────────────────────────────────────────────────────────┤
│ Features:                                                    │
│ + Single entry point for all clients                        │
│ + Request routing to microservices                          │
│ + Authentication/Authorization                             │
│ + Rate limiting                                             │
│ + Load balancing                                            │
└──────────────────────────────────────────────────────────────┘

┌──────────────────────────────────────────────────────────────┐
│                  REDIS CACHE                                │
│                   (Port 6379)                               │
├──────────────────────────────────────────────────────────────┤
│ Used for:                                                    │
│ + Seat locking during booking (PENDING state)              │
│ + Flight pricing cache                                      │
│ + Session management                                        │
│ + Real-time seat availability                              │
└──────────────────────────────────────────────────────────────┘

┌──────────────────────────────────────────────────────────────┐
│                   FRONTEND (Angular)                         │
├──────────────────────────────────────────────────────────────┤
│ Components:                                                  │
│ + FlightSearch                                              │
│ + SeatSelection                                             │
│ + BookingConfirmation                                       │
│ + PaymentGateway                                            │
│ + UserProfile                                               │
│ + TicketDisplay                                             │
│                                                              │
│ Services:                                                   │
│ + FlightService                                             │
│ + BookingService                                            │
│ + PaymentService                                            │
│ + AuthService                                               │
│                                                              │
│ Interceptors:                                               │
│ + JwtInterceptor (Token attachment)                         │
│ + ErrorInterceptor (Error handling)                         │
│                                                              │
│ Guards:                                                     │
│ + AuthGuard (Route protection)                              │
│ + AdminGuard (Admin access control)                         │
└──────────────────────────────────────────────────────────────┘
```

---

## Service-to-Service Interactions

```
┌─────────────────────────────────────────────────────────────────┐
│                   MICROSERVICE COMMUNICATION                    │
└─────────────────────────────────────────────────────────────────┘

1. USER MANAGEMENT SERVICE → FLIGHT MANAGEMENT SERVICE
   ┌──────────────────┐     HTTP/REST      ┌──────────────────┐
   │    User Mgmt     │ ──────────────────> │   Flight Mgmt    │
   │                  │  Get flights        │                  │
   │                  │  Get seat map       │                  │
   │                  |  Lock seat (Redis)  │                  │
   └──────────────────┘                     └──────────────────┘
   
   Endpoints called:
   • GET /flights/search?source=&destination=&date=
   • GET /flights/{id}/seats
   • POST /seats/lock (Store in Redis)
   • POST /seats/unlock (Remove from Redis)

2. USER MANAGEMENT SERVICE → PAYMENT SERVICE
   ┌──────────────────┐     HTTP/REST      ┌──────────────────┐
   │    User Mgmt     │ ──────────────────> │    Payment       │
   │                  │  Process payment    │                  │
   │                  │  Get payment status │                  │
   └──────────────────┘                     └──────────────────┘
   
   Endpoints called:
   • POST /payments/process
   • GET /payments/{id}/status

3. ALL SERVICES → EUREKA SERVICE DISCOVERY
   ┌──────────────────┐
   │    User Mgmt     │\
   └──────────────────┘ \     Service Registration
   ┌──────────────────┐   \    & Discovery
   │   Flight Mgmt    │ ───────> EUREKA
   └──────────────────┘   /      (Port 8761)
   ┌──────────────────┐ /
   │    Payment       │/
   └──────────────────┘

4. ALL SERVICES → CONFIG SERVER
   ┌──────────────────┐
   │    User Mgmt     │\
   └──────────────────┘ \     Bootstrap Configuration
   ┌──────────────────┐   \    
   │   Flight Mgmt    │ ───────> CONFIG SERVER
   └──────────────────┘   /      (Port 8888)
   ┌──────────────────┐ /
   │    Payment       │/
   └──────────────────┘

5. DATABASE LAYER
   ┌──────────────────┐
   │    User Mgmt     │ ──> MySQL: user_management_db
   └──────────────────┘
   
   ┌──────────────────┐
   │   Flight Mgmt    │ ──> MySQL: airline_flight_db
   └──────────────────┘
   
   ┌──────────────────┐
   │    Payment       │ ──> MySQL: payment_db
   └──────────────────┘
   
   ┌──────────────────┐
   │   Flight Mgmt    │ ──> Redis: seat locks, pricing cache
   └──────────────────┘
```

---

## Booking Lifecycle State Diagram

```
                          ┌──────────────┐
                          │   INITIATED  │
                          │ (User clicks │
                          │   "Book")    │
                          └──────┬───────┘
                                 │
                    ┌────────────┴────────────┐
                    │                         │
                    │ (Seat not locked       │
                    │  in 5 minutes)         │
                    │                        │
                    │                   ┌────▼──────┐
                    │                   │  EXPIRED  │
                    │                   │  (Auto    │
                    │                   │ cancelled)│
                    │                   └───────────┘
                    │
            ┌───────▼───────┐
            │   PENDING     │
            │ (Seat locked  │
            │  in Redis)    │
            └───────┬───────┘
                    │
        ┌───────────┼───────────┐
        │           │           │
        │ (User     │ (5 min    │ (User
        │  confirms)│ timeout)  │ cancels)
        │           │           │
    ┌───▼──┐   ┌────▼──────┐   │
    │      │   │  EXPIRED  │   │
    │      │   │ (Auto     │   │
    │      │   │ cancelled)│   │
    │      │   └───────────┘   │
    │      │                   │
┌───▼──────▼─┐          ┌──────▼────────┐
│ CONFIRMED  │          │   CANCELLED   │
│ (Payment   │          │ (Seat freed   │
│ processed) │          │ in Redis)     │
└────────────┘          └───────────────┘

State Transitions:
INITIATED  → PENDING    (Seat locked in Redis)
PENDING    → CONFIRMED  (User confirms booking & pays)
PENDING    → EXPIRED    (Redis lock expires after 5 min)
INITIATED  → EXPIRED    (User timeout)
CONFIRMED  → CANCELLED  (User requests cancellation)
PENDING    → CANCELLED  (User cancels booking)
EXPIRED    → CANCELLED  (Auto-transitioned)
```

---

## Technology Stack

### Backend
- **Language**: Java 17
- **Framework**: Spring Boot 3.2.1
- **Cloud**: Spring Cloud (Config, Eureka, OpenFeign)
- **Database**: MySQL (Persistent storage)
- **Cache**: Redis (Session & seat locks)
- **Authentication**: JWT (JSON Web Tokens)
- **ORM**: Hibernate/JPA
- **Build**: Maven

### Infrastructure
- **Service Discovery**: Eureka Server
- **Config Management**: Spring Cloud Config Server
- **API Gateway**: Spring Cloud Gateway / Zuul
- **Monitoring**: Actuator endpoints, Prometheus metrics
- **Tracing**: Zipkin support

### Frontend
- **Framework**: Angular (Latest)
- **Language**: TypeScript
- **HTTP Client**: HttpClientModule
- **Interceptors**: JWT Authentication & Error Handling

### Key Dependencies
- Spring Cloud Config Client
- Spring Cloud Netflix Eureka
- Spring Data JPA
- MySQL Connector
- Jedis/Lettuce (Redis)
- Spring Security
- JWT (io.jsonwebtoken)

---

## Database Schema Overview

### User Management Service DB
```
┌─────────────────┐
│   app_user      │
├─────────────────┤
│ id (PK)         │
│ username        │
│ password        │
│ role            │
└─────────────────┘
        ▲
        │ 1:N
        │
┌─────────────────┐
│   bookings      │
├─────────────────┤
│ id (PK)         │
│ user_id (FK)    │
│ flight_id       │
│ flight_number   │
│ source          │
│ destination     │
│ date_of_journey │
│ seat_number     │
│ seat_class      │
│ price           │
│ pnr             │
│ booking_status  │
│ created_at      │
│ updated_at      │
└─────────────────┘
        ▲
        │ 1:N
        │
┌─────────────────┐
│   passengers    │
├─────────────────┤
│ id (PK)         │
│ user_id (FK)    │
│ first_name      │
│ last_name       │
│ email           │
│ phone_number    │
│ gender          │
└─────────────────┘
        ▲
        │ 1:N
        │
┌──────────────────────────┐
│ frequent_travelers       │
├──────────────────────────┤
│ id (PK/FK)              │
│ loyalty_points          │
│ tier                    │
│ membership_date         │
└──────────────────────────┘
```

### Flight Management Service DB
```
┌──────────────────────┐
│    flight           │
├──────────────────────┤
│ id (PK)              │
│ flight_number        │
│ airline_name         │
│ source               │
│ destination          │
│ date_of_journey      │
│ total_seats          │
│ available_seats      │
│ base_price           │
│ current_price        │
└──────────────────────┘
         ▲
         │ 1:N
         │
┌──────────────────────┐
│    seat              │
├──────────────────────┤
│ id (PK)              │
│ flight_id (FK)       │
│ seat_number          │
│ seat_class           │
│ is_available         │
│ base_price           │
│ current_price        │
└──────────────────────┘
```

### Payment Service DB
```
┌──────────────────────┐
│    payment           │
├──────────────────────┤
│ id (PK)              │
│ booking_id (FK)      │
│ amount               │
│ payment_mode         │
│ status               │
│ transaction_id       │
│ timestamp            │
└──────────────────────┘
```

---

## API Endpoints Summary

### User Management Service (Port 8081)
```
POST   /api/auth/register          - Register new user
POST   /api/auth/login             - Login user
POST   /api/users/{id}             - Create user profile
GET    /api/users/{id}             - Get user details
GET    /api/bookings               - Get user bookings
POST   /api/bookings/initiate      - Initiate booking
POST   /api/bookings/confirm       - Confirm booking
POST   /api/bookings/cancel        - Cancel booking
GET    /api/bookings/{id}/status   - Get booking status
POST   /api/seats/lock             - Lock seat (Redis)
POST   /api/seats/unlock           - Unlock seat (Redis)
GET    /api/tickets/{id}           - Get ticket
GET    /api/reports/booking        - Generate booking report
GET    /api/profile                - Get user profile
```

### Flight Management Service (Port 8082)
```
GET    /api/flights                - List all flights
GET    /api/flights/search         - Search flights
GET    /api/flights/{id}           - Get flight details
GET    /api/flights/{id}/seats     - Get seat map
POST   /api/flights/{id}/price     - Update dynamic price
GET    /api/flights/{id}/price     - Get flight price
```

### Payment Service (Port 8083)
```
POST   /api/payments/process       - Process payment
GET    /api/payments/{id}          - Get payment status
POST   /api/payments/{id}/refund   - Refund payment
```

### Config Server (Port 8888)
```
GET    /{application}/{profile}    - Get config for service
GET    /{application}/{profile}    - Get config with git label
```

### Eureka Server (Port 8761)
```
GET    /eureka/apps                - List registered services
GET    /eureka/apps/{app-name}     - Get app instances
```

---

## Component Interaction Flow - Booking Example

```
1. USER SEARCHES FLIGHTS
   Frontend (Angular) 
   → GET /api/flights/search?source=DEL&destination=BOM&date=2026-06-30
   → API Gateway 
   → Flight Management Service (8082)
   → Returns Flight List

2. USER SELECTS FLIGHT & VIEWS SEATS
   Frontend 
   → GET /api/flights/{flightId}/seats
   → Flight Management Service
   → Returns Seat Map

3. USER LOCKS SEAT
   Frontend 
   → POST /api/seats/lock (flightId, seatNumber)
   → User Management Service
   → Calls Flight Service API
   → Flight Service stores lock in Redis (5-min expiry)
   → Returns locked seat confirmation

4. USER INITIATES BOOKING
   Frontend 
   → POST /api/bookings/initiate (userId, flightId, seatNumber)
   → User Management Service
   → Creates Booking entity (Status: INITIATED)
   → Stores in MySQL
   → Returns bookingId & PNR

5. USER PROCEEDS TO PAYMENT
   Frontend 
   → POST /api/payments/process (bookingId, amount, paymentMode)
   → Payment Service
   → Processes payment
   → Returns payment status

6. USER CONFIRMS BOOKING
   Frontend 
   → POST /api/bookings/confirm (bookingId, paymentId)
   → User Management Service
   → Updates Booking Status to CONFIRMED
   → Updates Flight availableSeats
   → Removes Redis lock
   → Returns confirmation with ticket

7. SYSTEM GENERATES TICKET
   User Management Service
   → Ticket Service generates PNR
   → Updates Booking Status to CONFIRMED
   → Stores ticket in database

8. USER VIEWS TICKET
   Frontend 
   → GET /api/tickets/{ticketId}
   → User Management Service
   → Returns ticket details
```

---

## Cross-Cutting Concerns

### Security Architecture
```
┌─────────────────────────────────────────┐
│         SECURITY FLOW                   │
├─────────────────────────────────────────┤
│                                         │
│ 1. User Registration/Login              │
│    ├─ Validate credentials              │
│    ├─ Hash password (BCrypt)            │
│    ├─ Generate JWT token                │
│    └─ Return token to client            │
│                                         │
│ 2. JWT Token Structure                  │
│    ├─ Header: {alg: "HS256"}           │
│    ├─ Payload: {userId, role, exp}     │
│    └─ Signature: HMAC-SHA256           │
│                                         │
│ 3. Request Interceptor (Frontend)       │
│    ├─ Attach JWT to Authorization      │
│    ├─ Send all API requests            │
│    └─ Handle token refresh             │
│                                         │
│ 4. JwtAuthenticationFilter (Backend)    │
│    ├─ Extract token from header        │
│    ├─ Validate token signature         │
│    ├─ Check token expiry               │
│    ├─ Load user details                │
│    └─ Set security context             │
│                                         │
│ 5. Role-Based Access Control (RBAC)    │
│    ├─ @PreAuthorize checks             │
│    ├─ Endpoint-level authorization     │
│    ├─ PASSENGER vs ADMIN roles         │
│    └─ Request filtering                │
│                                         │
└─────────────────────────────────────────┘
```

### Caching Strategy (Redis)
```
┌─────────────────────────────────────────┐
│      REDIS CACHING STRATEGY             │
├─────────────────────────────────────────┤
│                                         │
│ 1. Seat Locking                         │
│    Key: "seat:{flightId}:{seatNumber}" │
│    Value: userId                        │
│    TTL: 5 minutes                       │
│    Purpose: Prevent double booking      │
│                                         │
│ 2. Flight Pricing Cache                 │
│    Key: "flight:price:{flightId}"      │
│    Value: currentPrice                  │
│    TTL: 1 hour                          │
│    Purpose: Fast price lookup           │
│                                         │
│ 3. Seat Map Cache                       │
│    Key: "flight:seats:{flightId}"      │
│    Value: List of seat objects         │
│    TTL: 30 minutes                      │
│    Purpose: Cache seat availability     │
│                                         │
│ 4. User Session Cache                   │
│    Key: "session:{sessionId}"          │
│    Value: User authentication data     │
│    TTL: 2 hours                         │
│    Purpose: Fast session lookup         │
│                                         │
└─────────────────────────────────────────┘
```

---

## Deployment Architecture

```
┌───────────────────────────────────────────────────────────────┐
│                    DEPLOYMENT TOPOLOGY                        │
├───────────────────────────────────────────────────────────────┤
│                                                               │
│  Load Balancer / API Gateway                                 │
│  ├─ Route /api/users/*** → User Management Service          │
│  ├─ Route /api/flights/*** → Flight Management Service      │
│  ├─ Route /api/payments/*** → Payment Service               │
│  └─ Rate limiting, Authentication                           │
│                                                              │
│  ┌─────────────────────────────────────────────────────────┐ │
│  │               Container Orchestration                   │ │
│  │  (Docker/Kubernetes - Optional)                         │ │
│  │                                                         │ │
│  │  ┌──────────────────────────────────────────────────┐  │ │
│  │  │    Pod: User Management Service                  │  │ │
│  │  │    ├─ Container 1 (Replica 1)                    │  │ │
│  │  │    ├─ Container 2 (Replica 2)                    │  │ │
│  │  │    └─ Horizontal Pod Autoscaler                  │  │ │
│  │  └──────────────────────────────────────────────────┘  │ │
│  │                                                         │ │
│  │  ┌──────────────────────────────────────────────────┐  │ │
│  │  │    Pod: Flight Management Service                │  │ │
│  │  │    ├─ Container 1 (Replica 1)                    │  │ │
│  │  │    ├─ Container 2 (Replica 2)                    │  │ │
│  │  │    └─ Horizontal Pod Autoscaler                  │  │ │
│  │  └──────────────────────────────────────────────────┘  │ │
│  │                                                         │ │
│  │  ┌──────────────────────────────────────────────────┐  │ │
│  │  │    Pod: Payment Service                          │  │ │
│  │  │    ├─ Container 1 (Replica 1)                    │  │ │
│  │  │    └─ Container 2 (Replica 2)                    │  │ │
│  │  └──────────────────────────────────────────────────┘  │ │
│  │                                                         │ │
│  └─────────────────────────────────────────────────────────┘ │
│                                                              │
│  ┌─────────────────────────────────────────────────────────┐ │
│  │               Persistence Layer                         │ │
│  │  ├─ MySQL Database (Master-Slave Replication)          │ │
│  │  │  ├─ user_management_db                              │ │
│  │  │  ├─ airline_flight_db                               │ │
│  │  │  └─ payment_db                                      │ │
│  │  ├─ Redis Cluster (Cache & Session Store)              │ │
│  │  └─ Message Queue (Optional - RabbitMQ/Kafka)          │ │
│  └─────────────────────────────────────────────────────────┘ │
│                                                              │
│  ┌─────────────────────────────────────────────────────────┐ │
│  │              Infrastructure Services                    │ │
│  │  ├─ Eureka Service Discovery                            │ │
│  │  ├─ Config Server                                       │ │
│  │  ├─ API Gateway                                         │ │
│  │  ├─ Monitoring (Prometheus)                             │ │
│  │  ├─ Logging (ELK Stack)                                 │ │
│  │  └─ Tracing (Zipkin)                                    │ │
│  └─────────────────────────────────────────────────────────┘ │
│                                                              │
└───────────────────────────────────────────────────────────────┘
```

---

## Error Handling & Resilience Patterns

```
Circuit Breaker Pattern:
Service A (User Mgmt) --[REST Call]--> Service B (Flight Mgmt)
                                              ↓
                                    ┌─────────────────┐
                                    │  CLOSED STATE   │
                                    │ (Normal flow)   │
                                    └────────┬────────┘
                                             │
                           ┌─────────────────┴──────────────────┐
                           │                                    │
                    (Failures exceed     (Success after
                     threshold)          timeout)
                           │                                    │
                    ┌──────▼─────────┐            ┌────────────▼────┐
                    │  OPEN STATE    │            │ HALF-OPEN STATE │
                    │ (Fail fast)    │            │ (Test request)  │
                    └────────────────┘            └─────────────────┘

Timeout Handling:
- HTTP Request Timeout: 5 seconds
- Redis Lock Timeout: 5 minutes
- JWT Token Expiry: 1 hour
- Database Connection Timeout: 30 seconds

Retry Logic:
- Exponential backoff: 1s, 2s, 4s, 8s, 16s
- Max retries: 3
- Jitter: Random delay to prevent thundering herd
```

This comprehensive UML diagram provides a complete overview of your airline booking microservices architecture!

