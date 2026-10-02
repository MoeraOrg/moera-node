package org.moera.node.model;

import org.moera.lib.node.types.SharingInfo;
import org.moera.node.data.EntrySharing;
import org.moera.node.media.DirectServeOperations;
import org.moera.node.util.Util;

public class SharingInfoUtil {

    public static SharingInfo build(EntrySharing sharing, DirectServeOperations directServeOperations) {
        SharingInfo info = new SharingInfo();
        info.setId(sharing.getId().toString());
        info.setSharingType(sharing.getSharingType());
        info.setRemoteNodeName(sharing.getRemoteNodeName());
        info.setRemoteFullName(sharing.getRemoteFullName());
        info.setRemoteSourceUri(sharing.getRemoteSourceUri());
        info.setRemoteGender(sharing.getRemoteGender());
        if (sharing.getRemoteAvatarMediaFile() != null) {
            info.setRemoteAvatar(AvatarImageUtil.build(
                sharing.getRemoteAvatarMediaFile(), sharing.getRemoteAvatarShape(), directServeOperations
            ));
        }
        info.setRemoteFeedName(sharing.getRemoteFeedName());
        info.setRemoteStoryId(sharing.getRemoteStoryId());
        info.setRemoteStoryMoment(sharing.getRemoteStoryMoment());
        info.setRemotePostingId(sharing.getRemotePostingId());
        info.setMoment(sharing.getMoment());
        info.setPostingId(sharing.getEntry().getId().toString());
        if (sharing.getEntryRevision() != null) {
            info.setPostingRevisionId(sharing.getEntryRevision().getId().toString());
        }
        info.setCreatedAt(Util.toEpochSecond(sharing.getCreatedAt()));
        return info;
    }

}
