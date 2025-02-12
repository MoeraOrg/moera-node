package org.moera.node.model;

import org.moera.lib.node.types.AvatarImage;
import org.moera.lib.util.LogUtil;
import org.moera.node.data.Avatar;
import org.moera.node.data.MediaFile;

public class AvatarImageUtil {

    public static AvatarImage build(Avatar avatar) {
        return build(avatar.getMediaFile(), avatar.getShape());
    }

    public static AvatarImage build(AvatarInfo avatarInfo) {
        AvatarImage avatarImage = new AvatarImage();
        avatarImage.setMediaId(avatarInfo.getMediaId());
        avatarImage.setPath(avatarInfo.getPath());
        avatarImage.setWidth(avatarInfo.getWidth());
        avatarImage.setHeight(avatarInfo.getHeight());
        avatarImage.setShape(avatarInfo.getShape());
        return avatarImage;
    }

    public static AvatarImage build(MediaFile mediaFile, String shape) {
        AvatarImage avatarImage = new AvatarImage();
        setMediaFile(avatarImage, mediaFile);
        if (mediaFile != null) {
            avatarImage.setMediaId(mediaFile.getId());
            avatarImage.setPath("public/" + mediaFile.getFileName());
            avatarImage.setWidth(mediaFile.getSizeX());
            avatarImage.setHeight(mediaFile.getSizeY());
        }
        avatarImage.setShape(shape);
        return avatarImage;
    }

    public static AvatarImage build(AvatarDescription avatarDescription, MediaFile mediaFile) {
        return build(mediaFile, avatarDescription != null ? avatarDescription.getShape() : null);
    }

    public static MediaFile getMediaFile(AvatarImage avatarImage) {
        return (MediaFile) avatarImage.getExtra();
    }

    public static void setMediaFile(AvatarImage avatarImage, MediaFile mediaFile) {
        avatarImage.setExtra(mediaFile);
    }

    public static String toLogString(AvatarImage avatarImage) {
        return String.format(
            "AvatarImage(path=%s, shape=%s)",
            LogUtil.format(avatarImage.getPath()), LogUtil.format(avatarImage.getShape())
        );
    }

}
