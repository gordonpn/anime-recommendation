# Project-Specific Preferences & Workflows

## JVM and Build Environment
- **Java Versions:** The project is configured to target **Java 25** via Gradle Toolchains.
- **Gradle Runner:** The local system defaults to Java 26. Since Gradle (v8.12.1) and internal Spring Boot libraries do not yet support running the daemon directly on Java 26 or compiling Java 25 bytecode under older Spring Boot versions, the following adjustments were made:
  - Spring Boot upgraded to **3.5.15** (provides ASM support for Java 25).
  - Lombok upgraded to **1.18.46** (provides Java 25/26 compiler support).
  - Gradle configured via [gradle.properties](file:///Users/gordonpn/workspace/anime-recommendation/gradle.properties) to run the daemon itself on **JDK 21** (`org.gradle.java.home`), while leveraging **JDK 25** (`org.gradle.java.installations.paths`) as the build toolchain.
- **Gradle Commands:** Standard Gradle tasks (e.g. `./gradlew test`) can be run directly from the root folder; settings in `gradle.properties` handle finding the correct JDK installations on macOS.
