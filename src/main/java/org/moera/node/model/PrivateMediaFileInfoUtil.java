package org.moera.node.model;

import java.util.stream.Collectors;

import org.moera.lib.node.types.PrivateMediaFileInfo;
import org.moera.lib.node.types.PrivateMediaFileOperations;
import org.moera.lib.node.types.principal.Principal;
import org.moera.node.media.DirectServeOperations;
import org.moera.node.data.MediaFileOwner;
import org.moera.node.media.grant.MediaGrantSupplier;
import org.moera.node.media.MimeUtil;
import org.moera.node.media.MediaUtil;
import org.moera.node.util.Util;
import org.springframework.util.ObjectUtils;

public class PrivateMediaFileInfoUtil {

    public static PrivateMediaFileInfo build(
        MediaFileOwner mediaFileOwner,
        DirectServeOperations directServe,
        MediaGrantSupplier grantSupplier
    ) {
        PrivateMediaFileInfo info = new PrivateMediaFileInfo();
        
        info.setId(mediaFileOwner.getId().toString());
        info.setHash(mediaFileOwner.getMediaFile().getId());
        info.setDigest(Util.base64encode(mediaFileOwner.getMediaFile().getDigest()));
        info.setMimeType(mediaFileOwner.getMediaFile().getMimeType());
        info.setWidth(mediaFileOwner.getMediaFile().getSizeX());
        info.setHeight(mediaFileOwner.getMediaFile().getSizeY());
        info.setOrientation(mediaFileOwner.getMediaFile().getOrientation());
        info.setSize(mediaFileOwner.getMediaFile().getFileSize());
        info.setDuration(mediaFileOwner.getMediaFile().getDuration());
        if (mediaFileOwner.getMediaFile().isUncompressed() && mediaFileOwner.isDownsize()) {
            info.setUncompressed(true);
        }
        if (mediaFileOwner.getCompressedOwner() != null) {
            info.setCompressedMediaId(mediaFileOwner.getCompressedOwner().getId().toString());
        }
        info.setTitle(mediaFileOwner.getTitle());
        info.setTextContent(mediaFileOwner.getMediaFile().getRecognizedText());
        info.setAttachment(
            !mediaFileOwner.getMediaFile().isReasonableImage() && !mediaFileOwner.getMediaFile().isVideo()
        );
        if (mediaFileOwner.getMalwareMarks().isEmpty()) {
            fillPath(info, grantSupplier);
            fillDirectPaths(info, mediaFileOwner, directServe);
        } else {
            info.setPath(MediaUtil.privatePath(mediaFileOwner, null, null));
            info.setMalware(true);
        }
        
        info.setPreviews(
            mediaFileOwner.getMediaFile().getPreviews().stream()
                .filter(pw -> pw.getMediaFile() != null)
                .map(pw -> MediaFilePreviewInfoUtil.build(pw, mediaFileOwner, directServe, grantSupplier))
                .collect(Collectors.toList())
        );

        PrivateMediaFileOperations operations = new PrivateMediaFileOperations();
        operations.setView(mediaFileOwner.isUnrestricted() ? Principal.PUBLIC : Principal.ADMIN, Principal.PUBLIC);
        info.setOperations(operations);
        
        return info;
    }

    public static void fillPath(PrivateMediaFileInfo info, MediaGrantSupplier grantSupplier) {
        boolean download = Boolean.TRUE.equals(info.getAttachment());
        String fileName = !ObjectUtils.isEmpty(info.getTitle())
            ? MimeUtil.fileName(info.getTitle(), info.getMimeType())
            : null;
        String grant = grantSupplier != null
            ? grantSupplier.generateLocal(info.getId(), MediaUtil.MEDIA_GRANT_TTL, download, fileName)
            : null;
        info.setGrant(grant);
        info.setPath(MediaUtil.privatePath(info, null, grant));
        info.setGrantExpiresAt(
            grantSupplier != null ? Util.toEpochSecond(grantSupplier.expires(MediaUtil.MEDIA_GRANT_TTL)) : null
        );
    }

    public static void fillDirectPaths(
        PrivateMediaFileInfo info, MediaFileOwner mediaFileOwner, DirectServeOperations directServe
    ) {
        String displayFileName = !ObjectUtils.isEmpty(info.getTitle())
            ? MimeUtil.fileName(info.getTitle(), info.getMimeType())
            : null;
        var displayPath = directServe.directPath(
            mediaFileOwner.getMediaFile(), MediaUtil.MEDIA_GRANT_TTL, displayFileName
        );
        info.setDirectPath(displayPath.url());
        info.setDirectPathExpiresAt(displayPath.expires());

        var downloadPath = directServe.directDownloadPath(
            mediaFileOwner.getMediaFile(), MediaUtil.MEDIA_GRANT_TTL, mediaFileOwner.getUserFileName()
        );
        info.setDirectDownloadPath(downloadPath.url());
        info.setDirectDownloadPathExpiresAt(downloadPath.expires());
    }

    public static void refreshDirectPaths(PrivateMediaFileInfo info, DirectServeOperations directServe) {
        String sourcePath = info.getDirectPath();
        String displayFileName = !ObjectUtils.isEmpty(info.getTitle())
            ? MimeUtil.fileName(info.getTitle(), info.getMimeType())
            : null;
        var displayPath = directServe.refreshDirectPath(
            sourcePath, info.getHash(), MediaUtil.MEDIA_GRANT_TTL, displayFileName
        );
        info.setDirectPath(displayPath.url());
        info.setDirectPathExpiresAt(displayPath.expires());

        String downloadFileName = MimeUtil.fileName(
            !ObjectUtils.isEmpty(info.getTitle()) ? info.getTitle() : info.getId(),
            info.getMimeType()
        );
        var downloadPath = directServe.refreshDirectDownloadPath(
            sourcePath, info.getHash(), MediaUtil.MEDIA_GRANT_TTL, downloadFileName
        );
        info.setDirectDownloadPath(downloadPath.url());
        info.setDirectDownloadPathExpiresAt(downloadPath.expires());
    }

}
