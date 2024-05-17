package org.moera.node.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import org.moera.node.data.MediaFile;
import org.moera.node.data.MediaFilePreview;
import org.moera.node.model.MediaFilePreviewInfo;
import org.moera.node.model.PrivateMediaFileInfo;
import org.springframework.util.ObjectUtils;

public class MediaUtil {

    public static String mediaPreview(String location, int width) {
        return String.format("%s?width=%d", location, width);
    }

    public static String mediaPreviewDirect(String location, int width) {
        int pos = location.lastIndexOf('.');
        return String.format("%s_%d%s", location.substring(0, pos), width, location.substring(pos));
    }

    public static String mediaSources(String location, Collection<MediaFilePreview> previews, boolean directServing) {
        if (ObjectUtils.isEmpty(previews)) {
            return "";
        }
        return mediaSourcesInfo(
                location,
                previews.stream()
                        .filter(preview -> preview.getMediaFile() != null)
                        .map(preview -> new MediaFilePreviewInfo(preview, directServing ? location : null))
                        .collect(Collectors.toList()));
    }

    public static String mediaSourcesInfo(String location, Collection<MediaFilePreviewInfo> previews) {
        List<String> sources = new ArrayList<>();
        for (MediaFilePreviewInfo preview : previews) {
            String url = preview.isOriginal()
                    ? location
                    : (preview.getDirectPath() != null
                        ? preview.getDirectPath()
                        : mediaPreview(location, preview.getTargetWidth()));
            sources.add(String.format("%s %dw", url, preview.getWidth()));
        }
        return String.join(",", sources);
    }

    private static int findLargerPreviewWidth(MediaFile mediaFile, int width) {
        MediaFilePreview preview = mediaFile.findLargerPreview(width);
        return preview != null && preview.getMediaFile() != null && preview.getMediaFile().getSizeX() != null
                ? preview.getMediaFile().getSizeX() : width;
    }

    private static int findLargerPreviewWidth(PrivateMediaFileInfo mediaFile, int width) {
        MediaFilePreviewInfo preview = mediaFile.findLargerPreview(width);
        return preview != null ? preview.getWidth() : width;
    }

    public static String mediaSizes(MediaFile mediaFile) {
        return String.format("(max-width: 400px) %dpx, %dpx",
                Math.min(350, findLargerPreviewWidth(mediaFile, 350)),
                Math.min(900, findLargerPreviewWidth(mediaFile, 900)));
    }

    public static String mediaSizes(PrivateMediaFileInfo mediaFile) {
        return String.format("(max-width: 400px) %dpx, %dpx",
                Math.min(350, findLargerPreviewWidth(mediaFile, 350)),
                Math.min(900, findLargerPreviewWidth(mediaFile, 900)));
    }

}
