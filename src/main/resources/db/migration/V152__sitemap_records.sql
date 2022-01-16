CREATE TABLE sitemap_records (
    id uuid NOT NULL PRIMARY KEY,
    node_id uuid NOT NULL,
    sitemap_id uuid NOT NULL,
    entry_id uuid NOT NULL,
    created_at timestamp without time zone NOT NULL,
    modified_at timestamp without time zone NOT NULL,
    total_updates integer NOT NULL
);
CREATE INDEX sitemap_records_node_id_sitemap_id_modified_at_idx ON sitemap_records(node_id, sitemap_id, modified_at);
CREATE UNIQUE INDEX sitemap_records_entry_id_idx ON sitemap_records(entry_id);
ALTER TABLE sitemap_records ADD FOREIGN KEY (entry_id) REFERENCES entries(id)
    ON UPDATE CASCADE ON DELETE CASCADE;
