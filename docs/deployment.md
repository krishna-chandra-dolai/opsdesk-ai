# Deployment

Deployment is pending. A GitHub upload is source publication, not a live deployment.

The repository contains three Dockerfiles and GitHub Actions jobs that test the application and build each image. Docker is not installed on the development workstation; container verification runs in GitHub Actions.

## Hosting configuration

Use managed PostgreSQL and three web services. Render supports Docker builds directly from a GitHub repository; Next.js can also use Vercel. Create services only after reviewing the provider's current prices. No hosting subscription is assumed or purchased by this project.

| Service | Build context | Port | Health check |
| --- | --- | --- | --- |
| Backend | `backend/` | 8080 | `/actuator/health` |
| Classifier | `ai-service/` | 8000 | `/health` |
| Frontend | `frontend/` | 3000 | `/login` |

1. Create a managed PostgreSQL database. Set backend `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USERNAME`, and `DB_PASSWORD` from its connection details. For hosted PostgreSQL, set `SPRING_DATASOURCE_URL=jdbc:postgresql://<host>:5432/<database>?sslmode=require` to override the assembled URL; keep credentials in separate environment variables. Use the provider's required TLS settings and private networking where available.
2. Deploy the classifier with its Dockerfile. Model training happens during image construction using the committed synthetic dataset.
3. Deploy the backend with `JWT_SECRET` (at least 32 cryptographically random bytes), `JWT_EXPIRATION=3600`, `AI_SERVICE_URL`, `AI_SERVICE_TIMEOUT_MS=1500`, and the exact HTTPS `FRONTEND_ORIGIN`. Use the classifier's private URL when available. Set `SERVER_PORT` if the host requires a different port.
4. Deploy the frontend with `NEXT_PUBLIC_API_URL` set to the backend's public HTTPS URL **at build time**. In Docker, pass it as a build argument. Changing it requires rebuilding. The standalone server listens on `PORT` (default 3000).
5. Keep `SPRING_PROFILES_ACTIVE=dev` disabled in public hosting. Register real demonstration users with unique passwords, then promote only the intended engineer/admin accounts through controlled database administration. Never publish working production passwords.
6. Verify login, create, assignment, comment, resolve, reopen, close, summary, persistence across restart, and 400/401/403/404 responses against the deployed URLs. Verify AI outage fallback in a staging environment by temporarily pointing `AI_SERVICE_URL` at an unavailable address, then restoring it.

The backend applies Flyway migrations and validates the resulting schema at startup. Back up a persistent database before deploying a migration. A failing health check is not a successful deployment.

References: [Render Docker](https://render.com/docs/docker), [Render health checks](https://render.com/docs/health-checks), [Next.js self-hosting](https://nextjs.org/docs/app/guides/self-hosting).

## Free portfolio deployment

The selected path is three Render Free web services and a Neon Free PostgreSQL database. No paid plan, subscription or payment method is authorized. Verify the displayed plan before creating each resource.

| Render service | Dockerfile path | Docker build context | Additional environment |
| --- | --- | --- | --- |
| Classifier | `ai-service/Dockerfile` | `ai-service` | `PORT=8000` |
| Backend | `backend/Dockerfile` | `backend` | `PORT=8080`, `SERVER_PORT=8080` |
| Frontend | `frontend/Dockerfile` | `frontend` | `PORT=3000`, `NEXT_PUBLIC_API_URL=<backend HTTPS URL>` |

Import the public repository URL when creating each service. Render passes configured environment variables as Docker build arguments; the frontend Dockerfile declares NEXT_PUBLIC_API_URL. Set its value before the first build. Use the exact assigned frontend URL for backend FRONTEND_ORIGIN and the classifier's public HTTPS URL for AI_SERVICE_URL. Free services do not provide private-network inbound access.

For Neon, create a Free project and copy the connection host, database and role into the backend environment. Keep the password only in the host's secret configuration. Use a JDBC URL, not a psql connection command. Never commit connection credentials. Leave the development profile and demo seeder disabled.

Render Free services sleep after inactivity and share a monthly instance-hour allowance. This is suitable for an occasional portfolio demonstration, not a promise of continuous availability. Before an interview demo, open both service health endpoints and wait until they respond. A sleeping classifier can exceed the 1.5-second timeout; incident creation still succeeds with no suggestion. Scheduled SLA checks run only while the Java service is awake and catch overdue incidents after it resumes. Do not use artificial keep-alive traffic to evade free-tier limits.

Render's free PostgreSQL offering expires after 30 days, so it is not used for the persistent portfolio database. Recheck [Render Free limits](https://render.com/docs/free) and [Neon Free pricing](https://neon.com/pricing) when provisioning. The free plans remain subject to provider quotas and changes.

Provisioning status: account setup is in progress. No hosted service URL or successful deployment is claimed yet.
