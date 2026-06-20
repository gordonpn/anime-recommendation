# Project-Specific Preferences & Workflows

## JVM and Build Environment
- **Java Versions:** The project is configured to target **Java 25** via Gradle Toolchains.
- **Gradle Runner:** The local system defaults to Java 26. Since Gradle (v8.12.1) and internal Spring Boot libraries do not yet support running the daemon directly on Java 26 or compiling Java 25 bytecode under older Spring Boot versions, the following adjustments were made:
  - Spring Boot upgraded to **3.5.15** (provides ASM support for Java 25).
  - Lombok upgraded to **1.18.46** (provides Java 25/26 compiler support).
  - Gradle configured via [gradle.properties](file:///Users/gordonpn/workspace/anime-recommendation/gradle.properties) to run the daemon itself on **JDK 21** (`org.gradle.java.home`), while leveraging **JDK 25** (`org.gradle.java.installations.paths`) as the build toolchain.
- **Gradle Commands:** Standard Gradle tasks (e.g. `./gradlew test`) can be run directly from the root folder; settings in `gradle.properties` handle finding the correct JDK installations on macOS.

## Design Decisions

### Series Grouping in Seasonal Recommendations
- **Grouping Strategy:** Related franchise titles (sequels, prequels, movies) are identified using MAL's `related_anime` relations. We construct an undirected graph of relations and run a BFS connected components algorithm to partition distinct franchises, using intermediate related titles as bridge nodes to connect parts that do not link to each other directly.
- **Representative Title:** Consolidates all matching entries under the title of the entry with the *highest popularity* (minimum popularity rank number) in the **entire connected component (including non-seasonal bridge nodes)**. This naturally chooses clean, original series names (e.g. "Shingeki no Kyojin" or "Hibike! Euphonium") even if the first season itself is not in the seasonal results list.
- **Rating Calculation (Straight Average):** We use a **straight arithmetic average** of the scores of all seasonal entries in the grouped component.
  - *Rationale for picking straight average:* In anime databases, sequels naturally suffer from viewer attrition (only dedicated fanbases continue watching later seasons). As a result, early seasons (like Season 1) have a massive number of ratings, while later seasons have fewer reviews but significantly higher scores. Weighting the average by the number of ratings (sample size) would cause the Season 1 score to heavily dominate, suppressing the high acclaim of later seasons. A straight average treats each season as an equal unit of the series' overall quality, preserving sequel success.

### Generic API Caching Strategy
- **SQLite-backed Raw Response Caching:** Raw JSON response strings from MAL API (for seasonal lists and user lists) are persisted in the `generic_cache` table to reduce network roundtrips.
- **TTL Strategy:**
  - Past seasons (older than current year): **30 days** TTL since historical stats change very slowly.
  - Current/future seasons: **1 day** TTL.
  - User lists: **1 day** TTL.
- **Fail-Safe Fallback:** If the external API is down or rate-limited, the system falls back to returning the expired cache contents rather than throwing an exception.

### Completed Series Grouping & Recommendation Pooling
- **Grouping Strategy:** Watched seasons/parts of the same series are grouped into connected components using MAL's `related_anime` relations. The components partition distinct franchises.
- **Group Representative Title:** Consolidates all matching completed entries under the title of the entry with the *highest popularity* (minimum popularity rank number) in the entire connected component.
- **Group User Rating Calculation (Straight Average):** We compute the average of user-submitted scores for the completed shows in that group.
- **Recommendation Pooling:** All user-recommended similar titles from all shows in a grouped completed series are pooled together, so recommending signals from sequels/prequels of a franchise are consolidated.

### Multi-Signal Recommendation Ranking
- **User Score Weighted Scaling:** To allow the user's taste to guide the ranking, the recommendation occurrence count is scaled by `avgCompletedGroupRating / 7.0` (using `1.0` if the completed show was unrated).

### API Request Retry Interceptor with Exponential Backoff
- **Transient vs. Permanent Errors:**
  - **Fail-Fast (No Retry):** `404 Not Found`, `401 Unauthorized`, `403 Forbidden`. These represent permanent access or resource states. Skipping retries saves request rate limits and prevents long execution delays.
  - **Retryable Errors:** `429 Too Many Requests` (rate limits), `5xx` Server Errors, and network dropouts (`IOException`).
- **Mechanism:** Implemented as a Spring `ClientHttpRequestInterceptor` (`MalRetryInterceptor`) on the `apiKeyClient` bean.
- **Configurability:** Standard parameters are configured via `application.properties`:
  - `mal.retry.max-attempts` (default: 3)
  - `mal.retry.initial-backoff-ms` (default: 500ms)
  - `mal.retry.backoff-multiplier` (default: 2.0)

### CLI Interactive Prompting
- **Behavior:** The `recommendation`, `seasonal`, and `suggestions` commands will prompt the user interactively if parameters are not provided as flags in the shell.
- **Fallback:** In interactive mode, if the user leaves the input blank and hits `Enter`, the default value is automatically applied.
- **Compatibility:** Passing command-line options directly (e.g., `recommendation --userName test`) skips prompting, which preserves non-interactive usage and compatibility for scripts and unit tests.
- **Mechanism:** Uses a lazily-injected JLine `LineReader` via `ObjectProvider<LineReader>` to safely query user input if Spring Shell is running.
