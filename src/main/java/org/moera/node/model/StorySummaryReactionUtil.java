package org.moera.node.model;

import org.moera.lib.node.types.StorySummaryReaction;

public class StorySummaryReactionUtil {

    public static StorySummaryReaction build(
        String ownerName, String ownerFullName, String ownerSourceUri, String ownerGender, int emoji
    ) {
        StorySummaryReaction reaction = new StorySummaryReaction();
        reaction.setOwnerName(ownerName);
        reaction.setOwnerFullName(ownerFullName);
        reaction.setOwnerSourceUri(ownerSourceUri);
        reaction.setOwnerGender(ownerGender);
        reaction.setEmoji(emoji);
        return reaction;
    }

}
