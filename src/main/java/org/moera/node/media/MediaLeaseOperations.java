package org.moera.node.media;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.UUID;

import jakarta.inject.Inject;

import org.moera.node.data.Entry;
import org.moera.node.data.MediaFileOwner;
import org.moera.node.data.MediaLease;
import org.moera.node.data.MediaLeaseRepository;
import org.moera.node.global.UniversalContext;
import org.springframework.stereotype.Component;

@Component
public class MediaLeaseOperations {

    @Inject
    private UniversalContext universalContext;

    @Inject
    private MediaLeaseRepository mediaLeaseRepository;

    public MediaLease create(String ownerName, MediaFileOwner mediaFileOwner, Entry entry, boolean draftOnly) {
        MediaLease mediaLease = new MediaLease();
        mediaLease.setId(UUID.randomUUID());
        mediaLease.setNodeId(universalContext.nodeId());
        mediaLease.setOwnerName(ownerName);
        mediaLease.setMediaFileOwner(mediaFileOwner);
        mediaLease.setEntry(entry);
        if (draftOnly) {
            mediaLease.setDraftOnly(true);
            mediaLease.setDeadline(
                Timestamp.from(Instant.now().plus(MediaCleanupOperations.DRAFT_ONLY_LEASE_TTL))
            );
        }
        return mediaLeaseRepository.save(mediaLease);
    }

}
