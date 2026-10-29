# Project Execution Queue

**Last Refined:** 2026-10-20 10:05 EDT  
**Repository:** [gordonpn/anime-recommendation](https://github.com/gordonpn/anime-recommendation)  
**GitHub Backlog:** [View All Issues](https://github.com/gordonpn/anime-recommendation/issues)  

---

## NOW — Recommended Next Task

### [#31: [Story] Add reproducible Java build and test CI](https://github.com/gordonpn/anime-recommendation/issues/31)

- **Status:** **IN REVIEW** (Pull Request [#110](https://github.com/gordonpn/anime-recommendation/pull/110) open; all 53 tests passing on Linux CI)
- **Priority:** P0 (Critical Blocker)
- **Effort:** S
- **Readiness:** IN REVIEW
- **Delivered implementation:**  
  Created `.github/workflows/ci-build-test.yml` using `actions/checkout@v4`, `go-task/setup-task@v2`, Temurin JDK 25 and JDK 21 daemon toolchains with `cache: 'gradle'`, dynamic Linux `GRADLE_OPTS` configuration, and test report artifact archiving. Verified green on GitHub Actions (run `40798689636`).
- **Tasks it unblocks:**  
  Unblocks all pull request reviews and default branch protection ([#106](https://github.com/gordonpn/anime-recommendation/issues/106)), providing the quality gate for [#19](https://github.com/gordonpn/anime-recommendation/issues/19), [#108](https://github.com/gordonpn/anime-recommendation/issues/108), [#20](https://github.com/gordonpn/anime-recommendation/issues/20), and [#23](https://github.com/gordonpn/anime-recommendation/issues/23).

---

## NEXT — Upcoming Work

1. **[#19: [Story] Skip startup synchronization for fresh list snapshots](https://github.com/gordonpn/anime-recommendation/issues/19)**  
   - **Priority:** P1 | **Effort:** S | **Readiness:** READY  
   - **Dependencies:** Unblocked by CI ([#31](https://github.com/gordonpn/anime-recommendation/issues/31))  
   - **Reason:** Prevents destructive deletion of local user list records during partial fetch failures while respecting `sync_status` TTLs before invoking live MAL endpoints.

2. **[#108: [Story] Enable SQLite WAL mode and busy timeout for safe concurrent access](https://github.com/gordonpn/anime-recommendation/issues/108)**  
   - **Priority:** P1 | **Effort:** XS | **Readiness:** READY  
   - **Dependencies:** None (Parallelizable)  
   - **Reason:** Quick-win reliability fix. Adds `PRAGMA journal_mode=WAL;` and `PRAGMA busy_timeout=5000;` to `DataSourceConfig.java` to prevent database locks during interactive shell sessions.

3. **[#20: [Story] Enforce the documented MAL retry classification](https://github.com/gordonpn/anime-recommendation/issues/20)**  
   - **Priority:** P1 | **Effort:** S | **Readiness:** READY  
   - **Dependencies:** None  
   - **Reason:** Eliminates competing, redundant retry loops in `AnimeService` and `UserListService`. Delegates all transient backoff to `MalRetryInterceptor` and ensures permanent errors (401, 403, 404) fail fast.

4. **[#23: [Story] Share franchise grouping across recommendation paths](https://github.com/gordonpn/anime-recommendation/issues/23)**  
   - **Priority:** P1 | **Effort:** M | **Readiness:** READY  
   - **Dependencies:** None (Parallelizable)  
   - **Reason:** High-leverage architectural refactor. Extracts the duplicated connected-components BFS graph builder out of `RecommendationCommands` and `RecommendationFacade` into a dedicated service.

5. **[#39: [Story] Fetch metadata required by discovery features](https://github.com/gordonpn/anime-recommendation/issues/39)**  
   - **Priority:** P1 | **Effort:** S | **Readiness:** READY  
   - **Dependencies:** [#20](https://github.com/gordonpn/anime-recommendation/issues/20)  
   - **Reason:** Core discovery enabler. Expands MAL metadata queries to fetch and cache `genres`, `studios`, `media_type`, and `rating`, immediately unblocking [#38](https://github.com/gordonpn/anime-recommendation/issues/38) (`anime-search`), [#40](https://github.com/gordonpn/anime-recommendation/issues/40) (`anime-detail`), and recommendation filters ([#42](https://github.com/gordonpn/anime-recommendation/issues/42), [#43](https://github.com/gordonpn/anime-recommendation/issues/43)).

---

## BLOCKERS

- None. Pull request [#110](https://github.com/gordonpn/anime-recommendation/pull/110) has verified Linux build and test automation, ready for merge.

---

## LATER

- Remaining feature workflows, discovery filters, local analytics, release automation, and deferred stretch evaluations are tracked in GitHub Issues:  
  👉 **[Active Backlog View](https://github.com/gordonpn/anime-recommendation/issues?q=is%3Aissue+is%3Aopen)**
