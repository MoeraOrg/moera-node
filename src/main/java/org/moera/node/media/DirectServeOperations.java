package org.moera.node.media;

import java.nio.charset.StandardCharsets;

import jakarta.inject.Inject;

import org.bouncycastle.crypto.macs.HMac;
import org.bouncycastle.crypto.params.KeyParameter;
import org.bouncycastle.jcajce.provider.util.DigestFactory;
import org.moera.node.config.Config;
import org.moera.node.config.DirectServeConfig;
import org.moera.node.data.MediaFile;
import org.moera.node.data.MediaFileOwner;
import org.moera.node.util.ExtendedDuration;
import org.moera.node.util.UriUtil;
import org.moera.node.util.Util;
import org.springframework.stereotype.Component;
import org.springframework.util.ObjectUtils;

@Component
public class DirectServeOperations {

    private final DirectServeConfig config;

    @Inject
    public DirectServeOperations(Config config) {
        this(config.getMedia().getDirectServe());
    }

    public DirectServeOperations(DirectServeConfig config) {
        this.config = config;
    }

    private DirectServePath presignUrl(
        String location,
        String id,
        ExtendedDuration valid,
        String userFileName,
        boolean download
    ) {
        var mac = new HMac(DigestFactory.getDigest("SHA-256"));
        mac.init(new KeyParameter(config.getSecret().getBytes(StandardCharsets.UTF_8)));
        byte[] data = id.getBytes(StandardCharsets.UTF_8);
        mac.update(data, 0, data.length);
        long expires = Util.toEpochSecond(MediaUtil.expirationTimestamp(valid));
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
        if (download) {
            url += "&download=true";
        }

        return new DirectServePath(url, expires);
    }

    private DirectServePath directPath(
        String location,
        String id,
        ExtendedDuration valid,
        String userFileName,
        boolean download
    ) {
        return switch (config.getSource()) {
            case NONE -> DirectServePath.NONE;
            case FILESYSTEM -> ObjectUtils.isEmpty(location)
                ? DirectServePath.NONE
                : presignUrl(location, id, valid, userFileName, download);
        };
    }

    public DirectServePath directPath(
        MediaFile mediaFile,
        ExtendedDuration valid,
        String userFileName
    ) {
        return directPath(mediaFile.getFileName(), mediaFile.getId(), valid, userFileName, false);
    }

    public DirectServePath directPath(MediaFile mediaFile, ExtendedDuration valid) {
        return directPath(mediaFile, valid, null);
    }

    public DirectServePath directPath(MediaFileOwner mediaFileOwner) {
        String userFileName = !ObjectUtils.isEmpty(mediaFileOwner.getTitle())
            ? MimeUtil.fileName(mediaFileOwner.getTitle(), mediaFileOwner.getMediaFile().getMimeType())
            : null;
        return directPath(mediaFileOwner.getMediaFile(), MediaUtil.MEDIA_GRANT_TTL, userFileName);
    }

    public DirectServePath directDownloadPath(
        MediaFile mediaFile,
        ExtendedDuration valid,
        String userFileName
    ) {
        return directPath(mediaFile.getFileName(), mediaFile.getId(), valid, userFileName, true);
    }

    public DirectServePath refreshDirectPath(
        String directPath,
        String id,
        ExtendedDuration valid
    ) {
        if (ObjectUtils.isEmpty(directPath)) {
            return DirectServePath.NONE;
        }

        String query = UriUtil.query(directPath);
        String userFileName = query != null ? UriUtil.queryParameter(query, "fn") : null;
        return directPath(UriUtil.stripQueryAndFragment(directPath), id, valid, userFileName, false);
    }

    public DirectServePath refreshDirectDownloadPath(
        String directPath,
        String id,
        ExtendedDuration valid,
        String userFileName
    ) {
        if (ObjectUtils.isEmpty(directPath)) {
            return DirectServePath.NONE;
        }
        return directPath(UriUtil.stripQueryAndFragment(directPath), id, valid, userFileName, true);
    }

    public record DirectServePath(String url, Long expires) {

        public static final DirectServePath NONE = new DirectServePath(null, null);

    }

}
