-- Whether a public profile may show how many games the user owns (on the STATS shelf). Off by default, like every
-- other profile switch.
ALTER TABLE profile_settings ADD COLUMN show_library_size boolean NOT NULL DEFAULT false;
