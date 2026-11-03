-- Game description and Steam store page, shown by the frontend.
ALTER TABLE game ADD COLUMN description varchar(2000);
ALTER TABLE game ADD COLUMN store_url   varchar(255);

-- Store page for the games that were imported before this migration.
UPDATE game g
SET store_url = 'https://store.steampowered.com/app/' || gm.platform_game_id
FROM game_mapping gm
WHERE gm.game_id = g.id AND gm.platform = 'STEAM';

-- Apps the platform cannot give us as a game (delisted, tools, soundtracks, DLC...).
-- Remembered so that a sync never wastes Steam calls on them again.
CREATE TABLE catalog_import_failure (
    platform         varchar(16)  NOT NULL,
    platform_game_id varchar(128) NOT NULL,
    reason           varchar(255) NOT NULL,
    attempts         integer      NOT NULL DEFAULT 1,
    failed_at        timestamptz  NOT NULL,
    PRIMARY KEY (platform, platform_game_id),
    CONSTRAINT ck_catalog_import_failure_platform CHECK (platform IN ('STEAM', 'XBOX', 'PSN'))
);
ALTER TABLE catalog_import_failure ENABLE ROW LEVEL SECURITY;
