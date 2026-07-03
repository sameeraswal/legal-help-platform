# Deployment & Operations Runbook

## Environments

- **dev** — `docker compose -f deploy/docker-compose.dev.yml up`, run from the repo root, with `deploy/.env` populated from `deploy/.env.example`. Hot-reloads both backend (`mvnw spring-boot:run` inside the `maven` image) and frontend (`vite`).
- **staging / production** — `docker compose -f deploy/docker-compose.prod.yml up -d --build`, run from the repo root, with `deploy/.env` populated with real secrets and `deploy/certs/{fullchain.pem,privkey.pem}` for TLS. Uses the multi-stage `backend/Dockerfile` and `frontend/Dockerfile`.

## First deploy

1. Provision a MySQL-capable host (or use the `mysql` service in `docker-compose.prod.yml`).
2. Populate `deploy/.env` — see `deploy/.env.example` for the full variable list. `ENCRYPTION_KEY` must be a base64-encoded 32-byte value (`openssl rand -base64 32`); `JWT_SECRET` should be 32+ random characters.
3. Place TLS certs at `deploy/certs/fullchain.pem` and `deploy/certs/privkey.pem`.
4. `docker compose -f deploy/docker-compose.prod.yml up -d --build`. Flyway migrations run automatically on backend startup.
5. Confirm `GET /actuator/health` returns `{"status":"UP"}` through the reverse proxy.

## Rollback

Application code: redeploy the previous image tag / git ref and re-run `docker compose up -d --build`. Flyway migrations are forward-only and never edited after being applied (see `CLAUDE.md`) — a rollback that needs a schema change requires a new forward migration that reverses the effect, not reverting an already-applied migration file.

## Backups

Take the MySQL data volume backup on a schedule appropriate to your host (mysqldump or volume snapshot); the `wallet_ledger`, `payments`, and `audit_logs` tables are append-only and are the source of truth for reconciliation — prioritize their integrity in any restore drill.

## Known manual/process steps not covered by this repo

- Razorpay merchant account provisioning and going live (sandbox → live key rotation).
- Anthropic API budget/rate-limit provisioning for production traffic.
- Legal review of petition templates per case category (`backend/src/main/resources/petition-templates`).
- Penetration testing and load testing (500 concurrent chat sessions) ahead of launch.
- UAT with pilot users.
