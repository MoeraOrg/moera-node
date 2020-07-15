ALTER TABLE entries ADD COLUMN parent_id uuid;
ALTER TABLE entries ADD COLUMN children_total integer NOT NULL DEFAULT 0;
CREATE INDEX ON entries(parent_id);
ALTER TABLE entries ADD FOREIGN KEY (parent_id) REFERENCES entries(id) ON UPDATE CASCADE ON DELETE CASCADE;
