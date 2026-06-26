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

### Temporal Recommendation Decay (Taste Decay)
- **Concept:** Recent shows in the user's completed list should influence recommendations more heavily than older shows, since taste evolves over time.
- **Decay Formula:** Assign decay weights based on completion date: $w = 2.0^{-\frac{t}{\text{decayMonths}}}$, where $t$ is the elapsed completion age in decimal months ($t = \text{days} / 30.4375$). The `decayMonths` represents the half-life parameter.
- **CLI/Parameter Integration:** Exposed as an optional `--decay-months` parameter in the `recommendation` command, prompting interactively if not provided as a direct flag.

### Freshness Boost & Hybrid Temporal Weighting
- **Freshness Boost Formula:** Boost candidate recommendations based on their release date: $b = e^{-\mu \times \text{yearsSinceRelease}}$, where $\text{yearsSinceRelease} = \max(0.0, \text{currentYear} - \text{startYear})$, and $\mu$ is the `freshnessBoost` parameter (decay rate).
- **Hybrid Integration:** When both parameters are enabled, they are combined as: $\text{Final Score} = \text{Base Relevance Score} \times \text{Taste Decay} \times \text{Freshness Boost}$. Completion decay is applied at the occurrence signal level, and freshness boost is applied at the final candidate score level.
- **CLI/Parameter Integration:** Exposed as an optional `--freshness-boost` parameter in the `recommendation` command, prompting interactively if not provided as a direct flag.


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

### Fail-Fast Error Propagation Strategy
- **Transient vs. Permanent Exception Differentiation:** To ensure the accuracy of recommendation results, the application differentiates error types:
  - **Permanent Errors (404 Not Found):** Wrapped in `MalResourceNotFoundException`. Caught in detail-fetching loops (facade, commands) to log warning/info and skip the item cleanly, allowing execution to continue.
  - **Transient Errors (unexpected HTML/429/5xx/network errors):** Rethrown by the services (`AnimeService`, `UserListService`) and facade layers. The command/facade execution aborts immediately, guaranteeing that recommendations are never built on incomplete datasets.

### DTO/Entity Separation & Package Reorganization
- **Token Entity & DTO Separation:** `TokenResponse` functions as a pure external client response DTO (under package `models.dto`) containing Jackson annotations, decoupled from JPA mapping. `TokenEntity` (under package `entity`) is a dedicated database entity mapped to the SQLite `tokens` table. Services perform explicit mapping when loading/saving credentials, preventing API contract changes from corrupting the database schema.
- **Model Reorganization:** Relocated target models into specialized packages (`models/dto` and `models/exception`) under `models` to separate concerns cleanly, while keeping raw MyAnimeList response mappings at the root of `models`.

## Interactive CLI & Presentation Layer

### Spring Shell Native ComponentFlow Prompts
- **Wizard Mechanism:** Replaced manual `LineReader.readLine()` parsing helpers with Spring Shell's native `ComponentFlow.Builder`.
- **Keyboard Navigation:** Uses `SingleItemSelector` for boolean prompts (JSON format select) and multi-choice limits (e.g., limit selector 5/10/20/50) to provide arrow-key menu selections directly inline.
- **Gated Flow:** Interactive prompts are dynamically built and executed ONLY for command options that are passed as `null`, ensuring non-interactive scripting pipelines and unit tests run seamlessly without blocking on prompts.

### TableFormatter Presentation Service
- **Decoupled Layout:** Table layout formatting is moved out of the shell command entry points into `TableFormatter.java` to isolate CLI presentation concerns.
- **High-Fidelity Unicode Grids:** Renders table frames using unicode box-drawing characters (`│`, `─`, `┼`) with colors configured using JLine 3's `AttributedStringBuilder` and `AttributedStyle`.
- **Styling Rules:** Muted borders are colored using bright black/grey (`AttributedStyle.BLACK | AttributedStyle.BRIGHT`). Recommendations and entries scoring > 8.0 are highlighted in green and bold (`AttributedStyle.GREEN` / `AttributedStyle.BOLD`) to make outstanding recommendations stand out.

### Asynchronous Status Spinner
- **Mechanism:** Implemented `TerminalSpinner.java` (implementing `AutoCloseable`) running a background daemon thread that cycles braille frame characters (`⠋`, `⠙`, `⠹`...) at an 80ms interval.
- **Non-Destructive Cleanup:** Uses escape carriage returns (`\r`) and erase-line instructions (`\033[K`) to update loading status inline. The spinner cleans up after itself upon calling `close()` in `finally` or try-with-resources blocks, avoiding text pollution.

### Distraction-Free Analytics Dashboard
- **Alternate Buffer View:** The `dashboard` command switches the user out of standard text history into JLine's alternate screen buffer (`\033[?1049h`) and hides the cursor (`\033[?25l`).
- **Raw Input Event Loop:** Switches the terminal into raw character input mode (`terminal.enterRawMode()`) to register immediate keypresses without waiting for line buffering. Pressing `[r]` triggers live recommendations recalculation, and pressing `[q]` exits raw mode and cleanly returns the user to standard shell scrolling buffers (`\033[?1049l` & `\033[?25h`).

