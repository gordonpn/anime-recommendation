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

