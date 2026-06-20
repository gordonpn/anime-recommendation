# Architecture (Java Spring CLI)

This document defines the target architecture for this repository.
The application is focused on a **CLI-first** experience using Spring Shell.

## 1) Goals

- Stay in the **Java + Spring** ecosystem.
- Keep CLI workflows as the primary interface.
- Keep recommendation logic explicit and easy to tune.
- Avoid rewrites: migrate incrementally from current code.

## 2) Current Baseline (what already exists)

- Spring Boot app with Spring Shell commands.
- MAL OAuth login + token refresh.
- SQLite + Flyway migrations + JPA repositories.
- Scheduled list synchronization.
- Recommendation logic based on MAL recommendation aggregation.

## 3) Target Logical Architecture

## 3.1 Layers

1. **Interface layer**
   - CLI commands (`commands/`)

2. **Application layer**
   - Orchestration services (`service/`)
   - Use-case-focused services (search, recommendations, sync)

3. **Domain layer**
   - Recommendation ranking + filtering logic
   - Domain models

4. **Infrastructure layer**
   - MAL API clients (`RestClient` based)
   - Persistence (JPA repositories, SQLite)
   - Scheduling and background jobs

## 3.2 Design rule

No business logic lives directly in command classes.
Services handle all domain logic and are independently testable.

## 4) Package Direction (target)

```text
com.gordonpn.anime
├── commands/                  # Spring Shell entry points
├── service/                   # Orchestration services
│   ├── recommendation/
│   │   ├── RecommendationFacade
│   │   ├── RankingService
│   │   ├── FilteringService
│   │   └── ExplanationService
│   ├── auth/
│   ├── sync/
│   └── list/
├── client/                    # MAL and external API clients
├── repository/                # JPA repositories
├── entity/                    # JPA entities
├── models/                    # Domain models
├── config/
└── handler/
```

Notes:
- Keep existing package names during migration; move only when useful.
- CLI-focused structure, no controller layer needed.

## 5) Recommendation Strategy

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

## 6) Data & Persistence

Keep SQLite + Flyway for now.

Core tables:
- `tokens`
- `sync_status`
- `user_anime_list`

Near-term additions (optional):
- `anime_cache` (cached MAL anime details with TTL metadata)
- `anime_id_mapping` (cross-reference table mapping MyAnimeList IDs to AniList IDs)
- `recommendation_snapshot` (optional offline debugging/audit)

## 7) Operational Requirements

- All secrets from environment variables or profile-specific external config.
- Never commit real API credentials to source control.
- Structured logs for command execution.
- Graceful shutdown for scheduler and datasource.

## 8) Implementation Checklist

## Phase A — Service Refactor

- [ ] Introduce `RecommendationFacade` for orchestration.
- [ ] Extract scoring into `RankingService`.
- [ ] Extract filtering into `FilteringService`.
- [ ] Add explanation payload from `ExplanationService`.

Acceptance criteria:
- Recommendation logic is testable without shell context.
- Weight tuning can be done by config changes.

## Phase B — Reliability & security hardening

- [ ] Move MAL credentials to environment variables.
- [ ] Rotate compromised/revealed credentials.
- [ ] Add command structured logging.
- [ ] Add unit tests for major services and key CLI command paths.

Acceptance criteria:
- No secrets in tracked files.
- Build + tests pass in CI.

## Phase C — Optional advanced relevance

- [ ] Add local cached anime metadata table/service.
- [ ] Add hybrid ranking signals (rating/popularity/time-decay).
- [ ] Optional: evaluate Java-native vector search path (OpenSearch/pgvector/Lucene KNN).

Acceptance criteria:
- Recommendation quality improves with measurable ranking metrics.
- Existing CLI behavior remains backward-compatible.

## 9) Non-Goals

- No HTTP API.
- No Python service split.
- No frontend in this repository.
- No microservice decomposition.

## 10) Multi-Source Anime Lookup (AniList + MyAnimeList Cross-Referencing)

The system supports cross-referencing anime across multiple databases:

- **MyAnimeList (primary)**: Used for user authentication, list management, and primary recommendation source.
- **AniList (secondary)**: Provides GraphQL-based queries for supplementary anime metadata and ID cross-referencing.

### Strategy

1. User searches for an anime by title (via CLI).
2. First lookup in AniList (GraphQL, typically faster for large datasets).
3. Use AniList ID to find corresponding MyAnimeList ID via `anime_id_mapping` cache.
4. Retrieve full recommendation chain from MyAnimeList.

This dual-source approach improves search accuracy and availability (fallback to alternate source if one API is unavailable).

## 11) Advanced: Vector Search (vec2vec) for Similarity Ranking

**Phase C+ exploration:** Evaluate embedding-based recommendation refinement:

- **Java-native options**:
  - **Lucene KNN**: Built-in approximate nearest neighbor search.
  - **OpenSearch**: Distributed vector search engine.
  - **pgvector**: PostgreSQL vector extension (if migrating from SQLite).

- **Use case**: Instead of purely occurrence-based ranking, embed anime descriptions/metadata and use vector similarity to identify recommendations that are semantically similar to user preferences, not just directly recommended.

- **Gradual rollout**: Introduce as optional scoring signal (remains backward-compatible with current occurrence-based ranking).

## 12) Migration Notes

- Keep changes incremental and backward-compatible.
- Prefer adding new CLI commands over rewriting existing ones.
- Use feature flags/properties for behavior switches where practical.
