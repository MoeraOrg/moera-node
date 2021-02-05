package org.moera.node.instant;

import java.util.UUID;
import javax.inject.Inject;

import org.moera.node.data.Feed;
import org.moera.node.data.Story;
import org.moera.node.data.StoryRepository;
import org.moera.node.data.StoryType;
import org.moera.node.model.event.StoryAddedEvent;
import org.moera.node.model.event.StoryDeletedEvent;
import org.moera.node.operations.StoryOperations;
import org.moera.node.util.Util;
import org.springframework.stereotype.Component;

@Component
public class MentionPostingInstants extends InstantsCreator {

    @Inject
    private StoryRepository storyRepository;

    @Inject
    private StoryOperations storyOperations;

    public void added(String remoteNodeName, String remoteFullName, String remotePostingId,
                      String remotePostingHeading) {
        Story story = findStory(remoteNodeName, remotePostingId);
        if (story != null) {
            return;
        }
        story = new Story(UUID.randomUUID(), nodeId(), StoryType.MENTION_POSTING);
        story.setFeedName(Feed.INSTANT);
        story.setRemoteNodeName(remoteNodeName);
        story.setRemoteFullName(remoteFullName);
        story.setRemotePostingId(remotePostingId);
        story.setSummary(buildSummary(story, remotePostingHeading));
        storyOperations.updateMoment(story);
        story = storyRepository.saveAndFlush(story);
        send(new StoryAddedEvent(story, true));
        webPush(story);
        feedStatusUpdated();
    }

    public void deleted(String remoteNodeName, String remotePostingId) {
        Story story = findStory(remoteNodeName, remotePostingId);
        if (story == null) {
            return;
        }
        storyRepository.delete(story);
        send(new StoryDeletedEvent(story, true));
        webPushDeleted(story.getId());
        feedStatusUpdated();
    }

    private Story findStory(String remoteNodeName, String remotePostingId) {
        return storyRepository.findByRemotePostingId(nodeId(), Feed.INSTANT, StoryType.MENTION_POSTING,
                remoteNodeName, remotePostingId).stream().findFirst().orElse(null);
    }

    private static String buildSummary(Story story, String remotePostingHeading) {
        return String.format("%s mentioned you in a post \"%s\"",
                formatNodeName(story.getRemoteNodeName(), story.getRemoteFullName()), Util.he(remotePostingHeading));
    }

}
