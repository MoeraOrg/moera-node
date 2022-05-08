ALTER TABLE entries ADD COLUMN view_comments_principal varchar(70) NOT NULL DEFAULT 'public';
ALTER TABLE entries ADD COLUMN receiver_view_comments_principal varchar(70);
