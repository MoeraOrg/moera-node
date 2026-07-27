package org.moera.node.media;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

import jakarta.inject.Inject;

import org.bouncycastle.crypto.macs.HMac;
import org.bouncycastle.crypto.params.KeyParameter;
import org.bouncycastle.jcajce.provider.util.DigestFactory;
import org.moera.node.config.Config;
import org.moera.node.config.DirectServeSource;
import org.moera.node.data.MediaFile;
import org.moera.node.data.MediaFileOwner;
import org.moera.node.domain.Domains;
import org.moera.node.global.UniversalContext;
import org.moera.node.media.awss3.AwsS3MediaStorage;
import org.moera.node.util.ExtendedDuration;
import org.moera.node.util.UriUtil;
import org.moera.node.util.Util;
import org.springframework.stereotype.Component;
import org.springframework.http.ContentDisposition;
import org.springframework.util.ObjectUtils;

@Component
public class DirectServeOperations {

    private static final Duration AWS_URL_MAX_TTL = Duration.ofDays(7);

    @Inject
    private Config config;

    @Inject
    private UniversalContext universalContext;

    @Inject
    private AwsS3MediaStorage awsS3MediaStorage;

    @Inject
    private Domains domains;

    public DirectServeSource source() {
        return config.getMedia().getDirectServe().getSource();
    }

    private DirectServePath presignFilesystemUrl(
        String location,
        String id,
        ExtendedDuration valid,
        String userFileName,
        boolean download
    ) {
        var mac = new HMac(DigestFactory.getDigest("SHA-256"));
        mac.init(new KeyParameter(
            config.getMedia().getDirectServe().getSecret().getBytes(StandardCharsets.UTF_8)
        ));
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

    private DirectServePath presignS3Url(
        String key, ExtendedDuration valid, String userFileName, boolean download
    ) {
        if (ObjectUtils.isEmpty(key) || awsS3MediaStorage == null || !awsS3MediaStorage.isConfigured()) {
            return DirectServePath.NONE;
        }

        Duration duration = switch (valid.getZone()) {
            case FIXED -> valid.getDuration().compareTo(AWS_URL_MAX_TTL) < 0
                ? valid.getDuration()
                : AWS_URL_MAX_TTL;
            case ALWAYS -> AWS_URL_MAX_TTL;
            case NEVER -> Duration.ofSeconds(1);
        };
        String disposition = null;
        if (!ObjectUtils.isEmpty(userFileName)) {
            var builder = download ? ContentDisposition.attachment() : ContentDisposition.inline();
            disposition = builder.filename(userFileName, StandardCharsets.UTF_8).build().toString();
        } else if (download) {
            disposition = ContentDisposition.attachment().build().toString();
        }
        var path = awsS3MediaStorage.presign(key, duration, disposition);
        return new DirectServePath(path.url(), path.expires());
    }

    private DirectServePath directPath(
        String filesystemLocation,
        String cloudLocation,
        String id,
        ExtendedDuration valid,
        String userFileName,
        boolean download
    ) {
        return switch (source()) {
            case NONE -> DirectServePath.NONE;
            case FILESYSTEM -> ObjectUtils.isEmpty(filesystemLocation)
                ? DirectServePath.NONE
                : presignFilesystemUrl(filesystemLocation, id, valid, userFileName, download);
            case AWSS3 -> presignS3Url(cloudLocation, valid, userFileName, download);
        };
    }

    public DirectServePath directPath(
        MediaFile mediaFile,
        ExtendedDuration valid,
        String userFileName
    ) {
        return directPath(
            mediaFile.getFileName(), mediaFile.getCloudFileName(), mediaFile.getId(), valid, userFileName, false
        );
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

    public String directUrl(MediaFile mediaFile) {
        String path = directPath(mediaFile, MediaUtil.MEDIA_GRANT_TTL).url();
        if (ObjectUtils.isEmpty(path)) {
            return null;
        }
        String mediaUrl = MediaUtil.mediaUrl(path);
        return URI.create(mediaUrl).isAbsolute()
            ? mediaUrl
            : URI.create("https://" + domains.getDomainDnsName(universalContext.nodeId())).resolve(mediaUrl).toString();
    }

    public String directLocation(MediaFile mediaFile) {
        String path = directPath(mediaFile, MediaUtil.MEDIA_GRANT_TTL).url();
        if (ObjectUtils.isEmpty(path)) {
            return null;
        }
        String mediaUrl = MediaUtil.mediaUrl(path);
        return URI.create(mediaUrl).isAbsolute() ? UriUtil.stripSchemeAndHost(mediaUrl) : mediaUrl;
    }

    public DirectServePath directDownloadPath(
        MediaFile mediaFile,
        ExtendedDuration valid,
        String userFileName
    ) {
        return directPath(
            mediaFile.getFileName(), mediaFile.getCloudFileName(), mediaFile.getId(), valid, userFileName, true
        );
    }

    public DirectServePath refreshDirectPath(
        String directPath,
        String id,
        ExtendedDuration valid
    ) {
        return refreshDirectPath(directPath, id, valid, null);
    }

    public DirectServePath refreshDirectPath(
        String directPath,
        String id,
        ExtendedDuration valid,
        String userFileName
    ) {
        if (ObjectUtils.isEmpty(directPath)) {
            return DirectServePath.NONE;
        }

        return switch (source()) {
            case NONE -> DirectServePath.NONE;
            case FILESYSTEM -> {
                String query = UriUtil.query(directPath);
                String filesystemFileName = query != null ? UriUtil.queryParameter(query, "fn") : null;
                yield presignFilesystemUrl(
                    UriUtil.stripQueryAndFragment(directPath), id, valid, filesystemFileName, false
                );
            }
            case AWSS3 -> {
                String key = awsS3MediaStorage.extractKeyForId(directPath, id);
                yield key != null ? presignS3Url(key, valid, userFileName, false) : DirectServePath.NONE;
            }
        };
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

        return switch (source()) {
            case NONE -> DirectServePath.NONE;
            case FILESYSTEM -> presignFilesystemUrl(
                UriUtil.stripQueryAndFragment(directPath), id, valid, userFileName, true
            );
            case AWSS3 -> {
                String key = awsS3MediaStorage.extractKeyForId(directPath, id);
                yield key != null ? presignS3Url(key, valid, userFileName, true) : DirectServePath.NONE;
            }
        };
    }

    public record DirectServePath(String url, Long expires) {

        public static final DirectServePath NONE = new DirectServePath(null, null);

    }

}
