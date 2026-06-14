# Database migrations (sqlx-cli)

The schema is managed as code with [`sqlx-cli`](https://github.com/transact-rs/sqlx/blob/main/sqlx-cli/README.md).
The Spring app runs with `ddl-auto: none`, so it never creates or alters the
schema — these migrations are the single source of truth.

This `migrations/` directory lives at the **repo root**; run `sqlx` from the repo
root so it finds `./migrations`.

## Environment model

Every environment (prod / qa / dev) is its **own database**, and they all use the
same fixed schema, **`sebas`**. The migrations are explicitly schema-qualified
(`sebas.*`) and self-create the schema (`CREATE SCHEMA IF NOT EXISTS sebas`), so
nothing depends on the connection's `search_path`. Standing up a fresh database
is just: create the database, then `sqlx migrate run`.

## Install

```bash
cargo install sqlx-cli --no-default-features --features native-tls,postgres
```

## Connection URL

There are **two** native Postgres URLs (`postgresql://...`, no `search_path`/
`options` query string — the schema is baked into the migrations):

- **`DATABASE_URL`** — used by **sqlx** and `psql`. It *may* embed credentials
  (`postgresql://user[:pw]@host:5432/db`), which is convenient for the CLI.
- **`APP_DATABASE_URL`** — used by the **Spring app**, which prepends `jdbc:`
  (`spring.datasource.url: jdbc:${APP_DATABASE_URL}`). It must **not** embed
  credentials: the JDBC driver parses `user@host` as a hostname and fails with
  `UnknownHostException`. The app reads creds from `DATABASE_USER` / `DATABASE_PW`.

The two are separate precisely because of that JDBC limitation. If you'd rather
keep `DATABASE_URL` credential-free, you can instead give sqlx its creds via
`PGUSER` / `PGPASSWORD` and point both vars at the same URL.

> sqlx records applied migrations in a `_sqlx_migrations` table. It lands in the
> connection's default schema (usually `public`), separate from the `sebas`
> tables — that's expected and harmless; a full `pg_dump` of the database
> captures both.

## Everyday commands

Run from the repo root (so sqlx finds `./migrations`):

```bash
sqlx migrate info   --database-url "$DATABASE_URL"   # what's applied vs pending
sqlx migrate run    --database-url "$DATABASE_URL"   # apply pending migrations
sqlx migrate revert --database-url "$DATABASE_URL"   # roll back the latest migration
```

## Adding a migration

```bash
sqlx migrate add -r <short_name>     # creates <timestamp>_<name>.up.sql + .down.sql
```

Migrations are **reversible**: always fill in both the `.up.sql` (the change) and
the `.down.sql` (how to undo it), and **schema-qualify every object** with
`sebas.` to match the existing migrations. Migrations are immutable once applied
to a shared environment — to change something already shipped, add a new
migration.

## Seed data

Seed data is **not** a migration (it must never auto-apply to prod). It lives in
`backend/sql/` and is also schema-qualified (`sebas.*`):

- `seed_prod.sql` — the real catalog pieces; review and run once per fresh prod DB.
- `seed_dev.sql` — throwaway pieces for qa/dev testing.

Run a seed after migrating, e.g. `psql "$DATABASE_URL" -f backend/sql/seed_dev.sql`.
