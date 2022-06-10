ALTER TABLE reactions ADD COLUMN view_principal varchar(70) NOT NULL DEFAULT 'public';
ALTER TABLE reactions ADD COLUMN posting_view_principal varchar(70) NOT NULL DEFAULT 'unset';
ALTER TABLE reactions ADD COLUMN posting_delete_principal varchar(70) NOT NULL DEFAULT 'unset';
ALTER TABLE reactions ADD COLUMN comment_view_principal varchar(70) NOT NULL DEFAULT 'unset';
ALTER TABLE reactions ADD COLUMN comment_delete_principal varchar(70) NOT NULL DEFAULT 'unset';
ALTER TABLE entries ADD COLUMN reaction_operations text NOT NULL DEFAULT '{}';
ALTER TABLE entries ADD COLUMN child_reaction_operations text NOT NULL DEFAULT '{}';
