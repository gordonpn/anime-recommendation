# Architecture (Java Spring Hybrid CLI + API)

This document defines the target architecture for this repository.
It replaces the previous Python/FastAPI plan.

## 1) Goals

- Stay in the **Java + Spring** ecosystem.
- Keep the existing **CLI workflows** working.
- Add a first-class **HTTP API** on top of the same domain services.
- Avoid rewrites: migrate incrementally from current code.
- Keep recommendation logic explicit and easy to tune.

## 2) Current Baseline (what already exists)

- Spring Boot app with Spring Shell commands.
- MAL OAuth login + token refresh.
- SQLite + Flyway migrations + JPA repositories.
- Scheduled list synchronization.
- Recommendation logic based on MAL recommendation aggregation.

## 3) Target Runtime Model

The application supports three runtime modes:

1. `cli` (default during local usage)
   - Spring Shell enabled.
   - HTTP endpoints optional.

2. `api`
   - Spring Shell disabled.
   - HTTP API only (for integration/automation).

3. `both`
   - Shell and HTTP endpoints active.
   - Useful for development and migration.

Suggested config flag:

- `app.mode=cli|api|both`

## 4) Target Logical Architecture

## 4.1 Layers

1. **Interface layer**
   - CLI commands (`commands/`)
   - REST controllers (`controller/`)

2. **Application layer**
   - Orchestration services (`service/`)
   - Use-case-focused services (search, recommendations, sync)

3. **Domain layer**
   - Recommendation ranking + filtering logic
   - DTOs and domain models

4. **Infrastructure layer**
   - MAL API clients (`RestClient` based)
   - Persistence (JPA repositories, SQLite)
   - Scheduling and background jobs

## 4.2 Design rule

Both CLI and API call the same service methods.
No business logic lives directly in command/controller classes.

## 5) Package Direction (target)

```text
com.gordonpn.anime
├── commands/                  # Spring Shell entry points
├── controller/                # REST controllers
├── service/                   # Existing + orchestration services
│   ├── recommendation/
│   │   ├── RecommendationFacade
│   │   ├── RankingService
│   │   ├── FilteringService
│   │   └── ExplanationService
│   ├── auth/
│   ├── sync/
│   └── list/
├── client/                    # MAL and future external clients
├── repository/                # JPA repositories
├── entity/                    # JPA entities
├── models/                    # Existing API/domain models (gradual cleanup)
├── dto/                       # Request/response DTOs for HTTP API
├── config/
└── handler/
```

Notes:
- Keep existing package names during migration; move only when useful.
- `models/` can be split over time into `dto/` + domain models.

## 6) API/CLI Feature Parity Targets

## 6.1 Health

- CLI: `health` (optional convenience command)
- API: `GET /api/v1/health`

Response:

```json
{
  "status": "UP",
  "mode": "both",
  "time": "2026-02-26T12:00:00Z"
}
```

## 6.2 Authentication

- CLI keeps interactive browser login (`login`, `refresh`).
- API adds non-interactive token status endpoints:
  - `GET /api/v1/auth/status`
  - `POST /api/v1/auth/refresh`

## 6.3 User list

- CLI: existing `list` command
- API:
  - `GET /api/v1/users/{userName}/list?status=completed`

## 6.4 Recommendations

- CLI: existing `recommendation` command
- API:
  - `GET /api/v1/recommendations/{userName}`
  - Optional filters: `excludeExisting=true`, `limit=50`, `minScore`.

## 6.5 Seasonal

- CLI: existing `seasonal` command
- API:
  - `GET /api/v1/recommendations/seasonal?years=5`

## 7) Recommendation Strategy (near-term)

Phase 1 scoring (current-compatible):

- Aggregate recommendation occurrence count from completed items.
- Exclude already-completed and dropped anime.
- Sort descending by occurrence.

Target scoring extension (Phase 2+):

- `final_score = w_occurrence * occurrence + w_rating * rating + w_popularity * popularity`

Where weights are externalized in properties:

- `recommendation.weights.occurrence`
- `recommendation.weights.rating`
- `recommendation.weights.popularity`

## 8) Data & Persistence

Keep SQLite + Flyway for now.

Core tables:
- `tokens`
- `sync_status`
- `user_anime_list`

Near-term additions (optional):
- `anime_cache` (cached MAL anime details with TTL metadata)
- `anime_id_mapping` (cross-reference table mapping MyAnimeList IDs to AniList IDs)
- `recommendation_snapshot` (optional offline debugging/audit)

## 9) Operational Requirements

- All secrets from environment variables or profile-specific external config.
- Never commit real API credentials to source control.
- Structured logs for command execution and API requests.
- Graceful shutdown for scheduler and datasource.

## 10) Implementation Checklist

## Phase A — Foundation (Hybrid runtime)

- [ ] Add `app.mode` property and conditional enabling for Shell/API.
- [ ] Add `controller/HealthController` and `GET /api/v1/health`.
- [ ] Add global exception handling for API (`@ControllerAdvice`).
- [ ] Standardize API response DTOs in `dto/`.

Acceptance criteria:
- App starts in all 3 modes (`cli`, `api`, `both`).
- Health endpoint returns 200 in `api` and `both` modes.

## Phase B — Expose existing capabilities via REST

- [ ] Add `AuthController` (`status`, `refresh`).
- [ ] Add `UserListController` (`GET /users/{userName}/list`).
- [ ] Add `RecommendationController` (`/recommendations/{userName}`, `/recommendations/seasonal`).
- [ ] Ensure controllers call existing services only (no duplicated logic).

Acceptance criteria:
- CLI outputs and API payloads for equivalent operations are logically consistent.
- Existing CLI commands continue to work unchanged.

## Phase C — Service refactor for maintainability

- [ ] Introduce `RecommendationFacade` for orchestration.
- [ ] Extract scoring into `RankingService`.
- [ ] Extract filtering into `FilteringService`.
- [ ] Add explanation payload from `ExplanationService`.

Acceptance criteria:
- Recommendation logic is testable without shell/controller context.
- Weight tuning can be done by config changes.

## Phase D — Reliability & security hardening

- [ ] Move MAL credentials to environment variables.
- [ ] Rotate compromised/revealed credentials.
- [ ] Add request logging + correlation ID.
- [ ] Add integration tests for major endpoints and key CLI command paths.

Acceptance criteria:
- No secrets in tracked files.
- Build + tests pass in CI.

## Phase E — Optional advanced relevance

- [ ] Add local cached anime metadata table/service.
- [ ] Add hybrid ranking signals (rating/popularity/time-decay).
- [ ] Optional: evaluate Java-native vector search path (OpenSearch/pgvector/Lucene KNN).

Acceptance criteria:
- Recommendation quality improves with measurable ranking metrics.
- Existing API contracts remain backward-compatible.

## 11) Non-Goals (for now)

- No Python service split.
- No frontend in this repository.
- No microservice decomposition.

## 11) Multi-Source Anime Lookup (AniList + MyAnimeList Cross-Referencing)

The system supports cross-referencing anime across multiple databases:

- **MyAnimeList (primary)**: Used for user authentication, list management, and primary recommendation source.
- **AniList (secondary)**: Provides GraphQL-based queries for supplementary anime metadata and ID cross-referencing.

### Strategy

1. User searches for an anime by title.
2. First lookup in AniList (GraphQL, typically faster for large datasets).
3. Use AniList ID to find corresponding MyAnimeList ID via `anime_id_mapping` cache.
4. Retrieve full recommendation chain from MyAnimeList.

This dual-source approach improves search accuracy and availability (fallback to alternate source if one API is unavailable).

## 12) Advanced: Vector Search (vec2vec) for Similarity Ranking

**Phase E+ exploration:** Evaluate embedding-based recommendation refinement:

- **Java-native options**:
  - **Lucene KNN**: Built-in approximate nearest neighbor search.
  - **OpenSearch**: Distributed vector search engine.
  - **pgvector**: PostgreSQL vector extension (if migrating from SQLite).

- **Use case**: Instead of purely occurrence-based ranking, embed anime descriptions/metadata and use vector similarity to identify recommendations that are semantically similar to user preferences, not just directly recommended.

- **Gradual rollout**: Introduce as optional scoring signal (remains backward-compatible with current occurrence-based ranking).

## 13) Migration Notes

- Keep changes incremental and backward-compatible.
- Prefer adding controllers over rewriting command classes.
- Use feature flags/properties for behavior switches where practical.
