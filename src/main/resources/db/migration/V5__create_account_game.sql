CREATE TABLE account_game (
    platform_account_id bigint      NOT NULL REFERENCES platform_account (id) ON DELETE CASCADE,
    game_id             bigint      NOT NULL REFERENCES game (id) ON DELETE CASCADE,
    playtime_minutes    integer     NOT NULL DEFAULT 0,
    last_played_at      timestamptz,
    last_synced_at      timestamptz,
    PRIMARY KEY (platform_account_id, game_id),
    CONSTRAINT ck_account_game_playtime CHECK (playtime_minutes >= 0)
);
-- The stats query filters by game.
CREATE INDEX idx_account_game_game_id ON account_game (game_id);
ALTER TABLE account_game ENABLE ROW LEVEL SECURITY;
