package org.moera.node.operations;

import java.util.List;
import java.util.UUID;
import jakarta.inject.Inject;

import org.moera.lib.node.types.SharingType;
import org.moera.lib.node.types.AvatarImage;
import org.moera.lib.node.types.SharingsSliceInfo;
import org.moera.lib.node.types.notifications.PostingSharedNotification;
import org.moera.lib.node.types.notifications.PostingUnsharedNotification;
import org.moera.lib.node.types.validate.ValidationFailure;
import org.moera.lib.node.types.validate.ValidationUtil;
import org.moera.node.data.EntryRevision;
import org.moera.node.data.EntryRevisionRepository;
import org.moera.node.data.EntrySharing;
import org.moera.node.data.EntrySharingRepository;
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
    private StoryRepository storyRepository;

    @Inject
    private DirectServeOperations directServeOperations;

    private final MomentFinder momentFinder = new MomentFinder();

    public SharingsSliceInfo getBefore(UUID entryId, long before, int limit) {
        List<EntrySharing> sharings = entrySharingRepository.findSlice(
            entryId, SafeInteger.MIN_VALUE, before, PageRequest.of(0, limit + 1)
        );
        SharingsSliceInfo sliceInfo = new SharingsSliceInfo();
        sliceInfo.setBefore(before);
        sliceInfo.setAfter(sharings.size() <= limit ? SafeInteger.MIN_VALUE : sharings.get(limit).getMoment());
        sliceInfo.setSharings(
            sharings.stream()
                .limit(limit)
                .map(sharing -> SharingInfoUtil.build(sharing, directServeOperations))
                .toList()
        );
        return sliceInfo;
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
        if (postingRevisionId != null) {
            ValidationUtil.notBlank(postingRevisionId, "sharing.posting-revision-id.missing");
            if (Util.uuid(postingRevisionId).isEmpty()) {
                throw new ValidationFailure("sharing.posting-revision-id.invalid");
            }
        }
        boolean hasStory = remoteFeedName != null || remoteStoryId != null || remoteStoryMoment != null;
        if (hasStory) {
            ValidationUtil.notBlank(remoteFeedName, "sharing.story-reference-incomplete");
            ValidationUtil.notBlank(remoteStoryId, "sharing.story-reference-incomplete");
            ValidationUtil.notNull(remoteStoryMoment, "sharing.story-reference-incomplete");
        }
        if (remotePostingId != null) {
            ValidationUtil.notBlank(remotePostingId, "sharing.remote-posting-id.missing");
        }
        if (!hasStory && remotePostingId == null) {
            throw new ValidationFailure("sharing.reference-missing");
        }
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
        UUID revisionId = notification.getPostingRevisionId() != null
            ? Util.uuid(notification.getPostingRevisionId()).orElseThrow()
            : null;
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
            moment -> entrySharingRepository.countMoments(posting.getId(), moment) == 0, sharing.getCreatedAt()
        ));
        entrySharingRepository.saveAndFlush(sharing);
        posting.setTotalSharings(entrySharingRepository.countByEntryId(posting.getId()));
        universalContext.send(new PostingTotalSharingsUpdatedLiberin(posting, posting.getTotalSharings()));

        return sharing;
    }

    public void completeShared(UUID postingId, UUID sharingId, AvatarImage senderAvatar) {
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
                        universalContext.send(new PostingSharingAddedLiberin(posting, sharing));
                    })
            );
    }

    public void unshared(PostingUnsharedNotification notification) {
        Posting posting = findPosting(notification.getPostingId());
        UUID revisionId = notification.getPostingRevisionId() != null
            ? Util.uuid(notification.getPostingRevisionId()).orElseThrow()
            : null;
        List<EntrySharing> sharings = entrySharingRepository.findMatching(
            posting.getId(),
            notification.getSharingType(),
            notification.getSenderNodeName(),
            revisionId,
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
        posting.setTotalSharings(entrySharingRepository.countByEntryId(posting.getId()));

        deletedLiberins.forEach(universalContext::send);
        universalContext.send(new PostingTotalSharingsUpdatedLiberin(posting, posting.getTotalSharings()));
    }

}
