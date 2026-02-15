# Anime Recommendation

## Development

### Prerequisites

- JDK23

### Run

To run the app

```sh
./gradlew clean bootJar && java -jar build/libs/anime-0.0.1-SNAPSHOT.jar
```

## To-do

- [x] Persist the token, to reuse on next run
- [x] Persist fetched anime details for certain time, to avoid unnecessary refetches
- [ ] Check the sync status table on start and skip is unnecessary
- [ ] Fetch anime details for each anime in the user list and persist

An anime recommendation service that combines **content-based similarity** (text embeddings + vector search) with **graph data** from [AniList](navigational_search:AniList API) and [MyAnimeList](navigational_search:MyAnimeList API) to produce ranked anime recommendations.

The focus is on:
- Learning **fundamental recommender system concepts** (similarity, ranking, filtering).
- Using **adopted, industry-relevant tools** (FastAPI, sentence-transformers, FAISS, Redis).
- Keeping the codebase **agent-friendly** and easy to extend.

## High-Level Features

- Search for one or more anime and get a ranked recommendation list.
- Combine **vector similarity** (synopsis + tags) with **API recommendations** from AniList/MAL.
- Exclude or de-prioritize sequels/prequels when desired.
- Allow users to mark anime they **liked** and **did not like**.
- Filter suggestions by season/year and genres.

## Tech Stack

**Backend**

- Language: JDK23+
- Web framework:
- HTTP client:
- GraphQL client: minimal wrapper around AniList GraphQL using `httpx` or `gql`.
- Recommender:
  - `sentence-transformers` for text embeddings.
  - `faiss-cpu` for approximate nearest-neighbor search.
- DataFrame / ETL: `polars` or `pandas` (for offline preprocessing).
- Cache: Redis (for API responses and ID mappings).

**Data Sources**

- AniList API (GraphQL) for anime metadata, relations, and recommendations.
- MyAnimeList API v2 for recommendations and additional metadata.
- Community ID mapping dataset for MAL ↔ AniList ID conversion.
- Optional: public anime datasets (e.g., Kaggle) for bulk preprocessing (synopsis, tags, genres).

**Deployment**

- Containerized with Docker.
- Optional: Docker Compose setup for `api`, `redis`, and a `worker` or `preprocessor`.
- Future: Deployment to k3s.

## Core Design Goals

- **Explainable recommendations** (e.g., “Recommended because it is similar to X and shares tags Y/Z”).
- Clear separation between:
  - Data ingestion & preprocessing.
  - Online serving (API).
  - External integrations (AniList/MAL clients).
- Avoid over-abstracted magic; keep core ranking/filtering logic explicit.

## Getting Started (for an agent)

1. Set up a basic FastAPI app with a health check route.
2. Add configuration loading (env vars) for API keys and data paths.
3. Implement a simple `/search` endpoint that searches local anime metadata by title.
4. Add offline embedding + FAISS index builder.
5. Implement `/recommend`:
   - Input: list of liked anime IDs, optional disliked IDs, optional filters.
   - Output: ranked list of anime with reasons and metadata.
