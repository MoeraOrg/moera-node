package org.moera.node.model;

import org.moera.lib.node.types.ParentMediaInfo;
import org.moera.node.data.Entry;
import org.moera.node.data.EntryType;

public class ParentMediaInfoUtil {

    public static ParentMediaInfo build(Entry posting) {
        ParentMediaInfo info = new ParentMediaInfo();
        if (posting.getParentMedia() != null) {
            info.setMediaId(posting.getParentMedia().getId().toString());
        } else if (posting.getParentRemoteMedia() != null) {
            info.setNodeName(posting.getParentRemoteMedia().getNodeName());
            info.setMediaId(posting.getParentRemoteMedia().getMediaId());
        }
        if (posting.getParentMediaEntry() != null) {
            if (posting.getParentMediaEntry().getEntryType() == EntryType.COMMENT) {
                info.setPostingId(posting.getParentMediaEntry().getParent().getId().toString());
                info.setCommentId(posting.getParentMediaEntry().getId().toString());
            } else {
                info.setPostingId(posting.getParentMediaEntry().getId().toString());
            }
        }
        return info;
    }

}
