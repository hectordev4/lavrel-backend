-- The systems a game is available on, by family: PC, XBOX, PLAYSTATION (Nintendo later). PC comes from Steam
-- (source IMPORTED); Xbox and PlayStation are set by an admin (source MANUAL) until their adapters fill them.
CREATE TABLE game_system (
    game_id bigint      NOT NULL REFERENCES game (id) ON DELETE CASCADE,
    system  varchar(16) NOT NULL,
    source  varchar(16) NOT NULL,
    PRIMARY KEY (game_id, system),
    CONSTRAINT ck_game_system_system CHECK (system IN ('PC', 'XBOX', 'PLAYSTATION')),
    CONSTRAINT ck_game_system_source CHECK (source IN ('IMPORTED', 'MANUAL'))
);
CREATE INDEX idx_game_system_system ON game_system (system);
ALTER TABLE game_system ENABLE ROW LEVEL SECURITY;

-- Every game in the catalog so far comes from Steam, so it is available on PC.
INSERT INTO game_system (game_id, system, source)
SELECT DISTINCT game_id, 'PC', 'IMPORTED' FROM game_mapping WHERE platform = 'STEAM'
ON CONFLICT DO NOTHING;
