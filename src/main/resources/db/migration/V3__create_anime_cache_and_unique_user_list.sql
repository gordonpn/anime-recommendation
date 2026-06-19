-- Drop the old user_anime_list table
DROP TABLE IF EXISTS user_anime_list;

-- Recreate user_anime_list table with unique constraint on anime_id
CREATE TABLE user_anime_list
(
    id         INTEGER PRIMARY KEY AUTOINCREMENT,
    anime_id   INTEGER NOT NULL UNIQUE,
    status     TEXT    NOT NULL CHECK (status IN ('completed', 'plan_to_watch', 'dropped', 'watching', 'on_hold')),
    user_score INTEGER
);

-- Create anime_cache table
CREATE TABLE anime_cache
(
    anime_id     INTEGER PRIMARY KEY NOT NULL,
    title        TEXT NOT NULL,
    details_json TEXT NOT NULL,
    cached_at    TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
