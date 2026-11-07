-- Names an admin marked as "not a franchise" (Steam's franchise field often holds studio names, store brands or
-- placeholders). Compared by key: lower case, no accents, letters and digits only. Imports skip them.
CREATE TABLE franchise_blocklist (
    name_key varchar(200) PRIMARY KEY,
    name     varchar(255) NOT NULL,
    added_at timestamptz  NOT NULL
);
ALTER TABLE franchise_blocklist ENABLE ROW LEVEL SECURITY;

-- A subscription service, not a franchise.
INSERT INTO franchise_blocklist (name_key, name, added_at) VALUES ('eaplay', 'EA Play', now());
