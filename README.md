# Anime Recommendation

Anime recommendation CLI built with **Java + Spring Boot + Spring Shell**.

The project integrates with **MyAnimeList (MAL)** for authentication and data retrieval, persists local state in **SQLite**, and uses **Flyway** for schema migrations.

## Tech Stack

- Java 25 (toolchain)
- Spring Boot 3.x
- Spring Shell (CLI)
- Spring Data JPA + SQLite
- Flyway migrations
- Lombok
- MyAnimeList API (OAuth 2.0, anime metadata, recommendations)
- AniList GraphQL API (alternate anime lookup source)

## Current Capabilities

- Interactive MAL OAuth login from CLI.
- Access token refresh and token persistence.
- Fetch user anime list by status.
- Aggregate MAL recommendations based on completed titles.
- Seasonal recommendation helper command.
- Scheduled background sync of user list statuses.
- Series grouping and franchise consolidation using related anime graphs.
- Multi-signal recommendation ranking (occurrence, user rating, popularity).

## Target Direction

- Keep CLI commands working and expanding.
- Incrementally refactor recommendation logic into focused services (ranking/filtering/explanations).
- Improve test coverage and code organization.

See [ARCHITECTURE.md](ARCHITECTURE.md) for the detailed target architecture and refactoring roadmap.

## Prerequisites

- JDK 25

## Configuration

Set MAL credentials via environment variables or externalized Spring config:

- `MAL_CLIENT_ID`
- `MAL_CLIENT_SECRET`
- `MAL_REDIRECT_URI`
- `MAL_CALLBACK_PORT`

Recommended: do not store real credentials in tracked files.

## Run

Build and run:

```sh
./gradlew clean bootJar && java -jar build/libs/anime-0.0.1-SNAPSHOT.jar
```

Dev run:

```sh
./gradlew bootRun
```

## CLI Commands (current)

- `login` - Authenticate with MAL via browser callback.
- `refresh` - Refresh access token.
- `list --userName @me` - Fetch completed anime list.
- `recommendation --userName @me` - Show aggregated recommendations.
- `seasonal --numYears 5` - Seasonal ranking utility.
- `suggestions` - MAL suggestions call.
- `flyway-info` / `flyway-repair` - migration maintenance commands.

## Testing

```sh
./gradlew test
```

## Project Status

This is an active refactoring focused on the CLI interface and service layer improvements.
Contributions should follow the Java-first architecture in [ARCHITECTURE.md](ARCHITECTURE.md).
