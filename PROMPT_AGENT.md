# Agent Instructions

This file is addressed to AI coding agents (Copilot, Cursor, Codeium, etc.).

## Goals

- Implement an anime recommendation backend that:
  - Uses Spring Boot for runtime.
  - Supports both Spring Shell CLI and Spring Web HTTP APIs.
  - Integrates with MyAnimeList for authentication, metadata, and recommendations.
- Keep the design modular and explicit, with clear separation between:
  - Interface layer (CLI + API).
  - Service/business logic.
  - Infrastructure (HTTP clients, persistence, scheduling).

## Constraints

- Use **Java 25** and **Spring Boot 3.x**.
- Keep existing JPA/Flyway/SQLite setup unless explicitly asked to change it.
- Do **not** generate a frontend in this repository for now.
- Keep external calls behind small, focused services/clients.
- Avoid duplicating business logic across command and controller classes.

## Implementation Priorities (Order)

1. **Bootstrap**
  - Keep Spring Boot app as the single runtime.
  - Add runtime mode support: `app.mode=cli|api|both`.
  - Implement `GET /api/v1/health`.

2. **API Parity over Existing Services**
  - Add controllers for auth status/refresh, user list retrieval, and recommendations.
  - Reuse existing services (`RecommendationService`, `UserListService`, `AnimeService`) instead of rewriting.
  - Keep CLI commands functional.

3. **Recommendation Refactor**
  - Introduce orchestration facade (`RecommendationFacade`).
  - Extract ranking logic into `RankingService`.
  - Extract filters into `FilteringService`.
  - Add explanation generation service.

4. **Reliability and Security**
  - Move credentials to environment variables/external config.
  - Add API exception handling (`@ControllerAdvice`).
  - Add tests for service logic + key API routes.

5. **Optional Advanced Relevance**
  - Add weighted blending with additional signals (rating/popularity).
  - Optionally evaluate Java-native vector search (Lucene/OpenSearch/pgvector).

## Coding Expectations

- Keep changes incremental and backward-compatible.
- Prefer introducing new classes over large rewrites in-place.
- Keep methods small and composable.
- Add/adjust tests when introducing significant behavior changes.
- Preserve existing command names unless migration explicitly requires changes.

## Style Preferences

- Use explicit DTOs for API request/response payloads.
- Handle errors explicitly with meaningful HTTP status codes and messages.
- Keep class responsibilities focused (controller/service/repository separation).
- Prefer constructor injection and immutable dependencies.

## Out of Scope (unless asked)

- Python/FastAPI services.
- Frontend/UI generation.
- Microservice decomposition.
