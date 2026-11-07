-- "My latest unlocks" (GET /api/currentuser/achievements/recent): newest first for one user, without sorting all of a
-- big library's unlocks. Matches the query's ORDER BY exactly.
CREATE INDEX idx_user_achievement_user_recent
    ON user_achievement (user_id, unlocked_at DESC NULLS LAST, achievement_id DESC);
