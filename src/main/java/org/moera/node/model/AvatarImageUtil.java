package org.moera.node.model;

import org.moera.lib.node.types.AvatarDescription;
import org.moera.lib.node.types.AvatarImage;
import org.moera.lib.node.types.AvatarInfo;
import org.moera.lib.util.LogUtil;
import org.moera.node.config.DirectServeConfig;
import org.moera.node.data.Avatar;
import org.moera.node.data.MediaFile;
import org.moera.node.media.MimeUtils;
import org.moera.node.util.ExtendedDuration;
import org.moera.node.media.MediaUtil;

public class AvatarImageUtil {

    public static AvatarImage build(Avatar avatar, DirectServeConfig config) {
        return build(avatar.getMediaFile(), avatar.getShape(), config);
    }

    public static AvatarImage build(AvatarInfo avatarInfo, DirectServeConfig config) {
        AvatarImage avatarImage = new AvatarImage();
        avatarImage.setMediaId(avatarInfo.getMediaId());
        avatarImage.setPath(avatarInfo.getPath());
        avatarImage.setMimeType(avatarInfo.getMimeType());
        avatarImage.setWidth(avatarInfo.getWidth());
        avatarImage.setHeight(avatarInfo.getHeight());
        fillDirectPath(avatarImage, config);
        avatarImage.setShape(avatarInfo.getShape());
        return avatarImage;
    }

    public static AvatarImage build(MediaFile mediaFile, String shape, DirectServeConfig config) {
        AvatarImage avatarImage = new AvatarImage();
        setMediaFile(avatarImage, mediaFile);
        if (mediaFile != null) {
            avatarImage.setMediaId(mediaFile.getId());
            avatarImage.setPath(MediaUtil.publicPath(mediaFile));
            avatarImage.setMimeType(mediaFile.getMimeType());
            avatarImage.setWidth(mediaFile.getSizeX());
            avatarImage.setHeight(mediaFile.getSizeY());
            fillDirectPath(avatarImage, config);
        }
        avatarImage.setShape(shape);
        return avatarImage;
    }

    private static void fillDirectPath(AvatarImage info, DirectServeConfig config) {
        var fileName = MimeUtils.fileName(info.getMediaId(), info.getMimeType());
        var pu = MediaUtil.directPath(fileName, info.getMediaId(), ExtendedDuration.ALWAYS, config);
        info.setDirectPath(pu.url());
        info.setDirectPathExpiresAt(pu.expires());
    }

    public static AvatarImage build(
        AvatarDescription avatarDescription, MediaFile mediaFile, DirectServeConfig config
    ) {
        return build(mediaFile, avatarDescription != null ? avatarDescription.getShape() : null, config);
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
