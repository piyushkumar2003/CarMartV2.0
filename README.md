# Airline Ticket Booking System

A Microservices-based Airline Booking System using Spring Boot and Eureka.

## Architecture
- **Eureka Server**: Service Registry (Port 8761)
- **Flight Management Service**: Manages flights (Port 8082)
- **User Management Service**: Manages users and bookings (Port 8081)

## Prerequisites
- Java 17
- Maven

## How to Run

1. **Start Eureka Server**
   ```bash
   cd eureka-server
   mvn spring-boot:run
   ```

2. **Start Flight Management Service**
   ```bash
   cd flight-management-service
   mvn spring-boot:run
   ```

3. **Start User Management Service**
   ```bash
   cd user-management-service
   mvn spring-boot:run
   ```

## URLs to Test

### Eureka Dashboard
- URL: [http://localhost:8761](http://localhost:8761)

### User Service (Entry Point for Users)
- **Landing Page**: [http://localhost:8081/](http://localhost:8081/)
- **Login**: [http://localhost:8081/login](http://localhost:8081/login)
  - Admin Credentials: `admin` / `admin123`
  - Passenger Credentials: `passenger` / `pass123`
- **Register**: [http://localhost:8081/register](http://localhost:8081/register)
- **Passenger Dashboard**: [http://localhost:8081/passenger-dashboard](http://localhost:8081/passenger-dashboard)
- **Admin Dashboard**: [http://localhost:8081/admin-dashboard](http://localhost:8081/admin-dashboard)

### Flight Service (Direct Access)
- **All Flights**: [http://localhost:8082/all-flights](http://localhost:8082/all-flights)
- **Reports**: [http://localhost:8082/report](http://localhost:8082/report)

## Features
- **User Service**: Registration, Login, Booking, View Bookings, Cancel, Upgrade.
- **Flight Service**: Add Flight (Admin), View Flights, Search API, Occupancy Report.
- **Inter-service**: User Service fetches flight data and books seats via REST API calls to Flight Service.
