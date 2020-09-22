package org.moera.node.instant;

import java.util.UUID;

import javax.inject.Inject;

import org.moera.node.data.Feed;
import org.moera.node.data.Story;
import org.moera.node.data.StoryRepository;
import org.moera.node.data.StoryType;
import org.moera.node.global.RequestContext;
import org.moera.node.model.event.StoryAddedEvent;
import org.moera.node.model.event.StoryDeletedEvent;
import org.moera.node.operations.StoryOperations;
import org.moera.node.util.Util;
import org.springframework.stereotype.Component;

@Component
public class MentionPostingInstants {

    @Inject
    private RequestContext requestContext;

    @Inject
    private StoryRepository storyRepository;

    @Inject
    private StoryOperations storyOperations;

    @Inject
    private InstantOperations instantOperations;

    public void added(String remoteNodeName, String remotePostingId, String remotePostingHeading) {
        Story story = findStory(remoteNodeName, remotePostingId);
        if (story != null) {
            return;
        }
        story = new Story(UUID.randomUUID(), requestContext.nodeId(), StoryType.MENTION_POSTING);
        story.setFeedName(Feed.INSTANT);
        story.setRemoteNodeName(remoteNodeName);
        story.setRemotePostingId(remotePostingId);
        story.setSummary(buildSummary(story, remotePostingHeading));
        storyOperations.updateMoment(story);
        story = storyRepository.saveAndFlush(story);
        requestContext.send(new StoryAddedEvent(story, true));
        instantOperations.feedStatusUpdated();
    }

    public void deleted(String remoteNodeName, String remotePostingId) {
        Story story = findStory(remoteNodeName, remotePostingId);
        if (story == null) {
            return;
        }
        storyRepository.delete(story);
        requestContext.send(new StoryDeletedEvent(story, true));
        instantOperations.feedStatusUpdated();
    }

    private Story findStory(String remoteNodeName, String remotePostingId) {
        return storyRepository.findByRemotePostingId(requestContext.nodeId(), Feed.INSTANT, StoryType.MENTION_POSTING,
                remoteNodeName, remotePostingId).stream().findFirst().orElse(null);
    }

    private static String buildSummary(Story story, String remotePostingHeading) {
        return String.format("<b>%s</b> mentioned you in a post \"%s\"",
                InstantUtil.formatNodeName(story.getRemoteNodeName()), Util.he(remotePostingHeading));
    }

}
