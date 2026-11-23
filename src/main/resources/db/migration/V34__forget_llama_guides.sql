-- The assistant moves from Llama 3.1 8B to Gemma 4 26B, with new prompts (game names kept as the wiki writes them,
-- general advice only without a wiki). Saved guides written before would keep the old answers for up to 30 days,
-- including names translated word for word, so they are forgotten: the next player who opens one gets a new guide.
-- Curated guides (written or reviewed by an admin) stay.
DELETE FROM assistant_guide WHERE curated = false;
