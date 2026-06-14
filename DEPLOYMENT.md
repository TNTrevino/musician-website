# Deployment

## Architecture

```
                 DigitalOcean Droplet              Raspberry Pi (self-hosted runner)
                ┌───────────────────┐          ┌──────────────────────────────────┐
                │   Reverse Proxy   │          │  /opt/sebastian/prod  (main)     │
   User ──────► │   (Caddy/Nginx)   │ ───────► │    sebastian-api-prod  :38741    │
                │                   │          │                                  │
                │ /var/www/sebastian│          │  /opt/sebastian/qa    (qa)       │
                │   (static files)  │          │    sebastian-api-qa   :38742     │
                └───────────────────┘          │                                  │
                  hostname: reverse-proxy      │  PostgreSQL :5432                │
                                               │  OTEL Collector :4317/4318       │
                                               └──────────────────────────────────┘
```

- **Frontend**: Built static files are rsynced to the reverse proxy droplet at `/var/www/sebastian/`
- **Backend**: Spring Boot JAR running as a systemd service on the Pi
- **Database**: PostgreSQL on the Pi (`localhost:5432`)
- **Observability**: OpenTelemetry collector on the Pi, Tempo/Grafana for tracing

## Environments

| | Prod | QA |
|---|---|---|
| Branch | `main` | `qa` |
| Directory | `/opt/sebastian/prod` | `/opt/sebastian/qa` |
| Systemd service | `sebastian-api-prod` | `sebastian-api-qa` |
| Port | `38741` | `38742` |
| Workflow | `deploy-prod.yml` | `deploy-qa.yml` |

Both environments run on the same Raspberry Pi. Each has its own `.env`, service file, and checkout directory.

## CI/CD

GitHub Actions with a self-hosted runner on the Pi. Workflows trigger on push to their respective branches.

**Pipeline steps** (same for both environments):
1. `git fetch && git reset --hard` to pull latest code
2. Build frontend: `npm ci && npm run build`
3. Build backend: `./gradlew bootJar`
4. `sudo systemctl restart sebastian-api-{env}`
5. `rsync` frontend dist to reverse proxy droplet
6. Health check against `/api/health`

## Systemd Services

Service files live at `/etc/systemd/system/sebastian-api-{prod,qa}.service`.

```ini
[Unit]
Description=Sebastian Website API
After=network.target postgresql.service

[Service]
Type=simple
User=noetrevino
WorkingDirectory=/opt/sebastian/{env}
EnvironmentFile=/opt/sebastian/{env}/.env
ExecStart=/usr/bin/java -jar /opt/sebastian/{env}/backend/build/libs/sebastian-api.jar
Restart=always
RestartSec=5

[Install]
WantedBy=multi-user.target
```

After modifying a service file:
```bash
sudo systemctl daemon-reload
sudo systemctl restart sebastian-api-{env}
```

## Sudoers

The GitHub Actions runner needs passwordless sudo for service restarts. Add via `sudo visudo -f /etc/sudoers.d/sebastian-deploy`:

```
noetrevino ALL=(ALL) NOPASSWD: /usr/bin/systemctl restart sebastian-api-prod, /usr/bin/systemctl restart sebastian-api-qa
```

## Environment Variables

Each environment has its own `.env` file at `/opt/sebastian/{env}/.env`. See `.env.example` for the template.

| Variable | Description |
|---|---|
| `DATABASE_URL` | Native Postgres URL for sqlx-cli/psql (may embed credentials) |
| `APP_DATABASE_URL` | Native Postgres URL for the app (no credentials; it prepends `jdbc:`) |
| `DATABASE_USER` | Database username (used by the app) |
| `DATABASE_PW` | Database password (used by the app) |
| `DEFAULT_SCHEMA` | PostgreSQL schema name (fixed: `sebas`) |
| `BACKEND_PORT` | API server port (38741 prod, 38742 qa) |
| `EMAIL_USER` | SMTP username (Gmail) |
| `EMAIL_PW` | SMTP app password |
| `FRONTEND_URL` | Public frontend URL |
| `JWT_SECRET` | JWT signing key |
| `STRIPE_SECRET` | Stripe secret key |
| `STRIPE_WEBHOOK_SECRET` | Signing secret (`whsec_...`) of the Stripe webhook endpoint |
| `PIECE_FILES_DIR` | Directory holding the sheet music PDFs (default `/var/lib/sebastian/files`) |
| `DOWNLOAD_TOKEN_TTL_DAYS` | Optional: days a download link stays valid (default 30) |
| `VITE_BACKEND_URL` | Backend URL used by frontend build |
| `OTEL_EXPORTER_OTLP_ENDPOINT` | OpenTelemetry collector endpoint |
| `OTEL_EXPORTER_OTLP_PROTOCOL` | OTLP protocol (`grpc`) |
| `OTEL_LOGS_EXPORTER` | Logs exporter config |

Each environment is its **own database** (all using the same fixed schema,
`sebas`). QA should use Stripe **test** keys, its own database, and its own JWT
secret.

## Setting Up a New Environment

1. Clone the repo to `/opt/sebastian/{env}`
2. Copy `.env.example` to `.env` and fill in values
3. Provision the database: create an empty database, then apply migrations from
   the repo root with `sqlx migrate run --database-url "$DATABASE_URL"` (the
   migrations self-create the `sebas` schema — see `migrations/README.md`). Seed
   afterward with `backend/sql/seed_prod.sql` (prod) or `seed_dev.sql` (qa/dev)
   — never the dev seed on prod.
4. Create the systemd service file (use prod as a template, change paths and description)
5. Enable the service: `sudo systemctl enable sebastian-api-{env}`
6. Add the sudoers entry for the new service
7. Build and start:
   ```bash
   cd /opt/sebastian/{env}/frontend && npm ci && npm run build
   cd /opt/sebastian/{env}/backend && ./gradlew bootJar
   sudo systemctl start sebastian-api-{env}
   ```

## Sheet Music Fulfillment

Purchases are fulfilled automatically: the Stripe webhook (or the success-page
redirect, whichever lands first) creates an `orders` row, emails the buyer a
download link, and the buyer downloads PDFs through tokenized endpoints.

### PDF storage

PDFs live **outside the repo checkout** (deploys run `git reset --hard`) in
`PIECE_FILES_DIR`:

```bash
sudo mkdir -p /var/lib/sebastian/files
sudo chown noetrevino:noetrevino /var/lib/sebastian/files
sudo chmod 750 /var/lib/sebastian/files
```

> **Back this directory up** along with the database — losing it breaks every
> outstanding download link.

### Adding a new piece for sale

1. Copy the PDF to the Pi: `scp piece.pdf pi:/var/lib/sebastian/files/`
2. Point the piece at it: `UPDATE piece SET file_name = 'piece.pdf' WHERE id = <id>;`

Pieces without a `file_name` are rejected at checkout, so set the file before
expecting sales.

### Stripe webhook

Register the endpoint in the Stripe Dashboard (Developers → Webhooks):

- URL: `https://<public-backend-host>/payment/webhook`
- Events: `checkout.session.completed` only
- Copy the endpoint's `whsec_...` signing secret into `STRIPE_WEBHOOK_SECRET`

The droplet reverse proxy must route `/payment/webhook` to the Pi backend the
same way it routes the rest of the API. QA uses its own webhook endpoint with
test-mode keys (`stripe listen --forward-to localhost:38742/payment/webhook`
works for local testing).

## Health Check

```bash
curl http://localhost:38741/api/health   # prod
curl http://localhost:38742/api/health   # qa
```

Reports status of: database, Stripe, SMTP, and OpenTelemetry collector.
