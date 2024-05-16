ALTER TABLE media_file_owners ADD COLUMN nonce varchar(32);
ALTER TABLE media_file_owners ADD COLUMN prev_nonce varchar(32);
ALTER TABLE media_file_owners ADD COLUMN nonce_deadline timestamp without time zone;
CREATE INDEX media_file_owners_nonce_deadline_view_principal_idx ON media_file_owners(nonce_deadline, view_principal);
