package org.moera.node.operations;

import java.util.Objects;
import java.util.UUID;
import jakarta.inject.Inject;

import org.moera.lib.naming.NodeName;
import org.moera.lib.node.types.body.Body;
import org.moera.node.data.EntryRevision;
import org.moera.node.data.LinkedEntry;
import org.moera.node.data.LinkedEntryRepository;
import org.moera.node.data.Pick;
import org.moera.node.data.Posting;
import org.moera.node.data.PostingRepository;
import org.moera.node.global.UniversalContext;
import org.moera.node.picker.PickerPool;
import org.moera.node.text.PostingReferencesExtractor;
import org.moera.node.util.Util;
import org.springframework.stereotype.Component;

@Component
public class LinkedEntryOperations {

    @Inject
    private UniversalContext universalContext;

    @Inject
    private LinkedEntryRepository linkedEntryRepository;

    @Inject
    private PostingRepository postingRepository;

    @Inject
    private PickerPool pickerPool;

    public void create(EntryRevision revision) {
        var entry = revision.getEntry();
        if (
            entry.getParentMediaEntry() != null
            || entry.getParentMedia() != null
            || entry.getParentRemoteMedia() != null
        ) {
            return;
        }

        for (var reference : PostingReferencesExtractor.extract(new Body(revision.getBody()))) {
            if (reference.postingId().length() > 40) {
                continue;
            }

            LinkedEntry link = new LinkedEntry();
            link.setId(UUID.randomUUID());
            link.setEntryRevision(revision);
            link.setReceiverNodeName(reference.nodeName());
            link.setReceiverEntryId(reference.postingId());
            boolean local = isLocal(reference.nodeName());
            Posting target;
            if (local) {
                target = Util.uuid(reference.postingId())
                    .flatMap(id -> postingRepository.findByNodeIdAndId(universalContext.nodeId(), id))
                    .orElse(null);
            } else {
                target = postingRepository.findByReceiverId(
                    universalContext.nodeId(), reference.nodeName(), reference.postingId()
                ).filter(posting -> posting.getDeletedAt() == null).orElse(null);
            }
            link.setLinkedEntry(target);
            link = linkedEntryRepository.save(link);
            revision.addLinkedEntry(link);
            if (target == null && !local) {
                pickerPool.pickAfterCommit(toPick(link));
            }
        }
    }

    private Pick toPick(LinkedEntry link) {
        Pick pick = new Pick();
        pick.setNodeId(universalContext.nodeId());
        pick.setRemoteNodeName(link.getReceiverNodeName());
        pick.setRemotePostingId(link.getReceiverEntryId());
        pick.setLinkedEntry(link);
        return pick;
    }

    public void picked(Pick pick, Posting posting) {
        if (pick.getLinkedEntry() == null) {
            return;
        }

        linkedEntryRepository.findByNodeIdAndId(posting.getNodeId(), pick.getLinkedEntry().getId())
            .ifPresent(link -> link.setLinkedEntry(posting));
    }

    public boolean resolveLocal(Pick pick) {
        if (pick.getLinkedEntry() == null || !isLocal(pick.getRemoteNodeName())) {
            return false;
        }

        Util.uuid(pick.getRemotePostingId())
            .flatMap(id -> postingRepository.findByNodeIdAndId(universalContext.nodeId(), id))
            .ifPresent(posting -> picked(pick, posting));

        return true;
    }

    private boolean isLocal(String nodeName) {
        return Objects.equals(NodeName.expand(universalContext.nodeName()), NodeName.expand(nodeName));
    }

}
