package org.moera.node.model;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import org.moera.lib.node.types.MediaToAttach;
import org.moera.node.data.EntryAttachment;
import org.springframework.util.ObjectUtils;

public class MediaToAttachUtil {

    public static boolean equals(List<MediaToAttach> curr, Set<EntryAttachment> prev) {
        var currEmpty = ObjectUtils.isEmpty(curr);
        var prevEmpty = ObjectUtils.isEmpty(prev);

        if (currEmpty && prevEmpty) {
            return true;
        }

        if (currEmpty || prevEmpty) {
            return false;
        }

        if (curr.size() != prev.size()) {
            return false;
        }

        var list = prev.stream().sorted(Comparator.comparing(EntryAttachment::getOrdinal)).toList();

        for (int i = 0; i < curr.size(); i++) {
            var currItem = curr.get(i);
            var prevItem = list.get(i);

            if (currItem.getLocalMediaId() != null) {
                if (prevItem.getMediaFileOwner() == null) {
                    return false;
                }
                if (!currItem.getLocalMediaId().equals(prevItem.getMediaFileOwner().getId().toString())) {
                    return false;
                }
            } else if (prevItem.getMediaFileOwner() != null) {
                return false;
            }

            var currRemote = currItem.getRemoteMedia();
            var prevRemote = prevItem.getRemoteMediaFile();

            if (currRemote != null) {
                if (prevRemote == null) {
                    return false;
                }
                if (
                    !Objects.equals(currRemote.getNodeName(), prevRemote.getNodeName())
                    || !Objects.equals(currRemote.getMediaId(), prevRemote.getMediaId())
                    || !Objects.equals(currRemote.getHash(), prevRemote.getHash())
                ) {
                    return false;
                }
            } else if (prevRemote != null) {
                return false;
            }
        }

        return true;
    }

}
