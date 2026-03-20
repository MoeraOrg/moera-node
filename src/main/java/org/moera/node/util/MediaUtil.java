package org.moera.node.util;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import org.bouncycastle.crypto.macs.HMac;
import org.bouncycastle.crypto.params.KeyParameter;
import org.bouncycastle.jcajce.provider.util.DigestFactory;
import org.moera.lib.node.types.MediaFilePreviewInfo;
import org.moera.lib.node.types.PrivateMediaFileInfo;
import org.moera.node.data.MediaFile;
import org.moera.node.data.MediaFilePreview;
import org.moera.node.model.MediaFilePreviewInfoUtil;
import org.springframework.util.ObjectUtils;

public class MediaUtil {

    public static String mediaPreview(String location, int width) {
        return "%s?width=%d".formatted(location, width);
    }

    public static String mediaPreviewDirect(String location, int width) {
        int pos = location.lastIndexOf('.');
        return "%s_%d%s".formatted(location.substring(0, pos), width, location.substring(pos));
    }

    public static String mediaSources(String location, Collection<MediaFilePreview> previews, boolean directServing) {
        if (ObjectUtils.isEmpty(previews)) {
            return "";
        }
        return mediaSourcesInfo(
            location,
            previews
                .stream()
                .filter(preview -> preview.getMediaFile() != null)
                .map(preview -> MediaFilePreviewInfoUtil.build(preview, directServing ? location : null))
                .collect(Collectors.toList())
        );
    }

    public static String mediaSourcesInfo(String location, Collection<MediaFilePreviewInfo> previews) {
        List<String> sources = new ArrayList<>();
        for (MediaFilePreviewInfo preview : previews) {
            String url = Boolean.TRUE.equals(preview.getOriginal())
                    ? location
                    : (preview.getDirectPath() != null
                        ? "/moera/media/" + preview.getDirectPath()
                        : mediaPreview(location, preview.getTargetWidth()));
            sources.add("%s %dw".formatted(url, preview.getWidth()));
        }
        return String.join(",", sources);
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

    public static String presignUrl(String location, String id, ExtendedDuration valid, String secret) {
        long expires = switch (valid.getZone()) {
            case FIXED ->
                Instant.now()
                    .plus(valid.getDuration())
                    .atZone(ZoneOffset.UTC)
                    .toLocalDate()
                    .atStartOfDay()
                    .plusDays(1)
                    .toEpochSecond(ZoneOffset.UTC);
            case ALWAYS ->
                LocalDate.of(2100, 1, 1).atStartOfDay().toEpochSecond(ZoneOffset.UTC);
            case NEVER ->
                Instant.now().toEpochMilli() / 1000;
        };

        var mac = new HMac(DigestFactory.getDigest("SHA-256"));
        mac.init(new KeyParameter(secret.getBytes(StandardCharsets.UTF_8)));
        byte[] data = id.getBytes(StandardCharsets.UTF_8);
        mac.update(data, 0, data.length);
        data = Long.toString(expires).getBytes(StandardCharsets.UTF_8);
        mac.update(data, 0, data.length);
        byte[] signature = new byte[mac.getMacSize()];
        mac.doFinal(signature, 0);

        return String.format("%s?exp=%d&sig=%s", location, expires, Util.base64urlencode(signature));
    }

}
