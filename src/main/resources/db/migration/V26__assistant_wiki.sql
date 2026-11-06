-- Reference wikis for the assistant: at most one per game, approved by an admin who checked that its content license
-- allows reuse (stored with it and shown under every answer built from it). Only the address is stored; page text is
-- read live when a guide is written.
CREATE TABLE assistant_wiki (
    game_id  bigint       PRIMARY KEY REFERENCES game (id) ON DELETE CASCADE,
    api_url  varchar(300) NOT NULL,
    license  varchar(100) NOT NULL,
    added_at timestamptz  NOT NULL
);
ALTER TABLE assistant_wiki ENABLE ROW LEVEL SECURITY;
