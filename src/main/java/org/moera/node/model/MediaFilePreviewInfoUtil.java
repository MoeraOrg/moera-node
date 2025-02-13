package org.moera.node.model;

import java.util.List;

import org.moera.lib.node.types.MediaFilePreviewInfo;
import org.moera.node.data.MediaFilePreview;

public class MediaFilePreviewInfoUtil {

    public static MediaFilePreviewInfo build(MediaFilePreview preview, String originalDirectPath) {
        MediaFilePreviewInfo info = new MediaFilePreviewInfo();
        info.setTargetWidth(preview.getWidth());
        info.setWidth(preview.getMediaFile().getSizeX());
        info.setHeight(preview.getMediaFile().getSizeY());
        info.setOriginal(preview.isOriginal());
        info.setDirectPath(preview.getDirectPath(originalDirectPath));
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

}
