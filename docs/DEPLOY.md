# Deploying to Google Cloud Run

The backend ships as a Docker image (`Dockerfile` in the project root) and runs on **Google Cloud Run**, inside its
always-free monthly allowance at our traffic: 180,000 vCPU-seconds, 360,000 GiB-seconds and 2 million requests.

- The container **sleeps when idle** and wakes on the next request (a cold start of several seconds for Spring Boot).
- Nothing in the image is secret: passwords and keys come from **Secret Manager** at runtime.
- The image is built **in Google Cloud** (Cloud Build), so Docker is not needed on your Mac.

> Cloud Run needs a billing account with a card, even inside the free allowance. Create a **budget alert** first
> (step 1), so any unexpected cost is noticed straight away.

## 0. Before you start

- The database stays on **Supabase**. Note your Supabase region (it is in the pooler host of `DB_URL`, for example
  `aws-0-eu-west-3`) and pick the Google region closest to it, so every query stays fast.
- Use **Cloud Shell** (the `>_` button at the top of console.cloud.google.com): a terminal in the browser with `gcloud`
  already installed and logged in. Everything below runs there.

## 1. Project, APIs and budget (once)

```bash
gcloud projects create lavrel-backend-prod --name="Lavrel"
gcloud config set project lavrel-backend-prod
```

Link the project to a billing account in the console (Billing → Link a billing account), then:

```bash
gcloud services enable run.googleapis.com artifactregistry.googleapis.com \
    cloudbuild.googleapis.com secretmanager.googleapis.com
```

**Budget alert:** Billing → Budgets & alerts → Create budget → amount `5` (EUR or USD) → alerts at 50 %, 90 % and 100 %.

Choose the region once (example: Belgium; use the one closest to Supabase):

```bash
export REGION=europe-west1
gcloud artifacts repositories create lavrel --repository-format=docker --location=$REGION
```

## 2. Secrets (once, and whenever one changes)

Upload the Firebase service-account JSON (Cloud Shell's ⋮ menu → Upload), then:

```bash
gcloud secrets create firebase-sa --data-file=firebase-service-account.json
rm firebase-service-account.json
printf '%s' 'YOUR_DB_PASSWORD'      | gcloud secrets create db-password --data-file=-
printf '%s' 'YOUR_STEAM_API_KEY'    | gcloud secrets create steam-api-key --data-file=-
printf '%s' 'YOUR_STATE_SECRET'     | gcloud secrets create steam-state-secret --data-file=-
```

Let Cloud Run read them (it runs as the project's default compute service account):

```bash
PROJECT_NUMBER=$(gcloud projects describe $(gcloud config get-value project) --format='value(projectNumber)')
for s in firebase-sa db-password steam-api-key steam-state-secret; do
  gcloud secrets add-iam-policy-binding $s \
    --member="serviceAccount:${PROJECT_NUMBER}-compute@developer.gserviceaccount.com" \
    --role="roles/secretmanager.secretAccessor"
done
```

To change a secret later: `printf '%s' 'NEW_VALUE' | gcloud secrets versions add db-password --data-file=-`, then
deploy again (step 4).

## 3. Build the image

```bash
git clone https://github.com/hectordev4/lavrel-backend.git
cd lavrel-backend
git checkout v0.1.1
export IMAGE=$REGION-docker.pkg.dev/$(gcloud config get-value project)/lavrel/backend:v0.1.1
gcloud builds submit --tag $IMAGE
```

Next time: `git fetch --tags && git checkout <new tag>`, update `IMAGE` to the new tag, build again.

## 4. Deploy

```bash
gcloud run deploy lavrel-backend \
  --image=$IMAGE --region=$REGION \
  --allow-unauthenticated \
  --memory=1Gi --cpu=1 --cpu-boost \
  --min-instances=0 --max-instances=2 \
  --set-env-vars="DB_URL=jdbc:postgresql://YOUR_POOLER_HOST:5432/postgres?sslmode=require,DB_USER=postgres.YOUR_PROJECT_REF,FIREBASE_CREDENTIALS_PATH=/secrets/firebase/service-account.json,CORS_ALLOWED_ORIGINS=http://localhost:4200" \
  --set-secrets="DB_PASSWORD=db-password:latest,STEAM_API_KEY=steam-api-key:latest,STEAM_STATE_SECRET=steam-state-secret:latest,/secrets/firebase/service-account.json=firebase-sa:latest"
```

- `--allow-unauthenticated` makes the URL reachable from the internet; our own security (Firebase tokens, roles) still
  protects everything that is not public.
- `--min-instances=0` keeps it free (it sleeps when idle). `--max-instances=2` caps the cost if something goes wrong.
- `--cpu-boost` gives extra CPU while Spring Boot starts, which shortens cold starts.

The command prints the service URL, e.g. `https://lavrel-backend-xxxxx.a.run.app`. Point the Steam callback at it:

```bash
export URL=$(gcloud run services describe lavrel-backend --region=$REGION --format='value(status.url)')
gcloud run services update lavrel-backend --region=$REGION \
  --update-env-vars="STEAM_CALLBACK_URL=$URL/api/steam/callback"
```

When the frontend is online, add `FRONTEND_BASE_URL=https://your-frontend` and its origin to `CORS_ALLOWED_ORIGINS`
the same way (several origins are comma-separated; use `--update-env-vars=^@^KEY=a,b` so the comma is not taken as
a separator).

## 5. Check it

```bash
curl -i $URL/actuator/health          # {"status":"UP"}
curl -s "$URL/api/games?size=3"        # public search
```

In Postman, set `baseUrl` to the service URL and run requests 1, 2 and 39.

Logs: console → Cloud Run → lavrel-backend → Logs (or `gcloud run services logs read lavrel-backend --region=$REGION`).

## Running the image locally (optional)

Only if Docker runs on your machine:

```bash
docker build -t lavrel-backend .
docker run --rm -p 8080:8080 --env-file .env \
  -e FIREBASE_CREDENTIALS_PATH=/secrets/firebase-service-account.json \
  -v "$PWD/secrets:/secrets:ro" lavrel-backend
```

## Costs to keep an eye on

| Item | Free allowance | At our size |
|---|---|---|
| Cloud Run compute and requests | 180,000 vCPU-s, 360,000 GiB-s, 2 M requests per month | Free while traffic is low; the container only runs while handling requests |
| Artifact Registry (images) | 0.5 GB storage | Keep 2–3 image versions; delete older ones |
| Cloud Build | 2,500 build-minutes per month | A build takes a few minutes |
| Secret Manager | 6 secret versions, 10,000 accesses per month | 4 secrets |
| Outbound traffic (to Supabase and users) | 1 GB per month (North America) | Small JSON responses; watch it as users grow |
