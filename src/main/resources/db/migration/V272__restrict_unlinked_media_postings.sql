UPDATE entries
SET parent_view_principal = 'unset',
    view_principal = 'secret',
    parent_edit_principal = 'unset',
    parent_delete_principal = 'secret',
    parent_view_comments_principal = 'unset',
    view_comments_principal = 'admin',
    parent_add_comment_principal = 'unset',
    add_comment_principal = 'none',
    parent_trust_comment_principal = 'unset',
    trust_comment_principal = 'none',
    parent_override_comment_principal = 'none',
    parent_view_reactions_principal = 'unset',
    view_reactions_principal = 'admin',
    parent_view_negative_reactions_principal = 'unset',
    view_negative_reactions_principal = 'admin',
    parent_view_reaction_totals_principal = 'unset',
    view_reaction_totals_principal = 'admin',
    parent_view_negative_reaction_totals_principal = 'unset',
    view_negative_reaction_totals_principal = 'admin',
    parent_view_reaction_ratios_principal = 'unset',
    view_reaction_ratios_principal = 'admin',
    parent_view_negative_reaction_ratios_principal = 'unset',
    view_negative_reaction_ratios_principal = 'admin',
    parent_add_reaction_principal = 'unset',
    add_reaction_principal = 'none',
    parent_add_negative_reaction_principal = 'unset',
    add_negative_reaction_principal = 'none',
    parent_override_reaction_principal = 'none',
    parent_override_comment_reaction_principal = 'none',
    child_operations =
        '{"view":"admin","edit":"none","delete":"admin","viewReactions":"admin",'
        || '"viewNegativeReactions":"admin","viewReactionTotals":"admin",'
        || '"viewNegativeReactionTotals":"admin","viewReactionRatios":"admin",'
        || '"viewNegativeReactionRatios":"admin","addReaction":"none",'
        || '"addNegativeReaction":"none","overrideReaction":"none"}',
    reaction_operations = '{"view":"admin","delete":"admin"}',
    child_reaction_operations = '{"view":"admin","delete":"admin"}'
WHERE parent_media_id IS NOT NULL
  AND parent_media_entry_id IS NULL;
