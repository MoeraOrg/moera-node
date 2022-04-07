package org.moera.node.instant;

import java.util.UUID;
import javax.inject.Inject;

import org.moera.node.data.Feed;
import org.moera.node.data.Story;
import org.moera.node.data.StoryRepository;
import org.moera.node.data.StoryType;
import org.moera.node.model.AvatarImage;
import org.moera.node.model.PostingInfo;
import org.moera.node.model.WhoAmI;
import org.moera.node.util.Util;
import org.springframework.stereotype.Component;
import org.springframework.util.ObjectUtils;

@Component
public class PostingInstants extends InstantsCreator {

    @Inject
    private StoryRepository storyRepository;

    public void subscribingToCommentsFailed(String postingId, PostingInfo postingInfo) {
        String postingOwnerName = postingInfo != null ? postingInfo.getOwnerName() : "";
        String postingOwnerFullName = postingInfo != null ? postingInfo.getOwnerFullName() : null;
        AvatarImage postingOwnerAvatar = postingInfo != null ? postingInfo.getOwnerAvatar() : null;
        String postingHeading = postingInfo != null ? postingInfo.getHeading() : "";

        Story story = new Story(UUID.randomUUID(), nodeId(), StoryType.POSTING_SUBSCRIBE_TASK_FAILED);
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
        storyAdded(story);
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
        storyAdded(story);
    }

    private static String buildPostingUpdatedSummary(String nodeName, String fullName, String postingHeading,
                                                     String description) {
        String summary = String.format("%s updated their post \"%s\"", formatNodeName(nodeName, fullName),
                Util.he(postingHeading));
        return ObjectUtils.isEmpty(description) ? summary : summary + ": " + Util.he(description);
    }

    public void remoteAddingFailed(WhoAmI nodeInfo) {
        String remoteNodeName = nodeInfo != null ? nodeInfo.getNodeName() : "";
        String remoteFullName = nodeInfo != null ? nodeInfo.getFullName() : null;
        AvatarImage remoteAvatar = nodeInfo != null ? nodeInfo.getAvatar() : null;

        Story story = new Story(UUID.randomUUID(), nodeId(), StoryType.POSTING_POST_TASK_FAILED);
        story.setFeedName(Feed.INSTANT);
        story.setRemoteNodeName(remoteNodeName);
        story.setRemoteFullName(remoteFullName);
        if (remoteAvatar != null) {
            story.setRemoteAvatarMediaFile(remoteAvatar.getMediaFile());
            story.setRemoteAvatarShape(remoteAvatar.getShape());
        }
        story.setSummary(buildRemoteAddingFailedSummary(remoteNodeName, remoteFullName));
        story.setPublishedAt(Util.now());
        updateMoment(story);
        story = storyRepository.save(story);
        storyAdded(story);
    }

    private static String buildRemoteAddingFailedSummary(String nodeName, String fullName) {
        return String.format("Failed to add your post to %s node", formatNodeName(nodeName, fullName));
    }

    public void remoteUpdateFailed(WhoAmI nodeInfo, String postingId, PostingInfo postingInfo) {
        String remoteNodeName = nodeInfo != null ? nodeInfo.getNodeName() : "";
        String remoteFullName = nodeInfo != null ? nodeInfo.getFullName() : null;
        AvatarImage remoteAvatar = nodeInfo != null ? nodeInfo.getAvatar() : null;
        String postingHeading = postingInfo != null ? postingInfo.getHeading() : "";

        Story story = new Story(UUID.randomUUID(), nodeId(), StoryType.POSTING_UPDATE_TASK_FAILED);
        story.setFeedName(Feed.INSTANT);
        story.setRemoteNodeName(remoteNodeName);
        story.setRemoteFullName(remoteFullName);
        if (remoteAvatar != null) {
            story.setRemoteAvatarMediaFile(remoteAvatar.getMediaFile());
            story.setRemoteAvatarShape(remoteAvatar.getShape());
        }
        story.setRemotePostingId(postingId);
        story.setSummary(buildRemoteUpdateFailedSummary(remoteNodeName, remoteFullName, postingHeading));
        story.setPublishedAt(Util.now());
        updateMoment(story);
        story = storyRepository.save(story);
        storyAdded(story);
    }

    private static String buildRemoteUpdateFailedSummary(String nodeName, String fullName, String postingHeading) {
        return String.format("Failed to sign your post \"%s\" on %s node",
                Util.he(postingHeading), formatNodeName(nodeName, fullName));
    }

}
