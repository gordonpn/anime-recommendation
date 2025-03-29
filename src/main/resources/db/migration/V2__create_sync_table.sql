CREATE TABLE sync_status
(
    list_type      TEXT PRIMARY KEY NOT NULL CHECK (list_type IN
                                                    ('completed', 'plan_to_watch', 'dropped', 'watching', 'on_hold')),
    last_synced_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE user_anime_list
(
    id         INTEGER PRIMARY KEY AUTOINCREMENT,
    anime_id   INTEGER NOT NULL,
    status     TEXT    NOT NULL CHECK (status IN ('completed', 'plan_to_watch', 'dropped', 'watching', 'on_hold')),
    user_score INTEGER
);