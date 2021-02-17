ALTER TABLE own_comments ADD COLUMN remote_replied_to_name character varying(63);
ALTER TABLE own_comments ADD COLUMN remote_replied_to_full_name character varying(96);
