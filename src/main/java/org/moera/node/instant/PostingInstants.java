package org.moera.node.instant;

import java.util.UUID;
import javax.inject.Inject;

import org.moera.node.data.Feed;
import org.moera.node.data.Story;
import org.moera.node.data.StoryRepository;
import org.moera.node.data.StoryType;
import org.moera.node.model.AvatarImage;
import org.moera.node.model.PostingInfo;
import org.moera.node.model.event.StoryAddedEvent;
import org.moera.node.util.Util;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class PostingInstants extends InstantsCreator {

    @Inject
    private StoryRepository storyRepository;

    public void subscribingToCommentsFailed(String postingId, PostingInfo postingInfo) {
        String postingOwnerName = postingInfo != null ? postingInfo.getOwnerName() : "";
        String postingOwnerFullName = postingInfo != null ? postingInfo.getOwnerFullName() : null;
        AvatarImage postingOwnerAvatar = postingInfo != null ? postingInfo.getOwnerAvatar() : null;
        String postingHeading = postingInfo != null ? postingInfo.getHeading() : "";

        Story story = new Story(UUID.randomUUID(), nodeId(), StoryType.POSTING_TASK_FAILED);
        story.setFeedName(Feed.INSTANT);
        story.setRemoteNodeName(postingOwnerName);
        story.setRemoteFullName(postingOwnerFullName);
        if (postingOwnerAvatar != null) {
            story.setRemoteAvatarMediaFile(postingOwnerAvatar.getMediaFile());
            story.setRemoteAvatarShape(postingOwnerAvatar.getShape());
        }
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

    public void updated(String remoteNodeName, String remoteFullName, AvatarImage remoteAvatar, String remotePostingId,
                        String remotePostingHeading, String description) {
        Story story = new Story(UUID.randomUUID(), nodeId(), StoryType.POSTING_UPDATED);
        story.setFeedName(Feed.INSTANT);
        story.setRemoteNodeName(remoteNodeName);
        story.setRemoteFullName(remoteFullName);
        if (remoteAvatar != null) {
            story.setRemoteAvatarMediaFile(remoteAvatar.getMediaFile());
            story.setRemoteAvatarShape(remoteAvatar.getShape());
        }
        story.setRemotePostingId(remotePostingId);
        story.setRemoteHeading(remotePostingHeading);
        story.setSummary(
                buildPostingUpdatedSummary(remoteNodeName, remoteFullName, remotePostingHeading, description));
        story.setPublishedAt(Util.now());
        updateMoment(story);
        story = storyRepository.save(story);
        send(new StoryAddedEvent(story, true));
        webPush(story);
        feedStatusUpdated();
    }

    private static String buildPostingUpdatedSummary(String nodeName, String fullName, String postingHeading,
                                                     String description) {
        String summary = String.format("%s updated their post \"%s\"", formatNodeName(nodeName, fullName),
                Util.he(postingHeading));
        return StringUtils.isEmpty(description) ? summary : summary + ": " + Util.he(description);
    }

}
