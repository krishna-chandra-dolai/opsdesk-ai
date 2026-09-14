# Code walkthrough

## Phase 1 — startup

`OpsDeskApplication.main` -> `SpringApplication.run` -> Spring Boot autoconfiguration -> Hikari datasource -> Flyway initialization -> Hibernate EntityManagerFactory -> embedded Tomcat.

`application.yml` takes database credentials from environment variables. Hibernate validates rather than creates tables. No entity classes or domain migrations exist in this phase.

## Phase 1 — health request

HTTP `GET /actuator/health` -> Boot's actuator web endpoint -> registered health indicators (including datasource) -> HTTP 200 with `{"status":"UP"}` when dependencies are healthy.

There is no custom health controller because Actuator already provides this behavior. The configured exposure list contains only health; `/actuator/env` is unavailable.

## Phase 1 — verification

`BackendFoundationTest` -> `@SpringBootTest` starts a real server on a random port -> `application-test.yml` selects a separate PostgreSQL database -> `TestRestTemplate` makes health and restricted-endpoint requests -> `JdbcTemplate` verifies the connected database name.

This is the foundation test, not yet the required authenticated incident creation test. That test arrives after authentication and incidents exist.

## Phase 2 — registration

HTTP `POST /api/auth/register` -> `AuthController` -> Bean Validation -> `AuthService.register` -> normalize email -> check uniqueness -> BCrypt encode -> construct EMPLOYEE `User` -> `UserRepository.save` -> `UserResponse`.

The transaction covers the uniqueness check and insert. The database unique constraint is the final protection against concurrent duplicate registration. `UserResponse` has no password field.

## Phase 2 — login and authenticated request

HTTP `POST /api/auth/login` -> `AuthController` -> `AuthService.login` -> Spring `AuthenticationManager` -> database-backed `UserDetailsService` -> BCrypt comparison -> `JwtService.createToken` -> `LoginResponse`.

Later: `Authorization: Bearer <token>` -> Spring resource-server filter -> verify HS256 signature, issuer and expiry -> translate signed `role` claim into `ROLE_*` authority -> controller. Missing, malformed and expired tokens stop at the filter with 401. A valid employee token reaching `UserController.listUsers` fails `@PreAuthorize("hasRole('ADMIN')")` with 403.

## Phase 2 — development users

Starting with the `dev` profile enables `DemoUserInitializer`. It creates missing employee, engineer and admin accounts with BCrypt hashes. The initializer is idempotent and disabled by default.

## Phases 3–5 — incident lifecycle

Create: `IncidentController` → `IncidentCreationService` → `IncidentService` → `PriorityCalculator` and `SlaPolicy` → repositories → safe `IncidentResponse`. Creation and its initial activity records share one transaction.

Assignment/status: `IncidentController` → `IncidentWorkflowService` → `IncidentAccess` → `IncidentTransitionValidator` → entity change → activity record. `@Version` detects competing updates. Employees see only their tickets; support and admins can read the queue. Employee close/reopen and engineer work transitions are checked independently of the UI.

Comments and timeline use separate controllers and DTOs. Both first call `IncidentAccess`, so ticket visibility applies consistently. Activity records have no update/delete API.

SLA: creation stores the deadline from `SlaPolicy`. `SlaBreachChecker` runs on a configurable interval, finds incomplete overdue rows with `slaBreached=false`, marks them and records one system activity in a transaction.

## Phases 6–7 — classification and fallback

`train.py` reads the synthetic CSV → stratified train/test split → TF-IDF → logistic regression → measured test metrics → `model.joblib`. FastAPI loads that model and `/predict` returns the class with the largest genuine `predict_proba` value.

Java creation: request → `IncidentCreationService` → `AiClassificationClient` with connection/read timeout → optional `AiPrediction` → transactional `IncidentService`. On connection/timeout failure, the client logs one safe warning and supplies no prediction; incident persistence continues.

## Phase 8 — browser flow

`/login` → `api()` sends credentials → `saveSession()` stores the short-lived token and safe user → role landing page. `AppShell` reads the session through `useSyncExternalStore`, supplies role navigation and clears the session on sign-out. A backend 401 also clears it.

Employee: `/incidents` → paged API → `/incidents/new` → create → `/incidents/[id]`. Engineer uses the same detail page to claim, start work, comment and resolve. Employee sees close/reopen actions only when RESOLVED. Admin sees real summary cards and a support-engineer assignment selector. The API still decides whether each action is permitted.

## Phase 9 — API documentation and errors

`OpenApiConfig` describes the API and registers the JWT Bearer scheme → springdoc inspects controllers and DTOs → `/v3/api-docs` returns the contract → Swagger UI renders it and sends an entered token as `Authorization: Bearer ...`. `AuthController` removes the global security requirement from public register/login operations.

Controller exception → `GlobalExceptionHandler` → matching explicit handler → `ApiError`. Validation and malformed input become 400, access denial becomes 403, missing resources become 404, and an unexpected exception is logged with details only on the server before the client receives a generic 500. Authentication failures that occur before a controller use the same `ApiError` shape from `SecurityConfig`.

## Delivery checks

The GitHub Actions workflow runs the backend against a separate PostgreSQL database, trains/tests the classifier, checks/builds the frontend, and builds all three Docker images. Follow docs/verification.md for actual results. Hosting remains pending.

