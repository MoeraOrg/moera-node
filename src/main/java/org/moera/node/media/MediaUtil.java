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
import org.moera.node.config.DirectServeConfig;
import org.moera.node.data.MediaFile;
import org.moera.node.data.MediaFileOwner;
import org.moera.node.data.MediaFilePreview;
import org.moera.node.model.MediaFilePreviewInfoUtil;
import org.moera.node.util.ExtendedDuration;
import org.moera.node.util.Util;
import org.springframework.util.ObjectUtils;

public class MediaUtil {

    public static String publicPath(String fileName) {
        return "public/" + fileName;
    }

    public static String publicPath(MediaFile mediaFile) {
        return publicPath(mediaFile.getFileName());
    }

    public static String privatePath(String fileName, Integer width, String grant) {
        if (width == null) {
            if (grant == null) {
                return "private/%s".formatted(fileName);
            } else {
                return "private/%s?grant=%s".formatted(fileName, grant);
            }
        } else {
            if (grant == null) {
                return "private/%s?width=%d".formatted(fileName, width);
            } else {
                return "private/%s?width=%d&grant=%s".formatted(fileName, width, grant);
            }
        }
    }

    public static String privatePath(MediaFileOwner mediaFileOwner, Integer width, String grant) {
        return privatePath(mediaFileOwner.getFileName(), width, grant);
    }

    public static String privatePath(PrivateMediaFileInfo mediaFile, Integer width, String grant) {
        String fileName = MimeUtils.fileName(mediaFile.getId(), mediaFile.getMimeType());
        return privatePath(fileName, width, grant);
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
                String directPath = MediaUtil.directPath(mediaFileOwner, config).url();
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
            case FILESYSTEM ->
                MediaUtil.presignUrl(location, id, valid, userFileName, config.getSecret());
        };
    }

    public static PresignedUrl directPath(
        String location,
        String id,
        ExtendedDuration valid,
        DirectServeConfig config
    ) {
        return directPath(location, id, valid, null, config);
    }

    public static PresignedUrl directPath(MediaFileOwner mediaFileOwner, DirectServeConfig config) {
        String userFileName = !ObjectUtils.isEmpty(mediaFileOwner.getTitle())
            ? MimeUtils.fileName(mediaFileOwner.getTitle(), mediaFileOwner.getMediaFile().getMimeType())
            : null;
        return directPath(
            mediaFileOwner.getMediaFile().getFileName(),
            mediaFileOwner.getMediaFile().getId(),
            new ExtendedDuration(Duration.ofDays(3)),
            userFileName,
            config
        );
    }

}
