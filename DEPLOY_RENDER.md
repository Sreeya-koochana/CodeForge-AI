# Deploy to Render

This repo is now set up to deploy as a single Render web service:

- React frontend is built inside the Docker image.
- Spring Boot serves the built frontend files.
- Render only needs one Docker-based web service.

## Before you deploy

You should have:

- a GitHub repo with the latest project pushed
- your Aiven MySQL connection values
- your OpenAI API key

Important:

- This app is configured for MySQL via the `aiven` Spring profile.
- Render Postgres will not work with the current backend config without code changes.

## Required Render environment variables

Set these in Render:

- `SPRING_PROFILES_ACTIVE=aiven`
- `DB_URL=jdbc:mysql://YOUR-HOST:YOUR-PORT/YOUR-DB?sslMode=REQUIRED&enabledTLSProtocols=TLSv1.2,TLSv1.3&serverTimezone=UTC`
- `DB_USERNAME=your-db-username`
- `DB_PASSWORD=your-db-password`
- `OPENAI_API_KEY=your-openai-api-key`
- `JWT_SECRET=any-long-random-secret-at-least-32-characters`
- `CORS_ALLOWED_ORIGINS=https://your-render-domain.onrender.com`

## Deploy with Blueprint

1. Push this repo to GitHub.
2. In Render, click `New +`.
3. Choose `Blueprint`.
4. Select this GitHub repo.
5. Render will detect `render.yaml`.
6. Fill in the secret environment variables when prompted.
7. Create the service.

Render will build the root `Dockerfile`, start the Spring Boot app, and expose it publicly.

## Deploy manually without Blueprint

1. In Render, click `New +` -> `Web Service`.
2. Connect your GitHub repo.
3. Set `Runtime` to `Docker`.
4. Set the Dockerfile path to `./Dockerfile`.
5. Add the environment variables listed above.
6. Deploy.

## After deploy

Open:

- `/` for the frontend
- `/api/health` for the health check

Example:

- `https://your-service-name.onrender.com`
- `https://your-service-name.onrender.com/api/health`

## Notes

- Render sets the `PORT` environment variable automatically, and the backend now respects it.
- If you change the frontend, Render will rebuild the Docker image and rebundle the UI into Spring Boot.
- If CORS causes trouble, make sure `CORS_ALLOWED_ORIGINS` exactly matches your Render app URL.
