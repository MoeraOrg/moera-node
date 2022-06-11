ALTER TABLE entries ADD COLUMN parent_override_reaction_principal varchar(70) NOT NULL DEFAULT 'unset';
ALTER TABLE entries ADD COLUMN receiver_override_reaction_principal varchar(70);
ALTER TABLE entries ADD COLUMN parent_override_comment_reaction_principal varchar(70) NOT NULL DEFAULT 'unset';
ALTER TABLE entries ADD COLUMN receiver_override_comment_reaction_principal varchar(70);
