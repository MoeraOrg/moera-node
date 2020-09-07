ALTER TABLE entries ADD COLUMN replied_to_id uuid;
ALTER TABLE entries ADD COLUMN replied_to_name varchar(63);
ALTER TABLE entries ADD COLUMN replied_to_heading varchar(255);
CREATE INDEX ON entries(replied_to_id);
ALTER TABLE entries ADD FOREIGN KEY (replied_to_id) REFERENCES entries(id) ON UPDATE CASCADE ON DELETE SET NULL;
