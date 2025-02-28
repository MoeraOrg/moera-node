package org.moera.node.liberin.receptor;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import jakarta.inject.Inject;

import org.moera.lib.node.types.AvatarImage;
import org.moera.lib.node.types.PostingInfo;
import org.moera.lib.node.types.body.Body;
import org.moera.lib.node.types.principal.AccessCheckers;
import org.moera.lib.node.types.principal.Principal;
import org.moera.lib.node.types.principal.PrincipalExpression;
import org.moera.lib.node.types.principal.PrincipalFilter;
import org.moera.node.data.EntryRevision;
import org.moera.node.data.Posting;
import org.moera.node.data.Story;
import org.moera.node.data.StoryRepository;
import org.moera.node.friends.FriendCache;
import org.moera.node.friends.SubscribedCache;
import org.moera.node.liberin.LiberinMapping;
import org.moera.node.liberin.LiberinReceptor;
import org.moera.node.liberin.LiberinReceptorBase;
import org.moera.node.liberin.model.PostingAddedLiberin;
import org.moera.node.liberin.model.PostingCommentTotalsUpdatedLiberin;
import org.moera.node.liberin.model.PostingDeletedLiberin;
import org.moera.node.liberin.model.PostingRestoredLiberin;
import org.moera.node.liberin.model.PostingUpdatedLiberin;
import org.moera.node.model.AvatarImageUtil;
import org.moera.node.model.PostingInfoUtil;
import org.moera.node.model.event.PostingAddedEvent;
import org.moera.node.model.event.PostingCommentsChangedEvent;
import org.moera.node.model.event.PostingDeletedEvent;
import org.moera.node.model.event.PostingRestoredEvent;
import org.moera.node.model.event.PostingUpdatedEvent;
import org.moera.node.model.notification.MentionPostingAddedNotificationUtil;
import org.moera.node.model.notification.MentionPostingDeletedNotificationUtil;
import org.moera.node.model.notification.PostingCommentsUpdatedNotificationUtil;
import org.moera.node.model.notification.PostingDeletedNotificationUtil;
import org.moera.node.model.notification.PostingImportantUpdateNotificationUtil;
import org.moera.node.model.notification.PostingUpdatedNotificationUtil;
import org.moera.node.model.notification.StoryAddedNotificationUtil;
import org.moera.node.notification.send.Directions;
import org.moera.node.operations.MediaAttachmentsProvider;
import org.moera.node.operations.UserListOperations;
import org.moera.node.text.MentionsExtractor;
import org.moera.node.util.ExtendedDuration;

@LiberinReceptor
public class PostingReceptor extends LiberinReceptorBase {

    @Inject
    private StoryRepository storyRepository;

    @Inject
    private FriendCache friendCache;

    @Inject
    private SubscribedCache subscribedCache;

    @Inject
    private UserListOperations userListOperations;

    @LiberinMapping
    public void added(PostingAddedLiberin liberin) {
        Posting posting = liberin.getPosting();

        if (posting.isOriginal()) {
            notifyMentioned(
                posting,
                posting.getCurrentRevision(),
                posting.getViewE(),
                null,
                Principal.PUBLIC,
                posting.getOwnerName()
            );
        }
        send(liberin, new PostingAddedEvent(posting, posting.getViewE()));
    }

    @LiberinMapping
    public void updated(PostingUpdatedLiberin liberin) {
        Posting posting = liberin.getPosting();

        if (posting.isOriginal()) {
            notifyMentioned(
                posting,
                posting.getCurrentRevision(),
                posting.getViewE(),
                liberin.getLatestRevision(),
                liberin.getLatestViewPrincipal(),
                posting.getOwnerName()
            );
        }

        PrincipalExpression addedFilter = posting.getViewE().a()
            .andNot(liberin.getLatestViewPrincipal());
        send(liberin, new PostingAddedEvent(posting, addedFilter));
        if (isNotifyWhenRevealed(posting)) {
            List<Story> stories = storyRepository.findByEntryId(posting.getNodeId(), posting.getId());
            stories.forEach(story ->
                send(
                    Directions.feedSubscribers(posting.getNodeId(), story.getFeedName(), addedFilter),
                    StoryAddedNotificationUtil.build(story)
                )
            );
        }

        PrincipalExpression updatedFilter = posting.getViewE().a()
            .and(liberin.getLatestViewPrincipal());
        send(liberin, new PostingUpdatedEvent(posting, updatedFilter));
        send(
            Directions.postingSubscribers(posting.getNodeId(), posting.getId(), updatedFilter),
            PostingUpdatedNotificationUtil.build(posting.getId())
        );
        if (posting.getCurrentRevision().isUpdateImportant()) {
            AvatarImage ownerAvatar = AvatarImageUtil.build(
                posting.getOwnerAvatarMediaFile(), posting.getOwnerAvatarShape()
            );
            send(
                Directions.postingCommentsSubscribers(posting.getNodeId(), posting.getId(), updatedFilter),
                PostingImportantUpdateNotificationUtil.build(
                    posting.getOwnerName(),
                    posting.getOwnerFullName(),
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

        if (posting.isOriginal()) {
            notifyMentioned(
                posting,
                null,
                Principal.PUBLIC,
                liberin.getLatestRevision(),
                posting.getViewE(),
                posting.getOwnerName()
            );
        }
        send(liberin, new PostingDeletedEvent(posting, posting.getViewE()));
        send(
            Directions.postingSubscribers(posting.getNodeId(), posting.getId(), posting.getViewE()),
            PostingDeletedNotificationUtil.build(posting.getId())
        );
    }

    @LiberinMapping
    public void restored(PostingRestoredLiberin liberin) {
        Posting posting = liberin.getPosting();

        if (posting.isOriginal()) {
            notifyMentioned(
                posting,
                posting.getCurrentRevision(),
                posting.getViewE(),
                null,
                Principal.PUBLIC,
                posting.getOwnerName()
            );
        }
        send(liberin, new PostingRestoredEvent(posting, posting.getViewE()));
        send(
            Directions.postingSubscribers(posting.getNodeId(), posting.getId(), posting.getViewE()),
            PostingUpdatedNotificationUtil.build(posting.getId())
        );
    }

    private void notifyMentioned(
        Posting posting,
        EntryRevision current,
        Principal currentView,
        EntryRevision latest,
        Principal latestView,
        String ownerName
    ) {
        Set<String> currentMentions = current != null
            ? filterMentions(MentionsExtractor.extract(new Body(current.getBody())), ownerName, currentView)
            : Collections.emptySet();
        Set<String> latestMentions = latest != null && latest.getSignature() != null
            ? filterMentions(MentionsExtractor.extract(new Body(latest.getBody())), ownerName, latestView)
            : Collections.emptySet();
        if (!currentMentions.isEmpty()) {
            PostingInfo postingInfo = PostingInfoUtil.build(
                posting,
                posting.getStories(),
                MediaAttachmentsProvider.NONE,
                AccessCheckers.ADMIN,
                universalContext.getOptions()
            );
            userListOperations.fillSheriffListMarks(postingInfo);
            currentMentions.stream()
                .filter(m -> !latestMentions.contains(m))
                .map(m -> Directions.single(posting.getNodeId(), m))
                .forEach(d ->
                    send(d, MentionPostingAddedNotificationUtil.build(
                        posting.getId(),
                        posting.getOwnerName(),
                        posting.getOwnerFullName(),
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

    private Set<String> filterMentions(Set<String> mentions, String ownerName, Principal view) {
        return mentions.stream()
            .filter(m -> !Objects.equals(ownerName, m))
            .filter(m -> !m.equals(":"))
            .filter(m ->
                view.includes(false, m, () -> subscribedCache.isSubscribed(m), () -> friendCache.getClientGroupIds(m))
            )
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

}
