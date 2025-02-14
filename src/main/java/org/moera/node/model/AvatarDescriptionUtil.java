package org.moera.node.model;

import org.moera.lib.node.types.AvatarDescription;
import org.moera.node.data.Avatar;

public class AvatarDescriptionUtil {
    
    public static AvatarDescription build(Avatar avatar) {
        AvatarDescription avatarDescription = new AvatarDescription();
        if (avatar != null && avatar.getMediaFile() != null) {
            avatarDescription.setMediaId(avatar.getMediaFile().getId());
            avatarDescription.setShape(avatar.getShape());
        }
        return avatarDescription;
    }

}
