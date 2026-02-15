# Agent Instructions

This file is addressed to AI coding agents (Copilot, Cursor, Codeium, etc.).

## Goals

- Implement an anime recommendation backend that:
  - Uses FastAPI for HTTP APIs.
  - Uses sentence-transformers + FAISS for vector search.
  - Integrates with AniList and MyAnimeList for metadata and recommendations.
- Keep the design modular and explicit, with clear separation between:
  - API layer.
  - Service/business logic.
  - Infrastructure (FAISS, Redis, HTTP clients).

## Constraints

- Use **Python 3.11+**.
- Avoid heavy, monolithic ORMs and frameworks.
- Do **not** generate a frontend in this repository for now.
- Keep all external calls behind small, focused client modules.
- Prefer pure functions for data transformations and ranking logic.

## Implementation Priorities (Order)

1. **Bootstrap**
   - Create a FastAPI app (`app/main.py`) with `/health` endpoint.
   - Add configuration handling (`app/config.py`) using environment variables.

2. **Data Layer (local only)**
   - Define a `AnimeMetadata` model in `app/models/domain.py`.
   - Create a simple loader that reads a placeholder `data/processed/anime_metadata.json` or `.parquet`.
   - Implement `GET /search` against in-memory metadata.

3. **FAISS Integration**
   - Implement a FAISS wrapper in `app/infra/faiss_index.py`:
     - Loading an index from `data/indexes/index.faiss`.
     - Querying for nearest neighbors.
   - Provide a simple in-memory mapping from index position → `AnimeMetadata`.

4. **Recommendation Service**
   - Implement `services/recommender.py`:
     - Function to compute a user vector from liked/disliked anime.
     - Function to retrieve candidate neighbors from FAISS.
     - Function to compute final scores using a weighted formula.

5. **Filters & Explanations**
   - Implement `services/filters.py`:
     - Sequels/prequels filtering (interface only at first; can stub AniList).
     - Tag-based inclusion/exclusion filters.
   - Implement `services/explanations.py`:
     - Combine metadata + scores into short textual explanations.

6. **External API Stubs**
   - Create `clients/anilist_client.py` and `clients/mal_client.py` with stubbed async methods.
   - Implement minimal real calls later, using `httpx`.

7. **Dockerization**
   - Add a `Dockerfile` and `docker-compose.yml`:
     - API service.
     - Redis service.

## Style Preferences

- Use type hints and Pydantic models for request/response schemas.
- Handle errors explicitly; return meaningful HTTP status codes.
- Keep functions small and composable.
