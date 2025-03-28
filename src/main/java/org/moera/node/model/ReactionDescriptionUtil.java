package org.moera.node.model;

import org.moera.lib.node.types.ReactionAttributes;
import org.moera.lib.node.types.ReactionDescription;
import org.moera.lib.node.types.ReactionOperations;
import org.moera.lib.node.types.principal.Principal;
import org.moera.node.data.Avatar;
import org.moera.node.data.MediaFile;
import org.moera.node.data.Reaction;

public class ReactionDescriptionUtil {

    public static ReactionDescription build(
        String ownerName, String ownerFullName, String ownerGender, Avatar ownerAvatar, ReactionAttributes attributes
    ) {
        ReactionDescription description = new ReactionDescription();
        
        description.setOwnerName(ownerName);
        description.setOwnerFullName(ownerFullName);
        description.setOwnerGender(ownerGender);
        description.setOwnerAvatar(ownerAvatar != null ? AvatarDescriptionUtil.build(ownerAvatar) : null);
        description.setNegative(attributes.isNegative());
        description.setEmoji(attributes.getEmoji());

        if (attributes.getOperations() != null) {
            ReactionOperations operations = new ReactionOperations();
            operations.setView(attributes.getOperations().getView(), Principal.PUBLIC);
            operations.setDelete(attributes.getOperations().getDelete(), Principal.PRIVATE);
            description.setOperations(operations);
        }
        
        return description;
    }

    public static void toReaction(ReactionDescription description, Reaction reaction) {
        reaction.setOwnerName(description.getOwnerName());
        reaction.setOwnerFullName(description.getOwnerFullName());
        reaction.setOwnerGender(description.getOwnerGender());
        if (description.getOwnerAvatar() != null) {
            MediaFile ownerAvatarMediaFile = AvatarDescriptionUtil.getMediaFile(description.getOwnerAvatar());
            if (ownerAvatarMediaFile != null) {
                reaction.setOwnerAvatarMediaFile(ownerAvatarMediaFile);
            }
            if (description.getOwnerAvatar().getShape() != null) {
                reaction.setOwnerAvatarShape(description.getOwnerAvatar().getShape());
            }
        }
        reaction.setNegative(description.isNegative());
        reaction.setEmoji(description.getEmoji());
        if (description.getSignature() != null && description.getSignatureVersion() != null) {
            reaction.setSignature(description.getSignature());
            reaction.setSignatureVersion(description.getSignatureVersion());
        }
        Principal viewPrincipal = ReactionOperations.getView(description.getOperations(), null);
        if (viewPrincipal != null) {
            reaction.setViewPrincipal(viewPrincipal);
        }
    }

}
