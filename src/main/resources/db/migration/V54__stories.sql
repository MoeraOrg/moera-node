CREATE TABLE stories (
    id uuid NOT NULL DEFAULT uuid_generate_v4() PRIMARY KEY,
    node_id uuid NOT NULL,
    feed_name character varying(63) NOT NULL DEFAULT 'timeline',
    story_type smallint NOT NULL DEFAULT 0,
    created_at timestamp without time zone NOT NULL,
    moment bigint NOT NULL,
    viewed boolean NOT NULL DEFAULT false,
    read boolean NOT NULL DEFAULT false,
    entry_id uuid
);
CREATE INDEX ON stories(node_id, feed_name, moment);
CREATE INDEX ON stories(entry_id);
ALTER TABLE stories ADD FOREIGN KEY (entry_id) REFERENCES entries(id) ON UPDATE CASCADE ON DELETE CASCADE;
INSERT INTO stories(node_id, created_at, moment, entry_id)
SELECT entries.node_id, entry_revisions.created_at, entry_revisions.moment, entries.id
FROM entries
     INNER JOIN entry_revisions
     ON entries.current_revision_id = entry_revisions.id
WHERE entries.deleted_at IS NULL;
ALTER TABLE entry_revisions DROP COLUMN moment;
