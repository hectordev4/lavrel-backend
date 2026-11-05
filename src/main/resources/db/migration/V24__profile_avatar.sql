-- Profile pictures: GENERATED (drawn by the frontend from the public id, the default), GALLERY (one of our default
-- pictures, by id) or UPLOADED (a picture in the Supabase Storage bucket "avatars", by path).
ALTER TABLE profile_settings ADD COLUMN avatar_kind varchar(16) NOT NULL DEFAULT 'GENERATED';
ALTER TABLE profile_settings ADD COLUMN avatar_value varchar(255);
ALTER TABLE profile_settings ADD CONSTRAINT ck_profile_settings_avatar_kind
    CHECK (avatar_kind IN ('GENERATED', 'GALLERY', 'UPLOADED'));
ALTER TABLE profile_settings ADD CONSTRAINT ck_profile_settings_avatar_value
    CHECK ((avatar_kind = 'GENERATED') = (avatar_value IS NULL));
