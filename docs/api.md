# REST API reference

Base URL (local): `http://localhost:8080/api`
All endpoints except `POST /api/auth/login`, `GET /api/auth/providers` and the two
OAuth2 endpoints require `Authorization: Bearer <JWT>`.

Roles: **ADMIN** (all branches), **MANAGER** (own branch, management features),
**CASHIER** (own branch, selling).

## Authentication

| Method | Path | Access | Description |
|---|---|---|---|
| POST | `/auth/login` | public | Email/password → OAuth2 JWT + profile |
| GET | `/auth/providers` | public | Which sign-in methods are configured (`{password, google}`) |
| GET | `/auth/me` | authenticated | Profile from the current token |
| GET | `/oauth2/authorization/google` | public | Starts the OAuth2 authorization code flow (302 → Google) |
| GET | `/login/oauth2/code/google` | public | Google's callback → issues the JWT, redirects to `…/login#token=` |

The two OAuth2 endpoints are only active while `GOOGLE_CLIENT_ID` /
`GOOGLE_CLIENT_SECRET` are set; otherwise they behave like any other unknown
endpoint (`401`). A Google account is never auto-provisioned: its e-mail must
already belong to an active local account, otherwise the flow is refused.

## Products (Redis caching showcase)

| Method | Path | Access | Description |
|---|---|---|---|
| GET | `/products?branchId=` | authenticated | List products (branch-scoped for non-admins) |
| GET | `/products/low-stock?branchId=` | ADMIN, MANAGER | Products at/below threshold (always from MySQL) |
| GET | `/products/barcode/{barcode}` | authenticated | **Redis-first** barcode lookup → `{product, cacheHit, elapsedMs}` |
| GET | `/products/barcode/{barcode}/benchmark?iterations=` | ADMIN, MANAGER | Measured MySQL vs Redis comparison |
| POST | `/products` | ADMIN, MANAGER | Create product (validates unique barcode) |
| PUT | `/products/{id}` | ADMIN, MANAGER | Update product → **invalidates cache** (old + new barcode) |
| DELETE | `/products/{id}` | ADMIN, MANAGER | Deactivate product → invalidates cache |

## Sales (POS)

| Method | Path | Access | Description |
|---|---|---|---|
| POST | `/sales` | authenticated | Record a sale; rejects insufficient stock (never negative) |
| GET | `/sales?branchId=` | authenticated | Latest 50 sales |

## Inventory

| Method | Path | Access | Description |
|---|---|---|---|
| POST | `/inventory/goods-receiving` | ADMIN, MANAGER | Supplier delivery: adds stock, evicts cache |
| POST | `/inventory/adjustments` | ADMIN, MANAGER | Manual +/- correction; result can never be negative |
| GET | `/inventory/movements?branchId=` | authenticated | Stock movement history (latest 100) |

## Dashboard & reports

| Method | Path | Access | Description |
|---|---|---|---|
| GET | `/dashboard?branchId=` | authenticated | Summary cached in Redis (`dashboard:branch:*`, 5 min) |
| GET | `/reports/sales?from=YYYY-MM-DD&to=YYYY-MM-DD&branchId=` | ADMIN, MANAGER | Revenue, sale count, average, top products (from MySQL) |

## Reference data

| Method | Path | Access |
|---|---|---|
| GET | `/branches` | authenticated |
| POST / PUT | `/branches[/{id}]` | ADMIN |
| GET | `/categories` | authenticated |
| POST | `/categories` | ADMIN, MANAGER |
| GET | `/suppliers` | authenticated |
| POST / PUT | `/suppliers[/{id}]` | ADMIN, MANAGER |
| GET / POST / PUT | `/users[/{id}]` | ADMIN |

## Notifications & audit (MongoDB)

| Method | Path | Access | Description |
|---|---|---|---|
| GET | `/notifications` | authenticated | Own + branch-wide notifications |
| GET | `/notifications/unread-count` | authenticated | Badge counter |
| PATCH | `/notifications/{id}/read` | authenticated | Mark as read |
| GET | `/audit?branchId=` | ADMIN, MANAGER | Audit history (latest 50) |

## Operations

| Method | Path | Access |
|---|---|---|
| GET | `/actuator/health` | public | Health check (used by DevOps pipeline) |

## Error format

```json
{ "timestamp": "...", "status": 409, "error": "Conflict",
  "message": "Insufficient stock for 'Sugar 1kg': requested 999, available 40",
  "path": "/api/sales" }
```

Validation errors additionally carry `fieldErrors: [{field, message}]`.
