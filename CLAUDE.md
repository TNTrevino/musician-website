# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## What this is

A web store where the musician "Sebastian" sells sheet-music PDFs. Buyers pay
through Stripe Checkout and get **self-serve delivery**: immediately after
payment they can download the PDFs on the success page, and they also receive an
email with download links. There is no buyer login — buyers are anonymous and
access their files through unguessable per-order tokens.

## Stack & layout

- **`frontend/`** — React 18 + TypeScript + Vite + Tailwind. Talks to the backend
  via `import.meta.env.VITE_BACKEND_URL`.
- **`backend/`** — Spring Boot 3.5 / Java 21, Gradle (Kotlin DSL). Controller →
  Service → Repository, with DTOs and MapStruct mappers. JPA/Hibernate over
  Postgres.
- **`migrations/`** (repo root) — sqlx-cli reversible SQL migrations (the schema is
  managed here, not by Hibernate). **`backend/sql/`** — seed data (not migrations).

## Common commands

Backend (run from `backend/`, env vars must be sourced first — see Environment):
```bash
./gradlew bootRun          # run the API
./gradlew build            # compile + test + bootJar
./gradlew compileJava      # quick compile check
./gradlew test             # run tests
./gradlew test --tests BackendApplicationTests   # single test class
./gradlew bootJar          # build sebastian-api.jar only
```
Note: `./gradlew bootRun` auto-loads `../.env` (see the `tasks.bootRun` block in
`build.gradle.kts`). The lone test (`BackendApplicationTests.contextLoads`)
needs a reachable DB and fails without one — this is pre-existing, not your change.

Frontend (run from `frontend/`):
```bash
npm install
npm run dev      # vite dev server on :5173
npm run build    # tsc -b && vite build
npm run lint     # eslint
npx tsc --noEmit # type-check only
```

Database — run from the repo root, where `migrations/` lives (see
`migrations/README.md` for URL/credentials details):
```bash
sqlx migrate info   --database-url "$DATABASE_URL"   # applied vs pending
sqlx migrate run    --database-url "$DATABASE_URL"   # apply
sqlx migrate revert --database-url "$DATABASE_URL"   # roll back latest
sqlx migrate add -r <name>                           # new reversible migration pair
```

Full local setup (Stripe test mode, webhooks, email) is in **`DEVELOPMENT.md`**.

## Payment & fulfillment architecture (the core of this codebase)

The flow that took the most work to get right — read these files together before
touching anything here: `PaymentService`, `OrderFulfillmentService`,
`StripeWebhookController`, `PaymentController`, `DownloadService`,
`DownloadController`, `PurchaseEmailService`.

1. **Checkout** (`PaymentService.checkoutProducts`): the frontend sends **DB
   piece ids** (not Stripe ids). The backend loads each `Piece`, refuses any with
   a null `file_name` (can't sell what we can't deliver), and builds Stripe
   `price_data` from the **DB `price`** — the database is the source of truth for
   price, Stripe holds no product catalog. Piece identity is carried to
   fulfillment via session **metadata `piece_ids`** (a comma-separated list),
   never by matching product names. The success URL uses Stripe's literal
   `{CHECKOUT_SESSION_ID}` placeholder.

2. **Fulfillment** (`OrderFulfillmentService.fulfill`) is **idempotent** and
   called from two places that race: the Stripe webhook
   (`POST /payment/webhook`, signature-verified) and the success-page confirm
   call (`GET /payment/confirm?session_id=...`). Either one creates the `Order`;
   the **unique constraint on `orders.stripe_session_id`** settles the race
   (the loser catches `DataIntegrityViolationException` and re-reads the winner).
   The buyer email is sent exactly once via an atomic
   `OrderRepository.markEmailSent` guard. Because confirm also fulfills, buyers
   are served even if the webhook never arrives.

3. **Downloads** (`DownloadService` / `DownloadController`): one random token per
   order (`orders.download_token`), with `token_expires_at` and a
   `download_count`/`max_downloads` cap. `GET /download/{token}` returns a
   manifest; `GET /download/{token}/{pieceId}` streams the PDF. `PieceFileService`
   reads files from `PIECE_FILES_DIR` (outside the repo) and guards against path
   traversal. Unknown/expired/foreign requests all return a **generic 404** —
   never leak whether a token or file exists.

There is **no Stripe `productId`** anywhere — it was removed. Do not reintroduce a
DB↔Stripe id coupling; route piece identity through session metadata instead.

## Database as code

Hibernate runs with `ddl-auto: none`; **sqlx migrations are the single source of
truth** for schema, in `migrations/` at the repo root. Add changes as new
`sqlx migrate add -r` pairs (fill in both `.up.sql` and the `.down.sql`, and
schema-qualify every object with `sebas.`), not by editing entities and expecting
Hibernate to apply them. Seed data is intentionally separate
(`backend/sql/seed_prod.sql`, `seed_dev.sql`) so it never auto-applies.

**Each environment is its own database, all using the same fixed schema `sebas`.**
Migrations are explicitly `sebas.`-qualified and self-create the schema, so nothing
depends on `search_path`. The app targets the same schema via `DEFAULT_SCHEMA=sebas`
(Hibernate `default_schema`). There are two **native** Postgres URLs
(`postgresql://...`, no `search_path`/`currentSchema` query string): `DATABASE_URL`
for sqlx/psql (may embed credentials) and `APP_DATABASE_URL` for the app
(credential-free — the JDBC driver parses `user@host` as a hostname and dies with
`UnknownHostException`). The Spring app prepends `jdbc:` itself
(`spring.datasource.url: jdbc:${APP_DATABASE_URL}`) and reads creds from
`DATABASE_USER`/`DATABASE_PW` — never put `jdbc:` in an env var.

Gotchas when reproducing schema from entities: `Piece.timeLength` (a
`java.time.Duration`) is stored as **nanoseconds** in a `NUMERIC(21)` column, and
`Users.authority` (a `SimpleGrantedAuthority`, no converter) is a serialized
`BYTEA`.

## Environments & deploy

Self-hosted. Backend + Postgres run on a Raspberry Pi as systemd services
(`sebastian-api-prod` :38741, `sebastian-api-qa` :38742); the built frontend is
rsynced to a DigitalOcean droplet running the reverse proxy. CI deploys on push:
`deploy-prod.yml` (branch `main`) and `deploy-qa.yml` (branch `qa`) do
`git reset --hard`, build, restart the service, rsync the frontend, then health
check. Because deploy resets the working tree, **anything that must survive
deploys (uploaded PDFs in `PIECE_FILES_DIR`) lives outside the repo** and must be
backed up alongside the database.

Config and per-environment setup details: **`DEPLOYMENT.md`**. Required env vars
are in **`.env.example`**; notable ones beyond the obvious DB/email/Stripe keys:
`STRIPE_WEBHOOK_SECRET`, `PIECE_FILES_DIR`, `DEFAULT_SCHEMA`,
`DOWNLOAD_TOKEN_TTL_DAYS` (optional).

One Stripe account serves everything: live mode = prod, a shared sandbox/test
mode = QA + local dev. Local dev uses `stripe listen` for webhooks (it prints its
own signing secret).

## Conventions

- Backend follows controller/service/repository with DTOs; entities use Lombok
  (`@Data`, `@Accessors(chain = true)`). Mapping is via MapStruct (`mappers/`).
- The `orders` table is named in plural because `order` is a reserved SQL word.
- Commits in this repo use Conventional Commits (`feat:`, `refactor:`, `docs:`).
- Task tracking for this project uses the **paso** CLI, not markdown TODOs.
