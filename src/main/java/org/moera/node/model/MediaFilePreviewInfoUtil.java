package org.moera.node.model;

import java.time.Duration;
import java.util.List;

import org.moera.lib.node.types.MediaFilePreviewInfo;
import org.moera.lib.node.types.PrivateMediaFileInfo;
import org.moera.node.config.DirectServeConfig;
import org.moera.node.data.MediaFileOwner;
import org.moera.node.data.MediaFilePreview;
import org.moera.node.media.MediaGrantSupplier;
import org.moera.node.media.MimeUtil;
import org.moera.node.util.ExtendedDuration;
import org.moera.node.media.MediaUtil;

public class MediaFilePreviewInfoUtil {

    public static MediaFilePreviewInfo build(
        MediaFilePreview preview,
        MediaFileOwner original,
        DirectServeConfig config,
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
        fillDirectPath(info, config);
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
        ExtendedDuration valid = new ExtendedDuration(Duration.ofDays(3));
        String grant = grantSupplier != null
            ? grantSupplier.generate(originalId, valid, false, null)
            : null;
        String fileName = MimeUtil.fileName(originalId, originalMimeType);
        info.setPath(MediaUtil.privatePath(fileName, info.getTargetWidth(), grant));
    }

    public static void fillDirectPath(MediaFilePreviewInfo info, DirectServeConfig config) {
        var fileName = MimeUtil.fileName(info.getHash(), info.getMimeType());
        ExtendedDuration valid = new ExtendedDuration(Duration.ofDays(3));
        var pu = MediaUtil.directPath(fileName, info.getHash(), valid, config);
        info.setDirectPath(pu.url());
        info.setDirectPathExpiresAt(pu.expires());
    }

}
