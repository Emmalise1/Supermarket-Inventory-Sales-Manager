# Local development notes

## Seeded demo accounts

On the first backend start (empty database, `app.seed.enabled` not disabled)
`DataSeeder` creates two branches, three accounts, categories, suppliers and
products. The passwords below are **local development values only** - they
exist in no production environment and are never displayed in the UI.

| Email | Password | Role | Branch |
|---|---|---|---|
| `admin@supermarket.rw` | `Admin@123` | ADMIN | all branches |
| `manager@supermarket.rw` | `Manager@123` | MANAGER | Kigali Main |
| `cashier@supermarket.rw` | `Cashier@123` | CASHIER | Kigali Main |

The same values are printed once in the backend startup log by `DataSeeder`.

To disable seeding entirely, set `APP_SEED_ENABLED=false` (or
`app.seed.enabled=false`) before starting the backend.
