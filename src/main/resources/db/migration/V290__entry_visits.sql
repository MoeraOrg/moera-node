ALTER TABLE entries ADD COLUMN view_count integer NOT NULL DEFAULT 0;

CREATE TABLE entry_visits (
    id uuid NOT NULL PRIMARY KEY,
    node_id uuid NOT NULL,
    entry_id uuid NOT NULL,
    client_id varchar(24),
    client_name varchar(135),
    visited_at timestamp without time zone NOT NULL,
    deadline timestamp without time zone NOT NULL
);
CREATE INDEX entry_visits_entry_id_deadline_idx ON entry_visits(entry_id, deadline);
CREATE INDEX entry_visits_entry_id_client_id_deadline_idx ON entry_visits(entry_id, client_id, deadline);
CREATE INDEX entry_visits_entry_id_client_name_deadline_idx ON entry_visits(entry_id, client_name, deadline);
CREATE INDEX entry_visits_deadline_idx ON entry_visits(deadline);
ALTER TABLE entry_visits ADD FOREIGN KEY (entry_id) REFERENCES entries(id)
    ON UPDATE CASCADE ON DELETE CASCADE;
