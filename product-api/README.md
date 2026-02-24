# Product API — Zest India IT Assignment

A production-ready RESTful API for managing **Products** and **Items**, built with Java 17, Spring Boot 3, JWT security, PostgreSQL, and Docker.

---

## Architecture Overview

```
product-api/
├── config/          # Security, Async, Swagger configuration
├── controller/      # REST controllers  (AuthController, ProductController)
├── dto/
│   ├── request/     # Validated request bodies
│   └── response/    # Typed API & paged responses
├── entity/          # JPA entities  (Product, Item, User, RefreshToken)
├── exception/       # Global handler + domain exceptions
├── repository/      # Spring Data JPA repositories
├── security/        # JWT provider, filter, UserDetailsService
└── service/         # Business logic (interfaces + impls)
```

### Key Design Decisions

| Concern | Choice |
|---|---|
| API versioning | URL prefix `/api/v1/` |
| Auth | JWT access token (15 min) + UUID refresh token (7 days, rotated on every use) |
| Authorization | Method-level `@PreAuthorize` + HTTP security rules |
| Pagination | `Pageable` + generic `PagedResponse<T>` wrapper |
| Error format | `ApiResponse<T>` envelope with `success`, `message`, `data`, `timestamp` |
| Async | `@Async` audit logging via dedicated thread pool |
| DB indexing | Composite indexes on `product_name`, `created_by`, `product_id` |

---

## API Endpoints

### Authentication (`/api/v1/auth`)

| Method | Path | Description |
|---|---|---|
| POST | `/register` | Register new user (optional roles) |
| POST | `/login` | Login → returns access + refresh tokens |
| POST | `/refresh-token` | Rotate refresh token → new access token |
| POST | `/logout` | Invalidate refresh token |

### Products (`/api/v1/products`)

| Method | Path | Role | Description |
|---|---|---|---|
| GET | `/products` | USER / ADMIN | List all (paginated, searchable by `?name=`) |
| GET | `/products/{id}` | USER / ADMIN | Get by ID |
| POST | `/products` | USER / ADMIN | Create |
| PUT | `/products/{id}` | USER / ADMIN | Update |
| DELETE | `/products/{id}` | **ADMIN only** | Delete |
| GET | `/products/{id}/items` | USER / ADMIN | Items of a product (paginated) |
| POST | `/products/{id}/items` | USER / ADMIN | Add item to product |

#### Pagination query params

```
?page=0&size=10&sortBy=id&direction=asc
```

### Swagger UI

```
http://localhost:8080/swagger-ui.html
```

---

## Quick Start with Docker (recommended)

> **Requires:** Docker Desktop running.

```bash
# 1 — Clone / download the project
cd product-api

# 2 — Build & start both services (PostgreSQL + Spring Boot)
docker-compose up --build

# 3 — API is ready at
http://localhost:8080/swagger-ui.html
```

`docker-compose up --build` will:
1. Pull `postgres:16-alpine` and start it.
2. Build the Spring Boot image via the multi-stage `Dockerfile`.
3. Start the app once PostgreSQL passes its health check.

> Change secrets/ports in `docker-compose.yml` → `environment` section before deploying to production.

---

## Local Development (without Docker)

### Prerequisites
- Java 17+
- Maven 3.9+
- PostgreSQL 15+

### Steps

```bash
# Create the database
psql -U postgres -c "CREATE DATABASE productdb;"

# Run the application
./mvnw spring-boot:run
```

The app reads environment variables; defaults fall back to `localhost:5432/productdb`.

---

## Running Tests

```bash
# Unit + integration tests (H2 in-memory database)
./mvnw test

# Generate coverage report (target/site/jacoco/index.html)
./mvnw verify
```

---

## Environment Variables

| Variable | Default | Description |
|---|---|---|
| `DB_HOST` | `localhost` | PostgreSQL host |
| `DB_PORT` | `5432` | PostgreSQL port |
| `DB_NAME` | `productdb` | Database name |
| `DB_USERNAME` | `postgres` | DB user |
| `DB_PASSWORD` | `postgres` | DB password |
| `JWT_SECRET` | *(built-in dev key)* | Must be ≥ 256-bit in production |
| `JWT_ACCESS_EXPIRY` | `900000` | Access token TTL (ms) |
| `JWT_REFRESH_EXPIRY` | `604800000` | Refresh token TTL (ms) |
| `SERVER_PORT` | `8080` | Application port |

---

## Database Schema

```sql
CREATE TABLE product (
  id           BIGINT PRIMARY KEY GENERATED ALWAYS AS IDENTITY,
  product_name VARCHAR(255) NOT NULL,
  created_by   VARCHAR(100) NOT NULL,
  created_on   TIMESTAMP    NOT NULL,
  modified_by  VARCHAR(100),
  modified_on  TIMESTAMP
);

CREATE TABLE item (
  id         BIGINT PRIMARY KEY GENERATED ALWAYS AS IDENTITY,
  product_id BIGINT NOT NULL REFERENCES product(id),
  quantity   INT    NOT NULL
);

CREATE TABLE users (
  id       BIGINT PRIMARY KEY GENERATED ALWAYS AS IDENTITY,
  username VARCHAR(100) NOT NULL UNIQUE,
  email    VARCHAR(150) NOT NULL UNIQUE,
  password TEXT         NOT NULL
);

CREATE TABLE refresh_tokens (
  id          BIGINT PRIMARY KEY GENERATED ALWAYS AS IDENTITY,
  user_id     BIGINT NOT NULL REFERENCES users(id),
  token       VARCHAR(512) NOT NULL UNIQUE,
  expiry_date TIMESTAMP    NOT NULL
);
```

Hibernate auto-creates all tables on startup (`ddl-auto: update`).

---

## Tech Stack

| Layer | Technology |
|---|---|
| Language | Java 17 |
| Framework | Spring Boot 3.2 |
| ORM | Spring Data JPA / Hibernate |
| Database | PostgreSQL 16 |
| Security | Spring Security 6 + JJWT 0.12 |
| Validation | Jakarta Validation (Bean Validation 3) |
| Docs | springdoc-openapi (Swagger UI) |
| Testing | JUnit 5, Mockito, Spring Boot Test, H2 |
| Container | Docker + Docker Compose |
| Build | Maven 3.9 |
