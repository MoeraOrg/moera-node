package org.moera.node.media;

import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.StringJoiner;

import org.moera.lib.node.types.MediaFilePreviewInfo;
import org.moera.lib.node.types.PrivateMediaFileInfo;
import org.moera.lib.node.types.RemoteMediaInfo;
import org.moera.node.data.MediaFile;
import org.moera.node.data.MediaFileOwner;
import org.moera.node.data.MediaFilePreview;
import org.moera.node.data.RemoteMediaFile;
import org.moera.node.model.MediaFilePreviewInfoUtil;
import org.moera.node.util.ExtendedDuration;
import org.moera.node.util.UriUtil;
import org.moera.node.util.Util;
import org.springframework.util.ObjectUtils;

public class MediaUtil {

    /**
     * Media grants are valid for three days and expire at the next UTC midnight.
     */
    public static final ExtendedDuration MEDIA_GRANT_TTL = new ExtendedDuration(Duration.ofDays(3));

    public static String publicPath(String fileName) {
        return "public/" + fileName;
    }

    public static String publicPath(MediaFile mediaFile) {
        return publicPath(MimeUtil.fileName(mediaFile.getId(), mediaFile.getMimeType()));
    }

    public static String privatePath(String fileName, Integer width, String grant, boolean download) {
        var buf = new StringBuilder();
        if (width != null) {
            buf.append("&width=%d".formatted(width));
        }
        if (!ObjectUtils.isEmpty(grant)) {
            buf.append("&grant=%s".formatted(grant));
        }
        if (download) {
            buf.append("&download=true");
        }
        return "private/%s".formatted(fileName) + (!buf.isEmpty() ? "?" + buf.substring(1) : "");
    }

    public static String privatePath(MediaFileOwner mediaFileOwner, Integer width, String grant) {
        return privatePath(mediaFileOwner.getFileName(), width, grant, false);
    }

    public static String privatePath(RemoteMediaFile remoteMediaFile, Integer width, String grant) {
        String fileName = MimeUtil.fileName(remoteMediaFile.getMediaId(), remoteMediaFile.getMimeType());
        return privatePath(fileName, width, grant, false);
    }

    public static String privatePath(PrivateMediaFileInfo mediaFile, Integer width, String grant) {
        String fileName = MimeUtil.fileName(mediaFile.getId(), mediaFile.getMimeType());
        return privatePath(fileName, width, grant, false);
    }

    public static String privatePath(RemoteMediaInfo remoteMedia, Integer width, String grant) {
        return privatePath(remoteMedia, width, grant, false);
    }

    public static String privatePath(RemoteMediaInfo remoteMedia, Integer width, String grant, boolean download) {
        String fileName = MimeUtil.fileName(remoteMedia.getMediaId(), remoteMedia.getMimeType());
        return privatePath(fileName, width, grant, download);
    }

    public static String mediaUrl(String path) {
        return UriUtil.resolve(path, "/moera/media/");
    }

    public static String mediaSources(
        String originalPath,
        MediaFileOwner mediaFileOwner,
        DirectServeOperations directServe
    ) {
        StringJoiner sources = new StringJoiner(",");
        for (MediaFilePreview preview : mediaFileOwner.getMediaFile().getPreviews()) {
            if (preview.getMediaFile() == null) {
                continue;
            }
            String url;
            if (preview.isOriginal()) {
                url = originalPath;
            } else {
                String directPath = directServe.directPath(preview.getMediaFile(), MEDIA_GRANT_TTL).url();
                boolean directServing = directPath != null;
                url = mediaUrl(
                    directServing ? directPath : MediaUtil.privatePath(mediaFileOwner, preview.getWidth(), null)
                );
            }
            sources.add("%s %dw".formatted(url, preview.getWidth()));
        }
        return sources.toString();
    }

    public static String mediaSources(PrivateMediaFileInfo mediaFile) {
        String originalPath = mediaUrl(
            mediaFile.getDirectPath() != null ? mediaFile.getDirectPath() : mediaFile.getPath()
        );

        StringJoiner sources = new StringJoiner(",");
        for (MediaFilePreviewInfo preview : mediaFile.getPreviews()) {
            String url;
            if (Boolean.TRUE.equals(preview.getOriginal())) {
                url = originalPath;
            } else {
                url = mediaUrl(
                    preview.getDirectPath() != null
                        ? preview.getDirectPath()
                        : MediaUtil.privatePath(mediaFile, preview.getWidth(), null)
                );
            }
            sources.add("%s %dw".formatted(url, preview.getWidth()));
        }
        return sources.toString();
    }

    private static int findLargerPreviewWidth(MediaFile mediaFile, int width) {
        MediaFilePreview preview = mediaFile.findLargerPreview(width);
        return preview != null && preview.getMediaFile() != null && preview.getMediaFile().getSizeX() != null
            ? preview.getMediaFile().getSizeX() : width;
    }

    private static int findLargerPreviewWidth(PrivateMediaFileInfo mediaFile, int width) {
        MediaFilePreviewInfo preview = MediaFilePreviewInfoUtil.findLargerPreview(mediaFile.getPreviews(), width);
        return preview != null ? preview.getWidth() : width;
    }

    public static String mediaSizes(MediaFile mediaFile) {
        return "(max-width: 400px) %dpx, %dpx".formatted(
            Math.min(350, findLargerPreviewWidth(mediaFile, 350)),
            Math.min(900, findLargerPreviewWidth(mediaFile, 900))
        );
    }

    public static String mediaSizes(PrivateMediaFileInfo mediaFile) {
        return "(max-width: 400px) %dpx, %dpx".formatted(
            Math.min(350, findLargerPreviewWidth(mediaFile, 350)),
            Math.min(900, findLargerPreviewWidth(mediaFile, 900))
        );
    }

    public static Timestamp expirationTimestamp(ExtendedDuration valid) {
        long expires = switch (valid.getZone()) {
            case FIXED -> Instant.now()
                .plus(valid.getDuration())
                .atZone(ZoneOffset.UTC)
                .toLocalDate()
                .atStartOfDay()
                .plusDays(1)
                .toEpochSecond(ZoneOffset.UTC);
            case ALWAYS -> LocalDate.of(2100, 1, 1).atStartOfDay().toEpochSecond(ZoneOffset.UTC);
            case NEVER -> Instant.now().toEpochMilli() / 1000;
        };
        return Util.toTimestamp(expires);
    }

}
