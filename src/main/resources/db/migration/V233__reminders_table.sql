CREATE TABLE reminders (
    id uuid PRIMARY KEY,
    node_id uuid NOT NULL,
    story_type smallint NOT NULL,
    priority integer NOT NULL,
    published_at timestamp without time zone,
    story_id UUID,
    read_at timestamp without time zone,
    read_count integer NOT NULL DEFAULT 0,
    next_at timestamp without time zone NOT NULL
);

CREATE INDEX reminders_node_id_idx ON reminders(node_id);
ALTER TABLE reminders ADD FOREIGN KEY (story_id) REFERENCES stories(id)
    ON UPDATE CASCADE ON DELETE SET NULL;

INSERT INTO reminders
SELECT uuid_generate_v4() AS id, domains.node_id AS node_id, 41 AS story_type, 0 as priority, NULL AS published_at,
       NULL AS story_id, NULL AS read_at, 0 as read_count, now() AS next_at
FROM domains;
