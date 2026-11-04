-- Some Steam achievements have names (or API names) longer than 255 characters, which made a whole sync fail with
-- "value too long for type character varying(255)". Wider columns, plus clipping in the code (Achievement) as a guard.
ALTER TABLE achievement ALTER COLUMN steam_achievement_id TYPE varchar(1024);
ALTER TABLE achievement ALTER COLUMN name TYPE varchar(1024);
ALTER TABLE achievement ALTER COLUMN description TYPE varchar(8000);
ALTER TABLE achievement_mapping ALTER COLUMN platform_achievement_id TYPE varchar(1024);
