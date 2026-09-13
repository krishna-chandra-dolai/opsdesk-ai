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

1. Create a managed PostgreSQL database. Set backend `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USERNAME`, and `DB_PASSWORD` from its connection details. Use the provider's required SSL settings; a JDBC URL query can be appended to `DB_NAME` if required by the provider (for example `opsdesk?sslmode=require`). Prefer private database networking.
2. Deploy the classifier with its Dockerfile. Model training happens during image construction using the committed synthetic dataset.
3. Deploy the backend with `JWT_SECRET` (at least 32 cryptographically random bytes), `JWT_EXPIRATION=3600`, `AI_SERVICE_URL`, `AI_SERVICE_TIMEOUT_MS=1500`, and the exact HTTPS `FRONTEND_ORIGIN`. Use the classifier's private URL when available. Set `SERVER_PORT` if the host requires a different port.
4. Deploy the frontend with `NEXT_PUBLIC_API_URL` set to the backend's public HTTPS URL **at build time**. In Docker, pass it as a build argument. Changing it requires rebuilding. The standalone server listens on `PORT` (default 3000).
5. Keep `SPRING_PROFILES_ACTIVE=dev` disabled in public hosting. Register real demonstration users with unique passwords, then promote only the intended engineer/admin accounts through controlled database administration. Never publish working production passwords.
6. Verify login, create, assignment, comment, resolve, reopen, close, summary, persistence across restart, and 400/401/403/404 responses against the deployed URLs. Verify AI outage fallback in a staging environment by temporarily pointing `AI_SERVICE_URL` at an unavailable address, then restoring it.

The backend applies Flyway migrations and validates the resulting schema at startup. Back up a persistent database before deploying a migration. A failing health check is not a successful deployment.

References: [Render Docker](https://render.com/docs/docker), [Render health checks](https://render.com/docs/health-checks), [Next.js self-hosting](https://nextjs.org/docs/app/guides/self-hosting).
