package org.moera.node.model;

import java.time.Duration;
import java.util.stream.Collectors;

import org.moera.lib.node.types.PrivateMediaFileInfo;
import org.moera.lib.node.types.PrivateMediaFileOperations;
import org.moera.lib.node.types.principal.Principal;
import org.moera.node.config.DirectServeConfig;
import org.moera.node.data.MediaFileOwner;
import org.moera.node.data.Posting;
import org.moera.node.media.MimeUtils;
import org.moera.node.util.ExtendedDuration;
import org.moera.node.util.MediaUtil;
import org.springframework.util.ObjectUtils;

public class PrivateMediaFileInfoUtil {

    public static PrivateMediaFileInfo build(
        MediaFileOwner mediaFileOwner, String receiverName, DirectServeConfig config
    ) {
        PrivateMediaFileInfo info = new PrivateMediaFileInfo();
        
        info.setId(mediaFileOwner.getId().toString());
        info.setHash(mediaFileOwner.getMediaFile().getId());
        info.setPath(MediaUtil.privatePath(mediaFileOwner, null, null));
        info.setMimeType(mediaFileOwner.getMediaFile().getMimeType());
        info.setWidth(mediaFileOwner.getMediaFile().getSizeX());
        info.setHeight(mediaFileOwner.getMediaFile().getSizeY());
        info.setOrientation(mediaFileOwner.getMediaFile().getOrientation());
        info.setSize(mediaFileOwner.getMediaFile().getFileSize());
        info.setTitle(mediaFileOwner.getTitle());
        info.setTextContent(mediaFileOwner.getMediaFile().getRecognizedText());
        if (mediaFileOwner.getMalwareMarks().isEmpty()) {
            fillDirectPath(info, config);
        } else {
            info.setMalware(true);
        }

        Posting posting = mediaFileOwner.getPosting(receiverName);
        info.setPostingId(posting != null ? posting.getId().toString() : null);
        
        info.setPreviews(
            mediaFileOwner.getMediaFile().getPreviews().stream()
                .filter(pw -> pw.getMediaFile() != null)
                .map(pw -> MediaFilePreviewInfoUtil.build(pw, mediaFileOwner, config))
                .collect(Collectors.toList())
        );

        info.setAttachment(!mediaFileOwner.getMediaFile().isImage());

        PrivateMediaFileOperations operations = new PrivateMediaFileOperations();
        operations.setView(mediaFileOwner.getViewPrincipal(), Principal.PUBLIC);
        info.setOperations(operations);
        
        return info;
    }

    public static void fillDirectPath(PrivateMediaFileInfo info, DirectServeConfig config) {
        var fileName = MimeUtils.fileName(info.getHash(), info.getMimeType());
        ExtendedDuration valid = new ExtendedDuration(Duration.ofDays(3));
        var userFileName = !ObjectUtils.isEmpty(info.getTitle())
            ? MimeUtils.fileName(info.getTitle(), info.getMimeType())
            : null;
        var pu = MediaUtil.directPath(fileName, info.getHash(), valid, userFileName, config);
        info.setDirectPath(pu.url());
        info.setDirectPathExpiresAt(pu.expires());
    }

}
