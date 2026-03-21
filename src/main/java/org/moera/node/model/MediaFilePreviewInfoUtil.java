package org.moera.node.model;

import java.time.Duration;
import java.util.List;

import org.moera.lib.node.types.MediaFilePreviewInfo;
import org.moera.node.config.DirectServeConfig;
import org.moera.node.data.MediaFilePreview;
import org.moera.node.media.MimeUtils;
import org.moera.node.util.ExtendedDuration;
import org.moera.node.util.MediaUtil;

public class MediaFilePreviewInfoUtil {

    public static MediaFilePreviewInfo build(MediaFilePreview preview, DirectServeConfig config) {
        MediaFilePreviewInfo info = new MediaFilePreviewInfo();
        info.setTargetWidth(preview.getWidth());
        info.setHash(preview.getMediaFile().getId());
        info.setMimeType(preview.getMediaFile().getMimeType());
        info.setWidth(preview.getMediaFile().getSizeX());
        info.setHeight(preview.getMediaFile().getSizeY());
        info.setOriginal(preview.isOriginal());
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

    public static void fillDirectPath(MediaFilePreviewInfo info, DirectServeConfig config) {
        var fileName = MimeUtils.fileName(info.getHash(), info.getMimeType());
        ExtendedDuration valid = new ExtendedDuration(Duration.ofDays(3));
        var pu = MediaUtil.presignDirectPath(fileName, info.getHash(), valid, config);
        info.setDirectPath(pu.url());
        info.setDirectPathExpiresAt(pu.expires());
    }

}
