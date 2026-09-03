package org.moera.node.instant;

import java.util.UUID;
import jakarta.inject.Inject;

import org.moera.lib.node.types.AvatarImage;
import org.moera.lib.node.types.PostingInfo;
import org.moera.lib.node.types.StorySummaryData;
import org.moera.lib.node.types.StoryType;
import org.moera.lib.node.types.WhoAmI;
import org.moera.node.data.Feed;
import org.moera.node.data.Story;
import org.moera.node.data.StoryRepository;
import org.moera.node.model.AvatarImageUtil;
import org.moera.node.model.StorySummaryEntryUtil;
import org.moera.node.model.StorySummaryNodeUtil;
import org.moera.node.util.Util;
import org.springframework.stereotype.Component;

@Component
public class PostingInstants extends InstantsCreator {

    @Inject
    private StoryRepository storyRepository;

    public void subscribingToCommentsFailed(String nodeName, String postingId, PostingInfo postingInfo) {
        if (isBlocked(StoryType.POSTING_SUBSCRIBE_TASK_FAILED, null, nodeName, postingId)) {
            return;
        }

        String postingOwnerName = postingInfo != null ? postingInfo.getOwnerName() : "";
        String postingOwnerFullName = postingInfo != null ? postingInfo.getOwnerFullName() : null;
        String postingOwnerSourceUri = postingInfo != null ? postingInfo.getOwnerSourceUri() : null;
        String postingOwnerGender = postingInfo != null ? postingInfo.getOwnerGender() : null;
        AvatarImage postingOwnerAvatar = postingInfo != null ? postingInfo.getOwnerAvatar() : null;
        String postingHeading = postingInfo != null ? postingInfo.getHeading() : "";

        Story story = new Story(UUID.randomUUID(), nodeId(), StoryType.POSTING_SUBSCRIBE_TASK_FAILED);
        story.setFeedName(Feed.INSTANT);
        story.setRemoteNodeName(nodeName);
        story.setRemotePostingNodeName(postingOwnerName);
        story.setRemotePostingFullName(postingOwnerFullName);
        story.setRemotePostingSourceUri(postingOwnerSourceUri);
        if (postingOwnerAvatar != null) {
            story.setRemotePostingAvatarMediaFile(AvatarImageUtil.getMediaFile(postingOwnerAvatar));
            story.setRemotePostingAvatarShape(postingOwnerAvatar.getShape());
        }
        story.setRemotePostingId(postingId);
        story.setSummaryData(buildSubscribingToCommentsFailedSummary(
                postingOwnerName, postingOwnerFullName, postingOwnerSourceUri, postingOwnerGender, postingHeading));
        story.setPublishedAt(Util.now());
        updateMoment(story);
        story = storyRepository.save(story);
        storyAdded(story);
    }

    private static StorySummaryData buildSubscribingToCommentsFailedSummary(
            String ownerName, String ownerFullName, String ownerSourceUri, String ownerGender, String postingHeading
    ) {
        StorySummaryData summaryData = new StorySummaryData();
        summaryData.setPosting(
            StorySummaryEntryUtil.build(ownerName, ownerFullName, ownerSourceUri, ownerGender, postingHeading)
        );
        return summaryData;
    }

    public void updated(
        String nodeName,
        String ownerName,
        String ownerFullName, String ownerSourceUri,
        String ownerGender,
        AvatarImage ownerAvatar,
        String id,
        String heading,
        String description
    ) {
        if (isBlocked(StoryType.POSTING_UPDATED, null, nodeName, id, ownerName)) {
            return;
        }

        Story story = new Story(UUID.randomUUID(), nodeId(), StoryType.POSTING_UPDATED);
        story.setFeedName(Feed.INSTANT);
        story.setRemoteNodeName(nodeName);
        story.setRemotePostingNodeName(ownerName);
        story.setRemotePostingFullName(ownerFullName);
        story.setRemotePostingSourceUri(ownerSourceUri);
        if (ownerAvatar != null) {
            story.setRemotePostingAvatarMediaFile(AvatarImageUtil.getMediaFile(ownerAvatar));
            story.setRemotePostingAvatarShape(ownerAvatar.getShape());
        }
        story.setRemotePostingId(id);
        story.setSummaryData(buildPostingUpdatedSummary(ownerName, ownerFullName, ownerSourceUri, ownerGender, heading,
            description));
        story.setPublishedAt(Util.now());
        updateMoment(story);
        story = storyRepository.save(story);
        storyAdded(story);
    }

    private static StorySummaryData buildPostingUpdatedSummary(
        String ownerName, String ownerFullName, String ownerSourceUri, String ownerGender, String heading,
        String description
    ) {
        StorySummaryData summaryData = new StorySummaryData();
        summaryData.setPosting(
            StorySummaryEntryUtil.build(ownerName, ownerFullName, ownerSourceUri, ownerGender, heading)
        );
        summaryData.setDescription(description);
        return summaryData;
    }

    public void remoteAddingFailed(WhoAmI nodeInfo) {
        String remoteNodeName = nodeInfo != null ? nodeInfo.getNodeName() : "";
        String remoteFullName = nodeInfo != null ? nodeInfo.getFullName() : null;
        String remoteSourceUri = nodeInfo != null ? nodeInfo.getSourceUri() : null;
        String remoteGender = nodeInfo != null ? nodeInfo.getGender() : null;
        AvatarImage remoteAvatar = nodeInfo != null ? nodeInfo.getAvatar() : null;

        if (isBlocked(StoryType.POSTING_POST_TASK_FAILED, null, remoteNodeName)) {
            return;
        }

        Story story = new Story(UUID.randomUUID(), nodeId(), StoryType.POSTING_POST_TASK_FAILED);
        story.setFeedName(Feed.INSTANT);
        story.setRemoteNodeName(remoteNodeName);
        story.setRemoteFullName(remoteFullName);
        story.setRemoteSourceUri(remoteSourceUri);
        if (remoteAvatar != null) {
            story.setRemoteAvatarMediaFile(AvatarImageUtil.getMediaFile(remoteAvatar));
            story.setRemoteAvatarShape(remoteAvatar.getShape());
        }
        story.setSummaryData(buildRemoteAddingFailedSummary(
            remoteNodeName, remoteFullName, remoteSourceUri, remoteGender
        ));
        story.setPublishedAt(Util.now());
        updateMoment(story);
        story = storyRepository.save(story);
        storyAdded(story);
    }

    private static StorySummaryData buildRemoteAddingFailedSummary(String nodeName, String fullName, String sourceUri,
        String gender) {
        StorySummaryData summaryData = new StorySummaryData();
        summaryData.setNode(StorySummaryNodeUtil.build(nodeName, fullName, sourceUri, gender));
        return summaryData;
    }

    public void remoteUpdateFailed(WhoAmI nodeInfo, String postingId, PostingInfo postingInfo) {
        String remoteNodeName = nodeInfo != null ? nodeInfo.getNodeName() : "";
        String remoteFullName = nodeInfo != null ? nodeInfo.getFullName() : null;
        String remoteSourceUri = nodeInfo != null ? nodeInfo.getSourceUri() : null;
        String remoteGender = nodeInfo != null ? nodeInfo.getGender() : null;
        AvatarImage remoteAvatar = nodeInfo != null ? nodeInfo.getAvatar() : null;
        String postingHeading = postingInfo != null ? postingInfo.getHeading() : "";

        if (isBlocked(StoryType.POSTING_UPDATE_TASK_FAILED, null, remoteNodeName, postingId)) {
            return;
        }

        Story story = new Story(UUID.randomUUID(), nodeId(), StoryType.POSTING_UPDATE_TASK_FAILED);
        story.setFeedName(Feed.INSTANT);
        story.setRemoteNodeName(remoteNodeName);
        story.setRemoteFullName(remoteFullName);
        story.setRemoteSourceUri(remoteSourceUri);
        if (remoteAvatar != null) {
            story.setRemoteAvatarMediaFile(AvatarImageUtil.getMediaFile(remoteAvatar));
            story.setRemoteAvatarShape(remoteAvatar.getShape());
        }
        story.setRemotePostingId(postingId);
        story.setSummaryData(buildRemoteUpdateFailedSummary(
                remoteNodeName, remoteFullName, remoteSourceUri, remoteGender, postingHeading));
        story.setPublishedAt(Util.now());
        updateMoment(story);
        story = storyRepository.save(story);
        storyAdded(story);
    }

    private static StorySummaryData buildRemoteUpdateFailedSummary(
        String nodeName, String fullName, String sourceUri, String gender, String postingHeading
    ) {
        StorySummaryData summaryData = new StorySummaryData();
        summaryData.setNode(StorySummaryNodeUtil.build(nodeName, fullName, sourceUri, gender));
        summaryData.setPosting(StorySummaryEntryUtil.build(null, null,
            null, null, postingHeading));
        return summaryData;
    }

}
