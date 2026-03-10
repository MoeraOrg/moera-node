ALTER TABLE entries ADD COLUMN client_id varchar(40);
CREATE INDEX entries_client_id_idx ON entries(client_id);
