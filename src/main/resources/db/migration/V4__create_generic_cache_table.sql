-- Create generic_cache table to store raw MAL JSON payloads
CREATE TABLE IF NOT EXISTS generic_cache
(
    cache_key    TEXT PRIMARY KEY NOT NULL,
    cache_value  TEXT NOT NULL,
    cached_at    TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
