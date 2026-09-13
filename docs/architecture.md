# Architecture

Current implementation: Spring Boot owns users, security, incidents, workflow, SLA and audit history in PostgreSQL. A separate FastAPI service classifies text. One Next.js application provides role-aware employee, engineer and admin screens.

The browser calls Spring Boot directly using the configured API URL. CORS permits one configured frontend origin and only the required methods and headers. UI role checks control navigation and buttons; every sensitive backend operation independently enforces its role and ownership rules.

Spring Security authenticates login credentials through a database-backed `UserDetailsService`. Login creates a short-lived HMAC-SHA256 token containing the user email as subject and one signed role claim. On later requests, Spring's resource-server support verifies the signature, issuer and timestamps before creating the authenticated principal. Controllers never receive a password hash.

The intended architecture is one Next.js UI calling a Spring Boot business application. Java stores data in PostgreSQL and calls a separate Python FastAPI classifier over REST.

Spring Boot owns authorization, incident transitions, priority, SLA and persistence because these are the business rules and the focus of this project. PostgreSQL provides transactions, foreign keys and ordinary relational queries for users, incidents, comments and activities.

Python is separated only for its ML tooling. A small synchronous REST call with a bounded timeout is sufficient for a category suggestion. Failure must produce an empty suggestion while Java still saves the incident. The network request should not hold a database transaction open.

Priority follows an explicit impact/urgency matrix: predictable, auditable and unit-testable. The classifier has no authority over it.

There is no message broker or collection of Java services because this MVP does not need asynchronous infrastructure or additional deployment boundaries. It is a core business service with a separate ML service, not a large microservices system.

`IncidentCreationService` calls Python before delegating to the transactional `IncidentService`. A timeout or connection error becomes an empty optional prediction; the database operation proceeds. This avoids keeping a transaction open during network I/O.
