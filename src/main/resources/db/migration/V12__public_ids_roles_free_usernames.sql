-- 1. Usernames become free display names, like on Steam: several people may share one. The unique index
--    from V11 goes; the random "player_xxxxxxxx" names stay as they are.
DROP INDEX IF EXISTS uq_users_username_lower;

-- 2. The public id: a random 16-digit number that identifies a user to other people (profile addresses,
--    community search). The database id stays internal, because it is sequential and would reveal how many
--    users there are. Every existing user gets one now (the default is evaluated per row); new users get one
--    from the application, which checks it is free. The default also covers tooling such as the fake data.
ALTER TABLE users
    ADD COLUMN public_id bigint NOT NULL
        DEFAULT (1000000000000000 + floor(random() * 9000000000000000)::bigint);
ALTER TABLE users ADD CONSTRAINT uq_users_public_id UNIQUE (public_id);
ALTER TABLE users ADD CONSTRAINT ck_users_public_id CHECK (public_id BETWEEN 1000000000000000 AND 9999999999999999);

-- 3. The role. Everybody is a USER; an ADMIN promotes people through PATCH /api/admin/users/{publicId}/role.
--    The first admin is promoted by hand, in the Supabase SQL editor:
--      UPDATE users SET role = 'ADMIN' WHERE firebase_uid = '<your Firebase UID>';
ALTER TABLE users ADD COLUMN role varchar(16) NOT NULL DEFAULT 'USER';
ALTER TABLE users ADD CONSTRAINT ck_users_role CHECK (role IN ('USER', 'SUPPORT', 'ADMIN'));
