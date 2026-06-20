# TODO

A rough task list for the project, suitable for human or agent-driven implementation.

Scope: Java Spring CLI-focused application.

## Phase 0: Baseline cleanup

- [ ] Move MAL credentials to environment-based config.
- [ ] Rotate any previously committed MAL credentials.
- [ ] Verify app starts cleanly with local profile and SQLite.

## Phase 1: Service refactor

- [ ] Introduce `RecommendationFacade` for orchestration.
- [ ] Extract ranking policy into `RankingService`.
- [ ] Extract inclusion/exclusion rules into `FilteringService`.
- [ ] Add `ExplanationService` for recommendation reason text.
- [ ] Externalize score weights into application properties.

## Phase 2: Data quality and sync improvements

- [ ] Check sync status at startup and skip unnecessary fetches.
- [ ] Upsert strategy for `user_anime_list` to avoid duplicates.
- [ ] Fetch and persist richer anime metadata cache.
- [ ] Add cache invalidation/TTL policy for external API responses.

## Phase 3: Reliability, testing, and delivery

- [ ] Add service-level tests for recommendation and filtering logic.
- [ ] Add command-level smoke tests for critical CLI paths.
- [ ] Add command structured logging.
- [ ] Add exception handling and better error messages.

## Phase 4: Optional advanced relevance

- [ ] Add popularity/rating signal blending in final score.
- [ ] Add optional metadata-driven filters (year, genre, sequel handling).
- [ ] Evaluate Java-native vector search path (Lucene/OpenSearch/pgvector).
- [ ] Keep CLI behavior stable while improving ranking internals.

## Phase 5: Multi-source anime lookup (AniList cross-referencing)

- [ ] Integrate AniList GraphQL client.
- [ ] Add `anime_id_mapping` table for MyAnimeList ↔ AniList ID cross-reference.
- [ ] Implement anime search via AniList (fallback to MyAnimeList).
- [ ] Add cross-reference caching and TTL management.
- [ ] Add CLI command for multi-source anime search.

## Phase 6: Vector search and semantic similarity (vec2vec)

- [ ] Evaluate Java-native vector search libraries (Lucene KNN, OpenSearch, pgvector).
- [ ] Design embedding pipeline (anime metadata → vectors).
- [ ] Prototype vector similarity ranking as optional scoring signal.
- [ ] Add configuration flag to enable/disable vector scoring.
- [ ] Measure quality improvements vs. occurrence-based ranking.
- [ ] Maintain backward compatibility with existing CLI behavior.
