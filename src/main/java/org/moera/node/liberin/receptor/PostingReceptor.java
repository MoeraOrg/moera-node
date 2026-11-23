package org.moera.node.liberin.receptor;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import jakarta.inject.Inject;

import org.moera.lib.naming.NodeName;
import org.moera.lib.node.types.AvatarImage;
import org.moera.lib.node.types.PostingInfo;
import org.moera.lib.node.types.SearchContentUpdateType;
import org.moera.lib.node.types.SharingType;
import org.moera.lib.node.types.StoryType;
import org.moera.lib.node.types.principal.Principal;
import org.moera.lib.node.types.principal.PrincipalExpression;
import org.moera.lib.node.types.principal.PrincipalFilter;
import org.moera.node.data.EntryRevision;
import org.moera.node.data.Feed;
import org.moera.node.data.LinkedEntryRepository;
import org.moera.node.data.Posting;
import org.moera.node.data.PostingRepository;
import org.moera.node.data.Story;
import org.moera.node.data.StoryRepository;
import org.moera.node.global.RequestCounter;
import org.moera.node.liberin.LiberinMapping;
import org.moera.node.liberin.LiberinReceptor;
import org.moera.node.liberin.model.PostingAddedLiberin;
import org.moera.node.liberin.model.PostingCommentTotalsUpdatedLiberin;
import org.moera.node.liberin.model.PostingDeletedLiberin;
import org.moera.node.liberin.model.PostingHeadingUpdatedLiberin;
import org.moera.node.liberin.model.PostingMediaTextUpdatedLiberin;
import org.moera.node.liberin.model.PostingPublishedAtUpdatedLiberin;
import org.moera.node.liberin.model.PostingRecommendationUpdatedLiberin;
import org.moera.node.liberin.model.PostingRestoredLiberin;
import org.moera.node.liberin.model.PostingUpdatedLiberin;
import org.moera.node.liberin.model.PostingViewedLiberin;
import org.moera.node.model.AvatarImageUtil;
import org.moera.node.model.PostingInfoUtil;
import org.moera.node.model.event.PostingAddedEvent;
import org.moera.node.model.event.PostingCommentsChangedEvent;
import org.moera.node.model.event.PostingDeletedEvent;
import org.moera.node.model.event.PostingRecommendationUpdatedEvent;
import org.moera.node.model.event.PostingRestoredEvent;
import org.moera.node.model.event.PostingUpdatedEvent;
import org.moera.node.model.event.PostingViewedEvent;
import org.moera.node.model.notification.MentionPostingAddedNotificationUtil;
import org.moera.node.model.notification.MentionPostingDeletedNotificationUtil;
import org.moera.node.model.notification.PostingCommentsUpdatedNotificationUtil;
import org.moera.node.model.notification.PostingDeletedNotificationUtil;
import org.moera.node.model.notification.PostingHeadingUpdatedNotificationUtil;
import org.moera.node.model.notification.PostingImportantUpdateNotificationUtil;
import org.moera.node.model.notification.PostingMediaTextUpdatedNotificationUtil;
import org.moera.node.model.notification.PostingSharedNotificationUtil;
import org.moera.node.model.notification.PostingUnsharedNotificationUtil;
import org.moera.node.model.notification.PostingUpdatedNotificationUtil;
import org.moera.node.model.notification.SearchContentUpdatedNotificationUtil;
import org.moera.node.model.notification.StoryAddedNotificationUtil;
import org.moera.node.notification.send.Directions;
import org.moera.node.text.PostingReferencesExtractor.Reference;
import org.moera.node.userlist.SheriffUserListOperations;
import org.moera.node.util.ExtendedDuration;
import org.springframework.scheduling.annotation.Scheduled;

@LiberinReceptor
public class PostingReceptor extends EntryReceptor {

    private record ViewedPosting(UUID nodeId, UUID postingId) {
    }

    private final ConcurrentHashMap<ViewedPosting, Integer> viewCounts = new ConcurrentHashMap<>();

    @Inject
    private PostingRepository postingRepository;

    @Inject
    private StoryRepository storyRepository;

    @Inject
    private LinkedEntryRepository linkedEntryRepository;

    @Inject
    private SheriffUserListOperations sheriffUserListOperations;

    @Inject
    private RequestCounter requestCounter;

    @LiberinMapping
    public void added(PostingAddedLiberin liberin) {
        Posting posting = liberin.getPosting();

        if (posting.isOriginal()) {
            notifyMentionsAndReferences(posting, posting.getCurrentRevision(), null, Principal.PUBLIC);
        }
        send(liberin, new PostingAddedEvent(posting, posting.getViewE()));
    }

    @LiberinMapping
    public void updated(PostingUpdatedLiberin liberin) {
        Posting posting = liberin.getPosting();
        List<Story> stories = storyRepository.findByEntryId(posting.getNodeId(), posting.getId());

        if (posting.isOriginal()) {
            notifyMentionsAndReferences(
                posting,
                posting.getCurrentRevision(),
                liberin.getLatestRevision(),
                liberin.getLatestViewPrincipal()
            );
        } else {
            var timelineStories = stories.stream()
                .filter(t -> Feed.TIMELINE.equals(t.getFeedName()) && t.getStoryType().equals(StoryType.POSTING_ADDED))
                .filter(t -> t.getViewPrincipal().isUnset())
                .toList();
            for (var story : timelineStories) {
                send(
                    Directions.single(
                        liberin.getNodeId(),
                        posting.getReceiverName(),
                        posting.getViewE().a().andNot(liberin.getLatestViewPrincipal())
                    ),
                    PostingSharedNotificationUtil.buildRepublish(
                        posting.getReceiverEntryId(),
                        null,
                        story.getFeedName(),
                        story.getId(),
                        story.getMoment(),
                        posting.getId()
                    )
                );
                send(
                    Directions.single(
                        liberin.getNodeId(),
                        posting.getReceiverName(),
                        posting.getViewE().not().and(liberin.getLatestViewPrincipal())
                    ),
                    PostingUnsharedNotificationUtil.build(
                        SharingType.REPUBLISH,
                        posting.getReceiverEntryId(),
                        null,
                        story.getFeedName(),
                        story.getId(),
                        story.getMoment(),
                        posting.getId()
                    )
                );
            }
        }

        PrincipalExpression addedFilter = posting.getViewE().a()
            .andNot(liberin.getLatestViewPrincipal());
        send(liberin, new PostingAddedEvent(posting, addedFilter));
        boolean isNotify = isNotifyWhenRevealed(posting);
        for (Story story : stories) {
            if (!Feed.isAdmin(story.getFeedName()) && story.getViewPrincipal().isUnset()) {
                if (isNotify) {
                    send(
                        Directions.feedSubscribers(posting.getNodeId(), story.getFeedName(), addedFilter),
                        StoryAddedNotificationUtil.build(story)
                    );
                }
                send(
                    Directions.searchSubscribers(posting.getNodeId(), addedFilter),
                    SearchContentUpdatedNotificationUtil.buildPostingUpdate(
                        SearchContentUpdateType.POSTING_ADD, universalContext.nodeName(), story
                    )
                );
            }
        }

        PrincipalExpression updatedFilter = posting.getViewE().a()
            .and(liberin.getLatestViewPrincipal());
        send(liberin, new PostingUpdatedEvent(posting, updatedFilter));
        send(
            Directions.postingSubscribers(posting.getNodeId(), posting.getId(), updatedFilter),
            PostingUpdatedNotificationUtil.build(posting.getId())
        );
        send(
            Directions.searchSubscribers(posting.getNodeId(), updatedFilter),
            SearchContentUpdatedNotificationUtil.buildPostingUpdate(
                SearchContentUpdateType.POSTING_UPDATE, universalContext.nodeName(), posting
            )
        );
        if (posting.getCurrentRevision().isUpdateImportant()) {
            AvatarImage ownerAvatar = AvatarImageUtil.build(
                posting.getOwnerAvatarMediaFile(), posting.getOwnerAvatarShape(), directServeOperations
            );
            send(
                Directions.postingCommentsSubscribers(posting.getNodeId(), posting.getId(), updatedFilter),
                PostingImportantUpdateNotificationUtil.build(
                    posting.getOwnerName(),
                    posting.getOwnerFullName(),
                    posting.getOwnerSourceUri(),
                    posting.getOwnerGender(),
                    ownerAvatar, posting.getId(),
                    posting.getCurrentRevision().getHeading(),
                    posting.getCurrentRevision().getUpdateDescription()
                )
            );
        }

        PrincipalExpression deletedFilter = posting.getViewE().not()
            .and(liberin.getLatestViewPrincipal());
        send(liberin, new PostingDeletedEvent(posting, deletedFilter));
        send(
            Directions.postingSubscribers(posting.getNodeId(), posting.getId(), deletedFilter),
            PostingDeletedNotificationUtil.build(posting.getId())
        );
        send(
            Directions.searchSubscribers(posting.getNodeId(), deletedFilter),
            SearchContentUpdatedNotificationUtil.buildPostingUpdate(
                SearchContentUpdateType.POSTING_DELETE, universalContext.nodeName(), posting
            )
        );
    }

    private boolean isNotifyWhenRevealed(Posting posting) {
        ExtendedDuration duration = universalContext.getOptions().getDuration("posting.revealed.notification.age");
        if (duration.isAlways()) {
            return true;
        }
        if (duration.isNever()) {
            return false;
        }
        return posting.getCreatedAt().toInstant().plus(duration.getDuration()).isAfter(Instant.now());
    }

    @LiberinMapping
    public void deleted(PostingDeletedLiberin liberin) {
        Posting posting = liberin.getPosting();
        List<Story> stories = storyRepository.findByEntryId(posting.getNodeId(), posting.getId());

        if (posting.isOriginal()) {
            notifyMentionsAndReferences(posting, null, liberin.getLatestRevision(), posting.getViewE());
        } else {
            var timelineStories = stories.stream()
                .filter(t -> Feed.TIMELINE.equals(t.getFeedName()) && t.getStoryType().equals(StoryType.POSTING_ADDED))
                .toList();
            for (var story : timelineStories) {
                send(
                    Directions.single(liberin.getNodeId(), posting.getReceiverName(), story.viewPrincipalFilter()),
                    PostingUnsharedNotificationUtil.build(
                        SharingType.REPUBLISH,
                        posting.getReceiverEntryId(),
                        null,
                        story.getFeedName(),
                        story.getId(),
                        story.getMoment(),
                        posting.getId()
                    )
                );
            }
        }

        send(liberin, new PostingDeletedEvent(posting, posting.getViewE()));
        send(
            Directions.postingSubscribers(posting.getNodeId(), posting.getId(), posting.getViewE()),
            PostingDeletedNotificationUtil.build(posting.getId())
        );
        send(
            Directions.searchSubscribers(posting.getNodeId(), posting.getViewE()),
            SearchContentUpdatedNotificationUtil.buildPostingUpdate(
                SearchContentUpdateType.POSTING_DELETE, universalContext.nodeName(), posting
            )
        );
    }

    @LiberinMapping
    public void restored(PostingRestoredLiberin liberin) {
        Posting posting = liberin.getPosting();

        if (posting.isOriginal()) {
            notifyMentionsAndReferences(posting, posting.getCurrentRevision(), null, Principal.PUBLIC);
        }
        send(liberin, new PostingRestoredEvent(posting, posting.getViewE()));
        send(
            Directions.postingSubscribers(posting.getNodeId(), posting.getId(), posting.getViewE()),
            PostingUpdatedNotificationUtil.build(posting.getId())
        );
    }

    private void notifyMentionsAndReferences(
        Posting posting,
        EntryRevision current,
        EntryRevision previous,
        Principal previousView
    ) {
        if (current != null && current.getSignature() == null) {
            return;
        }
        notifyMentioned(posting, current, posting.getViewE(), previous, previousView, posting.getOwnerName());
        notifyReferences(posting, current, posting.getViewE(), previous, previousView);
    }

    private void notifyMentioned(
        Posting posting,
        EntryRevision current,
        Principal currentView,
        EntryRevision latest,
        Principal latestView,
        String ownerName
    ) {
        Set<String> names = mentions(current);
        Set<String> latestNames = mentions(latest);
        updateMentionFavors(posting, names, latestNames);
        Set<String> currentMentions = mentionedRecipients(names, ownerName, currentView);
        Set<String> latestMentions = mentionedRecipients(latestNames, ownerName, latestView);
        if (!currentMentions.isEmpty()) {
            PostingInfo postingInfo = PostingInfoUtil.buildForNotification(
                posting,
                universalContext.getOptions(),
                directServeOperations
            );
            sheriffUserListOperations.fillSheriffListMarks(postingInfo);
            currentMentions.stream()
                .filter(m -> !latestMentions.contains(m))
                .map(m -> Directions.single(posting.getNodeId(), m))
                .forEach(d ->
                    send(d, MentionPostingAddedNotificationUtil.build(
                        posting.getId(),
                        posting.getOwnerName(),
                        posting.getOwnerFullName(),
                        posting.getOwnerSourceUri(),
                        posting.getOwnerGender(),
                        postingInfo.getOwnerAvatar(),
                        postingInfo.getHeading(),
                        postingInfo.getSheriffs(),
                        postingInfo.getSheriffMarks()
                    ))
                );
        }
        latestMentions.stream()
            .filter(m -> !currentMentions.contains(m))
            .map(m -> Directions.single(posting.getNodeId(), m))
            .forEach(d -> send(d, MentionPostingDeletedNotificationUtil.build(posting.getId())));
    }

    private void notifyReferences(
        Posting posting,
        EntryRevision current,
        Principal currentView,
        EntryRevision latest,
        Principal latestView
    ) {
        Set<Reference> links = references(posting.getNodeId(), current);
        Set<Reference> latestLinks = references(posting.getNodeId(), latest);
        if (links.isEmpty() && latestLinks.isEmpty()) {
            return;
        }

        String owner = NodeName.expand(posting.getOwnerName());
        Predicate<Reference> otherOwner = ref -> !Objects.equals(owner, ref.nodeName());
        Set<Reference> currentReferences = links.stream().filter(otherOwner).collect(Collectors.toSet());
        Set<Reference> latestReferences = latestLinks.stream().filter(otherOwner).collect(Collectors.toSet());
        updateReferenceFavors(posting.getNodeId(), currentReferences, latestReferences);

        Set<Reference> currentVisible = visible(currentReferences, currentView);
        Set<Reference> latestVisible = visible(latestReferences, latestView);
        List<Reference> addedReferences = currentVisible.stream()
            .filter(reference -> !latestVisible.contains(reference))
            .toList();
        if (!addedReferences.isEmpty()) {
            PostingInfo postingInfo = PostingInfoUtil.buildForNotification(
                posting,
                universalContext.getOptions(),
                directServeOperations
            );
            sheriffUserListOperations.fillSheriffListMarks(postingInfo);
            addedReferences.forEach(reference -> send(
                Directions.single(posting.getNodeId(), reference.nodeName()),
                PostingSharedNotificationUtil.buildReference(
                    reference.postingId(),
                    posting.getId(),
                    posting.getOwnerName(),
                    posting.getOwnerFullName(),
                    posting.getOwnerSourceUri(),
                    posting.getOwnerGender(),
                    current.getHeading(),
                    postingInfo.getSheriffs(),
                    postingInfo.getSheriffMarks()
                )
            ));
        }
        latestVisible.stream()
            .filter(reference -> !currentVisible.contains(reference))
            .forEach(reference -> send(
                Directions.single(posting.getNodeId(), reference.nodeName()),
                PostingUnsharedNotificationUtil.build(
                    SharingType.REFER,
                    reference.postingId(),
                    null,
                    null,
                    null,
                    null,
                    posting.getId()
                )
            ));
    }

    private Set<Reference> references(UUID nodeId, EntryRevision revision) {
        if (revision == null) {
            return Set.of();
        }
        return linkedEntryRepository.findByRevisionId(nodeId, revision.getId()).stream()
            .map(link -> new Reference(link.getReceiverNodeName(), link.getReceiverEntryId()))
            .collect(Collectors.toSet());
    }

    private Set<Reference> visible(Set<Reference> references, Principal view) {
        return references.stream()
            .filter(reference -> canView(reference.nodeName(), view))
            .collect(Collectors.toSet());
    }

    @LiberinMapping
    public void commentTotalsUpdated(PostingCommentTotalsUpdatedLiberin liberin) {
        Posting posting = liberin.getPosting();

        Principal viewComments = posting.isOriginal()
            ? posting.getViewCommentsE()
            : posting.getReceiverViewCommentsE();
        PrincipalFilter viewFilter = posting.getViewE().a()
            .and(viewComments);
        send(liberin, new PostingCommentsChangedEvent(posting, viewFilter));
        send(
            Directions.postingSubscribers(posting.getNodeId(), posting.getId(), viewFilter),
            PostingCommentsUpdatedNotificationUtil.build(posting.getId(), liberin.getTotal())
        );
    }

    @LiberinMapping
    public void viewed(PostingViewedLiberin liberin) {
        viewCounts.put(new ViewedPosting(liberin.getNodeId(), liberin.getPostingId()), liberin.getViewCount());
    }

    @LiberinMapping
    public void headingUpdated(PostingHeadingUpdatedLiberin liberin) {
        Posting posting = postingRepository.findByNodeIdAndId(liberin.getNodeId(), liberin.getPostingId()).orElse(null);
        if (posting == null) {
            return;
        }
        send(liberin, new PostingUpdatedEvent(posting, posting.getViewE()));
        send(
            Directions.postingSubscribers(posting.getNodeId(), posting.getId(), posting.getViewE()),
            PostingHeadingUpdatedNotificationUtil.build(
                posting.getId(), liberin.getRevisionId(), liberin.getHeading(), liberin.getDescription()
            )
        );
        if (posting.isOriginal() && posting.getCurrentRevision().getId().equals(liberin.getRevisionId())) {
            send(
                Directions.searchSubscribers(liberin.getNodeId(), posting.getViewE()),
                SearchContentUpdatedNotificationUtil.buildPostingHeadingUpdate(posting.getId(), liberin.getHeading())
            );
        }
    }

    @LiberinMapping
    public void mediaTextUpdated(PostingMediaTextUpdatedLiberin liberin) {
        Posting posting = postingRepository.findByNodeIdAndId(liberin.getNodeId(), liberin.getPostingId()).orElse(null);
        if (posting == null) {
            return;
        }
        send(liberin, new PostingUpdatedEvent(posting, posting.getViewE()));
        send(
            Directions.postingSubscribers(posting.getNodeId(), posting.getId(), posting.getViewE()),
            PostingMediaTextUpdatedNotificationUtil.build(
                posting.getId(), liberin.getMediaId(), liberin.getMediaNodeName(), liberin.getTitle(),
                liberin.getTextContent()
            )
        );
        if (posting.isOriginal()) {
            send(
                Directions.searchSubscribers(liberin.getNodeId(), posting.getViewE()),
                SearchContentUpdatedNotificationUtil.buildPostingMediaTextUpdate(
                    posting.getId(), liberin.getMediaId(), liberin.getMediaNodeName(), liberin.getTitle(),
                    liberin.getTextContent()
                )
            );
        }
    }

    @LiberinMapping
    public void recommendationUpdated(PostingRecommendationUpdatedLiberin liberin) {
        postingRepository.findByNodeIdAndId(liberin.getNodeId(), liberin.getPostingId())
            .ifPresent(posting -> send(liberin, new PostingRecommendationUpdatedEvent(posting)));
    }

    @LiberinMapping
    public void publishedAtUpdated(PostingPublishedAtUpdatedLiberin liberin) {
        Posting posting = postingRepository.findByNodeIdAndId(liberin.getNodeId(), liberin.getPostingId()).orElse(null);
        if (posting == null) {
            return;
        }

        send(liberin, new PostingUpdatedEvent(posting, posting.getViewE()));
        send(
            Directions.postingSubscribers(posting.getNodeId(), posting.getId(), posting.getViewE()),
            PostingUpdatedNotificationUtil.build(posting.getId())
        );
    }

    @Scheduled(fixedDelayString = "PT1M")
    public void sendVisited() {
        try (var ignored = requestCounter.allot()) {
            for (var entry : viewCounts.entrySet()) {
                if (viewCounts.remove(entry.getKey(), entry.getValue())) {
                    eventManager.send(
                        entry.getKey().nodeId(),
                        null,
                        new PostingViewedEvent(entry.getKey().postingId(), entry.getValue())
                    );
                }
            }
        }
    }

}
