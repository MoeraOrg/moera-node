package org.moera.node.model;

import java.util.stream.Collectors;

import org.moera.lib.node.types.PrivateMediaFileInfo;
import org.moera.lib.node.types.PrivateMediaFileOperations;
import org.moera.lib.node.types.principal.Principal;
import org.moera.node.config.DirectServeConfig;
import org.moera.node.data.MediaFile;
import org.moera.node.data.MediaFileOwner;
import org.moera.node.media.MediaGrantSupplier;
import org.moera.node.media.MimeUtil;
import org.moera.node.media.MediaUtil;
import org.moera.node.util.Util;
import org.springframework.util.ObjectUtils;

public class PrivateMediaFileInfoUtil {

    public static PrivateMediaFileInfo build(
        MediaFileOwner mediaFileOwner,
        DirectServeConfig config,
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
        info.setTitle(mediaFileOwner.getTitle());
        info.setTextContent(mediaFileOwner.getMediaFile().getRecognizedText());
        info.setAttachment(!mediaFileOwner.getMediaFile().isReasonableImage());
        if (mediaFileOwner.getMalwareMarks().isEmpty()) {
            fillPath(info, grantSupplier);
            fillDirectPath(info, mediaFileOwner.getMediaFile(), config);
        } else {
            info.setPath(MediaUtil.privatePath(mediaFileOwner, null, null));
            info.setMalware(true);
        }
        
        info.setPreviews(
            mediaFileOwner.getMediaFile().getPreviews().stream()
                .filter(pw -> pw.getMediaFile() != null)
                .map(pw -> MediaFilePreviewInfoUtil.build(pw, mediaFileOwner, config, grantSupplier))
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

    public static void fillDirectPath(
        PrivateMediaFileInfo info, MediaFile mediaFile, DirectServeConfig config
    ) {
        var userFileName = !ObjectUtils.isEmpty(info.getTitle())
            ? MimeUtil.fileName(info.getTitle(), info.getMimeType())
            : null;
        var pu = MediaUtil.directPath(mediaFile, MediaUtil.MEDIA_GRANT_TTL, userFileName, config);
        info.setDirectPath(pu.url());
        info.setDirectPathExpiresAt(pu.expires());
    }

    public static void refreshDirectPath(PrivateMediaFileInfo info, DirectServeConfig config) {
        var pu = MediaUtil.refreshDirectPath(
            info.getDirectPath(), info.getHash(), MediaUtil.MEDIA_GRANT_TTL, config
        );
        info.setDirectPath(pu.url());
        info.setDirectPathExpiresAt(pu.expires());
    }

}
