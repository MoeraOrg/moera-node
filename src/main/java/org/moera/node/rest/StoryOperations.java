package org.moera.node.rest;

import java.sql.Timestamp;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;
import javax.inject.Inject;

import org.moera.node.data.Entry;
import org.moera.node.data.Feed;
import org.moera.node.data.Story;
import org.moera.node.data.StoryRepository;
import org.moera.node.data.StoryType;
import org.moera.node.model.event.Event;
import org.moera.node.model.event.FeedStatusUpdatedEvent;
import org.moera.node.model.event.StoryAddedEvent;
import org.moera.node.global.RequestContext;
import org.moera.node.model.FeedStatus;
import org.moera.node.model.StoryAttributes;
import org.moera.node.util.MomentFinder;
import org.moera.node.util.Util;
import org.springframework.stereotype.Component;

@Component
public class StoryOperations {

    private static final Timestamp PINNED_TIME = Util.toTimestamp(90000000000000L); // 9E+13

    @Inject
    private RequestContext requestContext;

    @Inject
    private StoryRepository storyRepository;

    private final MomentFinder momentFinder = new MomentFinder();

    public void updateMoment(Story story) {
        updateMoment(story, requestContext.nodeId());
    }

    private void updateMoment(Story story, UUID nodeId) {
        story.setMoment(momentFinder.find(
                moment -> storyRepository.countMoments(nodeId, story.getFeedName(), moment) == 0,
                !story.isPinned() ? story.getPublishedAt() : PINNED_TIME));
    }

    public void publish(Entry posting, List<StoryAttributes> publications) {
        publish(posting, publications, requestContext.nodeId(), requestContext::send);
    }

    public void publish(Entry posting, List<StoryAttributes> publications, UUID nodeId, Consumer<Event> eventSender) {
        if (publications == null) {
            return;
        }
        Set<String> feedNames = new HashSet<>();
        for (StoryAttributes publication : publications) {
            Story story = new Story(UUID.randomUUID(), nodeId, StoryType.POSTING_ADDED, posting);
            publication.toStory(story);
            updateMoment(story, nodeId);
            story = storyRepository.saveAndFlush(story);
            posting.addStory(story);
            if (!Feed.isAdmin(story.getFeedName())) {
                eventSender.accept(new StoryAddedEvent(story, false));
            }
            eventSender.accept(new StoryAddedEvent(story, true));
            feedNames.add(story.getFeedName());
        }
        feedNames.forEach(
                feedName -> eventSender.accept(new FeedStatusUpdatedEvent(feedName, getFeedStatus(feedName, nodeId))));
    }

    public FeedStatus getFeedStatus(String feedName) {
        return getFeedStatus(feedName, requestContext.nodeId());
    }

    private FeedStatus getFeedStatus(String feedName, UUID nodeId) {
        int notViewed = storyRepository.countNotViewed(nodeId, feedName);
        int notRead = storyRepository.countNotRead(nodeId, feedName);

        return new FeedStatus(notViewed, notRead);
    }

}
