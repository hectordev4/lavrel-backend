-- Content-policy exceptions: games an admin lets into the catalog although the content policy would reject
-- them (Marvel Rivals, whose user tags look adult). A flag on the game, no extra table. Exempt games skip the
-- policy on import, enrich and refresh; they must still be real games with achievements.
-- Managed through /api/admin/catalog/steam/exceptions.
ALTER TABLE game ADD COLUMN content_exempt boolean NOT NULL DEFAULT false;
