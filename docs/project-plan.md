# Project Plan — Online Legal Help Platform

**Version:** 1.0 | **Date:** 04 July 2026 | **Status:** Draft for review

---

## 1. Executive Summary

This project delivers a web-based legal assistance platform where customers can select a case category, submit case details, and receive an auto-generated, filing-ready petition. The platform offers an LLM-powered legal chat with a free trial period followed by paid, time-based recharge plans, plus live chat with online lawyers. Lawyers are compensated on configurable per-minute rates via a wallet-and-payout system. A comprehensive admin panel governs plans, payment gateway configuration, transactions, refunds, lawyer rates, payout thresholds, and full audit logging.

**Tech stack:** Java Spring Boot (backend), MySQL (database), ReactJS (frontend), Docker-based development and deployment.

---

## 2. Objectives

1. Enable customers to generate comprehensive, category-specific legal petitions.
2. Monetize legal chat (LLM + human lawyers) through time-metered, prepaid plans.
3. Provide lawyers a self-service portal with earnings tracking and payout requests.
4. Give administrators full operational control: plans, PG configuration, transactions, refunds, rates, thresholds.
5. Ensure complete auditability — every chat message, wallet transaction, and payout is logged.
6. Deliver a professional, production-grade, mobile-responsive frontend.

## 3. Scope

### In Scope
- Customer portal: registration/login, case category selection, case detail intake, petition generation (PDF/DOCX download), LLM chat, lawyer chat, wallet recharge, plan purchase, transaction history.
- LLM integration for legal Q&A chat and petition drafting.
- Free-chat period metering, configurable from admin.
- Payment gateway integration (e.g., Razorpay/PayU/Cashfree) with admin-configurable credentials.
- Lawyer portal: login, online/offline availability toggle, customer chat, session time tracking, wallet, payout requests.
- Admin panel: plan management, free-time configuration, PG configuration, transaction dashboard (success/failed/pending), refunds, lawyer rate configuration, payout threshold configuration, payout approval, audit log viewer.
- Logging & audit trail for chats, wallet transactions, payouts, admin actions.
- Dockerized development environment and clean deployment manifests (docker-compose / optional Kubernetes).

### Out of Scope (Phase 1)
- Native mobile apps (responsive web only).
- Voice/video consultation.
- E-filing integration with court systems (petition is downloadable; filing is manual).
- Multi-language UI (English first; Hindi can be Phase 2).
- Legal review/verification of generated petitions by the platform (disclaimer-based).

## 4. High-Level Architecture

```
[React SPA (responsive)] ── HTTPS ──> [Spring Boot API Gateway / Monolith (modular)]
                                          │
        ┌───────────────┬────────────────┼──────────────────┬──────────────┐
        │               │                │                  │              │
   Auth Module    Chat Module      Billing Module     Petition Module   Admin Module
   (JWT, RBAC)   (WebSocket/STOMP) (Wallet, Plans,    (LLM prompt        (Config, Txns,
                  + LLM service     PG adapter,        orchestration,     Refunds, Payouts,
                  + presence)       time metering)     PDF/DOCX gen)      Audit)
                                          │
                                       [MySQL]        [LLM Provider API]   [Payment Gateway]
                                          │
                                    [Object storage / file store for petitions]
```

**Key design decisions**
- **Modular monolith** in Spring Boot (faster to build/operate than microservices at this stage; clean module boundaries allow later extraction).
- **WebSocket (STOMP over SockJS)** for real-time chat with fallback; server-side session timer is the single source of truth for billing minutes.
- **Adapter pattern for payment gateway** so PG can be switched/configured from admin without code change.
- **LLM abstraction layer** so the model/provider can be swapped; petition generation uses structured prompts per case category with templated sections.
- **RBAC roles:** CUSTOMER, LAWYER, ADMIN (separate login flows, shared auth service).
- **Audit-first design:** append-only tables for chat messages, wallet ledger (double-entry style), payouts, and admin config changes.

## 5. Functional Modules & Requirement Mapping

| # | Requirement | Module | Notes |
|---|---|---|---|
| 1 | Case category selection | Petition | Admin-manageable category master |
| 2 | Case details + personal info | Petition | Multi-step wizard, draft save |
| 3 | Petition generation | Petition + LLM | Category-specific templates + LLM drafting; PDF/DOCX output |
| 4 | LLM chat interface | Chat | Streaming responses, context retention per session |
| 5 | Free chat period | Billing | Configurable minutes per new customer |
| 6 | Recharge + plans | Billing | Prepaid time balance in customer wallet |
| 7 | Plan configuration (₹10/5 min, ₹20/15 min, ₹100/90 min …) | Admin | CRUD on plans; enable/disable |
| 8 | PG integration + PG config from admin | Billing + Admin | Keys stored encrypted; webhook verification |
| 9 | Transaction dashboard + refunds | Admin | Filter by status; refund via PG API with ledger entry |
| 10 | Lawyer login | Auth | Separate role and onboarding flow |
| 11 | Customer ↔ online lawyer chat | Chat | Presence service; lawyer list with online status |
| 12 | Lawyer time tracking | Chat + Billing | Per-session duration captured server-side |
| 13–14 | Lawyer pay rate (configurable) | Admin + Billing | Per-minute rate; global and/or per-lawyer override |
| 15–16 | Lawyer wallet + payout at threshold | Billing + Admin | Threshold configurable; payout request → admin approval → payout record |
| 17 | Logging of everything | Cross-cutting | Chat, wallet ledger, payouts, admin actions, auth events |
| 18–19 | Mobile-friendly, professional UI | Frontend | Responsive design system, production polish |
| 20–22 | Spring Boot, MySQL, React, Docker, clean deploy files | Platform | See Section 9 |

## 6. Data Model (Core Entities)

- **users** (id, role, name, email, phone, status, kyc fields for lawyers)
- **case_categories**, **cases** (customer_id, category_id, details JSON, status)
- **petitions** (case_id, generated_content, file_url, version, generated_at)
- **plans** (name, price, minutes, active)
- **customer_wallets** (customer_id, remaining_seconds, free_seconds_remaining)
- **wallet_ledger** (append-only: user_id, type [RECHARGE/CONSUME/REFUND/EARNING/PAYOUT], seconds/amount, reference, balance_after, created_at)
- **payments** (order_id, pg_ref, amount, status [SUCCESS/FAILED/PENDING/REFUNDED], plan_id, raw webhook payload)
- **refunds** (payment_id, amount, pg_refund_ref, admin_id, status)
- **chat_sessions** (customer_id, counterpart [LLM/lawyer_id], start, end, billed_seconds)
- **chat_messages** (session_id, sender, content, timestamp) — append-only
- **lawyer_rates** (lawyer_id nullable for global default, per_minute_rate, effective_from)
- **lawyer_wallets**, **payout_requests** (lawyer_id, amount, status, admin_id, pg/bank ref)
- **app_config** (free_minutes, payout_threshold, PG credentials [encrypted], LLM settings)
- **audit_logs** (actor, action, entity, before/after, timestamp)

## 7. Non-Functional Requirements

- **Security:** JWT + refresh tokens, bcrypt password hashing, role-based access, encrypted PG secrets (AES-256 at rest), HTTPS everywhere, OWASP Top-10 hardening, rate limiting on auth and chat endpoints.
- **Privacy & compliance:** Case details and personal information handled per India's DPDP Act; consent capture at intake; clear legal disclaimer that generated petitions are drafts requiring professional review; data retention policy.
- **Billing integrity:** Server-authoritative timers; idempotent payment webhooks; double-entry wallet ledger; reconciliation job against PG settlement reports.
- **Performance:** Chat message delivery < 500 ms; LLM streaming first-token < 3 s; petition generation < 60 s; support 500 concurrent chat sessions initially (horizontally scalable).
- **Availability:** Target 99.5% for Phase 1; stateless app containers behind a load balancer; MySQL with automated backups and point-in-time recovery.
- **Observability:** Centralized structured logs, metrics (Prometheus/Grafana), alerting on payment failures and webhook errors.

## 8. Project Phases, Timeline & Milestones

Assumed team (see Section 10). Total duration: **~18 weeks to production launch.**

### Phase 0 — Discovery & Setup (Weeks 1–2)
- Finalize requirements, case categories, petition templates per category, plan structure.
- Select LLM provider and payment gateway; sign-ups and sandbox access.
- UX wireframes for customer, lawyer, and admin journeys; design system.
- Repo setup, Docker dev environment, CI pipeline, coding standards, environment strategy (dev/stage/prod).
- **Milestone M0:** Signed-off SRS + wireframes + running skeleton (React + Spring Boot + MySQL in docker-compose).

### Phase 1 — Foundation (Weeks 3–5)
- Auth service: registration, login, JWT, RBAC for customer/lawyer/admin.
- Customer onboarding, case category master, case intake wizard (multi-step form, draft save).
- Admin panel shell: user management, category management, app config (free minutes).
- Database schema v1, migrations (Flyway), audit logging framework.
- **Milestone M1:** Customer can register, select category, and submit case details end-to-end.

### Phase 2 — Petition Generation & LLM Chat (Weeks 6–9)
- LLM abstraction layer; prompt templates per case category.
- Petition generation pipeline: intake data → structured prompt → draft → PDF/DOCX export → download; versioning and regeneration.
- LLM chat: WebSocket infrastructure, streaming responses, session management, chat history.
- Free-period metering: countdown, warnings, session cut-off on exhaustion.
- **Milestone M2:** Customer generates a downloadable petition and uses LLM chat within free minutes.

### Phase 3 — Billing, Plans & Payments (Weeks 10–12)
- Plan management (admin CRUD) and customer plan purchase flow.
- Payment gateway adapter: order creation, checkout, webhook handling (idempotent), signature verification.
- Wallet & ledger: recharge credit, per-second consumption during chat, balance display.
- Admin: PG configuration screen (encrypted storage), transaction dashboard with filters (success/failed/pending), refund initiation and tracking.
- **Milestone M3:** End-to-end paid flow — recharge, chat consumes time, admin sees transaction and can refund.

### Phase 4 — Lawyer Marketplace (Weeks 13–15)
- Lawyer onboarding & login; profile, KYC fields, bank details.
- Presence service (online/offline), customer-facing online lawyer list, customer↔lawyer real-time chat.
- Session time tracking → lawyer earnings at configured rate → lawyer wallet credit.
- Admin: lawyer rate configuration (global + per-lawyer), payout threshold config.
- Lawyer payout request flow; admin approval and payout recording; payout ledger.
- **Milestone M4:** Customer chats with an online lawyer; lawyer earns, requests payout above threshold; admin approves.

### Phase 5 — Hardening & Launch (Weeks 16–18)
- Frontend polish: responsive QA across devices, professional visual pass, accessibility basics.
- Security review & penetration test fixes; load testing of chat and payment paths.
- Reconciliation jobs, alerting, backup/restore drill.
- Clean deployment artifacts: multi-stage Dockerfiles, docker-compose for dev, production compose/Helm charts, environment variable templates, deployment runbook.
- UAT with pilot users; bug-fix sprint; go-live.
- **Milestone M5 (Launch):** Production deployment with monitoring, runbooks, and admin training complete.

### Post-Launch (Weeks 19–20)
- Hypercare: daily monitoring, fast-turnaround fixes, payment reconciliation checks.
- Backlog grooming for Phase 2 (Hindi UI, mobile apps, video consults, lawyer ratings).

## 9. DevOps & Deployment

- **Development:** docker-compose with services — `frontend` (React dev server/Nginx), `backend` (Spring Boot), `mysql`, `adminer`; seeded test data; `.env.example` for configuration.
- **CI/CD:** Build → unit tests → integration tests (Testcontainers) → image build → push to registry → deploy to stage → manual gate → prod.
- **Deployment files:** Multi-stage Dockerfiles (small runtime images), production docker-compose (or Helm charts if Kubernetes), Nginx reverse-proxy config with TLS, Flyway migrations run on startup, health checks and readiness probes, documented rollback procedure.
- **Environments:** dev → staging (PG sandbox, LLM test keys) → production.

## 10. Team & Responsibilities

| Role | Count | Responsibility |
|---|---|---|
| Project Manager / BA | 1 (part-time) | Requirements, sprint planning, stakeholder demos |
| Tech Lead / Architect | 1 | Architecture, code reviews, LLM & PG integration design |
| Backend Engineers (Spring Boot) | 2 | APIs, billing, chat, wallet, admin services |
| Frontend Engineers (React) | 2 | Customer, lawyer, and admin UIs; responsive design |
| UI/UX Designer | 1 (part-time) | Design system, professional visual language |
| QA Engineer | 1 | Test plans, automation, payment/billing test matrix |
| DevOps (shared) | 0.5 | Docker, CI/CD, monitoring, deployment |

Delivery model: 2-week sprints, demo at each sprint end, milestone sign-offs at M0–M5.

## 11. Key Risks & Mitigations

| Risk | Impact | Mitigation |
|---|---|---|
| Generated petitions legally inadequate | High (reputation/legal) | Category-specific vetted templates, mandatory disclaimers, optional lawyer-review add-on, human-in-the-loop review during pilot |
| Billing/timer disputes (charged time vs. perceived time) | High | Server-side authoritative timing, visible countdown, per-session receipts, generous dispute/refund policy via admin panel |
| Payment webhook failures / double credit | High | Idempotency keys, signature verification, reconciliation job, alerting |
| LLM cost overrun or latency | Medium | Token budgets per session, caching, provider abstraction for switching, streaming UX |
| Lawyer availability at launch | Medium | Onboard a seed panel before launch; LLM chat as always-available fallback |
| Data privacy (sensitive case details) | High | DPDP-aligned consent & retention, encryption at rest/in transit, access controls, audit logs |
| Scope creep | Medium | Milestone-gated scope; Phase-2 backlog for new asks |

## 12. Assumptions & Dependencies

- Payment gateway merchant account (Razorpay/PayU/Cashfree) will be procured by the business before Phase 3.
- LLM provider API access and budget approved before Phase 2.
- Petition templates and case-category legal content will be reviewed by a qualified lawyer engaged by the business.
- Refunds are processed through the PG's refund API; settlement timelines follow PG norms.
- Single currency (INR) and India-first launch.

## 13. Acceptance Criteria (Launch Gate)

1. Customer journey: register → select category → submit details → download petition → chat (free → paid) works on desktop and mobile browsers.
2. All plans purchasable; success, failure, and pending payment states handled and visible in admin.
3. Refund executed from admin panel reflects at PG and in ledger.
4. Lawyer journey: go online → chat → earnings credited at configured rate → payout request above threshold → admin approval.
5. Every chat message, wallet entry, payout, and admin config change is queryable in audit logs.
6. Deployment reproducible from clean environment using provided Docker files and runbook.
