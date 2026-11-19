package org.moera.node.liberin.receptor;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import jakarta.inject.Inject;

import org.moera.lib.node.types.FeedStatus;
import org.moera.lib.node.types.SearchContentUpdateType;
import org.moera.lib.node.types.SharingType;
import org.moera.lib.node.types.StoryType;
import org.moera.lib.node.types.principal.Principal;
import org.moera.lib.node.types.principal.PrincipalFilter;
import org.moera.node.data.FavorType;
import org.moera.node.data.Feed;
import org.moera.node.data.Story;
import org.moera.node.data.StoryRepository;
import org.moera.node.liberin.LiberinMapping;
import org.moera.node.liberin.LiberinReceptor;
import org.moera.node.liberin.LiberinReceptorBase;
import org.moera.node.liberin.model.FeedStatusUpdatedLiberin;
import org.moera.node.liberin.model.StoryAddedLiberin;
import org.moera.node.liberin.model.StoryDeletedLiberin;
import org.moera.node.liberin.model.StoryUpdatedLiberin;
import org.moera.node.model.event.StoryAddedEvent;
import org.moera.node.model.event.StoryDeletedEvent;
import org.moera.node.model.event.StoryUpdatedEvent;
import org.moera.node.model.notification.PostingSharedNotificationUtil;
import org.moera.node.model.notification.PostingSharingUpdatedNotificationUtil;
import org.moera.node.model.notification.PostingUnsharedNotificationUtil;
import org.moera.node.model.notification.SearchContentUpdatedNotificationUtil;
import org.moera.node.model.notification.StoryAddedNotificationUtil;
import org.moera.node.notification.send.Directions;
import org.moera.node.operations.FavorOperations;
import org.moera.node.operations.StoryOperations;
import org.moera.node.push.PushContentBuilder;
import org.moera.node.util.ExtendedDuration;

@LiberinReceptor
public class StoryReceptor extends LiberinReceptorBase {

    @Inject
    private StoryRepository storyRepository;

    @Inject
    private StoryOperations storyOperations;

    @Inject
    private FavorOperations favorOperations;

    @LiberinMapping
    public void added(StoryAddedLiberin liberin) {
        Story story = liberin.getStory();

        if (!Feed.isAdmin(story.getFeedName())) {
            send(liberin, new StoryAddedEvent(story, false, directServeOperations));
            if (story.getStoryType() == StoryType.POSTING_ADDED) {
                send(
                    Directions.searchSubscribers(liberin.getNodeId(), story.viewPrincipalFilter()),
                    SearchContentUpdatedNotificationUtil.buildPostingUpdate(
                        SearchContentUpdateType.POSTING_ADD, universalContext.nodeName(), story
                    )
                );
            }
        }
        send(liberin, new StoryAddedEvent(story, true, directServeOperations));
        if (isNotifyWhenPublished(story)) {
            send(
                Directions.feedSubscribers(liberin.getNodeId(), story.getFeedName(), story.viewPrincipalFilter()),
                StoryAddedNotificationUtil.build(story)
            );
        }
        if (isTimelinePosting(story)) {
            favorOperations.addFavor(liberin.getNodeId(), story.getEntry().getOwnerName(), FavorType.PUBLICATION);
            if (!story.getEntry().isOriginal()) {
                notifyRepublished(liberin.getNodeId(), story, story.viewPrincipalFilter());
            }
        }

        push(story);
        feedStatusUpdated(story.getFeedName());
    }

    private static boolean isTimelinePosting(Story story) {
        return story.getStoryType() == StoryType.POSTING_ADDED
            && Feed.TIMELINE.equals(story.getFeedName())
            && story.getEntry() != null;
    }

    private void notifyRepublished(UUID nodeId, Story story, PrincipalFilter filter) {
        send(
            Directions.single(nodeId, story.getEntry().getReceiverName(), filter),
            PostingSharedNotificationUtil.buildRepublish(
                story.getEntry().getReceiverEntryId(),
                null,
                story.getFeedName(),
                story.getId(),
                story.getMoment(),
                story.getEntry().getId()
            )
        );
    }

    private boolean isNotifyWhenPublished(Story story) {
        ExtendedDuration duration = universalContext.getOptions().getDuration("posting.published.notification.age");
        if (duration.isAlways()) {
            return true;
        }
        if (duration.isNever()) {
            return false;
        }
        return story.getPublishedAt().toInstant().plus(duration.getDuration()).isAfter(Instant.now());
    }

    @LiberinMapping
    public void updated(StoryUpdatedLiberin liberin) {
        Story story = storyRepository.findByNodeIdAndId(liberin.getNodeId(), liberin.getStoryId()).orElse(null);
        if (story == null) {
            return;
        }

        if (!Feed.isAdmin(story.getFeedName())) {
            PrincipalFilter retainedView = story.viewPrincipalFilter();
            StoryUpdatedEvent updatedEvent = new StoryUpdatedEvent(story, false, directServeOperations);
            if (liberin.getPreviousViewFilter() != null) {
                retainedView = retainedView.a().and(liberin.getPreviousViewFilter().a());
                updatedEvent.setFilter(retainedView.a().andNot(Principal.ADMIN));
                visibilityUpdated(liberin, story);
            }
            if (
                liberin.getPreviousMoment() != null
                && !liberin.getPreviousMoment().equals(story.getMoment())
                && isTimelinePosting(story)
                && !story.getEntry().isOriginal()
            ) {
                send(
                    Directions.single(liberin.getNodeId(), story.getEntry().getReceiverName(), retainedView),
                    PostingSharingUpdatedNotificationUtil.build(story)
                );
            }
            send(liberin, updatedEvent);
        }
        send(liberin, new StoryUpdatedEvent(story, true, directServeOperations));
        if (!story.isViewed()) {
            push(story);
        } else {
            deletePush(story.getFeedName(), story.getId());
        }
        feedStatusUpdated(story.getFeedName());
    }

    private void visibilityUpdated(StoryUpdatedLiberin liberin, Story story) {
        PrincipalFilter addedFilter = story.viewPrincipalFilter().a().and(liberin.getPreviousViewFilter().a().not());
        PrincipalFilter deletedFilter = liberin.getPreviousViewFilter().a().and(story.viewPrincipalFilter().a().not());
        StoryAddedEvent addedEvent = new StoryAddedEvent(story, false, directServeOperations);
        addedEvent.setFilter(addedFilter.a().andNot(Principal.ADMIN));
        send(liberin, addedEvent);
        StoryDeletedLiberin deletion = new StoryDeletedLiberin(story);
        long previousMoment = Objects.requireNonNullElse(liberin.getPreviousMoment(), deletion.getMoment());
        send(liberin, new StoryDeletedEvent(
            deletion.getId().toString(),
            deletion.getStoryType(),
            deletion.getFeedName(),
            previousMoment,
            Objects.toString(deletion.getPostingId(), null),
            Objects.toString(deletion.getCommentId(), null),
            false,
            deletedFilter
        ));
        if (isNotifyWhenPublished(story)) {
            send(
                Directions.feedSubscribers(liberin.getNodeId(), story.getFeedName(), addedFilter),
                StoryAddedNotificationUtil.build(story)
            );
        }
        if (story.getStoryType() == StoryType.POSTING_ADDED && story.getEntry() != null) {
            send(
                Directions.searchSubscribers(liberin.getNodeId(), addedFilter),
                SearchContentUpdatedNotificationUtil.buildPostingUpdate(
                    SearchContentUpdateType.POSTING_ADD, universalContext.nodeName(), story
                )
            );
            send(
                Directions.searchSubscribers(liberin.getNodeId(), deletedFilter),
                SearchContentUpdatedNotificationUtil.buildPostingStoryDelete(
                    deletion.getReceiverName() != null ? deletion.getReceiverName() : universalContext.nodeName(),
                    deletion.getReceiverName() != null
                        ? deletion.getReceiverPostingId() : deletion.getPostingId().toString(),
                    story.getFeedName(),
                    story.getId()
                )
            );
            if (Feed.TIMELINE.equals(story.getFeedName()) && !story.getEntry().isOriginal()) {
                notifyRepublished(liberin.getNodeId(), story, addedFilter);
                send(
                    Directions.single(liberin.getNodeId(), story.getEntry().getReceiverName(), deletedFilter),
                    PostingUnsharedNotificationUtil.build(
                        SharingType.REPUBLISH,
                        story.getEntry().getReceiverEntryId(),
                        null,
                        story.getFeedName(),
                        story.getId(),
                        previousMoment,
                        story.getEntry().getId()
                    )
                );
            }
        }
    }

    @LiberinMapping
    public void deleted(StoryDeletedLiberin liberin) {
        if (!Feed.isAdmin(liberin.getFeedName())) {
            if (liberin.getPostingId() != null && liberin.getCommentId() == null && !liberin.isPostingDeleted()) {
                send(
                    Directions.searchSubscribers(liberin.getNodeId(), liberin.getViewFilter()),
                    SearchContentUpdatedNotificationUtil.buildPostingStoryDelete(
                        liberin.getReceiverName() != null ? liberin.getReceiverName() : universalContext.nodeName(),
                        liberin.getReceiverName() != null
                            ? liberin.getReceiverPostingId()
                            : liberin.getPostingId().toString(),
                        liberin.getFeedName(),
                        liberin.getId()
                    )
                );
            }
            send(
                liberin,
                new StoryDeletedEvent(
                    liberin.getId().toString(),
                    liberin.getStoryType(),
                    liberin.getFeedName(),
                    liberin.getMoment(),
                    Objects.toString(liberin.getPostingId(), null),
                    Objects.toString(liberin.getCommentId(), null),
                    false,
                    liberin.getViewFilter()
                )
            );
        }
        send(
            liberin,
            new StoryDeletedEvent(
                liberin.getId().toString(),
                liberin.getStoryType(),
                liberin.getFeedName(),
                liberin.getMoment(),
                Objects.toString(liberin.getPostingId(), null),
                Objects.toString(liberin.getCommentId(), null),
                true,
                liberin.getViewFilter()
            )
        );
        if (
            liberin.getStoryType() == StoryType.POSTING_ADDED
            && Feed.TIMELINE.equals(liberin.getFeedName())
        ) {
            favorOperations.addFavor(liberin.getNodeId(), liberin.getPostingOwnerName(), FavorType.UNPUBLICATION);
            if (liberin.getReceiverName() != null) {
                send(
                    Directions.single(liberin.getNodeId(), liberin.getReceiverName(), liberin.getViewFilter()),
                    PostingUnsharedNotificationUtil.build(
                        SharingType.REPUBLISH,
                        liberin.getReceiverPostingId(),
                        null,
                        liberin.getFeedName(),
                        liberin.getId(),
                        liberin.getMoment(),
                        liberin.getPostingId()
                    )
                );
            }
        }

        deletePush(liberin.getFeedName(), liberin.getId());
        feedStatusUpdated(liberin.getFeedName());
    }

    private void push(Story story) {
        if (Objects.equals(story.getFeedName(), Feed.INSTANT)) {
            send(PushContentBuilder.storyAdded(story, directServeOperations));
        }
    }

    private void deletePush(String feedName, UUID id) {
        if (Objects.equals(feedName, Feed.INSTANT)) {
            send(PushContentBuilder.storyDeleted(id));
        }
    }

    private void feedStatusUpdated(String feedName) {
        FeedStatus feedStatus = storyOperations.getFeedStatus(feedName, true);
        send(new FeedStatusUpdatedLiberin(feedName, feedStatus));
    }

}
