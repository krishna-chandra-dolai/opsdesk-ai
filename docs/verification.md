# Verification record

## Phase 0–1 — 2026-09-09

- Empty workspace inspected before creation; no existing source overwritten.
- Portable Temurin Java 21.0.12.1 and Maven 3.9.11 executable versions verified.
- Publisher checksums matched downloaded JDK and Maven archives.
- PostgreSQL 17.11 isolated on loopback port 55432; separate `opsdesk` and `opsdesk_test` databases.
- `mvn -B verify`: BUILD SUCCESS. Two tests run, zero failures/errors/skips.
- Foundation test starts a real server, checks health, queries actual test database name, and checks `/actuator/env` is unavailable.
- Packaged JAR started separately against `opsdesk` on port 8080.
- Actual `GET /actuator/health`: 200, status UP.
- Actual `GET /actuator/env`: 404.
- SQL confirmed application database and Flyway schema history table.
- Resolved runtime includes Hibernate 6.6.53.Final and Flyway 11.7.2 through Boot dependency management.

Expected current warnings: Flyway reports no migrations because domain entities arrive in Phase 2. The test runtime reports Java's dynamic agent loading warning from the Mockito/Byte Buddy test dependency. Neither warning was suppressed, and neither is a test failure.

One setup command failed because the temporary database password file was not yet created. After creating the random password file, initialization and startup succeeded. No application compilation or test failure occurred.

## This workstation

The application and isolated PostgreSQL instance were left running after verification. The portable tooling and private local password live in workspace `work/`, outside this deliverable repository. From the original workspace PowerShell directory, run:

```powershell
. ./work/enter-local-env.ps1
mvn verify
```

The helper exports local credentials without printing them and moves into `backend/`. To restart the packaged backend after stopping the current instance:

```powershell
java -jar target/opsdesk-backend-0.1.0-SNAPSHOT.jar
```

Runtime logs and the backend PID are in workspace `work/`. These workstation helpers are not a replacement for the portable README setup instructions.

## Remaining after Phase 1

At that gate, Phases 2–10 were not implemented and no authenticated workflow had been tested.

## Phase 2 — 2026-09-09

- `mvn -B verify`: BUILD SUCCESS. Six tests run, zero failures/errors/skips.
- Tests use PostgreSQL and verify registration, validation, case-insensitive email uniqueness, BCrypt matching, login, current user, EMPLOYEE -> admin 403, and missing/invalid/expired token 401.
- Packaged JAR started with the `dev` profile against the development database.
- Actual HTTP: health UP; employee login and `/api/users/me` succeeded; employee `/api/users` returned 403; admin user listing returned all three demo roles; missing token, invalid token and invalid password returned 401.
- SQL showed one account for each role and 60-character `$2a$` BCrypt hashes. No hashes or tokens were printed to the verification record.

The first Phase 2 test attempt failed because the isolated PostgreSQL process had stopped. It was restarted and the suite rerun. The next run exposed an actual JWT defect: the encoder defaulted to an incompatible signing algorithm. `JwtService` now explicitly requests HS256. The security expectation for an unmapped Actuator path was corrected to 401 because the filter protects it before handler mapping. The full suite then passed.

## Remaining after Phase 2

At that gate, Phases 3–10 were not implemented and incident workflow was next.

## Phases 3–7 — 2026-09-09

- Java: 22 tests passed with PostgreSQL. The suite includes all nine priority combinations, Mockito incident creation, authenticated API persistence, role visibility, the complete lifecycle, comments/activity, admin summary and SLA behavior.
- Live workflow: calculated priority, claim, progress, comment, resolve, reopen, resume, resolve and close all succeeded. CLOSED → IN_PROGRESS returned 400. Admin summary used database counts.
- Live SLA: a LOW incident received 24 hours; a development row moved into the past was marked breached with exactly one system activity.
- Python: 2 tests passed. Measured 48-row synthetic split: accuracy 0.5833, macro precision 0.6667, macro recall 0.5833, macro F1 0.5833.
- Live AI online: NETWORK with calculated confidence 0.26261415549886097; incident returned 201.
- Live AI offline: incident returned 201, persisted, and had null AI category/confidence.

## Phase 8 — 2026-09-09

- Pinned Next.js 16.3.4, React 19.2.8, TypeScript 5.9.3 and ESLint 9.39.5. npm audit reported zero vulnerabilities.
- `npm run typecheck`, `npm run lint` and `npm run build` passed. Eight App Router routes were generated.
- Production server returned 200 for login, register, incidents, create incident and admin pages.
- Backend `mvn verify`: 22 tests passed after CORS was added.
- CORS preflight from `http://localhost:3000` returned 200 with that exact allowed origin.
- Backend, AI and production frontend were started together locally.
- Windows browser automation could not run because its URL-policy enforcement does not support the installed Firefox surface. Rendered browser interaction is therefore not claimed; HTTP route checks, frontend build checks and the previously verified API lifecycle passed.

## Phase 9 — 2026-09-09

- Added springdoc OpenAPI 2.8.17 with a JWT Bearer scheme and public authentication operations.
- Swagger UI returned 200 at `/swagger-ui/index.html`; `/v3/api-docs` returned 11 paths and 16 schemas.
- Security-filter and controller errors now share the `ApiError` shape with timestamp, status, message and field errors.
- Malformed JSON/enum values return 400, missing resources and unmapped API routes return 404, and unexpected exceptions are logged server-side then return a generic 500 without a stack trace.
- `mvn verify`: BUILD SUCCESS. 23 tests passed, zero failures/errors/skips, against PostgreSQL 17.11.
- Live backend health was UP, an unauthenticated incident request returned the standardized 401 response, and all six checked frontend routes returned 200.
- Google Chrome could not be opened through Codex because the browser connector returned `Browser is not available: chrome` and listed only the Codex in-app browser. Chrome rendering is not claimed. The local URLs are ready once Chrome is connected to Codex.

Remaining: Phase 10 (containers, CI, delivery documentation and deployment). DEPLOYMENT PENDING.
