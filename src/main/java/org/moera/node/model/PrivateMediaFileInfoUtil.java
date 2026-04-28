package org.moera.node.model;

import java.time.Duration;
import java.util.stream.Collectors;

import org.moera.lib.node.types.PrivateMediaFileInfo;
import org.moera.lib.node.types.PrivateMediaFileOperations;
import org.moera.lib.node.types.principal.Principal;
import org.moera.node.config.DirectServeConfig;
import org.moera.node.data.MediaFileOwner;
import org.moera.node.data.Posting;
import org.moera.node.media.MediaGrantGenerator;
import org.moera.node.media.MimeUtil;
import org.moera.node.util.ExtendedDuration;
import org.moera.node.media.MediaUtil;
import org.springframework.util.ObjectUtils;

public class PrivateMediaFileInfoUtil {

    public static PrivateMediaFileInfo build(
        MediaFileOwner mediaFileOwner,
        String receiverName,
        DirectServeConfig config,
        MediaGrantGenerator grantGenerator
    ) {
        PrivateMediaFileInfo info = new PrivateMediaFileInfo();
        
        info.setId(mediaFileOwner.getId().toString());
        info.setHash(mediaFileOwner.getMediaFile().getId());
        info.setMimeType(mediaFileOwner.getMediaFile().getMimeType());
        info.setWidth(mediaFileOwner.getMediaFile().getSizeX());
        info.setHeight(mediaFileOwner.getMediaFile().getSizeY());
        info.setOrientation(mediaFileOwner.getMediaFile().getOrientation());
        info.setSize(mediaFileOwner.getMediaFile().getFileSize());
        info.setTitle(mediaFileOwner.getTitle());
        info.setTextContent(mediaFileOwner.getMediaFile().getRecognizedText());
        info.setAttachment(!mediaFileOwner.getMediaFile().isImage());
        if (mediaFileOwner.getMalwareMarks().isEmpty()) {
            fillPath(info, grantGenerator);
            fillDirectPath(info, config);
        } else {
            info.setPath(MediaUtil.privatePath(mediaFileOwner, null, null));
            info.setMalware(true);
        }

        Posting posting = mediaFileOwner.getPosting(receiverName);
        info.setPostingId(posting != null ? posting.getId().toString() : null);
        
        info.setPreviews(
            mediaFileOwner.getMediaFile().getPreviews().stream()
                .filter(pw -> pw.getMediaFile() != null)
                .map(pw -> MediaFilePreviewInfoUtil.build(pw, mediaFileOwner, config, grantGenerator))
                .collect(Collectors.toList())
        );

        PrivateMediaFileOperations operations = new PrivateMediaFileOperations();
        operations.setView(mediaFileOwner.getViewPrincipal(), Principal.PUBLIC);
        info.setOperations(operations);
        
        return info;
    }

    public static void fillPath(PrivateMediaFileInfo info, MediaGrantGenerator grantGenerator) {
        boolean download = Boolean.TRUE.equals(info.getAttachment());
        String fileName = !ObjectUtils.isEmpty(info.getTitle())
            ? MimeUtil.fileName(info.getTitle(), info.getMimeType())
            : null;
        ExtendedDuration valid = new ExtendedDuration(Duration.ofDays(3));
        String grant = grantGenerator != null
            ? grantGenerator.generate(info.getId(), valid, download, fileName)
            : null;
        info.setPath(MediaUtil.privatePath(info, null, grant));
    }

    public static void fillDirectPath(PrivateMediaFileInfo info, DirectServeConfig config) {
        var fileName = MimeUtil.fileName(info.getHash(), info.getMimeType());
        ExtendedDuration valid = new ExtendedDuration(Duration.ofDays(3));
        var userFileName = !ObjectUtils.isEmpty(info.getTitle())
            ? MimeUtil.fileName(info.getTitle(), info.getMimeType())
            : null;
        var pu = MediaUtil.directPath(fileName, info.getHash(), valid, userFileName, config);
        info.setDirectPath(pu.url());
        info.setDirectPathExpiresAt(pu.expires());
    }

}
