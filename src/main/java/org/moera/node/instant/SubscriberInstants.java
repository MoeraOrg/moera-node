package org.moera.node.instant;

import java.util.UUID;
import jakarta.inject.Inject;

import org.moera.lib.node.types.StoryType;
import org.moera.lib.node.types.SubscriptionType;
import org.moera.node.data.Feed;
import org.moera.node.data.Story;
import org.moera.node.data.StoryRepository;
import org.moera.node.data.Subscriber;
import org.moera.node.model.StorySummaryData;
import org.moera.node.model.StorySummaryNode;
import org.springframework.stereotype.Component;

@Component
public class SubscriberInstants extends InstantsCreator {

    @Inject
    private StoryRepository storyRepository;

    public void added(Subscriber subscriber) {
        if (subscriber.getSubscriptionType() != SubscriptionType.FEED) {
            return;
        }

        createStory(subscriber, StoryType.SUBSCRIBER_ADDED);
    }

    public void deleted(Subscriber subscriber) {
        if (subscriber.getSubscriptionType() != SubscriptionType.FEED) {
            return;
        }

        createStory(subscriber, StoryType.SUBSCRIBER_DELETED);
    }

    private void createStory(Subscriber subscriber, StoryType storyType) {
        if (isBlocked(storyType, null, null, null, subscriber.getRemoteNodeName())) {
            return;
        }

        Story story = new Story(UUID.randomUUID(), nodeId(), storyType);
        story.setFeedName(Feed.INSTANT);
        story.setRemoteNodeName(subscriber.getRemoteNodeName());
        story.setRemoteFullName(subscriber.getContact().getRemoteFullName());
        story.setRemoteAvatarMediaFile(subscriber.getContact().getRemoteAvatarMediaFile());
        story.setRemoteAvatarShape(subscriber.getContact().getRemoteAvatarShape());
        story.setSummaryData(buildSummary(subscriber));
        updateMoment(story);
        story = storyRepository.saveAndFlush(story);
        storyAdded(story);
    }

    private static StorySummaryData buildSummary(Subscriber subscriber) {
        StorySummaryData summaryData = new StorySummaryData();
        summaryData.setNode(new StorySummaryNode(subscriber.getContact()));
        summaryData.setFeedName(subscriber.getFeedName());
        return summaryData;
    }

}
