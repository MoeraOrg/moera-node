DELETE FROM reactions;
ALTER TABLE reactions ADD COLUMN moment bigint NOT NULL;
CREATE INDEX ON reactions(moment, entry_revision_id);
