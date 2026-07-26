package org.moera.node.model;

import java.util.List;

import org.moera.lib.node.types.MediaFilePreviewInfo;
import org.moera.lib.node.types.PrivateMediaFileInfo;
import org.moera.node.media.DirectServeOperations;
import org.moera.node.data.MediaFile;
import org.moera.node.data.MediaFileOwner;
import org.moera.node.data.MediaFilePreview;
import org.moera.node.media.grant.MediaGrantSupplier;
import org.moera.node.media.MimeUtil;
import org.moera.node.media.MediaUtil;

public class MediaFilePreviewInfoUtil {

    public static MediaFilePreviewInfo build(
        MediaFilePreview preview,
        MediaFileOwner original,
        DirectServeOperations directServe,
        MediaGrantSupplier grantSupplier
    ) {
        MediaFilePreviewInfo info = new MediaFilePreviewInfo();
        info.setTargetWidth(preview.getWidth());
        info.setHash(preview.getMediaFile().getId());
        info.setMimeType(preview.getMediaFile().getMimeType());
        info.setWidth(preview.getMediaFile().getSizeX());
        info.setHeight(preview.getMediaFile().getSizeY());
        info.setOriginal(preview.isOriginal());
        fillPath(info, original, grantSupplier);
        fillDirectPath(info, preview.getMediaFile(), directServe);
        return info;
    }

    public static MediaFilePreviewInfo findLargerPreview(List<MediaFilePreviewInfo> previews, int width) {
        MediaFilePreviewInfo larger = null;
        for (MediaFilePreviewInfo preview : previews) {
            if (preview.getWidth() >= width && (larger == null || larger.getWidth() > preview.getWidth())) {
                larger = preview;
            }
        }
        return larger;
    }

    public static void fillPath(
        MediaFilePreviewInfo info,
        MediaFileOwner original,
        MediaGrantSupplier grantSupplier
    ) {
        fillPath(info, original.getId().toString(), original.getMediaFile().getMimeType(), grantSupplier);
    }

    public static void fillPath(
        MediaFilePreviewInfo info,
        PrivateMediaFileInfo original,
        MediaGrantSupplier grantSupplier
    ) {
        fillPath(info, original.getId(), original.getMimeType(), grantSupplier);
    }

    private static void fillPath(
        MediaFilePreviewInfo info,
        String originalId,
        String originalMimeType,
        MediaGrantSupplier grantSupplier
    ) {
        String grant = grantSupplier != null
            ? grantSupplier.generateLocal(originalId, MediaUtil.MEDIA_GRANT_TTL, false, null)
            : null;
        String fileName = MimeUtil.fileName(originalId, originalMimeType);
        info.setPath(MediaUtil.privatePath(fileName, info.getTargetWidth(), grant, false));
    }

    public static void fillDirectPath(
        MediaFilePreviewInfo info, MediaFile mediaFile, DirectServeOperations directServe
    ) {
        var pu = directServe.directPath(mediaFile, MediaUtil.MEDIA_GRANT_TTL);
        info.setDirectPath(pu.url());
        info.setDirectPathExpiresAt(pu.expires());
    }

    public static void refreshDirectPath(MediaFilePreviewInfo info, DirectServeOperations directServe) {
        var pu = directServe.refreshDirectPath(info.getDirectPath(), info.getHash(), MediaUtil.MEDIA_GRANT_TTL);
        info.setDirectPath(pu.url());
        info.setDirectPathExpiresAt(pu.expires());
    }

}
