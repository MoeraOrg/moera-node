ALTER TABLE entries rename TO entry_revisions;
SELECT entry_id AS id, node_id, entry_type, owner_name, owner_generation, MIN(created_at) AS created_at
INTO entries
FROM entry_revisions
GROUP BY entry_id, node_id, entry_type, owner_name, owner_generation;
ALTER TABLE entry_revisions DROP CONSTRAINT entries_pkey;
DROP INDEX entries_entry_id_created_at_idx;
DROP INDEX entries_entry_id_deleted_at_idx;
DROP INDEX entries_entry_type_idx;
DROP INDEX entries_node_id_idx;
DROP INDEX entries_node_id_moment_idx;
ALTER TABLE entries ALTER COLUMN id SET NOT NULL;
ALTER TABLE entries ADD PRIMARY KEY(id);
ALTER TABLE entries ALTER COLUMN node_id SET NOT NULL;
ALTER TABLE entries ALTER COLUMN entry_type SET NOT NULL;
ALTER TABLE entries ALTER COLUMN created_at SET NOT NULL;
CREATE INDEX ON entries(node_id);
CREATE INDEX ON entries(entry_type);
ALTER TABLE entry_revisions DROP COLUMN node_id;
ALTER TABLE entry_revisions DROP COLUMN entry_type;
ALTER TABLE entry_revisions DROP COLUMN owner_name;
ALTER TABLE entry_revisions DROP COLUMN owner_generation;
ALTER TABLE entry_revisions ADD PRIMARY KEY(id);
CREATE INDEX ON entry_revisions(entry_id);
CREATE INDEX ON entry_revisions(moment);
CREATE INDEX ON entry_revisions(created_at);
ALTER TABLE entry_revisions ADD FOREIGN KEY (entry_id) REFERENCES entries(id) ON UPDATE CASCADE ON DELETE CASCADE;
ALTER TABLE entries ADD COLUMN current_revision_id uuid;
UPDATE entries
SET current_revision_id=(
    SELECT id
    FROM entry_revisions
    WHERE created_at=(
        SELECT MAX(created_at) FROM entry_revisions WHERE entry_id=entries.id
    )
);
ALTER TABLE entries ALTER COLUMN current_revision_id SET NOT NULL;
CREATE INDEX ON entries(current_revision_id);
ALTER TABLE entries ADD FOREIGN KEY (current_revision_id) REFERENCES entry_revisions(id) ON UPDATE SET NULL ON DELETE SET NULL;
ALTER TABLE entries ADD COLUMN deleted_at TIMESTAMP WITHOUT TIME ZONE;
UPDATE entries SET deleted_at=(SELECT deleted_at FROM entry_revisions WHERE id=entries.current_revision_id);
CREATE INDEX ON entries(node_id, deleted_at);
ALTER TABLE entries ADD COLUMN total_revisions INT;
UPDATE entries SET total_revisions=(SELECT COUNT(*) FROM entry_revisions WHERE entry_id=entries.id);
ALTER TABLE entries ALTER COLUMN total_revisions SET NOT NULL;
