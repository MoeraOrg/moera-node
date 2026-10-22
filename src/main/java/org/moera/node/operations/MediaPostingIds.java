package org.moera.node.operations;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

import org.moera.node.data.EntryAttachment;
import org.moera.node.data.MediaFileOwner;
import org.moera.node.data.MediaPostingId;
import org.moera.node.data.Posting;
import org.moera.node.data.PostingRepository;
import org.moera.node.data.RemoteMediaFile;

public class MediaPostingIds {

    private final UUID parentEntryId;
    private final Map<UUID, UUID> local = new HashMap<>();
    private final Map<UUID, UUID> remote = new HashMap<>();

    public MediaPostingIds(UUID parentEntryId) {
        this.parentEntryId = parentEntryId;
    }

    public static MediaPostingIds load(
        PostingRepository postingRepository, UUID parentEntryId, Collection<EntryAttachment> attachments
    ) {
        var ids = new MediaPostingIds(parentEntryId);
        var localIds = attachments.stream()
            .map(EntryAttachment::getMediaFileOwner)
            .filter(Objects::nonNull)
            .map(MediaFileOwner::getId)
            .collect(Collectors.toSet());
        var remoteIds = attachments.stream()
            .filter(ea -> ea.getMediaFileOwner() == null)
            .map(EntryAttachment::getRemoteMediaFile)
            .filter(Objects::nonNull)
            .map(RemoteMediaFile::getId)
            .collect(Collectors.toSet());
        if (!localIds.isEmpty() || !remoteIds.isEmpty()) {
            postingRepository.findMediaPostingIds(parentEntryId, localIds, remoteIds).forEach(ids::add);
        }
        return ids;
    }

    public void add(Posting posting) {
        checkParent(posting.getParentMediaEntry() != null ? posting.getParentMediaEntry().getId() : null);
        add(new MediaPostingId(
            posting.getId(),
            posting.getParentMedia() != null ? posting.getParentMedia().getId() : null,
            posting.getParentRemoteMedia() != null ? posting.getParentRemoteMedia().getId() : null
        ));
    }

    private void add(MediaPostingId posting) {
        if (posting.parentMediaId() != null) {
            local.putIfAbsent(posting.parentMediaId(), posting.postingId());
        }
        if (posting.parentRemoteMediaId() != null) {
            remote.putIfAbsent(posting.parentRemoteMediaId(), posting.postingId());
        }
    }

    public UUID find(EntryAttachment attachment) {
        checkParent(
            attachment.getEntryRevision() != null ? attachment.getEntryRevision().getEntry().getId() : null
        );
        return attachment.getMediaFileOwner() != null
            ? local.get(attachment.getMediaFileOwner().getId())
            : attachment.getRemoteMediaFile() != null
              ? remote.get(attachment.getRemoteMediaFile().getId())
              : null;
    }

    private void checkParent(UUID parentId) {
        if (!Objects.equals(parentEntryId, parentId)) {
            throw new IllegalArgumentException("Media posting IDs belong to a different parent entry");
        }
    }

}
