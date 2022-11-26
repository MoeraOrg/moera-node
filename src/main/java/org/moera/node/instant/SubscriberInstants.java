package org.moera.node.instant;

import java.util.UUID;
import javax.inject.Inject;

import org.moera.node.data.Feed;
import org.moera.node.data.Story;
import org.moera.node.data.StoryRepository;
import org.moera.node.data.StoryType;
import org.moera.node.data.Subscriber;
import org.moera.node.data.SubscriptionType;
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

        Story story = new Story(UUID.randomUUID(), nodeId(), StoryType.SUBSCRIBER_ADDED);
        story.setFeedName(Feed.INSTANT);
        story.setRemoteNodeName(subscriber.getRemoteNodeName());
        story.setRemoteFullName(subscriber.getRemoteFullName());
        story.setRemoteAvatarMediaFile(subscriber.getRemoteAvatarMediaFile());
        story.setRemoteAvatarShape(subscriber.getRemoteAvatarShape());
        story.setSummaryData(buildSummary(subscriber));
        updateMoment(story);
        story = storyRepository.saveAndFlush(story);
        storyAdded(story);
    }

    public void deleted(Subscriber subscriber) {
        if (subscriber.getSubscriptionType() != SubscriptionType.FEED) {
            return;
        }

        Story story = new Story(UUID.randomUUID(), nodeId(), StoryType.SUBSCRIBER_DELETED);
        story.setFeedName(Feed.INSTANT);
        story.setRemoteNodeName(subscriber.getRemoteNodeName());
        story.setRemoteFullName(subscriber.getRemoteFullName());
        story.setRemoteAvatarMediaFile(subscriber.getRemoteAvatarMediaFile());
        story.setRemoteAvatarShape(subscriber.getRemoteAvatarShape());
        story.setSummaryData(buildSummary(subscriber));
        updateMoment(story);
        story = storyRepository.saveAndFlush(story);
        storyAdded(story);
    }

    private static StorySummaryData buildSummary(Subscriber subscriber) {
        StorySummaryData summaryData = new StorySummaryData();
        summaryData.setNode(new StorySummaryNode(
                subscriber.getRemoteNodeName(), subscriber.getRemoteFullName(), subscriber.getRemoteGender()));
        summaryData.setFeedName(subscriber.getFeedName());
        return summaryData;
    }

}
