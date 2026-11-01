CREATE TABLE user_achievement (
    user_id           bigint      NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    achievement_id    bigint      NOT NULL REFERENCES achievement (id) ON DELETE CASCADE,
    source_account_id bigint      REFERENCES platform_account (id) ON DELETE SET NULL,
    unlocked_at       timestamptz NOT NULL,
    recorded_at       timestamptz NOT NULL DEFAULT now(),
    -- One row per user and achievement, never repeated. The insert uses ON CONFLICT on this key.
    PRIMARY KEY (user_id, achievement_id)
);
CREATE INDEX idx_user_achievement_achievement_id ON user_achievement (achievement_id);
ALTER TABLE user_achievement ENABLE ROW LEVEL SECURITY;
