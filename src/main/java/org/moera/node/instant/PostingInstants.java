package org.moera.node.instant;

import java.util.UUID;
import javax.inject.Inject;

import org.moera.node.data.Feed;
import org.moera.node.data.Story;
import org.moera.node.data.StoryRepository;
import org.moera.node.data.StoryType;
import org.moera.node.model.PostingInfo;
import org.moera.node.model.event.StoryAddedEvent;
import org.moera.node.util.Util;
import org.springframework.stereotype.Component;

@Component
public class PostingInstants extends InstantsCreator {

    @Inject
    private StoryRepository storyRepository;

    public void subscribingToCommentsFailed(String postingId, PostingInfo postingInfo) {
        String postingOwnerName = postingInfo != null ? postingInfo.getOwnerName() : "";
        String postingOwnerFullName = postingInfo != null ? postingInfo.getOwnerFullName() : null;
        String postingHeading = postingInfo != null ? postingInfo.getHeading() : "";

        Story story = new Story(UUID.randomUUID(), nodeId(), StoryType.POSTING_TASK_FAILED);
        story.setFeedName(Feed.INSTANT);
        story.setRemoteNodeName(postingOwnerName);
        story.setRemotePostingId(postingId);
        story.setSummary(
                buildSubscribingToCommentsFailedSummary(postingOwnerName, postingOwnerFullName, postingHeading));
        story.setPublishedAt(Util.now());
        updateMoment(story);
        story = storyRepository.save(story);
        send(new StoryAddedEvent(story, true));
        webPush(story);
        feedStatusUpdated();
    }

    private static String buildSubscribingToCommentsFailedSummary(String nodeName, String fullName,
                                                                  String postingHeading) {
        return String.format("Failed to subscribe to comments to %s post \"%s\"",
                formatNodeName(nodeName, fullName), Util.he(postingHeading));
    }

}
