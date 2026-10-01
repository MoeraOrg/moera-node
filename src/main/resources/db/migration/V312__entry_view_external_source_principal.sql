ALTER TABLE entries ADD COLUMN view_external_source_principal varchar(70) NOT NULL DEFAULT 'public';
ALTER TABLE entries ADD COLUMN parent_view_external_source_principal varchar(70) NOT NULL DEFAULT 'unset';
ALTER TABLE entries ADD COLUMN receiver_view_external_source_principal varchar(70);

UPDATE entries
SET view_external_source_principal = 'public', receiver_view_external_source_principal = 'public'
WHERE receiver_name IS NOT NULL;
