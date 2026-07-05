# Online Legal Help Platform

A web platform where customers pick a legal case category, submit case details, and get an
auto-generated petition (PDF/DOCX). Customers get a free LLM-chat period, then recharge via
time-based prepaid plans; they can also chat with online lawyers, who earn per-minute into a
wallet with threshold-based payouts. An admin panel manages plans, free minutes, payment-gateway
config, transactions, refunds, lawyer rates, payout thresholds, and audit logs.

Full requirements and phase plan: [`docs/project-plan.md`](docs/project-plan.md). Contributor/AI
agent guidance (domain rules, conventions, module boundaries): [`CLAUDE.md`](CLAUDE.md).

## Tech stack

- **Backend:** Java 21, Spring Boot 3, Spring Security (JWT), Spring WebSocket (STOMP), Flyway, MySQL 8
- **Frontend:** React 18 + TypeScript, Vite, React Router, TanStack Query, Tailwind CSS
- **LLM:** any OpenAI-compatible endpoint (OpenAI, Groq, OpenRouter, Ollama, LM Studio, Together AI,
  DeepInfra, ...) — see [`docs/llm-provider-switching.md`](docs/llm-provider-switching.md)
- **Infra:** Docker Compose for dev and prod

## Repository layout

```
/backend   Spring Boot API (auth, petition, chat, billing, admin, common)
/frontend  React + TypeScript SPA (customer, lawyer, admin features)
/deploy    docker-compose.dev.yml, docker-compose.prod.yml, nginx/, .env.example
/docs      project plan, ops runbook, LLM provider guide
```

## Running locally

```bash
cp deploy/.env.example deploy/.env   # fill in real values
docker compose -f deploy/docker-compose.dev.yml up
```

This starts MySQL, Adminer (DB UI), the backend (hot-reloading via `mvnw spring-boot:run`), and
the frontend (Vite dev server). Default ports (see `deploy/docker-compose.dev.yml` for the
current mapping): backend `8088`, frontend `5178`, Adminer `8081`, MySQL `3308`.

Flyway migrations run automatically on backend startup.

### Creating the first admin account

The public `/api/auth/register` endpoint intentionally rejects `Role.ADMIN` self-registration —
there's no in-app bootstrap for it. Use the one-off script instead:

```bash
DB_NAME=<db name> DB_USER=<db user> DB_PASSWORD=<db password> \
  ./backend/scripts/create-admin.sh <email> <password> [name]
```

(`DB_*` default to the values in `deploy/.env.example`; override them to match your `deploy/.env`.)
It's idempotent — safe to re-run to reset a password. See the script's header comment for details.

## Commands

```bash
# Backend
cd backend && ./mvnw spring-boot:run   # run locally
cd backend && ./mvnw test              # unit + Testcontainers integration tests
cd backend && ./mvnw verify            # full build with checks

# Frontend
cd frontend && npm run dev             # dev server
cd frontend && npm run test            # vitest
cd frontend && npm run build && npm run lint
```

## Deployment

See [`docs/runbook.md`](docs/runbook.md) for environments, first-deploy steps, rollback, and
backup guidance.

## Contributing

Read [`CLAUDE.md`](CLAUDE.md) before making changes — it documents the module boundaries and the
domain rules that must never be violated (server-authoritative time metering, append-only wallet
ledger, idempotent payment webhooks, RBAC on every endpoint, etc.), plus current milestone status.
