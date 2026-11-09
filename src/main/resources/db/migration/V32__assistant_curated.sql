-- Guides an admin wrote or had rewritten ("curated"): they never expire and are never replaced automatically.
ALTER TABLE assistant_guide ADD COLUMN curated boolean NOT NULL DEFAULT false;
