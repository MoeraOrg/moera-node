package org.moera.node.instant;

import java.util.UUID;
import javax.inject.Inject;

import org.moera.node.data.Feed;
import org.moera.node.data.Story;
import org.moera.node.data.StoryRepository;
import org.moera.node.data.StoryType;
import org.moera.node.model.AvatarImage;
import org.moera.node.model.PostingInfo;
import org.moera.node.model.StorySummaryData;
import org.moera.node.model.StorySummaryEntry;
import org.moera.node.model.StorySummaryNode;
import org.moera.node.model.WhoAmI;
import org.moera.node.util.Util;
import org.springframework.stereotype.Component;

@Component
public class PostingInstants extends InstantsCreator {

    @Inject
    private StoryRepository storyRepository;

    public void subscribingToCommentsFailed(String nodeName, String postingId, PostingInfo postingInfo) {
        String postingOwnerName = postingInfo != null ? postingInfo.getOwnerName() : "";
        String postingOwnerFullName = postingInfo != null ? postingInfo.getOwnerFullName() : null;
        AvatarImage postingOwnerAvatar = postingInfo != null ? postingInfo.getOwnerAvatar() : null;
        String postingHeading = postingInfo != null ? postingInfo.getHeading() : "";

        Story story = new Story(UUID.randomUUID(), nodeId(), StoryType.POSTING_SUBSCRIBE_TASK_FAILED);
        story.setFeedName(Feed.INSTANT);
        story.setRemoteNodeName(nodeName);
        story.setRemotePostingNodeName(postingOwnerName);
        story.setRemotePostingFullName(postingOwnerFullName);
        if (postingOwnerAvatar != null) {
            story.setRemotePostingAvatarMediaFile(postingOwnerAvatar.getMediaFile());
            story.setRemotePostingAvatarShape(postingOwnerAvatar.getShape());
        }
        story.setRemotePostingId(postingId);
        story.setSummaryData(
                buildSubscribingToCommentsFailedSummary(postingOwnerName, postingOwnerFullName, postingHeading));
        story.setPublishedAt(Util.now());
        updateMoment(story);
        story = storyRepository.save(story);
        storyAdded(story);
    }

    private static StorySummaryData buildSubscribingToCommentsFailedSummary(String ownerName, String ownerFullName,
                                                                            String postingHeading) {
        StorySummaryData summaryData = new StorySummaryData();
        summaryData.setPosting(new StorySummaryEntry(ownerName, ownerFullName, postingHeading));
        return summaryData;
    }

    public void updated(String nodeName, String ownerName, String ownerFullName, AvatarImage ownerAvatar, String id,
                        String heading, String description) {
        Story story = new Story(UUID.randomUUID(), nodeId(), StoryType.POSTING_UPDATED);
        story.setFeedName(Feed.INSTANT);
        story.setRemoteNodeName(nodeName);
        story.setRemotePostingNodeName(ownerName);
        story.setRemotePostingFullName(ownerFullName);
        if (ownerAvatar != null) {
            story.setRemotePostingAvatarMediaFile(ownerAvatar.getMediaFile());
            story.setRemotePostingAvatarShape(ownerAvatar.getShape());
        }
        story.setRemotePostingId(id);
        story.setRemoteHeading(heading);
        story.setSummaryData(buildPostingUpdatedSummary(ownerName, ownerFullName, heading, description));
        story.setPublishedAt(Util.now());
        updateMoment(story);
        story = storyRepository.save(story);
        storyAdded(story);
    }

    private static StorySummaryData buildPostingUpdatedSummary(String ownerName, String ownerFullName, String heading,
                                                               String description) {
        StorySummaryData summaryData = new StorySummaryData();
        summaryData.setPosting(new StorySummaryEntry(ownerName, ownerFullName, heading));
        summaryData.setDescription(description);
        return summaryData;
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
        story.setSummaryData(buildRemoteAddingFailedSummary(remoteNodeName, remoteFullName));
        story.setPublishedAt(Util.now());
        updateMoment(story);
        story = storyRepository.save(story);
        storyAdded(story);
    }

    private static StorySummaryData buildRemoteAddingFailedSummary(String nodeName, String fullName) {
        StorySummaryData summaryData = new StorySummaryData();
        summaryData.setNode(new StorySummaryNode(nodeName, fullName));
        return summaryData;
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
        story.setSummaryData(buildRemoteUpdateFailedSummary(remoteNodeName, remoteFullName, postingHeading));
        story.setPublishedAt(Util.now());
        updateMoment(story);
        story = storyRepository.save(story);
        storyAdded(story);
    }

    private static StorySummaryData buildRemoteUpdateFailedSummary(String nodeName, String fullName,
                                                                   String postingHeading) {
        StorySummaryData summaryData = new StorySummaryData();
        summaryData.setNode(new StorySummaryNode(nodeName, fullName));
        summaryData.setPosting(new StorySummaryEntry(null, null, postingHeading));
        return summaryData;
    }

}
