-- When the user finished (or skipped) the one-time "Make it yours" screen. Stored on the user, so every device
-- knows; null until then. Only written once: the first time is kept.
ALTER TABLE users ADD COLUMN setup_completed_at TIMESTAMPTZ NULL;

-- Everyone who already has an account is past that screen: only accounts created from now on will see it.
UPDATE users SET setup_completed_at = now() WHERE setup_completed_at IS NULL;
