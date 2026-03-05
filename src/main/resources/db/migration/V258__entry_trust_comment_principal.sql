ALTER TABLE entries ADD COLUMN trust_comment_principal varchar(70) NOT NULL DEFAULT 'signed';
ALTER TABLE entries ADD COLUMN parent_trust_comment_principal varchar(70) NOT NULL DEFAULT 'unset';
ALTER TABLE entries ADD COLUMN receiver_trust_comment_principal varchar(70);
