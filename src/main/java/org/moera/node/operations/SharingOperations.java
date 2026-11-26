package org.moera.node.operations;

import java.util.Comparator;
import java.util.List;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;
import jakarta.inject.Inject;

import org.moera.lib.node.types.SharingType;
import org.moera.lib.node.types.StorySummaryEntry;
import org.moera.lib.node.types.AvatarImage;
import org.moera.lib.node.types.Scope;
import org.moera.lib.node.types.SharingInfo;
import org.moera.lib.node.types.SharingsSliceInfo;
import org.moera.lib.node.types.notifications.PostingSharedNotification;
import org.moera.lib.node.types.notifications.PostingSharingUpdatedNotification;
import org.moera.lib.node.types.notifications.PostingUnsharedNotification;
import org.moera.lib.node.types.principal.AccessChecker;
import org.moera.lib.node.types.principal.AccessCheckers;
import org.moera.lib.node.types.validate.ValidationFailure;
import org.moera.lib.node.types.validate.ValidationUtil;
import org.moera.node.data.Entry;
import org.moera.node.data.EntryRevision;
import org.moera.node.data.EntryRevisionRepository;
import org.moera.node.data.EntrySharing;
import org.moera.node.data.EntrySharingRepository;
import org.moera.node.data.LinkedEntry;
import org.moera.node.data.LinkedEntryRepository;
import org.moera.node.data.Posting;
import org.moera.node.data.PostingRepository;
import org.moera.node.data.StoryRepository;
import org.moera.node.global.UniversalContext;
import org.moera.node.media.DirectServeOperations;
import org.moera.node.liberin.model.PostingSharingAddedLiberin;
import org.moera.node.liberin.model.PostingSharingDeletedLiberin;
import org.moera.node.liberin.model.PostingTotalSharingsUpdatedLiberin;
import org.moera.node.model.AvatarImageUtil;
import org.moera.node.model.ObjectNotFoundFailure;
import org.moera.node.model.SharingInfoUtil;
import org.moera.node.util.MomentFinder;
import org.moera.node.util.SafeInteger;
import org.moera.node.util.Util;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

@Component
public class SharingOperations {

    public static final int MAX_SHARINGS_PER_REQUEST = 200;

    @Inject
    private UniversalContext universalContext;

    @Inject
    private PostingRepository postingRepository;

    @Inject
    private EntryRevisionRepository entryRevisionRepository;

    @Inject
    private EntrySharingRepository entrySharingRepository;

    @Inject
    private LinkedEntryRepository linkedEntryRepository;

    @Inject
    private StoryRepository storyRepository;

    @Inject
    private DirectServeOperations directServeOperations;

    private final MomentFinder momentFinder = new MomentFinder();

    public SharingsSliceInfo getBefore(UUID entryId, long before, int limit) {
        return getBefore(entryId, before, limit, AccessCheckers.ADMIN);
    }

    public SharingsSliceInfo getBefore(UUID entryId, long before, int limit, AccessChecker accessChecker) {
        List<EntrySharing> sharings = entrySharingRepository.findSlice(
            entryId, SafeInteger.MIN_VALUE, before, PageRequest.of(0, limit + 1)
        );
        // Federated sharings use even moments, own sharings use odd moments, so it is possible to join them
        // in a single stream
        List<SharingInfo> page = Stream.concat(
            sharings.stream().map(sharing -> SharingInfoUtil.build(sharing, directServeOperations)),
            ownReferences(entryId).stream()
                .filter(link -> link.getSharingMoment() <= before)
                .filter(link ->
                    accessChecker.isPrincipal(link.getEntryRevision().getEntry().getViewE(), Scope.VIEW_CONTENT)
                )
                .limit(limit + 1L)
                .map(link -> SharingInfoUtil.build(link, universalContext.nodeName(), directServeOperations))
        ).sorted(Comparator.comparingLong(SharingInfo::getMoment).reversed()).limit(limit + 1L).toList();

        SharingsSliceInfo sliceInfo = new SharingsSliceInfo();
        sliceInfo.setBefore(before);
        sliceInfo.setAfter(page.size() <= limit ? SafeInteger.MIN_VALUE : page.getLast().getMoment());
        sliceInfo.setSharings(page.subList(0, Math.min(page.size(), limit)));
        return sliceInfo;
    }

    public void refreshTotal(UUID nodeId, UUID postingId) {
        postingRepository.findByNodeIdAndIdForUpdate(nodeId, postingId)
            .filter(Posting::isOriginal)
            .ifPresent(this::updateTotal);
    }

    private void updateTotal(Posting posting) {
        UUID id = posting.getId();
        int total = entrySharingRepository.countByEntryId(id) + ownReferences(id).size();
        if (total != posting.getTotalSharings()) {
            posting.setTotalSharings(total);
            universalContext.send(new PostingTotalSharingsUpdatedLiberin(posting, total));
        }
    }

    private List<LinkedEntry> ownReferences(UUID postingId) {
        List<LinkedEntry> links = linkedEntryRepository.findSharingLinks(postingId);
        Set<UUID> sources = new HashSet<>();
        return links.stream()
            .filter(link -> {
                EntryRevision revision = link.getEntryRevision();
                Entry source = revision.getEntry();
                EntryRevision signed = source.resolvePreviousRevision(null, source.getCurrentRevision());
                return link.getReceiverNodeName().equals(source.getOwnerName())
                    && signed != null && revision.getId().equals(signed.getId()) && sources.add(source.getId());
            })
            .toList();
    }

    public static void validate(
        SharingType sharingType,
        String postingId,
        String postingRevisionId,
        String remoteFeedName,
        String remoteStoryId,
        Long remoteStoryMoment,
        String remotePostingId
    ) {
        ValidationUtil.notNull(sharingType, "sharing.sharing-type.missing");
        ValidationUtil.notBlank(postingId, "sharing.posting-id.missing");
        parseRevisionId(postingRevisionId);
        boolean hasStory = remoteFeedName != null || remoteStoryId != null || remoteStoryMoment != null;
        if (hasStory) {
            ValidationUtil.notBlank(remoteFeedName, "sharing.remote-feed-name.missing");
            ValidationUtil.notBlank(remoteStoryId, "sharing.remote-story-id.missing");
            ValidationUtil.notNull(remoteStoryMoment, "sharing.remote-story-moment.missing");
        }
        if (remotePostingId != null) {
            ValidationUtil.notBlank(remotePostingId, "sharing.remote-posting-id.missing");
        }
        if (!hasStory && remotePostingId == null) {
            throw new ValidationFailure("sharing.reference-missing");
        }
    }

    private static UUID parseRevisionId(String postingRevisionId) {
        if (postingRevisionId == null) {
            return null;
        }
        ValidationUtil.notBlank(postingRevisionId, "sharing.posting-revision-id.missing");
        return Util.uuid(postingRevisionId)
            .orElseThrow(() -> new ValidationFailure("sharing.posting-revision-id.invalid"));
    }

    private Posting findPosting(String postingId) {
        UUID id = Util.uuid(postingId).orElseThrow(() -> new ObjectNotFoundFailure("posting.not-found"));
        Posting posting = postingRepository.findByNodeIdAndIdForUpdate(universalContext.nodeId(), id)
            .orElseThrow(() -> new ObjectNotFoundFailure("posting.not-found"));
        if (!posting.isOriginal()) {
            throw new ObjectNotFoundFailure("posting.not-found");
        }
        return posting;
    }

    public EntrySharing shared(PostingSharedNotification notification) {
        Posting posting = findPosting(notification.getPostingId());
        UUID revisionId = parseRevisionId(notification.getPostingRevisionId());
        EntryRevision revision = revisionId != null
            ? entryRevisionRepository.findByEntryIdAndId(universalContext.nodeId(), posting.getId(), revisionId)
                .orElseThrow(() -> new ObjectNotFoundFailure("posting-revision.not-found"))
            : null;

        List<EntrySharing> existing = entrySharingRepository.findMatching(
            posting.getId(), notification.getSharingType(), notification.getSenderNodeName(), revisionId,
            notification.getRemoteStoryId(), notification.getRemotePostingId()
        );
        if (!existing.isEmpty()) {
            return null;
        }

        EntrySharing sharing = new EntrySharing();
        sharing.setId(UUID.randomUUID());
        sharing.setSharingType(notification.getSharingType());
        sharing.setRemoteNodeName(notification.getSenderNodeName());
        sharing.setRemoteFullName(notification.getSenderFullName());
        sharing.setRemoteSourceUri(notification.getSenderSourceUri());
        sharing.setRemoteGender(notification.getSenderGender());
        if (notification.getSenderAvatar() != null) {
            sharing.setRemoteAvatarMediaFile(AvatarImageUtil.getMediaFile(notification.getSenderAvatar()));
            sharing.setRemoteAvatarShape(notification.getSenderAvatar().getShape());
        }
        sharing.setRemoteFeedName(notification.getRemoteFeedName());
        sharing.setRemoteStoryId(notification.getRemoteStoryId());
        sharing.setRemoteStoryMoment(notification.getRemoteStoryMoment());
        sharing.setRemotePostingId(notification.getRemotePostingId());
        sharing.setEntry(posting);
        sharing.setEntryRevision(revision);
        sharing.setMoment(momentFinder.find(
            moment -> moment % 2 == 0 && entrySharingRepository.countMoments(posting.getId(), moment) == 0,
            sharing.getCreatedAt()
        ));
        entrySharingRepository.saveAndFlush(sharing);
        updateTotal(posting);

        return sharing;
    }

    public void completeShared(
        UUID postingId,
        UUID sharingId,
        AvatarImage senderAvatar,
        StorySummaryEntry remotePosting
    ) {
        postingRepository
            .findByNodeIdAndIdForUpdate(universalContext.nodeId(), postingId)
            .ifPresent(posting ->
                entrySharingRepository
                    .findByEntryIdAndId(postingId, sharingId)
                    .ifPresent(sharing -> {
                        if (senderAvatar != null) {
                            sharing.setRemoteAvatarMediaFile(AvatarImageUtil.getMediaFile(senderAvatar));
                            sharing.setRemoteAvatarShape(senderAvatar.getShape());
                        }
                        universalContext.send(new PostingSharingAddedLiberin(posting, sharing, remotePosting));
                    })
            );
    }

    public void updated(PostingSharingUpdatedNotification notification) {
        Posting posting = findPosting(notification.getPostingId());
        List<EntrySharing> sharings = entrySharingRepository.findMatching(
            posting.getId(),
            SharingType.REPUBLISH,
            notification.getSenderNodeName(),
            parseRevisionId(notification.getPostingRevisionId()),
            notification.getRemoteStoryId(),
            notification.getRemotePostingId()
        );
        for (EntrySharing sharing : sharings) {
            sharing.setRemoteFeedName(notification.getRemoteFeedName());
            sharing.setRemoteStoryMoment(notification.getRemoteStoryMoment());
        }
    }

    public void unshared(PostingUnsharedNotification notification) {
        Posting posting = findPosting(notification.getPostingId());
        List<EntrySharing> sharings = entrySharingRepository.findMatching(
            posting.getId(),
            notification.getSharingType(),
            notification.getSenderNodeName(),
            parseRevisionId(notification.getPostingRevisionId()),
            notification.getRemoteStoryId(),
            notification.getRemotePostingId()
        );
        if (sharings.isEmpty()) {
            return;
        }

        List<PostingSharingDeletedLiberin> deletedLiberins = sharings.stream()
            .map(sharing -> new PostingSharingDeletedLiberin(
                posting, sharing, storyRepository.findSubIdsBySharingId(universalContext.nodeId(), sharing.getId())
            ))
            .toList();

        entrySharingRepository.deleteAll(sharings);
        entrySharingRepository.flush();
        deletedLiberins.forEach(universalContext::send);
        updateTotal(posting);
    }

}
