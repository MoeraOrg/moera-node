package org.moera.node.liberin.receptor;

import java.util.Objects;
import java.util.UUID;
import jakarta.inject.Inject;

import org.moera.lib.node.types.FeedStatus;
import org.moera.node.data.Feed;
import org.moera.node.data.Story;
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
import org.moera.node.model.notification.StoryAddedNotification;
import org.moera.node.notification.send.Directions;
import org.moera.node.operations.StoryOperations;
import org.moera.node.push.PushContentBuilder;

@LiberinReceptor
public class StoryReceptor extends LiberinReceptorBase {

    @Inject
    private StoryOperations storyOperations;

    @LiberinMapping
    public void added(StoryAddedLiberin liberin) {
        Story story = liberin.getStory();

        if (!Feed.isAdmin(story.getFeedName())) {
            send(liberin, new StoryAddedEvent(story, false));
        }
        send(liberin, new StoryAddedEvent(story, true));
        send(Directions.feedSubscribers(liberin.getNodeId(), story.getFeedName(), story.getViewPrincipalFilter()),
                new StoryAddedNotification(story));
        push(story);
        feedStatusUpdated(story.getFeedName());
    }

    @LiberinMapping
    public void updated(StoryUpdatedLiberin liberin) {
        Story story = liberin.getStory();

        if (!Feed.isAdmin(story.getFeedName())) {
            send(liberin, new StoryUpdatedEvent(story, false));
        }
        send(liberin, new StoryUpdatedEvent(story, true));
        if (!story.isViewed()) {
            push(story);
        } else {
            deletePush(story.getFeedName(), story.getId());
        }
        feedStatusUpdated(story.getFeedName());
    }

    @LiberinMapping
    public void deleted(StoryDeletedLiberin liberin) {
        if (!Feed.isAdmin(liberin.getFeedName())) {
            send(liberin,
                    new StoryDeletedEvent(liberin.getId().toString(), liberin.getStoryType(), liberin.getFeedName(),
                            liberin.getMoment(), Objects.toString(liberin.getPostingId(), null), false,
                            liberin.getViewFilter()));
        }
        send(liberin,
                new StoryDeletedEvent(liberin.getId().toString(), liberin.getStoryType(), liberin.getFeedName(),
                        liberin.getMoment(), Objects.toString(liberin.getPostingId(), null), true,
                        liberin.getViewFilter()));
        deletePush(liberin.getFeedName(), liberin.getId());
        feedStatusUpdated(liberin.getFeedName());
    }

    private void push(Story story) {
        if (Objects.equals(story.getFeedName(), Feed.INSTANT)) {
            send(PushContentBuilder.storyAdded(story));
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
