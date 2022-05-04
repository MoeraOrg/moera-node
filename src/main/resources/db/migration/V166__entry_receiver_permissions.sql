ALTER TABLE entries ADD COLUMN receiver_view_principal varchar(70);
ALTER TABLE entries ADD COLUMN receiver_deleted_at timestamp without time zone;
