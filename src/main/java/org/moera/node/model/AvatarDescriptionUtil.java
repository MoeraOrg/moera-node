package org.moera.node.model;

import org.moera.lib.node.types.AvatarDescription;
import org.moera.node.data.Avatar;
import org.moera.node.data.MediaFile;

public class AvatarDescriptionUtil {
    
    public static AvatarDescription build(Avatar avatar) {
        AvatarDescription avatarDescription = new AvatarDescription();
        if (avatar != null && avatar.getMediaFile() != null) {
            avatarDescription.setMediaId(avatar.getMediaFile().getId());
            avatarDescription.setShape(avatar.getShape());
        }
        return avatarDescription;
    }

    public static MediaFile getMediaFile(AvatarDescription description) {
        return description != null ? (MediaFile) description.getExtra() : null;
    }

    public static void setMediaFile(AvatarDescription description, MediaFile mediaFile) {
        description.setExtra(mediaFile);
    }

}
