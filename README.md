# SUPERmarket Inventory & Sales Manager

**Faculty:** Information Technology
**Instructor:** Jeremie U. Tuyisenge
**Course:** Web Technology
**Academic Year:** 2026-2027

A full-stack supermarket inventory and sales management system built with
**React + Vite** (frontend) and **Spring Boot** (backend), using **MySQL**,
**MongoDB**, **RabbitMQ** and **Redis**.

> environment: local MySQL, local MongoDB, local RabbitMQ, local Redis,
> Spring Boot and the Vite dev server.

---

## 1. Architecture

```
                    React Frontend  (Vite dev server, :5173)
                          ↓  REST / HTTP + JWT
                    Spring Boot Backend  (:8080)
                          ↓
             ┌────────────┼────────────┐
             ↓            ↓            ↓
           MySQL        MongoDB       Redis
   (source of truth)  (audit logs,   (cache only,
    operational data   notifications)  TTL, disposable)

Asynchronous events:

Spring Boot  →  RabbitMQ  →  Consumers  →  Notifications / Audit side effects
```

- **MySQL** - source of truth for all relational business data.
- **MongoDB** - document-oriented data: audit history + notifications.
- **RabbitMQ** - message broker for asynchronous domain events.
- **Redis** - *caching layer only* (never the source of truth, never passwords,
  never tokens). If Redis is down, the application still works using MySQL.

## 2. Technology stack

| Layer | Technologies |
|---|---|
| Frontend | React, Vite, Axios, React Router, plain CSS |
| Backend | Java 17+, Spring Boot, Spring Web, Spring Data JPA, Spring Data MongoDB, Spring Data Redis, Spring AMQP (RabbitMQ), Spring Security, OAuth2 (JWT resource server), Bean Validation, Actuator |
| Databases | MySQL (primary), MongoDB (documents) |
| Messaging | RabbitMQ |
| Cache | Redis |
| Testing | JUnit 5, Mockito, Spring test slices, H2 (test only), Vitest + React Testing Library |
| DevOps | GitHub Actions CI (no Docker), environment-based configuration, health checks |

## 3. Prerequisites (all local, no containers)

- JDK 17+ and Maven 3.9+
- Node.js 18+ and npm
- MySQL 8 running on `localhost:3306`
- MongoDB running on `localhost:27017`
- RabbitMQ running on `localhost:5672`
- Redis running on `localhost:6379`

Create an empty database once:

```sql
CREATE DATABASE supermarket CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

## 4. Running the application

### 4.1 Backend (Spring Boot)

```bash
cd backend
mvn spring-boot:run
```

On first start the tables are created automatically (`ddl-auto: update`) and
demo data is seeded (branches, users, categories, suppliers, products).

**Demo accounts:**

| Email | Password | Role | Branch |
|---|---|---|---|
| `admin@supermarket.rw` | `Admin@123` | ADMIN | all branches |
| `manager@supermarket.rw` | `Manager@123` | MANAGER | Kigali Main |
| `cashier@supermarket.rw` | `Cashier@123` | CASHIER | Kigali Main |

### 4.2 Frontend (React + Vite)

```bash
cd frontend
npm install
npm run dev
```

Open <http://localhost:5173>. The dev server proxies `/api` to the backend on
`http://localhost:8080`.

### 4.3 Configuration (environment variables)

Every service can be reconfigured without editing code:

| Variable | Default (local dev) | Purpose |
|---|---|---|
| `MYSQL_URL` | `jdbc:mysql://localhost:3306/supermarket...` | MySQL connection |
| `MYSQL_USER` / `MYSQL_PASSWORD` | `root` / `root` | MySQL credentials |
| `MONGODB_URI` | `mongodb://localhost:27017/supermarket` | MongoDB |
| `RABBITMQ_HOST` / `RABBITMQ_PORT` | `localhost` / `5672` | RabbitMQ |
| `REDIS_HOST` / `REDIS_PORT` | `localhost` / `6379` | Redis |
| `JWT_SECRET` | dev-only default in `application.yml` | OAuth2 token signing key |
| `CORS_ALLOWED_ORIGINS` | `http://localhost:5173,http://localhost:3000` | Frontend origins |

> The defaults are **local development values only**. For any real deployment,
> set every secret (especially `JWT_SECRET`, database passwords) as an
> environment variable - nothing production-grade is committed.

## 5. Redis caching (the additional technology)

The main use case is **product barcode lookup** (`GET /api/products/barcode/{barcode}`):

```
Receive barcode
    ↓
Check Redis  key: product:barcode:{barcode}   (TTL 10 minutes)
    ├─ HIT  → return cached product (MySQL is NOT queried)
    └─ MISS → query MySQL → store result in Redis → return product
```

**Cache keys**

| Key | Value | TTL |
|---|---|---|
| `product:barcode:{barcode}` | product JSON | 10 min |
| `product:id:{id}` | product JSON | 10 min |
| `dashboard:branch:{branchId}` | dashboard summary JSON | 5 min |

**Cache invalidation** - MySQL is updated first, then the affected keys are
removed (`product:barcode:*` and `product:id:*`), on:

- product create / update / deactivation
- **price changes**
- **barcode changes** (both the old and the new barcode keys are evicted)
- stock changes (sales, goods receiving, adjustments) - cached stock would be stale

**Rules respected**

- Redis is a cache only - MySQL is always the source of truth.
- If Redis is unavailable every Redis call is caught and the app falls back to
  MySQL (a warning is logged once).
- No sensitive data in Redis: no passwords, no tokens, no personal data - only
  product/aggregate JSON.

**Performance demonstration**

`GET /api/products/barcode/{barcode}/benchmark?iterations=20` (ADMIN/MANAGER)
measures both paths **for real** on the running system and returns actual
millisecond numbers (without cache = MySQL lookup, with cache = Redis lookup,
plus the measured speed-up). The API response also reports per-request
`cacheHit` and `elapsedMs`, and the UI shows which source answered.
See [docs/redis-caching.md](docs/redis-caching.md).

## 6. Testing

```bash
# Backend: 37 tests (unit, security/RBAC, integration)
cd backend && mvn test

# Frontend: 12 tests
cd frontend && npm test
```

Covered: Redis cache hit/miss/invalidation, negative-stock prevention,
branch-level authorization, RBAC over a real security filter chain,
JWT round-trip, RabbitMQ publish + broker-down fallback, JPA repository
integration (H2), a full Spring Boot context integration test
(login → token → barcode lookup end-to-end), login flow and route guards.

## 7. DevOps (GitHub Actions, no Docker)

[`.github/workflows/ci.yml`](.github/workflows/ci.yml) runs on every push/PR:

1. Checkout
2. Set up Java 17 / Node.js 20
3. Install dependencies
4. Build backend (`mvn verify`) and frontend (`vite build`)
5. Run all automated tests
6. Upload test reports when something fails

Health check for deployment verification: `GET /actuator/health`.

## 8. Project documentation

- [docs/api.md](docs/api.md) - REST API reference
- [docs/redis-caching.md](docs/redis-caching.md) - caching strategy + benchmark
- [docs/database.md](docs/database.md) - MySQL schema and MongoDB collections

## 9. Repository workflow

Git feature branches → pull requests → review → merge to `main`, with the CI
pipeline required to pass (automated build + tests).


