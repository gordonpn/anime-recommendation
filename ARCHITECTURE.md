# Architecture

This document describes the planned architecture so an AI coding agent can generate code aligned with the design.

## Overview

The system has three main parts:

1. **Offline Preprocessing & Index Builder**
   - Load anime metadata from datasets + APIs.
   - Build text representations (title + synopsis + tags/genres).
   - Compute embeddings using `sentence-transformers`.
   - Build and persist a FAISS index.

2. **Online API Server (FastAPI)**
   - Serve search and recommendation endpoints.
   - Query FAISS for nearest neighbors.
   - Combine vector similarity with external recommendations from AniList/MAL.
   - Apply custom ranking, filtering, and explanation logic.

3. **Integration Layer (External APIs & Mapping)**
   - AniList GraphQL client.
   - MyAnimeList REST client.
   - ID mapping manager (AniList ID ↔ MAL ID).

## Directory Structure (planned)

```text
.
├── app
│   ├── main.py               # FastAPI entrypoint
│   ├── config.py             # Settings (env, paths, API keys)
│   ├── api
│   │   ├── routes_recommend.py
│   │   ├── routes_search.py
│   │   └── dependencies.py
│   ├── services
│   │   ├── recommender.py    # Core ranking + vec math
│   │   ├── filters.py        # Sequels, tags, blacklists
│   │   ├── explanations.py   # "Why was this recommended?"
│   │   └── mapping.py        # AniList ↔ MAL mapping logic
│   ├── clients
│   │   ├── anilist_client.py # GraphQL calls
│   │   └── mal_client.py     # REST calls
│   ├── models
│   │   ├── domain.py         # Anime, Recommendation, etc.
│   │   └── schemas.py        # Pydantic models for API
│   ├── infra
│   │   ├── faiss_index.py    # Index loading/querying
│   │   └── cache.py          # Redis wrapper
│   └── utils
│       └── logging.py
├── data
│   ├── raw/                  # raw datasets
│   ├── processed/            # cleaned/normalized data
│   └── indexes/              # FAISS index + metadata
└── scripts
    ├── build_index.py        # Offline embedding + FAISS index builder
    └── sync_metadata.py      # Fetch/refresh metadata + mappings
```

## Data Flow (Happy Path)

1. **Offline**
   - `scripts/sync_metadata.py`:
     - Download or read anime datasets (CSV/JSON).
     - Enrich with AniList/MAL data if needed.
     - Normalize into a consistent schema (IDs, titles, synopsis, tags).
   - `scripts/build_index.py`:
     - Load normalized data.
     - Build text field: `"<title> [SEP] <genres> [SEP] <tags> [SEP] <synopsis>"`.
     - Use a sentence-transformers model (e.g., `all-MiniLM-L6-v2`) to create embeddings.
     - Initialize FAISS index (`IndexFlatL2` or similar) and add vectors.
     - Persist index and metadata (e.g., `index.faiss`, `anime_metadata.parquet`).

2. **Online**
   - `app/main.py` loads:
     - Config.
     - Redis client.
     - FAISS index + metadata into memory.
   - `POST /recommend`:
     - Accept input anime IDs (AniList or MAL, plus a type flag).
     - Map IDs to internal records and retrieve vectors.
     - Compute:
       - Centroid of liked anime vectors.
       - Optional subtraction/penalties for disliked anime (vector + tag filters).
     - Query FAISS for nearest neighbors.
     - Fetch additional scores (popularity, rating) from metadata and/or APIs.
     - Combine signals into a final ranking.
     - Filter out unwanted items (e.g., sequels/prequels if requested).
     - Return List[Recommendation] with explanation fields.

## Ranking Strategy (Initial)

The recommendation score may combine:

- `similarity_score` (vector-based, from FAISS distances).
- `popularity_score` (normalized from datasets or APIs).
- `rating_score` (normalized user score).

Example:

- `final_score = 0.7 * similarity_score + 0.2 * popularity_score + 0.1 * rating_score`.

Agents should keep the scoring logic in `services/recommender.py` and make it easy to tune weights.

## Filtering Rules

- **Sequels / Prequels:**
  - Use AniList relations to detect SEQUEL, PREQUEL, SIDE_STORY, etc.
  - Option to:
    - Exclude them from recommendations.
    - Group them under a “Franchise” node.
    - Or show separately in “Watch Next” suggestions.

- **Disliked Tags / Genres:**
  - Derive tags/genres from disliked anime.
  - Apply hard filters or heavy negative weights to candidates sharing those tags.

- **Season / Year / Genre Filters:**
  - Allow optional query params:
    - `min_year`, `max_year`
    - `genres_in`, `genres_exclude`

## External Services Integration

- **AniList**
  - Use minimal GraphQL queries for:
    - Metadata (title, tags, relations).
    - Recommendations.
  - Implement simple paging/limits.

- **MyAnimeList**
  - Use API v2 endpoints for:
    - Metadata.
    - Recommendations.
  - Respect rate limits and cache responses in Redis.

Agents should generate small, focused client modules in `app/clients/` with explicit methods instead of generic wrappers.

```

***

### `REQUIREMENTS.md`

```markdown
# Requirements

This file describes functional and non-functional requirements for the project. It is written so that an AI coding agent can treat each bullet as a unit of work.

## Functional Requirements

### 1. Search Anime

- Implement `GET /search` endpoint.
- Inputs:
  - `query` (string; anime title search).
  - Optional: `limit`, `offset`.
- Behavior:
  - Search locally stored metadata (no external API on each request).
  - Return matching anime with IDs, titles, year, and basic info.

### 2. Recommend Based on Liked Anime

- Implement `POST /recommend`.
- Inputs (JSON body):
  - `liked_ids`: list of anime IDs (AniList or MAL).
  - `id_type`: `"anilist"` or `"mal"`.
  - Optional:
    - `disliked_ids`: list of IDs.
    - `min_year`, `max_year`.
    - `include_sequels`: boolean (default: false).
    - `limit`: max number of results.
- Behavior:
  - Map external IDs to internal indices using precomputed mapping.
  - Retrieve vectors for liked/disliked anime.
  - Compute a user preference vector (centroid of liked; subtract weighted disliked).
  - Query FAISS for nearest neighbors.
  - Apply filters (year, sequels, tags).
  - Compute final scores and rank candidates.
  - Return:
    - `anime_id`, `source_ids` (AniList/MAL), `title`, `year`.
    - `scores` (similarity, popularity, rating, final_score).
    - `explanation` (textual summary such as “similar to X and shares tags A, B”).

### 3. Use External Recommendations (Hybrid Mode)

- For each liked anime, optionally fetch:
  - AniList recommendations.
  - MAL recommendations.
- Combine these lists with vector-based candidates:
  - Boost candidates that appear in both API recommendations and vector results.
  - De-duplicate by internal ID.
- Cache external API results in Redis with a reasonable TTL.

### 4. Offline Index Building

- Provide `scripts/build_index.py` that:
  - Loads normalized anime metadata from `data/processed`.
  - Builds text inputs for embeddings.
  - Computes embeddings via `sentence-transformers`.
  - Creates a FAISS index and saves it alongside a metadata file.

- Provide `scripts/sync_metadata.py` that:
  - Reads from a dataset (e.g., Kaggle anime dataset) and/or API.
  - Normalizes into a single schema with fields:
    - `internal_id`, external IDs, titles, synopsis, tags, genres, year, popularity, rating.

### 5. Health & Introspection

- Implement `GET /health` endpoint returning simple status JSON.
- Implement `GET /meta/models`:
  - Return information about:
    - Embedding model name.
    - Index type.
    - Number of items in index.

## Non-Functional Requirements

- Use async I/O for external HTTP calls.
- Keep API responses deterministic and JSON-serializable.
- Separate configuration (env vars) from code.
- Provide a `Dockerfile` suitable for production-ish use (no dev-only layers).
- Optional: Add basic logging for each request and for index loading.
