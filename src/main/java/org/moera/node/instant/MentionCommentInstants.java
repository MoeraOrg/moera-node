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
public class MentionCommentInstants {

    @Inject
    private RequestContext requestContext;

    @Inject
    private StoryRepository storyRepository;

    @Inject
    private StoryOperations storyOperations;

    @Inject
    private InstantOperations instantOperations;

    public void added(String remoteNodeName, String remotePostingId, String remotePostingHeading,
                      String remoteOwnerName, String remoteCommentId, String remoteCommentHeading) {
        Story story = findStory(remoteNodeName, remotePostingId, remoteCommentId);
        if (story != null) {
            return;
        }
        story = new Story(UUID.randomUUID(), requestContext.nodeId(), StoryType.MENTION_COMMENT);
        story.setFeedName(Feed.INSTANT);
        story.setRemoteNodeName(remoteNodeName);
        story.setRemotePostingId(remotePostingId);
        story.setRemoteOwnerName(remoteOwnerName);
        story.setRemoteCommentId(remoteCommentId);
        story.setSummary(buildSummary(story, remotePostingHeading, remoteCommentHeading));
        storyOperations.updateMoment(story);
        story = storyRepository.saveAndFlush(story);
        requestContext.send(new StoryAddedEvent(story, true));
        instantOperations.feedStatusUpdated();
    }

    public void deleted(String remoteNodeName, String remotePostingId, String remoteCommentId) {
        Story story = findStory(remoteNodeName, remotePostingId, remoteCommentId);
        if (story == null) {
            return;
        }
        storyRepository.delete(story);
        requestContext.send(new StoryDeletedEvent(story, true));
        instantOperations.feedStatusUpdated();
    }

    private Story findStory(String remoteNodeName, String remotePostingId, String remoteCommentId) {
        return storyRepository.findFullByRemotePostingAndCommentId(requestContext.nodeId(), Feed.INSTANT,
                StoryType.MENTION_COMMENT, remoteNodeName, remotePostingId, remoteCommentId)
                .stream().findFirst().orElse(null);
    }

    private static String buildSummary(Story story, String remotePostingHeading, String remoteCommentHeading) {
        String postingOwner = story.getRemoteNodeName().equals(story.getRemoteOwnerName()) ? "their"
                : InstantUtil.formatNodeName(story.getRemoteNodeName());
        return String.format("%s mentioned you in a comment \"%s\" on %s post \"%s\"",
                InstantUtil.formatNodeName(story.getRemoteOwnerName()), Util.he(remoteCommentHeading), postingOwner,
                Util.he(remotePostingHeading));
    }

}
