-- Clear caches to force refetching of detail payloads with alternative_titles included
DELETE FROM anime_cache;
DELETE FROM generic_cache;
