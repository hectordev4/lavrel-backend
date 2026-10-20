-- Steam's curated genres (Action, RPG, Indie...), copied into our own tables so the catalog does not depend
-- on Steam at read time, and so they can be used as a filter.
CREATE TABLE genre (
    id   integer      PRIMARY KEY,           -- Steam's own genre id
    name varchar(100) NOT NULL
);
ALTER TABLE genre ENABLE ROW LEVEL SECURITY;

CREATE TABLE game_genre (
    game_id  bigint  NOT NULL REFERENCES game (id) ON DELETE CASCADE,
    genre_id integer NOT NULL REFERENCES genre (id),
    PRIMARY KEY (game_id, genre_id)
);
CREATE INDEX idx_game_genre_genre ON game_genre (genre_id);
ALTER TABLE game_genre ENABLE ROW LEVEL SECURITY;

-- When the details of a game were last copied from Steam. Tags, genres and the rest never change on their
-- own: they are only re-copied by the admin "refresh" endpoint. NULL means "never refreshed", which is true
-- for every game that exists today (they have no genres yet), so they are all due for one refresh.
-- Games stay visible while they wait: visibility depends on enriched_at, not on this column.
ALTER TABLE game ADD COLUMN refreshed_at timestamptz;
