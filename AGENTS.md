# Agent Instructions

This file is addressed to AI coding agents (Copilot, Cursor, Codeium, etc.).

## Mandatory Project Context Reads

- Before planning or implementing any code change, read all other project-level Markdown files in this repository to gather context.
- At minimum, read `README.md`, `ARCHITECTURE.md`, and `TODO.md` (in addition to this `AGENTS.md`) at the start of a task.
- If additional `.md` files are added later, read those too before making changes.

## Goals

- Implement an anime recommendation CLI that:
  - Uses Spring Boot for runtime.
  - Supports Spring Shell CLI interface.
  - Integrates with MyAnimeList for authentication, metadata, and recommendations.
- Keep the design modular and explicit, with clear separation between:
  - Interface layer (CLI commands).
  - Service/business logic.
  - Infrastructure (HTTP clients, persistence, scheduling).

## Constraints

- Use **Java 25** and **Spring Boot 3.x**.
- Keep existing JPA/Flyway/SQLite setup unless explicitly asked to change it.
- Do **not** generate a frontend in this repository.
- Keep external calls behind small, focused services/clients.
- Avoid duplicating business logic across different command classes.

## Implementation Priorities (Order)

1. **Service Refactoring**
  - Keep Spring Boot app as the single CLI-focused runtime.
  - Introduce orchestration facade (`RecommendationFacade`).
  - Extract ranking logic into `RankingService`.
  - Extract filters into `FilteringService`.
  - Add explanation generation service.

2. **Recommendation Engine Improvements**
  - Reuse existing services (`RecommendationService`, `UserListService`, `AnimeService`) for all CLI commands.
  - Keep CLI commands focused and clear.
  - Enhance ranking with multiple signals (occurrence, rating, popularity).

3. **Reliability and Security**
  - Move credentials to environment variables/external config.
  - Add comprehensive exception handling and error messages.
  - Add tests for service logic and key CLI command paths.

4. **Optional Advanced Relevance**
  - Add weighted blending with additional signals (rating/popularity/temporal decay).
  - Optionally evaluate Java-native vector search (Lucene/OpenSearch/pgvector).
  - Add multi-source anime lookup (AniList cross-referencing).

## Coding Expectations

- Keep changes incremental and backward-compatible.
- Prefer introducing new classes over large rewrites in-place.
- Keep methods small and composable.
- Add/adjust tests when introducing significant behavior changes.
- Preserve existing command names unless migration explicitly requires changes.
- Keep commands focused on presentation; all business logic in services.

## Style Preferences

- Use explicit domain models for complex data structures.
- Handle errors explicitly with meaningful error messages.
- Keep class responsibilities focused (command/service/repository separation).
- Prefer constructor injection and immutable dependencies.

## Out of Scope (unless asked)

- Python/FastAPI services.
- Frontend/UI generation.
- Microservice decomposition.
