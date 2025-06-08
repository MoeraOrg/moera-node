package org.moera.node.model;

import java.util.stream.Collectors;

import org.moera.lib.node.types.PrivateMediaFileInfo;
import org.moera.lib.node.types.PrivateMediaFileOperations;
import org.moera.lib.node.types.principal.Principal;
import org.moera.node.data.MediaFileOwner;
import org.moera.node.data.Posting;

public class PrivateMediaFileInfoUtil {

    public static PrivateMediaFileInfo build(MediaFileOwner mediaFileOwner, String receiverName) {
        PrivateMediaFileInfo info = new PrivateMediaFileInfo();
        
        info.setId(mediaFileOwner.getId().toString());
        info.setHash(mediaFileOwner.getMediaFile().getId());
        info.setPath("private/" + mediaFileOwner.getFileName());
        info.setDirectPath(
            mediaFileOwner.getDirectFileName() != null ? "private/" + mediaFileOwner.getDirectFileName() : null
        );
        info.setMimeType(mediaFileOwner.getMediaFile().getMimeType());
        info.setWidth(mediaFileOwner.getMediaFile().getSizeX());
        info.setHeight(mediaFileOwner.getMediaFile().getSizeY());
        info.setOrientation(mediaFileOwner.getMediaFile().getOrientation());
        info.setSize(mediaFileOwner.getMediaFile().getFileSize());
        info.setTextContent(mediaFileOwner.getMediaFile().getRecognizedText());
        
        Posting posting = mediaFileOwner.getPosting(receiverName);
        info.setPostingId(posting != null ? posting.getId().toString() : null);
        
        info.setPreviews(
            mediaFileOwner.getMediaFile().getPreviews().stream()
                .filter(pw -> pw.getMediaFile() != null)
                .map(pw -> MediaFilePreviewInfoUtil.build(pw, info.getDirectPath()))
                .collect(Collectors.toList())
        );

        PrivateMediaFileOperations operations = new PrivateMediaFileOperations();
        operations.setView(mediaFileOwner.getViewPrincipal(), Principal.PUBLIC);
        info.setOperations(operations);
        
        return info;
    }

}
