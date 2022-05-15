ALTER TABLE entries ADD COLUMN add_comment_principal varchar(70) NOT NULL DEFAULT 'public';
ALTER TABLE entries ADD COLUMN receiver_add_comment_principal varchar(70);
