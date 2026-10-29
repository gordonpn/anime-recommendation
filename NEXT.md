# Project Execution Queue

**Last Refined:** 2026-10-29 01:10 EDT  
**Repository:** [gordonpn/anime-recommendation](https://github.com/gordonpn/anime-recommendation)  
**GitHub Backlog:** [View All Issues](https://github.com/gordonpn/anime-recommendation/issues)  

---

## NOW - Recommended Next Task

### [#19: [Story] Skip startup synchronization for fresh list snapshots](https://github.com/gordonpn/anime-recommendation/issues/19)

- **Status:** **READY**  
- **Priority:** P1  
- **Effort:** S  
- **Readiness:** READY  
- **Objective:**  
  Read sync freshness timestamps before triggering startup synchronization in `BackgroundFetchService`, preventing unnecessary external MAL API requests when list snapshots are fresh, and ensuring failed sync runs never destructively wipe local list state.
- **Parent:** [#6: [Epic] Preserve complete data and predictable external failures](https://github.com/gordonpn/anime-recommendation/issues/6)  
- **Tasks it unblocks:**  
  Protects data integrity and establishes reliable offline-first list caching, unblocking discovery and recommendation features ([#108](https://github.com/gordonpn/anime-recommendation/issues/108), [#20](https://github.com/gordonpn/anime-recommendation/issues/20), [#23](https://github.com/gordonpn/anime-recommendation/issues/23)).

---

## NEXT - Upcoming Work

1. **[#108: [Story] Enable SQLite WAL mode and busy timeout for safe concurrent access](https://github.com/gordonpn/anime-recommendation/issues/108)**  
   - **Priority:** P1 | **Effort:** XS | **Readiness:** READY  
   - **Dependencies:** None (Parallelizable)  
   - **Reason:** Quick-win reliability fix. Adds `PRAGMA journal_mode=WAL;` and `PRAGMA busy_timeout=5000;` to `DataSourceConfig.java` to prevent database locks during interactive shell sessions.

2. **[#20: [Story] Enforce the documented MAL retry classification](https://github.com/gordonpn/anime-recommendation/issues/20)**  
   - **Priority:** P1 | **Effort:** S | **Readiness:** READY  
   - **Dependencies:** None  
   - **Reason:** Eliminates competing, redundant retry loops in `AnimeService` and `UserListService`. Delegates all transient backoff to `MalRetryInterceptor` and ensures permanent errors (401, 403, 404) fail fast.

3. **[#23: [Story] Share franchise grouping across recommendation paths](https://github.com/gordonpn/anime-recommendation/issues/23)**  
   - **Priority:** P1 | **Effort:** M | **Readiness:** READY  
   - **Dependencies:** None (Parallelizable)  
   - **Reason:** High-leverage architectural refactor. Extracts the duplicated connected-components BFS graph builder out of `RecommendationCommands` and `RecommendationFacade` into a dedicated service.

4. **[#39: [Story] Fetch metadata required by discovery features](https://github.com/gordonpn/anime-recommendation/issues/39)**  
   - **Priority:** P1 | **Effort:** S | **Readiness:** READY  
   - **Dependencies:** [#20](https://github.com/gordonpn/anime-recommendation/issues/20)  
   - **Reason:** Core discovery enabler. Expands MAL metadata queries to fetch and cache `genres`, `studios`, `media_type`, and `rating`, immediately unblocking [#38](https://github.com/gordonpn/anime-recommendation/issues/38) (`anime-search`), [#40](https://github.com/gordonpn/anime-recommendation/issues/40) (`anime-detail`), and recommendation filters ([#42](https://github.com/gordonpn/anime-recommendation/issues/42), [#43](https://github.com/gordonpn/anime-recommendation/issues/43)).

5. **[#24: [Story] Move seasonal ranking out of the command class](https://github.com/gordonpn/anime-recommendation/issues/24)**  
   - **Priority:** P1 | **Effort:** S | **Readiness:** READY  
   - **Dependencies:** [#23](https://github.com/gordonpn/anime-recommendation/issues/23)  
   - **Reason:** Extracts seasonal ranking and component graph building out of `SeasonalAnimeCommands` into a dedicated service, preserving presentation formatting and ComponentFlow prompts in the CLI.

---

## BLOCKERS

- None. Pull request [#110](https://github.com/gordonpn/anime-recommendation/pull/110) merged to `main` (closing [#31](https://github.com/gordonpn/anime-recommendation/issues/31), [#89](https://github.com/gordonpn/anime-recommendation/issues/89), [#90](https://github.com/gordonpn/anime-recommendation/issues/90), and [#91](https://github.com/gordonpn/anime-recommendation/issues/91)) and pull request [#113](https://github.com/gordonpn/anime-recommendation/pull/113) merged to `main` (closing [#111](https://github.com/gordonpn/anime-recommendation/issues/111)).

---

## LATER

- Remaining feature workflows, discovery filters, local analytics, release automation, and deferred stretch evaluations are tracked in GitHub Issues:  
  **[Active Backlog View](https://github.com/gordonpn/anime-recommendation/issues?q=is%3Aissue+is%3Aopen)**
