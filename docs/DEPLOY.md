# Deploying to Render

The backend runs on **Render** as a Docker web service, on the **Free** instance type:
<https://lavrel.onrender.com>. Render builds the image from the `Dockerfile` in the repository, so Docker is not needed
locally.

- **Branch `main`**, auto-deploy **On Commit**: every merged pull request into `main` is built and deployed.
- Nothing secret is in the image or in git: passwords and keys are Render environment variables, and the Firebase key is
  a Render secret file.
- The database stays on **Supabase**.

## Service settings

| Field | Value |
|---|---|
| Type | **Web Service** (not Static Site) |
| Language | **Docker** (Build Command, Docker Command and Pre-Deploy Command stay empty) |
| Dockerfile Path / Build Context | `./Dockerfile` / `.` |
| Region | Frankfurt |
| Branch | `main` |
| Instance type | Free |
| Health Check Path | `/actuator/health` |

## Environment variables

| Key | Value |
|---|---|
| `DB_URL`, `DB_USER`, `DB_PASSWORD` | Supabase **Session pooler** values, as in the local `.env` |
| `STEAM_API_KEY`, `STEAM_STATE_SECRET` | As in the local `.env` |
| `STEAM_CALLBACK_URL` | `https://lavrel.onrender.com/api/steam/callback` |
| `FIREBASE_CREDENTIALS_PATH` | `/etc/secrets/firebase-service-account.json` (**not** the local `./secrets/...` path) |
| `CORS_ALLOWED_ORIGINS` | `http://localhost:4200`, plus the frontend's URL once it is online (comma-separated) |
| `FRONTEND_BASE_URL` | Empty until the frontend is online |
| `PORT` | `10000` |
| `JAVA_TOOL_OPTIONS` | `-XX:MaxRAMPercentage=60 -XX:MaxMetaspaceSize=128m -XX:ReservedCodeCacheSize=48m -Xss512k -XX:TieredStopAtLevel=1 -XX:+UseSerialGC -XX:+ExitOnOutOfMemoryError` |

`JAVA_TOOL_OPTIONS` replaces the default baked into the image: it keeps Java inside the free instance's 512 MB and
makes startup lighter on its small CPU. Never set `FAKE_DATA_ENABLED` or `app.dev.verify-email-uids` here.

**AI assistant (optional):** create a free Cloudflare account, then My Profile → API Tokens → Create Token → Custom
token with **Account → Workers AI → Read** for your account only (optionally restricted to Render's outbound IPs:
service → Connect → Outbound). Add `CLOUDFLARE_ACCOUNT_ID` and `CLOUDFLARE_AI_TOKEN` in Render. Use a separate token
for local development, so one can be revoked without breaking the other. Never paste tokens anywhere else. Without
them the assistant answers `503 assistant_unavailable` and everything else works.

**Avatar uploads (optional):** in Supabase, Storage → **New bucket** `avatars`, marked **Public**. Then add two
environment variables in Render: `SUPABASE_URL` (Project Settings → API → Project URL) and `SUPABASE_SERVICE_KEY`
(the `service_role` key on the same page; a secret, never put it in the frontend). Without them everything works except
uploads, which answer `503 avatar_storage_unavailable`.

**Secret file:** Environment → Secret Files → `firebase-service-account.json`, with the whole content of the local
`secrets/firebase-service-account.json`. Render places it at `/etc/secrets/firebase-service-account.json`.

## Checking a deploy

The log ends with `Started LavrelApplication`. Then:

```bash
curl -i https://lavrel.onrender.com/actuator/health        # {"status":"UP"}
curl -s "https://lavrel.onrender.com/api/games?size=3"      # public search
```

In Postman, set `baseUrl` to `https://lavrel.onrender.com` and run requests 1, 2 and 39.

## Releasing

1. Work on `dev` (or a `feat/*` branch merged into `dev`); run `mvn clean test`.
2. Open a pull request `dev` → `main` and merge it with **Create a merge commit**.
3. Render deploys `main` automatically. Tag the release: `git tag -a v0.x.y -m "..."`, `git push origin v0.x.y`.

Flyway applies new migrations when the app starts, so a deploy with a new `V…sql` updates the Supabase schema by itself.

## Free plan behaviour

- The service **sleeps after 15 minutes without requests**; the next request waits while it starts again (up to a
  minute or two for Spring Boot on the free CPU).
- To keep it awake, an external monitor calls the health check every 10 minutes. One service running all month uses
  about 720–745 of the 750 free hours, so do this for one service only. With **UptimeRobot** (free):
  1. Sign up at uptimerobot.com → **New monitor**.
  2. Type **HTTP(s)**, URL `https://lavrel.onrender.com/actuator/health`, interval **10 minutes** (or 5, the free
     minimum, if the first visit still finds it asleep).
  3. Optionally add your e-mail as alert contact: it then also tells you when the backend is down.
  The health check is not rate limited, so the monitor never gets a 429.

## Troubleshooting

| Symptom | Cause and fix |
|---|---|
| Fields "Build Command" and "Publish Directory" | You are creating a Static Site. Use **New → Web Service** with language **Docker** |
| `./secrets/firebase-service-account.json (No such file or directory)` | `FIREBASE_CREDENTIALS_PATH` still has the local path. Set `/etc/secrets/firebase-service-account.json` |
| `Permission denied` on `/etc/secrets/...` | The container runs as a non-root user and cannot read the file; read the key from an environment variable instead (small code change) |
| Restarts with "Out of memory" | Lower `MaxRAMPercentage` in `JAVA_TOOL_OPTIONS` to 50 |
| Steam link fails with `invalid_identity_proof` | `STEAM_CALLBACK_URL` must be the Render URL above, not `localhost` |
| Browser calls blocked by CORS | Add the frontend's origin to `CORS_ALLOWED_ORIGINS` |
