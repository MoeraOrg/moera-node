CREATE TABLE entry_sources (
    id uuid NOT NULL PRIMARY KEY,
    entry_id uuid NOT NULL,
    remote_node_name character varying(63) NOT NULL,
    remote_feed_name character varying(63) NOT NULL,
    remote_posting_id character varying(40) NOT NULL,
    created_at timestamp without time zone NOT NULL
);
CREATE INDEX ON entry_sources(entry_id);
ALTER TABLE entry_sources ADD FOREIGN KEY (entry_id) REFERENCES entries(id) ON UPDATE CASCADE ON DELETE CASCADE;
