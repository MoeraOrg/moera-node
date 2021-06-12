package org.moera.node.instant;

import java.util.UUID;
import javax.inject.Inject;

import org.moera.node.data.Feed;
import org.moera.node.data.Story;
import org.moera.node.data.StoryRepository;
import org.moera.node.data.StoryType;
import org.moera.node.data.Subscriber;
import org.moera.node.data.SubscriptionType;
import org.moera.node.model.event.StoryAddedEvent;
import org.moera.node.model.event.StoryDeletedEvent;
import org.moera.node.operations.StoryOperations;
import org.springframework.stereotype.Component;

@Component
public class SubscriberInstants extends InstantsCreator {

    @Inject
    private StoryRepository storyRepository;

    @Inject
    private StoryOperations storyOperations;

    public void added(Subscriber subscriber) {
        if (subscriber.getSubscriptionType() != SubscriptionType.FEED) {
            return;
        }

        Story story = findDeletedStory(subscriber.getRemoteNodeName());
        if (story != null && !story.isRead()) {
            storyRepository.delete(story);
            send(new StoryDeletedEvent(story, true));
        }

        story = new Story(UUID.randomUUID(), nodeId(), StoryType.SUBSCRIBER_ADDED);
        story.setFeedName(Feed.INSTANT);
        story.setRemoteNodeName(subscriber.getRemoteNodeName());
        story.setRemoteFullName(subscriber.getRemoteFullName());
        story.setRemoteAvatarMediaFile(subscriber.getRemoteAvatarMediaFile());
        story.setRemoteAvatarShape(subscriber.getRemoteAvatarShape());
        story.setSummary(buildAddedSummary(subscriber));
        storyOperations.updateMoment(story);
        story = storyRepository.saveAndFlush(story);
        send(new StoryAddedEvent(story, true));
        sendPush(story);
        feedStatusUpdated();
    }

    private static String buildAddedSummary(Subscriber subscriber) {
        return String.format("%s subscribed to your %s",
                formatNodeName(subscriber.getRemoteNodeName(), subscriber.getRemoteFullName()),
                Feed.getStandard(subscriber.getFeedName()).getTitle());
    }

    public void deleted(Subscriber subscriber) {
        if (subscriber.getSubscriptionType() != SubscriptionType.FEED) {
            return;
        }

        Story story = findAddedStory(subscriber.getRemoteNodeName());
        if (story != null && !story.isRead()) {
            storyRepository.delete(story);
            send(new StoryDeletedEvent(story, true));
        }

        story = new Story(UUID.randomUUID(), nodeId(), StoryType.SUBSCRIBER_DELETED);
        story.setFeedName(Feed.INSTANT);
        story.setRemoteNodeName(subscriber.getRemoteNodeName());
        story.setRemoteFullName(subscriber.getRemoteFullName());
        story.setRemoteAvatarMediaFile(subscriber.getRemoteAvatarMediaFile());
        story.setRemoteAvatarShape(subscriber.getRemoteAvatarShape());
        story.setSummary(buildDeletedSummary(subscriber));
        storyOperations.updateMoment(story);
        story = storyRepository.saveAndFlush(story);
        send(new StoryAddedEvent(story, true));
        sendPush(story);
        feedStatusUpdated();
    }

    private static String buildDeletedSummary(Subscriber subscriber) {
        return String.format("%s unsubscribed from your %s",
                formatNodeName(subscriber.getRemoteNodeName(), subscriber.getRemoteFullName()),
                Feed.getStandard(subscriber.getFeedName()).getTitle());
    }

    private Story findAddedStory(String remoteNodeName) {
        return storyRepository.findByRemoteNodeName(nodeId(), Feed.INSTANT, StoryType.SUBSCRIBER_ADDED, remoteNodeName)
                .stream().findFirst().orElse(null);
    }

    private Story findDeletedStory(String remoteNodeName) {
        return storyRepository.findByRemoteNodeName(nodeId(), Feed.INSTANT, StoryType.SUBSCRIBER_DELETED, remoteNodeName)
                .stream().findFirst().orElse(null);
    }

}
