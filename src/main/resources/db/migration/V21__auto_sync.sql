-- Automatic library sync: users.last_seen_at (when the user last used the app, hour precision) limits it to active
-- users; users.auto_sync lets each user turn it off (on by default). Existing users count as seen now, so their
-- accounts are included from the first round.
ALTER TABLE users ADD COLUMN last_seen_at timestamptz;
ALTER TABLE users ADD COLUMN auto_sync boolean NOT NULL DEFAULT true;
UPDATE users SET last_seen_at = now();
CREATE INDEX idx_platform_account_last_synced ON platform_account (last_synced_at NULLS FIRST);
