package org.moera.node.model;

import org.moera.lib.node.types.AvatarDescription;
import org.moera.lib.node.types.AvatarImage;
import org.moera.lib.node.types.AvatarInfo;
import org.moera.lib.util.LogUtil;
import org.moera.node.media.DirectServeOperations;
import org.moera.node.data.Avatar;
import org.moera.node.data.MediaFile;
import org.moera.node.util.ExtendedDuration;
import org.moera.node.media.MediaUtil;

public class AvatarImageUtil {

    public static AvatarImage build(Avatar avatar, DirectServeOperations directServe) {
        return build(avatar.getMediaFile(), avatar.getShape(), directServe);
    }

    public static AvatarImage build(AvatarInfo avatarInfo) {
        AvatarImage avatarImage = new AvatarImage();
        avatarImage.setMediaId(avatarInfo.getMediaId());
        avatarImage.setPath(avatarInfo.getPath());
        avatarImage.setMimeType(avatarInfo.getMimeType());
        avatarImage.setWidth(avatarInfo.getWidth());
        avatarImage.setHeight(avatarInfo.getHeight());
        avatarImage.setDirectPath(avatarInfo.getDirectPath());
        avatarImage.setDirectPathExpiresAt(avatarInfo.getDirectPathExpiresAt());
        avatarImage.setShape(avatarInfo.getShape());
        return avatarImage;
    }

    public static AvatarImage build(MediaFile mediaFile, String shape, DirectServeOperations directServe) {
        AvatarImage avatarImage = new AvatarImage();
        setMediaFile(avatarImage, mediaFile);
        if (mediaFile != null) {
            avatarImage.setMediaId(mediaFile.getId());
            avatarImage.setPath(MediaUtil.publicPath(mediaFile));
            avatarImage.setMimeType(mediaFile.getMimeType());
            avatarImage.setWidth(mediaFile.getSizeX());
            avatarImage.setHeight(mediaFile.getSizeY());
            fillDirectPath(avatarImage, mediaFile, directServe);
        }
        avatarImage.setShape(shape);
        return avatarImage;
    }

    private static void fillDirectPath(
        AvatarImage info, MediaFile mediaFile, DirectServeOperations directServe
    ) {
        var pu = directServe.directPath(mediaFile, ExtendedDuration.ALWAYS);
        info.setDirectPath(pu.url());
        info.setDirectPathExpiresAt(pu.expires());
    }

    public static AvatarImage build(
        AvatarDescription avatarDescription, MediaFile mediaFile, DirectServeOperations directServe
    ) {
        return build(mediaFile, avatarDescription != null ? avatarDescription.getShape() : null, directServe);
    }

    public static MediaFile getMediaFile(AvatarImage avatarImage) {
        return (MediaFile) avatarImage.getExtra();
    }

    public static void setMediaFile(AvatarImage avatarImage, MediaFile mediaFile) {
        avatarImage.setExtra(mediaFile);
    }

    public static String toLogString(AvatarImage avatarImage) {
        return "AvatarImage(path=%s, shape=%s)".formatted(
            LogUtil.format(avatarImage.getPath()), LogUtil.format(avatarImage.getShape())
        );
    }

}
