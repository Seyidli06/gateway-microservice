# API Gateway Microservices

A production-oriented Spring Boot microservice architecture demonstrating API Gateway routing, JWT authentication, role-based authorization, rate limiting, observability, containerization, and automated security validation.

The system consists of:

- **API Gateway** — the single public entry point, built with Spring Cloud Gateway
- **Profile Service** — manages user profiles with CRUD operations
- **Feedback Service** — accepts and retrieves user feedback
- **Keycloak** — provides authentication and issues JWT access tokens
- **PostgreSQL** — separate database for each backend service
- **Redis** — distributed rate limiting for the API Gateway

---

## Architecture

```text
                            Client
                              |
                              |
                              v
                     +------------------+
                     |     Keycloak     |
                     |      :8180       |
                     +------------------+
                              |
                              | JWT
                              v
                     +------------------+
                     |   API Gateway    |
                     |      :9090       |
                     |                  |
                     | JWT Validation   |
                     | RBAC             |
                     | Rate Limiting    |
                     | Logging          |
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
                  PostgreSQL         PostgreSQL


                     API Gateway
                          |
                          v
                        Redis
                   (Rate Limiting)
```

The API Gateway is the main entry point for application traffic.

Backend services communicate through Docker networks and are not intended to be directly exposed to clients when the complete stack is started with Docker Compose.

---

## Core Features

The project implements:

- Spring Cloud Gateway
- Path-based microservice routing
- JWT authentication
- OAuth2 Resource Server
- Keycloak integration
- Role-Based Access Control (RBAC)
- Gateway request logging
- Profile CRUD operations
- Feedback create/list operations
- PostgreSQL persistence
- Redis-backed rate limiting
- Dockerized deployment
- Docker Compose orchestration

### Gateway Routes

```text
/profiles/**  → Profile Service
/feedback/**  → Feedback Service
```

---

## Additional Features

The project also includes:

- Separate PostgreSQL database per service
- Flyway database migrations
- DTO-based API contracts
- Request validation
- Pagination and sorting
- Global exception handling
- Email normalization
- Optimistic locking
- Redis-backed rate limiting
- Correlation IDs
- Correlation ID validation
- Sensitive-header masking
- Request-size limits
- CORS configuration
- Spring Boot Actuator
- Health/readiness probes
- Swagger / OpenAPI documentation
- Multi-stage Docker builds
- Non-root runtime containers
- Docker secrets
- Isolated Docker networks
- Keycloak realm import
- JWT signature validation
- JWT issuer validation
- Realm-role to Spring Security authority mapping
- USER / ADMIN authorization
- Security integration tests
- GitHub Actions CI
- Trivy vulnerability scanning
- Secret scanning
- Dependabot dependency updates

---

## Tech Stack

### Backend

- Java 21
- Spring Boot 4
- Spring Cloud Gateway
- Spring WebFlux
- Spring Security
- OAuth2 Resource Server
- Spring Data JPA
- Bean Validation
- Spring Boot Actuator
- Springdoc OpenAPI

### Infrastructure

- Keycloak
- PostgreSQL
- Redis
- Flyway
- Maven
- Docker
- Docker Compose

### CI & Security

- GitHub Actions
- Trivy
- Dependabot
- Secret scanning

---

# Authentication & Authorization

Authentication is handled by **Keycloak**.

The API Gateway acts as an OAuth2 Resource Server and validates JWT access tokens before protected requests are forwarded to backend services.

```text
User
 |
 | username/password
 v
Keycloak
 |
 | JWT Access Token
 v
API Gateway
 |
 | validate:
 | - signature
 | - issuer
 | - expiration
 |
 v
Extract Keycloak realm roles
 |
 +------ USER
 |
 +------ ADMIN
 |
 v
Spring Security Authorities
 |
 +------ ROLE_USER
 |
 +------ ROLE_ADMIN
 |
 v
Authorization Rules
```

The backend services are therefore protected behind the Gateway security boundary.

---

## Keycloak

When running with Docker Compose, Keycloak is exposed at:

```text
http://localhost:8180
```

The Keycloak container internally listens on port:

```text
8080
```

Therefore:

```text
Host:
localhost:8180

Docker network:
keycloak:8080
```

The application uses the `gateway` realm.

Token endpoint:

```text
POST http://localhost:8180/realms/gateway/protocol/openid-connect/token
```

JWK endpoint used by the Gateway:

```text
http://keycloak:8080/realms/gateway/protocol/openid-connect/certs
```

The JWK endpoint allows Spring Security to obtain the public signing keys required to verify JWT signatures.

---

## Roles

Two application roles are configured:

```text
USER
ADMIN
```

Keycloak stores realm roles inside the JWT under:

```json
{
  "realm_access": {
    "roles": [
      "USER"
    ]
  }
}
```

The API Gateway converts them into Spring Security authorities:

```text
USER  → ROLE_USER
ADMIN → ROLE_ADMIN
```

This mapping is handled by the custom JWT role converter.

---

# Authorization Matrix

The Gateway applies authorization rules before forwarding requests.

| Method | Endpoint | USER | ADMIN |
|---|---|:---:|:---:|
| GET | `/profiles` | ✅ | ✅ |
| GET | `/profiles/{id}` | ✅ | ✅ |
| POST | `/profiles` | ❌ | ✅ |
| PUT | `/profiles/{id}` | ❌ | ✅ |
| DELETE | `/profiles/{id}` | ❌ | ✅ |
| POST | `/feedback` | ✅ | ✅ |
| GET | `/feedback` | ❌ | ✅ |

Expected security behavior:

```text
No JWT / invalid JWT
        ↓
401 Unauthorized

Valid JWT but insufficient role
        ↓
403 Forbidden

Valid JWT + required role
        ↓
Request forwarded to backend service
```

---

# API Endpoints

All normal client requests should be sent through the API Gateway:

```text
http://localhost:9090
```

## Profile Service

| Method | Endpoint | Description | Required Role |
|---|---|---|---|
| POST | `/profiles` | Create profile | ADMIN |
| GET | `/profiles` | List profiles | USER / ADMIN |
| GET | `/profiles/{id}` | Get profile by ID | USER / ADMIN |
| PUT | `/profiles/{id}` | Update profile | ADMIN |
| DELETE | `/profiles/{id}` | Delete profile | ADMIN |

Example profile:

```json
{
  "name": "Adil Mammadov",
  "email": "adil@example.com",
  "bio": "Java Backend Developer"
}
```

The profile list supports pagination and sorting.

Example:

```text
GET /profiles?page=0&size=20
```

---

## Feedback Service

| Method | Endpoint | Description | Required Role |
|---|---|---|---|
| POST | `/feedback` | Submit feedback | USER / ADMIN |
| GET | `/feedback` | List feedback | ADMIN |

Example:

```json
{
  "name": "Test User",
  "email": "test@example.com",
  "message": "Great service"
}
```

---

# Gateway Routing

Spring Cloud Gateway routes requests according to their path.

```text
http://localhost:9090/profiles/**
                |
                +----> Profile Service :9091
```

```text
http://localhost:9090/feedback/**
                |
                +----> Feedback Service :9092
```

The original request path and query parameters are preserved.

Security checks are performed before protected traffic reaches the backend services.

---

# Gateway Logging

Incoming requests are logged at the API Gateway.

The logging layer records information including:

- HTTP method
- request path
- query parameters
- headers
- correlation ID
- response status
- request duration

Sensitive headers are masked before being written to logs.

Examples include:

```text
Authorization
Cookie
Set-Cookie
```

This prevents authentication credentials and session-related data from being exposed through normal application logging.

---

## Correlation IDs

The Gateway supports:

```text
X-Correlation-Id
```

If the client supplies a valid correlation ID, the Gateway preserves it.

If the header is missing, the Gateway generates a new correlation ID.

Malformed or excessively long correlation IDs are replaced with a safe generated value.

The correlation ID is returned in the response and can be used to trace requests across the system.

---

# Rate Limiting

Redis is used to provide distributed API Gateway rate limiting.

The Gateway can therefore apply request limits consistently rather than maintaining counters only inside one application process.

Docker configuration provides Redis connection information to the Gateway.

Example configuration variables:

```text
REDIS_HOST=rate-limit-redis
REDIS_PORT=6379
RATE_LIMIT_REPLENISH_RATE=5
RATE_LIMIT_BURST_CAPACITY=10
```

---

# Database Architecture

Each backend service owns its database.

```text
Profile Service
      |
      v
 profile_db


Feedback Service
      |
      v
 feedback_db
```

This avoids sharing one application database directly between independent services.

PostgreSQL is used for persistence.

Flyway manages database schema migrations.

---

# Docker Network Isolation

The Docker Compose configuration separates infrastructure using dedicated networks.

Conceptually:

```text
                         edge-network
                              |
             +----------------+----------------+
             |                                 |
       API Gateway                       Backend Services


Profile Service
      |
profile-data-network
      |
Profile PostgreSQL


Feedback Service
      |
feedback-data-network
      |
Feedback PostgreSQL


API Gateway
      |
rate-limit-network
      |
Redis


API Gateway
      |
auth-network
      |
Keycloak
```

Database and Redis networks can remain isolated from unnecessary containers.

---

# Running with Docker Compose

## Prerequisites

Install:

- Docker
- Docker Compose

No locally installed PostgreSQL or Redis instance is required when using the complete Docker Compose stack.

---

## Database Secrets

Create:

```text
secrets/
```

inside the project root.

Then create:

```text
secrets/profile_db_password.txt
secrets/feedback_db_password.txt
```

Put the corresponding local database password in each file.

Example project structure:

```text
gateway-microservices/
|
+-- secrets/
|   +-- profile_db_password.txt
|   +-- feedback_db_password.txt
```

These files must not be committed to Git.

---

## Start the Full Stack

From the repository root:

```bash
docker compose up -d --build
```

Check the containers:

```bash
docker compose ps
```

The stack contains:

```text
api-gateway
profile-service
feedback-service
gateway-keycloak
gateway-profile-db
gateway-feedback-db
gateway-rate-limit-redis
```

---

## Application Ports

| Component | Host Port |
|---|---:|
| API Gateway | 9090 |
| Keycloak | 8180 |

Backend services and infrastructure primarily communicate through Docker networks.

---

## Stop the Stack

```bash
docker compose down
```

To remove persistent Docker volumes as well:

```bash
docker compose down -v
```

Be careful when using `-v`, because database data stored in those volumes will be removed.

---

# Getting a JWT Access Token

For local development/security testing, a token can be requested from Keycloak.

## USER Token — PowerShell

```powershell
$userTokenResponse = Invoke-RestMethod `
    -Method Post `
    -Uri "http://localhost:8180/realms/gateway/protocol/openid-connect/token" `
    -ContentType "application/x-www-form-urlencoded" `
    -Body @{
        client_id  = "gateway-client"
        username   = "user"
        password   = "User123!"
        grant_type = "password"
    }

$userToken = $userTokenResponse.access_token
```

Verify that the token exists:

```powershell
$userToken.Length
```

---

## ADMIN Token — PowerShell

```powershell
$adminTokenResponse = Invoke-RestMethod `
    -Method Post `
    -Uri "http://localhost:8180/realms/gateway/protocol/openid-connect/token" `
    -ContentType "application/x-www-form-urlencoded" `
    -Body @{
        client_id  = "gateway-client"
        username   = "admin"
        password   = "Admin123!"
        grant_type = "password"
    }

$adminToken = $adminTokenResponse.access_token
```

Verify:

```powershell
$adminToken.Length
```

> The bundled users and passwords are intended only for local development/testing. Production credentials must be managed securely and must not use these development values.

---

# Example Authenticated Requests

## List Profiles as USER

```powershell
$response = Invoke-WebRequest `
    -UseBasicParsing `
    -Uri "http://localhost:9090/profiles" `
    -Headers @{
        Authorization = "Bearer $userToken"
    }

$response.StatusCode
```

Expected:

```text
200
```

---

## Attempt Profile Creation as USER

```powershell
$profileBody = @{
    name  = "RBAC User Test"
    email = "rbac-user-test@example.com"
    bio   = "USER must not create profiles"
} | ConvertTo-Json

Invoke-WebRequest `
    -UseBasicParsing `
    -Uri "http://localhost:9090/profiles" `
    -Method Post `
    -ContentType "application/json" `
    -Headers @{
        Authorization = "Bearer $userToken"
    } `
    -Body $profileBody
```

Expected:

```text
403 Forbidden
```

---

## Create Profile as ADMIN

```powershell
$profileBody = @{
    name  = "RBAC Admin Test"
    email = "rbac-admin-test@example.com"
    bio   = "Created by ADMIN"
} | ConvertTo-Json

$response = Invoke-WebRequest `
    -UseBasicParsing `
    -Uri "http://localhost:9090/profiles" `
    -Method Post `
    -ContentType "application/json" `
    -Headers @{
        Authorization = "Bearer $adminToken"
    } `
    -Body $profileBody

$response.StatusCode
```

Expected:

```text
201
```

---

## Submit Feedback as USER

```powershell
$feedbackBody = @{
    name    = "RBAC User"
    email   = "rbac-user@example.com"
    message = "USER feedback test"
} | ConvertTo-Json

$response = Invoke-WebRequest `
    -UseBasicParsing `
    -Uri "http://localhost:9090/feedback" `
    -Method Post `
    -ContentType "application/json" `
    -Headers @{
        Authorization = "Bearer $userToken"
    } `
    -Body $feedbackBody

$response.StatusCode
```

Expected:

```text
201
```

---

## Attempt to List Feedback as USER

```powershell
Invoke-WebRequest `
    -UseBasicParsing `
    -Uri "http://localhost:9090/feedback" `
    -Headers @{
        Authorization = "Bearer $userToken"
    }
```

Expected:

```text
403 Forbidden
```

---

## List Feedback as ADMIN

```powershell
$response = Invoke-WebRequest `
    -UseBasicParsing `
    -Uri "http://localhost:9090/feedback" `
    -Headers @{
        Authorization = "Bearer $adminToken"
    }

$response.StatusCode
```

Expected:

```text
200
```

---

# Health Checks

Gateway health endpoint:

```text
GET /actuator/health
```

Local URL:

```text
http://localhost:9090/actuator/health
```

Example PowerShell request:

```powershell
Invoke-RestMethod `
    -Uri "http://localhost:9090/actuator/health"
```

Expected:

```json
{
  "status": "UP"
}
```

Docker Compose also uses health/readiness checks to control dependency startup.

---

# Keycloak Verification

Keycloak's OpenID Connect discovery document can be checked with:

```powershell
Invoke-RestMethod `
    -Uri "http://localhost:8180/realms/gateway/.well-known/openid-configuration"
```

This confirms that the realm's OIDC endpoints are available.

---

# Testing

Run the complete Maven test suite from the project root.

## Windows

```powershell
.\mvnw.cmd clean test
```

## Linux / macOS

```bash
./mvnw clean test
```

Run only the API Gateway tests:

### Windows

```powershell
.\mvnw.cmd -pl api-gateway clean test
```

### Linux / macOS

```bash
./mvnw -pl api-gateway clean test
```

---

## Security Integration Tests

The API Gateway integration tests cover authentication and RBAC behavior.

Important scenarios include:

```text
No token
GET /profiles
→ 401 Unauthorized

Invalid token
GET /profiles
→ 401 Unauthorized

USER
GET /profiles
→ 200 OK

USER
POST /profiles
→ 403 Forbidden

ADMIN
POST /profiles
→ 201 Created

USER
POST /feedback
→ 201 Created

USER
GET /feedback
→ 403 Forbidden

ADMIN
GET /feedback
→ 200 OK
```

Gateway integration tests use a test JWT decoder so automated tests do not depend on a running Keycloak instance.

Real Keycloak authentication is verified separately through the Docker-based runtime environment.

---

# Swagger / OpenAPI

The backend services include Springdoc OpenAPI support.

Swagger/OpenAPI documentation can be used during development to inspect the available API contracts.

Protected Gateway requests still require the appropriate JWT and role.

---

# Security

The project implements multiple security layers.

## Authentication

- Keycloak identity provider
- JWT access tokens
- OAuth2 Resource Server
- JWT signature verification
- issuer validation
- expiration validation

## Authorization

- USER role
- ADMIN role
- custom Keycloak realm-role mapping
- endpoint-level RBAC
- deny-by-default behavior for unknown protected routes

## Gateway Protection

- Redis-backed rate limiting
- request-size restrictions
- CORS configuration
- sensitive-header masking
- validated correlation IDs

## Infrastructure Security

- Docker secrets for database credentials
- isolated Docker networks
- non-root application containers
- multi-stage Docker builds
- backend services hidden behind the Gateway

## Supply Chain / CI Security

- Dependabot
- secret scanning
- Trivy filesystem/configuration scanning
- Trivy Docker image scanning
- HIGH / CRITICAL vulnerability checks

---

# Trivy Scanning

Container images can be scanned locally.

Example:

```powershell
docker run --rm `
    -v /var/run/docker.sock:/var/run/docker.sock `
    aquasec/trivy:0.66.0 `
    image `
    --severity HIGH,CRITICAL `
    --ignore-unfixed `
    gateway-microservices/api-gateway:local
```

The GitHub Actions security workflow also performs automated security scanning.

---

# CI/CD

GitHub Actions validates the project automatically.

## CI Workflow

The CI workflow builds and tests the Maven multi-module project.

This helps detect:

- compilation failures
- unit-test regressions
- integration-test regressions
- security behavior regressions

## Security Workflow

The security workflow includes:

- secret scanning
- dependency/configuration scanning
- Docker image builds
- Trivy vulnerability scanning

Security checks are configured to detect relevant HIGH and CRITICAL findings.

Dependabot is also configured to propose dependency and GitHub Actions updates.

---

# Project Structure

```text
gateway-microservices/
|
+-- api-gateway/
|   +-- src/
|   |   +-- main/
|   |   +-- test/
|   +-- Dockerfile
|   +-- pom.xml
|
+-- profile-service/
|   +-- src/
|   |   +-- main/
|   |   +-- test/
|   +-- Dockerfile
|   +-- pom.xml
|
+-- feedback-service/
|   +-- src/
|   |   +-- main/
|   |   +-- test/
|   +-- Dockerfile
|   +-- pom.xml
|
+-- keycloak/
|   +-- gateway-realm.json
|
+-- secrets/
|   +-- profile_db_password.txt
|   +-- feedback_db_password.txt
|
+-- .github/
|   +-- workflows/
|   +-- dependabot.yml
|
+-- docker-compose.yml
+-- docker-compose.dev.yml
+-- pom.xml
+-- mvnw
+-- mvnw.cmd
+-- README.md
```

The `secrets/` files are local-only and must not be committed.

---

# Request Flow

A protected request follows this path:

```text
1. Client authenticates with Keycloak

2. Keycloak issues JWT access token

3. Client sends:
   Authorization: Bearer <token>

4. API Gateway receives request

5. Spring Security validates:
   - JWT signature
   - issuer
   - expiration

6. JwtRoleConverter reads:
   realm_access.roles

7. Keycloak roles become:
   ROLE_USER / ROLE_ADMIN

8. SecurityConfig checks endpoint permissions

9. Gateway applies filters:
   - correlation ID
   - logging
   - rate limiting
   - other gateway protections

10. Request is routed to the appropriate backend

11. Backend service performs business logic

12. Response returns through the Gateway
```

---

# Assignment Coverage

| Requirement | Status |
|---|:---:|
| Spring Cloud Gateway | ✅ |
| `/profiles/**` routing | ✅ |
| `/feedback/**` routing | ✅ |
| Gateway logging filter | ✅ |
| Profile CRUD | ✅ |
| Feedback create/list | ✅ |
| Docker | ✅ |
| Docker Compose | ✅ Bonus |
| Swagger/OpenAPI | ✅ Bonus |
| PostgreSQL persistence | ✅ Additional |
| Flyway migrations | ✅ Additional |
| Redis rate limiting | ✅ Additional |
| Correlation IDs | ✅ Additional |
| JWT authentication | ✅ Additional |
| Keycloak integration | ✅ Additional |
| USER / ADMIN RBAC | ✅ Additional |
| Security integration tests | ✅ Additional |
| GitHub Actions CI | ✅ Additional |
| Trivy security scanning | ✅ Additional |
| Dependabot | ✅ Additional |

---

# Design Decisions

Several decisions intentionally go beyond the minimum assignment requirements.

### Single Entry Point

Clients communicate through the API Gateway rather than directly depending on backend-service addresses.

### Database per Service

Profile and Feedback services maintain separate persistence boundaries.

### Centralized Authentication

Keycloak handles identity and token issuance while the Gateway validates tokens.

### Centralized Authorization Boundary

Role-based access rules are applied before traffic reaches backend services.

### Distributed Rate Limiting

Redis provides shared rate-limit state suitable for a Gateway architecture.

### Defense in Depth

Application security is complemented by container hardening, network isolation, secret handling, dependency management, and automated vulnerability scanning.

### Automated Security Regression Protection

Authentication and authorization behavior is covered by integration tests instead of relying only on manual verification.

---

# Local Development Notes

The development Keycloak users are intended for local testing only.

Example roles:

```text
user
└── USER

admin
├── USER
└── ADMIN
```

Do not reuse development passwords or bootstrap credentials in a production environment.

For a production deployment, identity-provider credentials, database secrets, TLS configuration, hostname configuration, and environment-specific security settings should be supplied through the deployment platform rather than committed to source control.

---

# Repository

This project is maintained on GitHub:

`Seyidli06/gateway-microservice`

The repository contains the complete multi-module Spring Boot application, Docker configuration, automated tests, and CI/security workflows.
