-- Public profiles: what each user lets other people see. No row = everything private (the default). A profile that
-- is not public answers 404 and never appears in the player search.
CREATE TABLE profile_settings (
    user_id        bigint  PRIMARY KEY REFERENCES users (id) ON DELETE CASCADE,
    profile_public boolean NOT NULL DEFAULT false,
    show_country   boolean NOT NULL DEFAULT false,
    show_platforms boolean NOT NULL DEFAULT false,
    show_avatar    boolean NOT NULL DEFAULT false
);
CREATE INDEX idx_profile_settings_public ON profile_settings (user_id) WHERE profile_public;
ALTER TABLE profile_settings ENABLE ROW LEVEL SECURITY;
