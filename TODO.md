# TODO

A rough task list for the project, suitable for human or agent-driven implementation.

Scope: Java Spring hybrid CLI + API migration.

## Phase 0: Baseline cleanup

- [ ] Move MAL credentials to environment-based config.
- [ ] Rotate any previously committed MAL credentials.
- [ ] Verify app starts cleanly with local profile and SQLite.

## Phase 1: Hybrid runtime foundation

- [ ] Add runtime mode switch: `app.mode=cli|api|both`.
- [ ] Gate shell and/or web exposure based on mode.
- [ ] Add `GET /api/v1/health` endpoint.
- [ ] Add API exception handling via `@ControllerAdvice`.

## Phase 2: Expose existing features via API

- [ ] Add `AuthController`:
  - `GET /api/v1/auth/status`
  - `POST /api/v1/auth/refresh`
- [ ] Add `UserListController`:
  - `GET /api/v1/users/{userName}/list`
- [ ] Add `RecommendationController`:
  - `GET /api/v1/recommendations/{userName}`
  - `GET /api/v1/recommendations/seasonal`
- [ ] Ensure controllers delegate to existing services; no duplicated logic.

## Phase 3: Recommendation service refactor

- [ ] Introduce `RecommendationFacade` for orchestration.
- [ ] Extract ranking policy into `RankingService`.
- [ ] Extract inclusion/exclusion rules into `FilteringService`.
- [ ] Add `ExplanationService` for recommendation reason text.
- [ ] Externalize score weights into application properties.

## Phase 4: Data quality and sync improvements

- [ ] Check sync status at startup and skip unnecessary fetches.
- [ ] Upsert strategy for `user_anime_list` to avoid duplicates.
- [ ] Fetch and persist richer anime metadata cache.
- [ ] Add cache invalidation/TTL policy for external API responses.

## Phase 5: Reliability, testing, and delivery

- [ ] Add service-level tests for recommendation and filtering logic.
- [ ] Add integration tests for key API endpoints.
- [ ] Add command-level smoke tests for critical CLI paths.
- [ ] Add request/command structured logging and correlation IDs.
- [ ] Add containerization assets (`Dockerfile`, optional compose).

## Phase 6: Optional advanced relevance

- [ ] Add popularity/rating signal blending in final score.
- [ ] Add optional metadata-driven filters (year, genre, sequel handling).
- [ ] Evaluate Java-native vector search path (Lucene/OpenSearch/pgvector).
- [ ] Keep API contracts stable while improving ranking internals.

## Phase 7: Multi-source anime lookup (AniList cross-referencing)

- [ ] Integrate AniList GraphQL client.
- [ ] Add `anime_id_mapping` table for MyAnimeList ↔ AniList ID cross-reference.
- [ ] Implement anime search via AniList (fallback to MyAnimeList).
- [ ] Add cross-reference caching and TTL management.
- [ ] Add API endpoint for multi-source anime search: `GET /api/v1/anime/search?title={title}&sources=anilist,mal`.

## Phase 8: Vector search and semantic similarity (vec2vec)

- [ ] Evaluate Java-native vector search libraries (Lucene KNN, OpenSearch, pgvector).
- [ ] Design embedding pipeline (anime metadata → vectors).
- [ ] Prototype vector similarity ranking as optional scoring signal.
- [ ] Add configuration flag to enable/disable vector scoring.
- [ ] Measure quality improvements vs. occurrence-based ranking.
- [ ] Maintain backward compatibility with existing recommendation API.
