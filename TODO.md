# TODO

A rough task list for the project, suitable for human or agent-driven implementation.

## Phase 1: Skeleton

- [ ] Initialize Python project structure (`app/`, `scripts/`, `data/`).
- [ ] Add `pyproject.toml` or `requirements.txt` for:
  - fastapi
  - uvicorn[standard]
  - httpx
  - sentence-transformers
  - faiss-cpu
  - redis
  - pydantic
  - (optional) polars or pandas
- [ ] Implement `app/main.py` with:
  - FastAPI app instance.
  - `/health` route.

## Phase 2: Metadata & Search

- [ ] Define domain models in `app/models/domain.py`.
- [ ] Create loader for local metadata file in `data/processed/`.
- [ ] Implement `GET /search` endpoint.

## Phase 3: Embeddings & FAISS

- [ ] Implement `scripts/build_index.py` to:
  - Load metadata.
  - Build text fields.
  - Compute embeddings using sentence-transformers.
  - Build and persist FAISS index and mapping.
- [ ] Implement FAISS index load/query helper in `app/infra/faiss_index.py`.

## Phase 4: Recommender

- [ ] Implement vector aggregation for liked/disliked anime.
- [ ] Implement initial scoring function (similarity + popularity + rating).
- [ ] Implement `POST /recommend` route.

## Phase 5: Filters & Hybrid Mode

- [ ] Implement filters for:
  - Year range.
  - Genre/tag inclusion/exclusion.
  - Sequels/prequels (stub).
- [ ] Implement AniList and MAL client stubs.
- [ ] Wire hybrid recommendations (vector + API-based).

## Phase 6: Infra

- [ ] Add Redis integration for caching.
- [ ] Add Dockerfile and docker-compose.yml.
- [ ] Add logging for requests and recommendation calls.
