package org.moera.node.media;

import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.StringJoiner;

import org.bouncycastle.crypto.macs.HMac;
import org.bouncycastle.crypto.params.KeyParameter;
import org.bouncycastle.jcajce.provider.util.DigestFactory;
import org.moera.lib.node.types.MediaFilePreviewInfo;
import org.moera.lib.node.types.PrivateMediaFileInfo;
import org.moera.lib.node.types.RemoteMediaInfo;
import org.moera.node.config.DirectServeConfig;
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

    public static String mediaSources(String originalPath, MediaFileOwner mediaFileOwner, DirectServeConfig config) {
        StringJoiner sources = new StringJoiner(",");
        for (MediaFilePreview preview : mediaFileOwner.getMediaFile().getPreviews()) {
            if (preview.getMediaFile() == null) {
                continue;
            }
            String url;
            if (preview.isOriginal()) {
                url = originalPath;
            } else {
                String directPath = MediaUtil.directPath(
                    preview.getMediaFile(), MEDIA_GRANT_TTL, config
                ).url();
                boolean directServing = directPath != null;
                url = "/moera/media/"
                    + (directServing ? directPath : MediaUtil.privatePath(mediaFileOwner, preview.getWidth(), null));
            }
            sources.add("%s %dw".formatted(url, preview.getWidth()));
        }
        return sources.toString();
    }

    public static String mediaSources(PrivateMediaFileInfo mediaFile) {
        String originalPath = "/moera/media/"
            + (mediaFile.getDirectPath() != null ? mediaFile.getDirectPath() : mediaFile.getPath());

        StringJoiner sources = new StringJoiner(",");
        for (MediaFilePreviewInfo preview : mediaFile.getPreviews()) {
            String url;
            if (Boolean.TRUE.equals(preview.getOriginal())) {
                url = originalPath;
            } else {
                url = "/moera/media/" + (
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

    public record PresignedUrl(String url, Long expires) {
    }

    private static PresignedUrl presignUrl(
        String location,
        String id,
        ExtendedDuration valid,
        String userFileName,
        String secret
    ) {
        var mac = new HMac(DigestFactory.getDigest("SHA-256"));
        mac.init(new KeyParameter(secret.getBytes(StandardCharsets.UTF_8)));
        byte[] data = id.getBytes(StandardCharsets.UTF_8);
        mac.update(data, 0, data.length);
        long expires = Util.toEpochSecond(expirationTimestamp(valid));
        data = Long.toString(expires).getBytes(StandardCharsets.UTF_8);
        mac.update(data, 0, data.length);
        if (!ObjectUtils.isEmpty(userFileName)) {
            data = userFileName.getBytes(StandardCharsets.UTF_8);
            mac.update(data, 0, data.length);
        }
        byte[] signature = new byte[mac.getMacSize()];
        mac.doFinal(signature, 0);

        var url = ObjectUtils.isEmpty(userFileName)
            ? String.format("%s?exp=%d&sig=%s", location, expires, Util.base64urlencode(signature))
            : String.format(
                  "%s?exp=%d&fn=%s&sig=%s", location, expires, Util.ue(userFileName), Util.base64urlencode(signature)
              );

        return new PresignedUrl(url, expires);
    }

    public static PresignedUrl directPath(
        String location,
        String id,
        ExtendedDuration valid,
        String userFileName,
        DirectServeConfig config
    ) {
        return switch (config.getSource()) {
            case NONE -> new PresignedUrl(null, null);
            case FILESYSTEM -> ObjectUtils.isEmpty(location)
                ? new PresignedUrl(null, null)
                : MediaUtil.presignUrl(location, id, valid, userFileName, config.getSecret());
        };
    }

    public static PresignedUrl directPath(
        MediaFile mediaFile,
        ExtendedDuration valid,
        String userFileName,
        DirectServeConfig config
    ) {
        return directPath(mediaFile.getFileName(), mediaFile.getId(), valid, userFileName, config);
    }

    public static PresignedUrl directPath(
        MediaFile mediaFile,
        ExtendedDuration valid,
        DirectServeConfig config
    ) {
        return directPath(mediaFile, valid, null, config);
    }

    public static PresignedUrl directPath(MediaFileOwner mediaFileOwner, DirectServeConfig config) {
        String userFileName = !ObjectUtils.isEmpty(mediaFileOwner.getTitle())
            ? MimeUtil.fileName(mediaFileOwner.getTitle(), mediaFileOwner.getMediaFile().getMimeType())
            : null;
        return directPath(
            mediaFileOwner.getMediaFile(),
            MEDIA_GRANT_TTL,
            userFileName,
            config
        );
    }

    public static PresignedUrl refreshDirectPath(
        String directPath,
        String id,
        ExtendedDuration valid,
        DirectServeConfig config
    ) {
        if (ObjectUtils.isEmpty(directPath)) {
            return new PresignedUrl(null, null);
        }

        String directFileName = UriUtil.stripQueryAndFragment(directPath);
        String query = UriUtil.query(directPath);
        String userFileName = query != null
            ? UriUtil.queryParameter(query, "fn")
            : null;
        return directPath(directFileName, id, valid, userFileName, config);
    }

}
