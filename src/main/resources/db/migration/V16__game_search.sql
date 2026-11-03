-- Game search (GET /api/games): accent-insensitive, typo-tolerant name matching with pg_trgm and unaccent, both
-- available on Supabase. On Supabase the extensions live in the "extensions" schema, which is on the search path;
-- the normalize function pins its own search path so it works wherever the extensions were installed.
CREATE EXTENSION IF NOT EXISTS pg_trgm;
CREATE EXTENSION IF NOT EXISTS unaccent;

-- Lower case, no accents, punctuation as single spaces: "The Witcher® 3: Wild Hunt" -> "the witcher 3 wild hunt".
-- Declared IMMUTABLE so it can feed a stored column and an index (unaccent with an explicit dictionary is stable).
CREATE OR REPLACE FUNCTION lavrel_normalize(value text) RETURNS text
    LANGUAGE sql IMMUTABLE PARALLEL SAFE
    SET search_path = public, extensions, pg_catalog
AS $$
    SELECT btrim(regexp_replace(lower(unaccent('unaccent'::regdictionary, coalesce(value, ''))),
                                '[^[:alnum:]]+', ' ', 'g'))
$$;

ALTER TABLE game ADD COLUMN search_name text GENERATED ALWAYS AS (lavrel_normalize(title)) STORED;

CREATE INDEX idx_game_search_name_trgm ON game USING gin (search_name gin_trgm_ops);
CREATE INDEX idx_game_published_reviews ON game (review_count DESC NULLS LAST) WHERE enriched_at IS NOT NULL;
CREATE INDEX idx_game_published_release ON game (release_date DESC NULLS LAST) WHERE enriched_at IS NOT NULL;
