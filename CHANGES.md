# Refactor: platform dispatch, SOLID cleanup, validation and errors

Replace `src/main/java/com/lavrel` with the folder in this package (commit first), so deleted files go too.
Copy `src/test/java` over yours as well.

## Deleted
- `achievements/`, `domain/Platform.java`, `steam/`: empty leftovers duplicating `progress/`,
  `shared/domain/Platform` and `platform/adapter/out/steam`.
- `platform/adapter/out/steam/SteamClient.java`: split into `SteamLibraryClient` and `SteamCatalogClient`.
- `SteamIdentityPort`, `CreateSteamLinkUrlUseCase/Service`, `LinkSteamAccountUseCase/Service`, `SteamLinkController`:
  replaced by the platform-generic linking classes.
- `ImportSteamCatalogUseCase` and `ImportSteamCatalogService`: split, see "Steam catalog import" below.

## Platform dispatch
- `shared/domain/PlatformRegistry` picks the implementation for a platform; `PlatformBound` marks per-platform classes.
- Per-platform ports: `PlatformLibraryPort`, `PlatformCatalogPort`, `PlatformIdentityPort`, `PlatformGameImporter`.
- Adding Xbox means implementing those ports with `platform() == XBOX`. No service or controller changes.
- A platform with nothing wired up -> `PlatformNotSupportedException` -> 422 `platform_not_supported`.
- The link state is now bound to the platform it was issued for.

## Steam catalog import (was one 500-line class)
Everything lives in `catalog/application/service/steam/`. Behaviour is unchanged: same Steam calls, same order,
same remembered failures.

Use cases (one per admin concern, so the controller depends only on what it calls):

| Port (`port/in`) | Service | What it does |
|---|---|---|
| `SeedSteamCatalogUseCase` | `SeedSteamCatalogService` | seed a list, most played, resumable walk |
| `EnrichGamesUseCase` | `EnrichGamesService` | details for games that never got them |
| `RefreshGamesUseCase` | `RefreshGamesService` | manual re-copy of published games |
| `ImportPlatformGamesUseCase` | `ImportPlatformGamesService` -> `ImportSteamGamesService` | account syncs |

Shared pieces (package-private):

| Class | Responsibility |
|---|---|
| `SteamGameScreening` | cheap and full rejection checks (type, content policy, min reviews); no I/O |
| `SteamImportRun` | the batch loop: known/failed apps are free, store items 25 per call, attempt budget, resume point |
| `SteamGameReprocessor` | re-checks existing games; enrichment and refresh only differ in which games they pick |
| `SteamGameWriter` | writes a game, mapping, achievements and details in one transaction; republishes |
| `SteamTagNames` | Steam tag names; one shared bean so all paths together ask Steam at most once an hour |
| `GameMetadataAssembler` | turns store item + store page into `GameMetadata`; pure functions |
| `ImportMode` | `SYNC` (no popularity check) or `BULK` (min reviews) |

New tests: `SteamGameScreeningTest`, `SteamImportRunTest` (Mockito, from `spring-boot-starter-test`).

## Endpoints
- `POST /api/currentuser/accounts/{platform}/link` (was `/accounts/steam/link`; `steam` still works as the value)
- `GET  /api/platforms/{platform}/callback` (new), `GET /api/steam/callback` (kept)
- Admin catalog endpoints: same paths and bodies. Bodies are now validated: a `limit` above 200 or a
  non-numeric app id is a 400 `validation_failed` (it used to be clamped or skipped silently).

## Errors
- `ApiError` gains `details` (one entry per invalid field), left out of the JSON when empty.
- `GlobalExceptionHandler` is grouped by status: 400, 404, 409, 422, 502.

## Required changes outside the code
- `pom.xml`: `spring-boot-starter-validation` (already present).
- `application.yml`: remove `app.catalog.content-filter`, `app.catalog.sexual-tag-top-rank`,
  `app.catalog.sexual-tag-weight-ratio`. Optional: rename `app.steam.state-secret` / `state-ttl` to
  `app.link.state-secret` / `state-ttl` (the old keys still work).
- Run `clear-old-rejections.sql` once.
- Existing tests that use the deleted classes need the new names (see "Deleted").
