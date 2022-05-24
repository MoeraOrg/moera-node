ALTER TABLE entries ADD COLUMN receiver_edit_principal varchar(70);
ALTER TABLE entries ADD COLUMN receiver_delete_principal varchar(70);
ALTER TABLE entries ADD COLUMN view_reactions_principal varchar(70) NOT NULL DEFAULT 'public';
ALTER TABLE entries ADD COLUMN receiver_view_reactions_principal varchar(70);
ALTER TABLE entries ADD COLUMN view_negative_reactions_principal varchar(70) NOT NULL DEFAULT 'public';
ALTER TABLE entries ADD COLUMN receiver_view_negative_reactions_principal varchar(70);
ALTER TABLE entries ADD COLUMN view_reaction_totals_principal varchar(70) NOT NULL DEFAULT 'public';
ALTER TABLE entries ADD COLUMN receiver_view_reaction_totals_principal varchar(70);
ALTER TABLE entries ADD COLUMN view_negative_reaction_totals_principal varchar(70) NOT NULL DEFAULT 'public';
ALTER TABLE entries ADD COLUMN receiver_view_negative_reaction_totals_principal varchar(70);
ALTER TABLE entries ADD COLUMN view_reaction_ratios_principal varchar(70) NOT NULL DEFAULT 'public';
ALTER TABLE entries ADD COLUMN receiver_view_reaction_ratios_principal varchar(70);
ALTER TABLE entries ADD COLUMN view_negative_reaction_ratios_principal varchar(70) NOT NULL DEFAULT 'public';
ALTER TABLE entries ADD COLUMN receiver_view_negative_reaction_ratios_principal varchar(70);
ALTER TABLE entries ADD COLUMN add_reaction_principal varchar(70) NOT NULL DEFAULT 'public';
ALTER TABLE entries ADD COLUMN receiver_add_reaction_principal varchar(70);
ALTER TABLE entries ADD COLUMN add_negative_reaction_principal varchar(70) NOT NULL DEFAULT 'public';
ALTER TABLE entries ADD COLUMN receiver_add_negative_reaction_principal varchar(70);
UPDATE entries
SET view_reactions_principal = 'private', receiver_view_reactions_principal = 'private'
WHERE reactions_visible = false;
UPDATE entries
SET view_reactions_principal = 'private', receiver_view_reactions_principal = 'private',
    view_reaction_totals_principal = 'private', receiver_view_reaction_totals_principal = 'private'
WHERE reaction_totals_visible = false;
ALTER TABLE entries DROP COLUMN reactions_visible;
ALTER TABLE entries DROP COLUMN reaction_totals_visible;
ALTER TABLE drafts DROP COLUMN reactions_visible;
ALTER TABLE drafts DROP COLUMN reaction_totals_visible;
UPDATE entries
SET receiver_view_principal = 'public',
    receiver_edit_principal = 'private',
    receiver_delete_principal = 'private',
    view_comments_principal = 'none',
    receiver_view_comments_principal = 'public',
    add_comment_principal = 'none',
    receiver_add_comment_principal = 'public',
    view_reactions_principal = 'none',
    receiver_view_reactions_principal = COALESCE(receiver_view_reactions_principal, 'public'),
    view_negative_reactions_principal = 'none',
    receiver_view_negative_reactions_principal = COALESCE(receiver_view_reactions_principal, 'public'),
    view_reaction_totals_principal = 'none',
    receiver_view_reaction_totals_principal = COALESCE(receiver_view_reaction_totals_principal, 'public'),
    view_negative_reaction_totals_principal = 'none',
    receiver_view_negative_reaction_totals_principal = COALESCE(receiver_view_reaction_totals_principal, 'public'),
    view_reaction_ratios_principal = 'none',
    receiver_view_reaction_ratios_principal = 'public',
    view_negative_reaction_ratios_principal = 'none',
    receiver_view_negative_reaction_ratios_principal = 'public',
    add_reaction_principal = 'none',
    receiver_add_reaction_principal = 'public',
    add_negative_reaction_principal = 'none',
    receiver_add_negative_reaction_principal = 'public'
WHERE receiver_name IS NOT NULL;
ALTER TABLE reaction_totals ADD COLUMN forged boolean NOT NULL DEFAULT false;
UPDATE reaction_totals SET forged = true WHERE total >= 1000;
