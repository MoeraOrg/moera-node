package org.moera.node.rest;

import java.sql.Timestamp;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import javax.inject.Inject;

import org.moera.node.data.Entry;
import org.moera.node.data.Feed;
import org.moera.node.data.Story;
import org.moera.node.data.StoryRepository;
import org.moera.node.data.StoryType;
import org.moera.node.event.model.FeedStatusUpdatedEvent;
import org.moera.node.event.model.StoryAddedEvent;
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
        story.setMoment(momentFinder.find(
                moment -> storyRepository.countMoments(requestContext.nodeId(), story.getFeedName(), moment) == 0,
                !story.isPinned() ? story.getPublishedAt() : PINNED_TIME));
    }

    public void publish(Entry posting, List<StoryAttributes> publications) {
        if (publications == null) {
            return;
        }
        Set<String> feedNames = new HashSet<>();
        for (StoryAttributes publication : publications) {
            Story story = new Story(UUID.randomUUID(), requestContext.nodeId(), StoryType.POSTING_ADDED, posting);
            publication.toStory(story);
            updateMoment(story);
            story = storyRepository.saveAndFlush(story);
            posting.addStory(story);
            if (!Feed.isAdmin(story.getFeedName())) {
                requestContext.send(new StoryAddedEvent(story, false));
            }
            requestContext.send(new StoryAddedEvent(story, true));
            feedNames.add(story.getFeedName());
        }
        feedNames.forEach(
                feedName -> requestContext.send(new FeedStatusUpdatedEvent(feedName, getFeedStatus(feedName))));
    }

    public FeedStatus getFeedStatus(String feedName) {
        int notViewed = storyRepository.countNotViewed(requestContext.nodeId(), feedName);
        int notRead = storyRepository.countNotRead(requestContext.nodeId(), feedName);

        return new FeedStatus(notViewed, notRead);
    }

}
