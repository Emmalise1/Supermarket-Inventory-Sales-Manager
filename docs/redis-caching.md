# Redis caching strategy

Redis is the **additional technology** of this project. It is a *caching
layer only* - **MySQL remains the source of truth** for all business data.

## 1. Use cases

| # | Use case | Cache key | TTL | Why |
|---|---|---|---|---|
| 1 | Product barcode lookup (POS scanning) | `product:barcode:{barcode}` | 10 min | Hottest read path - every scan |
| 2 | Product by id | `product:id:{id}` | 10 min | Frequently repeated reads |
| 3 | Dashboard summary | `dashboard:branch:{branchId}` | 5 min | Aggregates are expensive to compute |
| 4 | Low-stock queries | *not cached* | - | Stock must always be fresh for correctness |

## 2. Barcode lookup flow

```
GET /api/products/barcode/{barcode}
        ↓
  ┌───────────────┐   hit    ┌──────────────────────┐
  │ Redis lookup   │────────→│ Return cached product │  MySQL NOT queried
  └───────────────┘          └──────────────────────┘
        │ miss
        ↓
  ┌───────────────┐          ┌──────────────────────┐
  │ MySQL query    │────────→│ Store JSON in Redis   │  (TTL 10 min)
  └───────────────┘          └──────────────────────┘
        ↓
  Return product (cacheHit = false, elapsedMs measured)
```

The API response always reports which source answered:

```json
{ "product": {...}, "cacheHit": true, "elapsedMs": 1 }
```

## 3. Cache invalidation

**Database first, cache second.** After every successful MySQL write the
related keys are evicted:

| Event | Keys evicted |
|---|---|
| Product created | `product:id:{id}`, `product:barcode:{barcode}` (defensive) |
| Product updated (price, name, stock threshold...) | `product:id:{id}` + `product:barcode:{barcode}` |
| **Barcode changed** | old **and** new `product:barcode:{...}` keys + `product:id:{id}` |
| Product deactivated | same as update |
| **Price changed** (also via PUT) | same as update |
| Sale completed | all sold products' keys (stock changed) |
| Goods received / stock adjusted | that product's keys |

Dashboard cache uses a short TTL (5 minutes) instead of write-through
invalidation - acceptable because it is a non-critical summary.

## 4. Failure behaviour (Redis down)

Every Redis command is wrapped:

```java
try { return redis.opsForValue().get(key); }
catch (RuntimeException ex) { log.warn("Redis unavailable..."); return Optional.empty(); }
```

- A Redis outage **never** fails a request: the lookup silently falls back to
  MySQL, and the write path still updates MySQL.
- Cache writes are best-effort (ignored on failure).
- Redis data is disposable by design - restarting Redis only means the next
  reads are cache misses.

## 5. Security rules

Stored in Redis: product JSON and dashboard aggregates only.

**Never** stored in Redis:

- passwords / password hashes
- JWT or OAuth tokens
- personal data of users
- anything session-related

Authentication stays completely stateless: tokens live in the client and are
verified from the JWT signature on every request.

## 6. Performance comparison (no invented numbers)

Endpoint: `GET /api/products/barcode/{barcode}/benchmark?iterations=20`
(ADMIN/MANAGER only).

It performs a **real measurement** on the machine running the system:

1. *Without cache*: `iterations` direct MySQL queries, time accumulated.
2. *With cache*: cache warmed, then `iterations` Redis reads, time accumulated.
3. Returns both averages (ms) and the measured speed-up, plus a note with the
   iteration count and timestamp.

Example shape of the response (values are whatever the run actually measured):

```json
{
  "barcode": "6001000000017",
  "iterations": 20,
  "avgDbLookupMs": 4.321,
  "avgRedisLookupMs": 0.215,
  "speedup": 20.1,
  "note": "Measured on this machine with 20 iterations at 2026-10-02T21:00:00Z. ..."
}
```

> If a benchmark is not run on a live system, the honest statement is:
> *"no measurement was performed"* - this project never reports fabricated
> numbers. Run the endpoint yourself during the presentation to show real data.

Typical expectations (to be confirmed by running the endpoint): a local Redis
GET is sub-millisecond, while a MySQL lookup includes a TCP round trip, query
planning and row fetching, so it is usually several times slower. The exact
factor depends on machine, data size and load.
