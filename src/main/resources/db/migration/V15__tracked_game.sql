-- The tracking list: games a user chose to follow, with a status. Only the user adds games here; a sync never
-- does. pin_position puts a game in the user's quick access (1 = first), with at most 20 pinned games.
CREATE TABLE tracked_game (
    user_id      bigint      NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    game_id      bigint      NOT NULL REFERENCES game (id) ON DELETE CASCADE,
    status       varchar(16) NOT NULL,
    pin_position smallint,
    added_at     timestamptz NOT NULL,
    updated_at   timestamptz NOT NULL,
    PRIMARY KEY (user_id, game_id),
    CONSTRAINT ck_tracked_game_status CHECK (status IN ('PLAYING', 'BACKLOG', 'COMPLETED', 'DROPPED')),
    CONSTRAINT ck_tracked_game_pin CHECK (pin_position IS NULL OR pin_position BETWEEN 1 AND 20)
);
CREATE INDEX idx_tracked_game_user_status ON tracked_game (user_id, status);
CREATE INDEX idx_tracked_game_game ON tracked_game (game_id);
ALTER TABLE tracked_game ENABLE ROW LEVEL SECURITY;
