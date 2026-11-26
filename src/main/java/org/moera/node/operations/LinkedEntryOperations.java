package org.moera.node.operations;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import jakarta.inject.Inject;

import org.moera.lib.naming.NodeName;
import org.moera.lib.node.types.body.Body;
import org.moera.node.data.Entry;
import org.moera.node.data.EntryRevision;
import org.moera.node.data.EntrySharingRepository;
import org.moera.node.data.LinkedEntry;
import org.moera.node.data.LinkedEntryRepository;
import org.moera.node.data.Pick;
import org.moera.node.data.Posting;
import org.moera.node.data.PostingRepository;
import org.moera.node.global.UniversalContext;
import org.moera.node.picker.PickerPool;
import org.moera.node.text.PostingReferencesExtractor;
import org.moera.node.text.PostingReferencesExtractor.Reference;
import org.moera.node.util.Util;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class LinkedEntryOperations {

    @Inject
    private UniversalContext universalContext;

    @Inject
    private LinkedEntryRepository linkedEntryRepository;

    @Inject
    private PostingRepository postingRepository;

    @Inject
    private EntrySharingRepository entrySharingRepository;

    @Inject
    private SharingOperations sharingOperations;

    @Inject
    private PickerPool pickerPool;

    private Set<Reference> extractReferences(EntryRevision revision) {
        Entry entry = revision.getEntry();
        if (
            entry.getParentMediaEntry() != null
            || entry.getParentMedia() != null
            || entry.getParentRemoteMedia() != null
            || !StringUtils.hasLength(revision.getBody())
        ) {
            return Set.of();
        }
        return PostingReferencesExtractor.extract(new Body(revision.getBody())).stream()
            .filter(reference -> NodeName.isLengthValid(reference.nodeName()) && reference.postingId().length() <= 40)
            .collect(Collectors.toSet());
    }

    public void create(EntryRevision revision) {
        List<LinkedEntry> links = new ArrayList<>();
        for (var reference : extractReferences(revision)) {
            LinkedEntry link = new LinkedEntry();
            link.setId(UUID.randomUUID());
            link.setEntryRevision(revision);
            link.setReceiverNodeName(reference.nodeName());
            link.setReceiverEntryId(reference.postingId());
            boolean local = isLocal(reference.nodeName());
            Posting target;
            if (local) {
                target = Util.uuid(reference.postingId())
                    .flatMap(id -> postingRepository.findByNodeIdAndIdIncludingDeleted(universalContext.nodeId(), id))
                    .orElse(null);
            } else {
                target = postingRepository.findByReceiverId(
                    universalContext.nodeId(), reference.nodeName(), reference.postingId()
                ).filter(posting -> posting.getDeletedAt() == null).orElse(null);
            }
            link.setLinkedEntry(target);
            links.add(link);
        }
        // Acquire the target locks in the same order for every source revision.
        links.sort(Comparator.comparing(
            link -> link.getLinkedEntry() != null ? link.getLinkedEntry().getId() : null,
            Comparator.nullsLast(Comparator.naturalOrder())
        ));
        for (LinkedEntry link : links) {
            setSharingMoment(link);
            link = linkedEntryRepository.save(link);
            revision.addLinkedEntry(link);
            if (link.getLinkedEntry() == null && !isLocal(link.getReceiverNodeName())) {
                pickerPool.pickAfterCommit(toPick(link));
            }
        }
    }

    private void setSharingMoment(LinkedEntry link) {
        Entry target = link.getLinkedEntry();
        Entry source = link.getEntryRevision().getEntry();
        if (
            target == null
            || !target.isOriginal()
            || !source.isOriginal()
            || !Objects.equals(NodeName.expand(source.getOwnerName()), link.getReceiverNodeName())
        ) {
            return;
        }

        UUID targetId = target.getId();
        linkedEntryRepository.lockSharingMoments(targetId.toString());
        long moment = Math.max(
            Util.toEpochSecond(Util.now()) * 1000,
            Math.max(entrySharingRepository.lastMoment(targetId), linkedEntryRepository.lastSharingMoment(targetId)) + 1
        );
        // New federated sharings use even moments. Start after any legacy sharing of either parity.
        link.setSharingMoment(moment + 1 - moment % 2);
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
            .ifPresent(link -> {
                boolean unresolved = link.getLinkedEntry() == null;
                link.setLinkedEntry(posting);
                if (unresolved) {
                    setSharingMoment(link);
                    if (link.getSharingMoment() != null) {
                        sharingOperations.refreshTotal(posting.getNodeId(), posting.getId());
                    }
                }
            });
    }

    public boolean resolveLocal(Pick pick) {
        if (pick.getLinkedEntry() == null || !isLocal(pick.getRemoteNodeName())) {
            return false;
        }

        Util.uuid(pick.getRemotePostingId())
            .flatMap(id -> postingRepository.findByNodeIdAndIdIncludingDeleted(universalContext.nodeId(), id))
            .ifPresent(posting -> picked(pick, posting));

        return true;
    }

    private boolean isLocal(String nodeName) {
        return Objects.equals(universalContext.nodeName(), NodeName.expand(nodeName));
    }

}
