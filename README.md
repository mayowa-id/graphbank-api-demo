A clean, lightweight, production-grade Spring Boot demo that shows how to safely handle retries in payment systems using idempotency


## Features
- Idempotency-Key header enforcement
- Redis-backed response caching
- In-memory payment simulation
- Debug endpoint: `/api/debug/processed-count`
- Full Postman collection included
- PlantUML sequence diagram

## Tech Stack
- Java 17
- Spring Boot 3.5.7
- Spring Web + Spring Data Redis
- Lombok
- Redis (Docker)
- JUnit 5
