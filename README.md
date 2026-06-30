# ✈️ Airline Booking System

A scalable, cloud-ready **Airline Booking System** built using **Spring Boot Microservices**, **Spring Cloud**, **Apache Kafka**, **Redis**, **MySQL**, and **Angular**.

The project follows **Microservices Architecture**, **Domain Driven Design (DDD)**, **Event-Driven Communication**, and modern cloud-native development practices.

---

# 📖 Architecture Overview

## Key Highlights

- ✅ Microservices Architecture
- ✅ API Gateway
- ✅ Eureka Service Discovery
- ✅ Spring Cloud Config Server
- ✅ OAuth2 / JWT Authentication
- ✅ Apache Kafka Event Streaming
- ✅ Redis Caching
- ✅ MySQL Database Per Service
- ✅ Circuit Breaker (Resilience4j)
- ✅ Distributed Tracing (Zipkin)
- ✅ Monitoring (Prometheus + Grafana)
- ✅ Docker Ready
- ✅ CI/CD Ready

---

# 🚀 Microservices

| Service | Port | Description |
|----------|------|-------------|
| Config Server | 8888 | Centralized configuration |
| Eureka Server | 8761 | Service Discovery |
| API Gateway | 8080 | Entry point for all APIs |
| User Service | 8081 | User Management |
| Flight Service | 8082 | Flight Inventory |
| Booking Service | 8083 | Booking & Seat Reservation |
| Payment Service | 8084 | Payment Processing |
| Notification Service | 8085 | Email/SMS Notifications |

---

# 📦 Technology Stack

## Backend

- Java 21
- Spring Boot
- Spring Cloud
- Spring Security
- Spring Data JPA
- Hibernate
- Maven

## Microservices

- API Gateway
- Eureka Discovery Server
- Config Server
- Resilience4j
- OpenFeign

## Database

- MySQL

Each service owns its own database.

```
user_db
flight_db
booking_db
payment_db
notification_db
```

---

## Messaging

Apache Kafka is used for asynchronous communication.

### Topics

```
booking.created

payment.success

payment.failed

booking.cancelled

notification.send
```

---

## Cache

Redis is used for

- Seat Locking
- Session Storage
- Price Cache
- Rate Limiting

---

## Security

- JWT Authentication
- OAuth2
- RBAC
- HTTPS

---

# 📂 Project Structure

```
AirlineBookingSystem

├── api-gateway
├── config-server
├── service-registry
├── auth-service
├── user-service
├── flight-service
├── booking-service
├── payment-service
├── notification-service
├── docker-compose.yml
└── README.md
```

---

# 🔄 Booking Workflow

### Step 1

User searches flights.

↓

### Step 2

Flight Service returns available flights.

↓

### Step 3

Booking Service locks seat.

↓

### Step 4

Booking Created Event is published.

↓

### Step 5

Payment Service processes payment.

↓

### Step 6

Payment Success Event is published.

↓

### Step 7

Notification Service sends

- Email
- SMS
- Ticket Confirmation

---

# 📡 API Flow

```
Client

↓

API Gateway

↓

Authentication

↓

User Service
Flight Service
Booking Service
Payment Service
Notification Service

↓

Database
```

---

# 📨 Event Driven Communication

```
Booking Created
        │
        ▼
Apache Kafka
        │
        ▼
Payment Service
        │
        ▼
Payment Success
        │
        ▼
Notification Service
```

---

# 🗄️ Database Design

Every microservice has its own database.

| Service | Database |
|----------|-----------|
| User | user_db |
| Flight | flight_db |
| Booking | booking_db |
| Payment | payment_db |
| Notification | notification_db |

---

# 📊 Monitoring

- Spring Boot Actuator
- Prometheus
- Grafana

---

# 🔍 Distributed Tracing

- Zipkin
- Spring Cloud Sleuth

---

# 🛡️ Fault Tolerance

- Resilience4j
- Circuit Breaker
- Retry
- Rate Limiter

---

# 🐳 Docker

Start complete infrastructure

```bash
docker-compose up -d
```

---

# ▶️ Running the Project

### Clone

```bash
git clone https://github.com/piyushkumar2003/CarMartV2.0.git
```

### Start Services

1. Config Server
2. Eureka Server
3. API Gateway
4. User Service
5. Flight Service
6. Booking Service
7. Payment Service
8. Notification Service

---

# UML Domain Model

## User Service

- User
- Role

## Flight Service

- Flight
- Seat

## Booking Service

- Booking
- Payment

## Payment Service

- PaymentTransaction

## Notification Service

- Notification

---

# Cross Cutting Concerns

- Centralized Logging (ELK)
- Distributed Tracing
- Monitoring & Alerts
- Health Checks
- CI/CD
- Security
- Redis Cache

---

# Future Enhancements

- Kubernetes Deployment
- AWS Deployment
- ELK Stack Integration
- OpenTelemetry
- RabbitMQ Support
- GraphQL Gateway
- Dynamic Pricing Engine
- AI-based Flight Recommendations

---

# 👨‍💻 Author

**Piyush Kumar**

HCL Tech | IIIT Delhi

Software Engineer | Java | Spring Boot | Microservices | System Design

---
