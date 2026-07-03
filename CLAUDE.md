# CLAUDE.md — Online Legal Help Platform

Guidance for Claude Code when working in this repository. Read this before making changes.

## Project Overview

A web platform where customers select a legal case category, submit case details, and receive an auto-generated petition (PDF/DOCX). Customers get a free LLM-chat period, then recharge via time-based prepaid plans (e.g., ₹10/5 min, ₹100/90 min). Customers can also chat with online lawyers, who earn at configurable per-minute rates into a wallet with threshold-based payouts. An admin panel manages plans, free minutes, payment-gateway config, transactions, refunds, lawyer rates, payout thresholds, and audit logs.

Full requirements and phase plan: see `docs/project-plan.md`. We build milestone by milestone (M0–M5); do not implement features from later phases unless asked.

## Tech Stack

- **Backend:** Java 21, Spring Boot 3.x (modular monolith), Spring Security (JWT), Spring WebSocket (STOMP), Flyway migrations
- **Database:** MySQL 8 (InnoDB, utf8mb4)
- **Frontend:** React 18 + TypeScript, Vite, React Router, TanStack Query, Tailwind CSS
- **Real-time:** WebSocket (STOMP over SockJS fallback)
- **Infra:** Docker & docker-compose for dev; multi-stage Dockerfiles for prod
- **Testing:** JUnit 5 + Testcontainers (backend), Vitest + React Testing Library (frontend)

Do NOT introduce new frameworks, ORMs, or state-management libraries without asking.

## Repository Structure

```
/backend
  /src/main/java/com/legalhelp
    /auth        # registration, login, JWT, RBAC (CUSTOMER, LAWYER, ADMIN)
    /petition    # case categories, case intake, LLM petition generation, PDF/DOCX export
    /chat        # WebSocket sessions, LLM chat, lawyer chat, presence, time metering
    /billing     # plans, wallets, ledger, payment gateway adapter, refunds, payouts
    /admin       # config (free minutes, PG creds, rates, thresholds), dashboards
    /common      # audit logging, exceptions, security utils, config
  /src/main/resources/db/migration   # Flyway scripts (V<N>__description.sql)
/frontend
  /src
    /features    # customer/, lawyer/, admin/ feature folders
    /components  # shared UI components
    /api         # typed API client per module
/docs            # project-plan.md, ADRs, runbooks
/deploy          # docker-compose.dev.yml, docker-compose.prod.yml, nginx/, .env.example
```

Keep module boundaries strict: modules communicate through service interfaces, never by reaching into another module's repositories/entities.

## Commands

```bash
# Full dev environment (backend + frontend + mysql + adminer)
docker compose -f deploy/docker-compose.dev.yml up

# Backend
cd backend && ./mvnw spring-boot:run        # run locally
cd backend && ./mvnw test                   # unit + Testcontainers integration tests
cd backend && ./mvnw verify                 # full build with checks

# Frontend
cd frontend && npm run dev                  # dev server
cd frontend && npm run test                 # vitest
cd frontend && npm run build && npm run lint
```

Always run the relevant tests before declaring a task done.

## Critical Domain Rules (never violate)

1. **Server-authoritative time metering.** Chat billing is computed server-side from session start/stop events. Never trust client-reported durations.
2. **Double-entry, append-only wallet ledger.** Every balance change (RECHARGE, CONSUME, REFUND, EARNING, PAYOUT) is a new `wallet_ledger` row with `balance_after`. Never UPDATE or DELETE ledger rows. Wallet balance must always be derivable from the ledger.
3. **Idempotent payment webhooks.** Verify PG signature, dedupe on `pg_ref`/order ID, and make webhook handlers safe to replay. Store the raw payload.
4. **Money is `BigDecimal` (or minor units as `long`), never `float`/`double`.** Time balances are stored in seconds as integers.
5. **Append-only chat and audit logs.** `chat_messages` and `audit_logs` are insert-only. Every admin config change writes an audit entry with before/after values.
6. **Secrets are never hardcoded or committed.** PG credentials and LLM keys come from environment variables / encrypted `app_config`; keep `.env` out of git, update `.env.example` instead.
7. **Refunds only via the PG refund API** plus a compensating ledger entry — never by mutating the original payment row's amount.
8. **RBAC on every endpoint.** Default-deny; annotate explicitly. Admin endpoints live under `/api/admin/**`, lawyer under `/api/lawyer/**`.
9. **LLM calls go through the abstraction layer** (`llm` client interface in `common`), never direct provider SDK calls from feature code. Petition generation uses per-category prompt templates in `petition/templates`.
10. **Generated petitions must carry the legal disclaimer** and be versioned; regeneration creates a new version, never overwrites.

## Coding Conventions

**Backend**
- Layering per module: `controller → service → repository`; DTOs at the API boundary (no JPA entities in responses); MapStruct for mapping.
- Validation with Jakarta annotations at the controller; business rules in services.
- Errors: throw domain exceptions; a global `@ControllerAdvice` maps them to a consistent JSON error shape `{code, message, traceId}`.
- Schema changes ONLY via new Flyway migrations — never edit an applied migration.
- Transactions: service-layer `@Transactional`; ledger write + balance read must share a transaction with row locking where needed.

**Frontend**
- TypeScript strict mode; no `any`. API types mirror backend DTOs in `/src/api/types`.
- Server state via TanStack Query; UI state via component state/context — no Redux.
- Mobile-first responsive design; all new screens must work at 360px width.
- Currency displayed as ₹ with Indian digit grouping; times shown in IST.
- Reuse components from `/components`; follow the existing Tailwind design tokens.

**Both**
- Small, focused commits with imperative messages (`feat(billing): add plan purchase endpoint`).
- New features require tests: unit tests for services, integration tests (Testcontainers) for anything touching payments, ledger, or metering.

## Testing Priorities

Highest-risk areas that must always have tests when touched:
1. Wallet ledger arithmetic and concurrency (parallel consume vs. recharge)
2. Payment webhook idempotency and signature verification
3. Free-minutes exhaustion and session cut-off behavior
4. Lawyer earnings calculation and payout threshold checks
5. RBAC — a CUSTOMER token must never access lawyer/admin endpoints

## Current Milestone

> Update this section as work progresses.

**Status:** Building the full M0–M5 scope in one continuous effort (per explicit user request, overriding the usual milestone-gating below).

- ✅ M0 — repo skeleton, docker-compose dev environment, CI pipeline, Flyway baseline (`V1__baseline.sql`), auth module (register/login/refresh/logout, JWT, RBAC).
- 🚧 In progress — petition, billing, chat, admin backend modules; full frontend feature set; critical-path tests.

Once this build completes, treat the milestone-by-milestone gating below as the default for any *new* feature work.

## Things to Ask Before Doing

- Adding any new dependency or library
- Changing the database schema of `wallet_ledger`, `payments`, or `chat_messages`
- Modifying the PG adapter interface or LLM client interface
- Anything touching refund or payout money movement
