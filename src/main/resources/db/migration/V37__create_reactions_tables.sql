CREATE TABLE reactions(
    id uuid NOT NULL PRIMARY KEY,
    owner_name character varying(127) NOT NULL,
    entry_revision_id uuid NOT NULL,
    negative boolean NOT NULL,
    emoji integer NOT NULL,
    created_at timestamp without time zone NOT NULL,
    deadline timestamp without time zone,
    signature_version smallint,
    signature bytea
);
CREATE INDEX ON reactions(owner_name, entry_revision_id);
CREATE INDEX ON reactions(entry_revision_id);
ALTER TABLE reactions ADD FOREIGN KEY (entry_revision_id) REFERENCES entry_revisions(id) ON UPDATE CASCADE ON DELETE CASCADE;
CREATE INDEX ON reactions(entry_revision_id, negative, emoji);
CREATE INDEX ON reactions(deadline);

CREATE TABLE reaction_totals(
    id uuid NOT NULL PRIMARY KEY,
    entry_id uuid,
    entry_revision_id uuid,
    negative boolean NOT NULL,
    emoji integer NOT NULL,
    total integer NOT NULL,
    CHECK(entry_id IS NOT NULL OR entry_revision_id IS NOT NULL)
);
CREATE INDEX ON reaction_totals(entry_id);
CREATE INDEX ON reaction_totals(entry_revision_id);
ALTER TABLE reaction_totals ADD FOREIGN KEY (entry_id) REFERENCES entries(id) ON UPDATE CASCADE ON DELETE CASCADE;
ALTER TABLE reaction_totals ADD FOREIGN KEY (entry_revision_id) REFERENCES entry_revisions(id) ON UPDATE CASCADE ON DELETE CASCADE;
