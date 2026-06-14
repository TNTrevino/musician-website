# Local Development

How to run the full site — including Stripe checkout, fulfillment, downloads,
and email — on your machine.

## Architecture in dev

```
Frontend (vite :5173) ──► Backend (bootRun :8081) ──► Local Postgres (sebas schema)
                              ▲          │
        stripe listen ────────┘          ├──► PIECE_FILES_DIR (~/sebastian-files)
        (forwards test webhooks)         └──► Gmail SMTP (real email)
```

Dev uses the **same Stripe account** as prod, but with **test-mode (sandbox)
keys** — payments are fake, the `4242` test card works, and nothing touches
live data. Each environment has its **own database** (dev runs a local Postgres);
they all use the same fixed schema, **`sebas`**.

## One-time setup

1. **Stripe CLI** — install it (https://docs.stripe.com/stripe-cli), then:

   ```bash
   stripe login   # authorize against the sandbox/test mode of the account
   ```

2. **Piece files dir** — local folder the backend serves PDFs from:

   ```bash
   mkdir -p ~/sebastian-files
   cp <any-small-pdf> ~/sebastian-files/sample.pdf   # matches seed_dev.sql
   ```

3. **Database** — a local Postgres database (e.g. `sebastian-website`). The
   migrations self-create the `sebas` schema, so from the repo root just apply
   them and load the dev seed (see `migrations/README.md`):

   ```bash
   createdb sebastian-website                                  # once
   sqlx migrate run --database-url "$DATABASE_URL"             # creates sebas.* tables
   psql "$DATABASE_URL" -f backend/sql/seed_dev.sql           # 3 test pieces
   ```

   `DATABASE_URL` (sqlx/psql, may embed creds) and `APP_DATABASE_URL` (the Spring
   app, no creds — it prepends `jdbc:`) are both native Postgres URLs
   (`postgresql://...`). No `search_path` is needed because the schema is baked
   into the migrations. They're split because the JDBC driver can't parse
   `user@host` in a URL; see `migrations/README.md`.

4. **`env.sh`** — same pattern as the README, with the fulfillment vars:

   ```bash
   #!/bin/bash
   # Database (local Postgres; schema sebas is fixed in the migrations)
   export DATABASE_URL="postgresql://postgres@localhost:5432/sebastian-website"  # sqlx/psql; may embed creds
   export APP_DATABASE_URL="postgresql://localhost:5432/sebastian-website"       # app; NO creds, adds jdbc:
   export DATABASE_USER="postgres"
   export DATABASE_PW=""
   export DEFAULT_SCHEMA="sebas"

   # Email (real Gmail; dev sends real mail, use your own inbox when testing)
   export EMAIL_USER="<your gmail>"
   export EMAIL_PW="<gmail app password>"

   # Stripe — TEST keys only, never sk_live_
   export STRIPE_SECRET="sk_test_..."
   export STRIPE_WEBHOOK_SECRET="whsec_..."   # printed by `stripe listen`, step below

   # Fulfillment
   export PIECE_FILES_DIR="$HOME/sebastian-files"
   # export DOWNLOAD_TOKEN_TTL_DAYS=30        # optional override

   # URLs / ports
   export FRONTEND_URL="http://localhost:5173"
   export VITE_BACKEND_URL="http://localhost:8081"
   export BACKEND_PORT=8081

   export JWT_SECRET="<any long random string for dev>"
   ```

   `source ./env.sh` in **every** terminal before running anything.

## Running (three terminals)

```bash
# 1 — backend
source ./env.sh && cd backend && ./gradlew bootRun

# 2 — frontend
source ./env.sh && cd frontend && npm install && npm run dev

# 3 — Stripe webhooks
stripe listen --forward-to localhost:8081/payment/webhook
```

`stripe listen` prints a `whsec_...` on startup. Put it in `env.sh` as
`STRIPE_WEBHOOK_SECRET` and restart the backend (the secret is stable per
machine, so this is a one-time step).

## Testing a purchase end-to-end

1. Open http://localhost:5173, add a seeded piece (e.g. "Test Sonata") to the
   cart, and check out.
2. Pay with Stripe's test card: `4242 4242 4242 4242`, any future expiry, any
   CVC/ZIP, and an email inbox you control.
3. Verify:
   - `stripe listen` logs `checkout.session.completed` → `200`
   - the success page shows download buttons and the PDF downloads
   - exactly one row landed in `orders` (status `FULFILLED`) with items in
     `order_item`, and `download_count` bumps on each download
   - the download-links email arrived (real email — check spam)
4. Useful checks in psql:

   ```sql
   SELECT id, buyer_email, status, download_count, email_sent_at
   FROM sebas.orders ORDER BY id DESC LIMIT 5;
   ```

## Gotchas

- **Vite env vars are baked at startup** — change `VITE_BACKEND_URL` and you
  must restart `npm run dev`.
- **No `file_name`, no sale** — checkout rejects pieces whose `file_name` is
  null, and the file must actually exist inside `PIECE_FILES_DIR`.
- **Webhook 400 "Invalid signature"** — your `STRIPE_WEBHOOK_SECRET` doesn't
  match the one `stripe listen` printed.
- **Wrong keys** — if the dashboard shows your test checkout under live mode,
  you exported `sk_live_` by mistake. Stop and swap to `sk_test_`.
