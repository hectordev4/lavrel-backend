-- Achievement icons that survive Steam moving its images to another host.
-- Steam's API still hands out addresses on old hosts (steamcdn-a.akamaihd.net) that stopped answering. Each icon now
-- keeps its Steam reference (icon_path = "{appId}/{file}.jpg"); the address served is the configured host + that
-- reference, so a host change is one setting (app.steam.image-base, copied into app_setting at start-up).
-- icon_mirrored: an admin copied the icon into our own storage (bucket "achievement-icons"); icon_url then points
-- there and imports or refreshes keep it while the Steam picture stays the same.
CREATE TABLE app_setting (
    key   varchar(100) PRIMARY KEY,
    value varchar(500) NOT NULL
);
ALTER TABLE app_setting ENABLE ROW LEVEL SECURITY;
INSERT INTO app_setting (key, value)
VALUES ('steam_image_base', 'https://shared.fastly.steamstatic.com/community_assets/images/apps/');

ALTER TABLE achievement ADD COLUMN icon_path varchar(300);
ALTER TABLE achievement ADD COLUMN icon_mirrored boolean NOT NULL DEFAULT false;

-- "{appId}/{file}" from any Steam image address (old or new host), or null for anything else.
CREATE OR REPLACE FUNCTION lavrel_steam_icon_path(url text) RETURNS text
    LANGUAGE sql IMMUTABLE PARALLEL SAFE
AS $$
    SELECT (regexp_match(url, '/images/apps/([0-9]+/[A-Za-z0-9_.-]+\.(?:jpg|jpeg|png|gif))(?:[?#].*)?$'))[1]
$$;

CREATE OR REPLACE FUNCTION lavrel_achievement_icon() RETURNS trigger
    LANGUAGE plpgsql
AS $$
DECLARE
    path text := lavrel_steam_icon_path(NEW.icon_url);
    base text;
BEGIN
    IF path IS NULL THEN
        RETURN NEW;                       -- not a Steam address (e.g. our own copy): keep it as it is
    END IF;
    IF TG_OP = 'UPDATE' AND OLD.icon_mirrored AND NEW.icon_mirrored AND OLD.icon_path = path THEN
        NEW.icon_url := OLD.icon_url;     -- same Steam picture: keep serving our copy
        RETURN NEW;
    END IF;
    SELECT value INTO base FROM app_setting WHERE key = 'steam_image_base';
    NEW.icon_path := path;
    NEW.icon_mirrored := false;
    IF base IS NOT NULL THEN
        NEW.icon_url := base || path;
    END IF;
    RETURN NEW;
END
$$;

CREATE TRIGGER trg_achievement_icon
    BEFORE INSERT OR UPDATE OF icon_url ON achievement
    FOR EACH ROW EXECUTE FUNCTION lavrel_achievement_icon();

-- Every stored icon gets its reference and moves to the current host.
UPDATE achievement SET icon_url = icon_url WHERE icon_url IS NOT NULL;
