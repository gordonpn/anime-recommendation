CREATE TABLE IF NOT EXISTS tokens
(
    id
        INTEGER
        PRIMARY
            KEY AUTOINCREMENT,
    access_token
        TEXT
        NOT
            NULL,
    refresh_token
        TEXT
        NOT
            NULL,
    expires_at
        TIMESTAMP
);