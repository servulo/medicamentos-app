# Deploy — Medicamentos App

## Compose

```bash
cp deploy/.env.example deploy/.env
# edit ADMIN_EMAIL, OIDC, VAPID as needed

docker compose -f deploy/docker-compose.yml up --build -d
```

- Frontend: http://localhost:4200  
- Backend API: http://localhost:8080/api/v1  
- Postgres (host): localhost:5433 → container 5432  

Se a porta 5432 já estiver ocupada no Windows, o Compose usa **5433** no host.

Optional TLS profile: `docker compose --profile tls -f deploy/docker-compose.yml up -d`

## Auth modes

- **Test mode** (`APP_AUTH_TEST_MODE=true`): frontend sends `X-Test-User-Email`. Useful for local/demo without Google.
- **Production**: set `APP_AUTH_TEST_MODE=false` and configure Google OIDC env vars.

## Web Push (iOS)

iOS Safari requires the PWA added to the Home Screen (Safari ≥ 16.4) for Web Push.
Generate VAPID keys and set `VAPID_PUBLIC_KEY` / `VAPID_PRIVATE_KEY`.

## SC-002 check

After ≥10 successful dose pushes in a controlled run:

```sql
SELECT
  COUNT(*) FILTER (WHERE success AND sent_at <= scheduled_at + interval '1 minute') * 100.0
  / NULLIF(COUNT(*) FILTER (WHERE success), 0) AS pct_within_1min
FROM notification_log
WHERE sent_at > now() - interval '1 day';
```

See also `specs/001-medication-control/quickstart.md`.
