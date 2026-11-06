-- The achievement guide assistant.
-- assistant_guide: one saved answer per achievement and language, the same for every player (it never contains
-- anyone's progress); grounded = written from reference text whose license allows reuse; sources is a JSON array.
-- assistant_usage: daily counters of new answers, per user ('user:<id>') and for everybody ('all').
CREATE TABLE assistant_guide (
    achievement_id bigint        NOT NULL REFERENCES achievement (id) ON DELETE CASCADE,
    language       varchar(5)    NOT NULL,
    text           varchar(8000) NOT NULL,
    grounded       boolean       NOT NULL,
    sources        text          NOT NULL DEFAULT '[]',
    model          varchar(100),
    created_at     timestamptz   NOT NULL,
    PRIMARY KEY (achievement_id, language)
);
ALTER TABLE assistant_guide ENABLE ROW LEVEL SECURITY;

CREATE TABLE assistant_usage (
    scope varchar(40) NOT NULL,
    day   date        NOT NULL,
    count integer     NOT NULL,
    PRIMARY KEY (scope, day)
);
ALTER TABLE assistant_usage ENABLE ROW LEVEL SECURITY;
