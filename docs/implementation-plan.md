# Implementation plan

## Phase 0 inspection — 2026-09-09

The starting workspace contained empty `work/` and `outputs/` directories. No source, repository metadata, project configuration, or applicable AGENTS.md was found. The deliverable monorepo is `outputs/opsdesk-ai/`. No remote push or commit has been performed.

Detected tools: Python 3.11.9, Node 24.19.0, npm 11.17.0, Git, PostgreSQL 17.11. Docker was not found. Java and Maven were absent from PATH. Portable Temurin JDK 21.0.12.1 and Maven 3.9.11 were downloaded into workspace `work/tools/` and checked against publisher checksums. They do not modify system PATH.

An existing PostgreSQL service listens on 5432. A separate development cluster in workspace `work/postgres-data/` listens only on 127.0.0.1:55432. Its random password is outside the deliverable repository. Databases: `opsdesk` and `opsdesk_test`. Existing databases were not modified.

## Version decisions

| Component | Selection | Verification |
| --- | --- | --- |
| Java | 21 LTS, local Temurin 21.0.12.1 | `java -version` |
| Spring Boot | 3.5.16 | Published in Maven Central; official requirements support Java 17–25 |
| Maven | 3.9.11 | `mvn -version`; exceeds Boot minimum 3.6.3 |
| Spring/JPA/Hibernate/Flyway/JDBC/JUnit/Mockito | Boot 3.5.16 dependency management | Resolve and test together; do not independently override managed versions |
| PostgreSQL | Local 17.11 | Real database used by foundation tests |
| OpenAPI | Planned springdoc 2.8.17 | Published 2.8 patch; official compatibility matrix lists Boot 3.5 with springdoc 2.8 |
| JWT | Planned Spring Security JOSE encoder/decoder | Use Boot-managed libraries; configure stateless Bearer security in Phase 2 |
| Next.js | Exact 16.3.4 | Installed and verified with lint, typecheck and production build |
| React | Exact 19.2.8 | Installed with matching React DOM version and verified in the Next.js build |
| Node | 24.19.0 | Installed; Next 16.3.4 requires Node >=20.9 |
| Python | 3.11.9 | FastAPI 0.141.1 and scikit-learn 1.9.0 dependencies are pinned and tested |

Sources: [Boot requirements](https://docs.spring.io/spring-boot/3.5/system-requirements.html), [springdoc compatibility](https://springdoc.org/v2/), [Next security release](https://nextjs.org/blog/august-2026-security-release). Recheck advisories before frontend installation and deployment.

## Phase gates

Each phase must compile/build, pass relevant tests, start the relevant service, pass HTTP or browser verification, and update walkthrough/interview notes before the next phase.

| Phase | Implementation | Required evidence |
| --- | --- | --- |
| 0 | Inspect, tooling, compatibility, plan | Tool versions, no overwritten source |
| 1 | Maven, Boot, PostgreSQL, JPA/Flyway configuration, health | Maven verify; real HTTP health; test database connection |
| 2 | User/Role, migration, BCrypt, register/login, JWT, authorization, opt-in demo seeding | Valid/invalid login; missing/invalid/expired token 401; role denial 403 |
| 3 | Incident/enums, migration, DTOs, priority component, create/read/paged list | Nine priority combinations; persistence; own-ticket access |
| 4 | Assignment, explicit transitions, comments, activity, admin users/summary | Three roles; invalid transition 400; ownership; claim correctness then optimistic locking |
| 5 | SLA duration component, deadline and scheduled checker | Fixed-clock/past-deadline fixture; exactly one breach activity |
| 6 | Synthetic dataset, reproducible split/training/evaluation, FastAPI | Health/predict HTTP; measured held-out metrics; confidence range test |
| 7 | Dedicated Java HTTP client, bounded timeout, stored suggestion and fallback | Create with AI online and offline; incident persists in both cases |
| 8 | Next/React/TypeScript, login/register, list/create/detail, engineer actions, admin summary | Type/lint/build; real browser lifecycle against Java |
| 9 | Error format, security review, OpenAPI Bearer support, meaningful tests | Mockito service test; authenticated API persistence integration test; 400/401/403/404 |
| 10 | Dockerfiles, readable CI, final docs, hosting/environment configuration | Deploy all components and PostgreSQL; physically repeat final verification checklist |

## Clarifications without expanding scope

- Spring Boot 3 uses `jakarta.*` APIs, not legacy `javax.*` Java EE imports.
- Security starts in Phase 2; validation and readable errors start when request APIs appear, then receive a fuller review in Phase 9.
- Flyway is configured now. The first domain migration arrives with User in Phase 2; no dummy domain tables are necessary.
- Use PostgreSQL for integration tests. A separate test database and later CI PostgreSQL service avoid H2 differences and a Docker prerequisite.
- Support engineers claim unassigned incidents for themselves. Admins assign/reassign to support engineers. Employees close/reopen only their own resolved incidents. Authorization policy will be centralized.
- Assignment handles OPEN -> ASSIGNED. Reassignment preserves an active status. Resolved/closed incidents cannot be assigned until a valid reopen.
- AI is called before the database transaction; an unavailable service produces null suggestion/confidence. Category defaults to OTHER. Priority never comes from AI or the request.
- On reopen, retain the original SLA deadline; clear resolvedAt. Do not reset breach history. Resolved and closed incidents are excluded from SLA checks.
- Demo users require an explicit development profile and documented demo passwords; never seed them silently in production.
- Hosting credentials/access are not established. Select providers during delivery and request only missing account access or a required billing decision then. DEPLOYMENT PENDING.
