# API Gateway Microservices

A Spring Boot microservice architecture demonstrating path-based routing with Spring Cloud Gateway.

The system consists of:

- **API Gateway** — routes incoming requests and applies gateway-level filters
- **Profile Service** — manages user profiles with CRUD operations
- **Feedback Service** — accepts and retrieves user feedback
- **PostgreSQL** — separate database for each backend service
- **Redis** — rate limiting for the API Gateway

---

## Architecture

```text
                         Client
                           |
                           v
                  +------------------+
                  |   API Gateway    |
                  |      :9090       |
                  +------------------+
                     |            |
              /profiles/**    /feedback/**
                     |            |
                     v            v
            +----------------+  +------------------+
            | Profile Service|  | Feedback Service |
            |     :9091      |  |      :9092       |
            +----------------+  +------------------+
                    |                  |
                    v                  v
              PostgreSQL          PostgreSQL

                  API Gateway
                       |
                       v
                     Redis
                 (Rate Limiting)
```

Only the API Gateway is exposed to the host when the complete stack is
started with Docker Compose. Backend services communicate through internal
Docker networks.

---

## Core Requirements

The project implements the required functionality:

- Spring Cloud Gateway as the API Gateway
- Path-based routing
  - `/profiles/**` → Profile Service
  - `/feedback/**` → Feedback Service
- Gateway request logging
- Profile CRUD operations
- Feedback create and list operations
- Dockerized services

---

## Additional Features

The project also includes:

- PostgreSQL persistence
- Separate database per service
- Flyway database migrations
- DTO-based API contracts
- Request validation
- Pagination and sorting
- Global exception handling
- Email normalization
- Optimistic locking for profiles
- Redis-backed rate limiting
- Correlation IDs
- Sensitive-header masking
- Request-size limits
- CORS configuration
- Actuator health/readiness probes
- Swagger / OpenAPI documentation
- Multi-stage Docker builds
- Non-root runtime containers
- Docker secrets
- Isolated Docker networks
- GitHub Actions CI
- Trivy vulnerability scanning
- Secret scanning
- Dependabot dependency updates

---

## Tech Stack

- Java 21
- Spring Boot 4
- Spring Cloud Gateway
- Spring WebFlux
- Spring Data JPA
- Spring Security
- PostgreSQL
- Redis
- Flyway
- Maven
- Docker
- Docker Compose
- Springdoc OpenAPI
- GitHub Actions
- Trivy

---

## API Endpoints

All client requests should be sent through the API Gateway on port `9090`.

### Profile Service

| Method | Endpoint | Description |
|---|---|---|
| POST | `/profiles` | Create a profile |
| GET | `/profiles` | List profiles |
| GET | `/profiles/{id}` | Get profile by ID |
| PUT | `/profiles/{id}` | Update a profile |
| DELETE | `/profiles/{id}` | Delete a profile |

Example:

```json
{
  "name": "Adil Mammadov",
  "email": "adil@example.com",
  "bio": "Java Backend Developer"
}
```

### Feedback Service

| Method | Endpoint | Description |
|---|---|---|
| POST | `/feedback` | Submit feedback |
| GET | `/feedback` | List feedback |

Example:

```json
{
  "name": "Test User",
  "email": "test@example.com",
  "message": "Great service"
}
```

---

## Gateway Routing

The Gateway routes requests based on their URL prefix:

```text
http://localhost:9090/profiles/**
                |
                +----> Profile Service :9091

http://localhost:9090/feedback/**
                |
                +----> Feedback Service :9092
```

The original request path and query parameters are preserved.

---

## Gateway Logging

Incoming requests are logged at the Gateway.

The logging filter records information including:

- HTTP method
- request path
- query parameters
- headers
- correlation ID
- response status
- request duration

Sensitive headers such as authorization credentials and cookies are masked
before being written to logs.

If a request does not contain an `X-Correlation-Id`, the Gateway generates
one and includes it in the response.

---

## Running with Docker Compose

### Prerequisites

Install:

- Docker
- Docker Compose

### Database Secrets

Create the following directory in the project root:

```text
secrets/
```

Create two files:

```text
secrets/profile_db_password.txt
secrets/feedback_db_password.txt
```

Put the corresponding local database password inside each file.

These files are ignored by Git and must not be committed.

### Start the Application

From the project root:

```bash
docker compose up --build -d
```

Check container status:

```bash
docker compose ps
```

The API Gateway will be available at:

```text
http://localhost:9090
```

### Stop the Application

```bash
docker compose down
```

To also remove persistent volumes:

```bash
docker compose down -v
```

---

## Health Check

Gateway health:

```text
GET http://localhost:9090/actuator/health
```

Expected response:

```json
{
  "status": "UP"
}
```

Docker Compose also uses readiness checks to control service startup order.

---

## Testing

Run the complete Maven test suite.

### Windows

```powershell
.\mvnw.cmd clean test
```

### Linux / macOS

```bash
./mvnw clean test
```

The project contains unit and integration tests covering the backend services
and Gateway routing behavior.

---

## Example Requests

### Create Profile

```bash
curl -X POST http://localhost:9090/profiles \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Adil Mammadov",
    "email": "adil@example.com",
    "bio": "Java Backend Developer"
  }'
```

### List Profiles

```bash
curl "http://localhost:9090/profiles?page=0&size=20"
```

### Submit Feedback

```bash
curl -X POST http://localhost:9090/feedback \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Test User",
    "email": "test@example.com",
    "message": "Great service"
  }'
```

### List Feedback

```bash
curl "http://localhost:9090/feedback?page=0&size=20"
```

---

## Security

Several additional security measures are implemented:

- Redis-backed request rate limiting
- sensitive request-header masking
- request-size limits
- restricted Gateway routes
- Docker secrets for database credentials
- non-root application containers
- isolated database networks
- automated dependency updates
- secret scanning
- filesystem and container vulnerability scanning

Container images are scanned with Trivy for HIGH and CRITICAL
vulnerabilities as part of the GitHub Actions security workflow.

---

## CI/CD

GitHub Actions automatically validates the project.

### CI

The CI workflow builds and tests the Maven multi-module project.

### Security

The security workflow performs:

- secret scanning
- dependency/configuration scanning
- Docker image builds
- Trivy image vulnerability scans

Security scans are configured to fail when fixable HIGH or CRITICAL
vulnerabilities are detected.

---

## Project Structure

```text
gateway-microservices/
|
+-- api-gateway/
|   +-- src/
|   +-- Dockerfile
|   +-- pom.xml
|
+-- profile-service/
|   +-- src/
|   +-- Dockerfile
|   +-- pom.xml
|
+-- feedback-service/
|   +-- src/
|   +-- Dockerfile
|   +-- pom.xml
|
+-- .github/
|   +-- workflows/
|   +-- dependabot.yml
|
+-- docker-compose.yml
+-- docker-compose.dev.yml
+-- pom.xml
+-- README.md
```

---

## Assignment Coverage

| Requirement | Status |
|---|---|
| Spring Cloud Gateway | ✅ |
| `/profiles/**` routing | ✅ |
| `/feedback/**` routing | ✅ |
| Gateway logging filter | ✅ |
| Profile CRUD | ✅ |
| Feedback create/list | ✅ |
| Docker | ✅ |
| Docker Compose | ✅ Bonus |
| Swagger/OpenAPI | ✅ Bonus |
