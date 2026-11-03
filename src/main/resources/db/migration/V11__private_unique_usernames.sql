-- The public username is the only name other people see. Until now it was copied from the Google account
-- (display name or the start of the e-mail), which leaks a real identity. Everybody gets a random name
-- (the person can change it afterwards) and usernames become unique, ignoring case.
UPDATE users
SET username = 'player_' || substr(replace(gen_random_uuid()::text, '-', ''), 1, 8);

CREATE UNIQUE INDEX uq_users_username_lower ON users (lower(username));
