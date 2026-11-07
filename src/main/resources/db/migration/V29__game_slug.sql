-- Readable, unique page addresses for games: /games/dark-souls-iii instead of /games/18-dark-souls-iii.
-- lavrel_slug: lower case, no accents, no ™ ® ©, anything else becomes "-" ("game" when nothing is left, e.g. a title in another
-- script). A repeated slug gets -2, -3... The slug is given once, when the game is created, and never changes, so links
-- keep working if a title is edited later.
CREATE OR REPLACE FUNCTION lavrel_slug(value text) RETURNS text
    LANGUAGE sql IMMUTABLE PARALLEL SAFE
    SET search_path = public, extensions, pg_catalog
AS $$
    SELECT coalesce(nullif(btrim(regexp_replace(lower(unaccent('unaccent'::regdictionary,
                                                                  regexp_replace(coalesce(value, ''), '[™®©]', '', 'g'))),
                                                '[^a-z0-9]+', '-', 'g'), '-'), ''), 'game')
$$;

CREATE OR REPLACE FUNCTION lavrel_free_game_slug(title text) RETURNS text
    LANGUAGE plpgsql
    SET search_path = public, extensions, pg_catalog
AS $$
DECLARE
    base      text := left(lavrel_slug(title), 180);
    candidate text := base;
    n         integer := 1;
BEGIN
    WHILE EXISTS (SELECT 1 FROM game WHERE slug = candidate) LOOP
        n := n + 1;
        candidate := base || '-' || n;
    END LOOP;
    RETURN candidate;
END
$$;

ALTER TABLE game ADD COLUMN slug varchar(200);

-- Existing games, oldest first, so the first game with a title keeps the plain slug.
DO $$
DECLARE
    g record;
BEGIN
    FOR g IN SELECT id, title FROM game ORDER BY id LOOP
        UPDATE game SET slug = lavrel_free_game_slug(g.title) WHERE id = g.id;
    END LOOP;
END
$$;

ALTER TABLE game ALTER COLUMN slug SET NOT NULL;
ALTER TABLE game ADD CONSTRAINT uq_game_slug UNIQUE (slug);

CREATE OR REPLACE FUNCTION lavrel_set_game_slug() RETURNS trigger
    LANGUAGE plpgsql
    SET search_path = public, extensions, pg_catalog
AS $$
BEGIN
    IF NEW.slug IS NULL THEN
        NEW.slug := lavrel_free_game_slug(NEW.title);
    END IF;
    RETURN NEW;
END
$$;

CREATE TRIGGER trg_game_slug BEFORE INSERT ON game FOR EACH ROW EXECUTE FUNCTION lavrel_set_game_slug();
