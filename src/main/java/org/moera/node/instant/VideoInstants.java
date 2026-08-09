package org.moera.node.instant;

import java.util.UUID;
import jakarta.inject.Inject;

import org.moera.lib.node.types.AvatarImage;
import org.moera.lib.node.types.CommentInfo;
import org.moera.lib.node.types.PostingInfo;
import org.moera.lib.node.types.StorySummaryData;
import org.moera.lib.node.types.StoryType;
import org.moera.lib.node.types.WhoAmI;
import org.moera.node.data.Feed;
import org.moera.node.data.MediaFileRepository;
import org.moera.node.data.Story;
import org.moera.node.data.StoryRepository;
import org.moera.node.model.AvatarImageUtil;
import org.moera.node.model.StorySummaryEntryUtil;
import org.moera.node.util.Util;
import org.springframework.stereotype.Component;

@Component
public class VideoInstants extends InstantsCreator {

    @Inject
    private StoryRepository storyRepository;

    @Inject
    private MediaFileRepository mediaFileRepository;

    public void postingPublished(WhoAmI nodeInfo, PostingInfo postingInfo) {
        String remoteNodeName = nodeInfo.getNodeName();
        String remotePostingId = postingInfo.getId();
        if (isBlocked(StoryType.VIDEO_POSTING_PUBLISHED, null, remoteNodeName, remotePostingId)) {
            return;
        }

        Story story = new Story(UUID.randomUUID(), nodeId(), StoryType.VIDEO_POSTING_PUBLISHED);
        story.setFeedName(Feed.INSTANT);
        story.setRemoteNodeName(remoteNodeName);
        story.setRemoteFullName(nodeInfo.getFullName());
        AvatarImage avatar = nodeInfo.getAvatar();
        if (avatar != null && avatar.getMediaId() != null) {
            story.setRemoteAvatarMediaFile(mediaFileRepository.findById(avatar.getMediaId()).orElse(null));
            story.setRemoteAvatarShape(avatar.getShape());
        }
        story.setRemotePostingId(remotePostingId);
        story.setSummaryData(buildPostSummary(postingInfo.getHeading()));
        story.setPublishedAt(Util.now());
        updateMoment(story);
        story = storyRepository.save(story);
        storyAdded(story);
    }

    public void commentPublished(String remoteNodeName, PostingInfo postingInfo, CommentInfo commentInfo) {
        String remotePostingId = postingInfo.getId();
        String remoteCommentId = commentInfo.getId();
        if (isBlocked(StoryType.VIDEO_COMMENT_PUBLISHED, null, remoteNodeName, remotePostingId)) {
            return;
        }

        Story story = new Story(UUID.randomUUID(), nodeId(), StoryType.VIDEO_COMMENT_PUBLISHED);
        story.setFeedName(Feed.INSTANT);
        story.setRemoteNodeName(remoteNodeName);
        story.setRemotePostingNodeName(postingInfo.getOwnerName());
        story.setRemotePostingFullName(postingInfo.getOwnerFullName());
        AvatarImage postingOwnerAvatar = postingInfo.getOwnerAvatar();
        if (postingOwnerAvatar != null) {
            story.setRemotePostingAvatarMediaFile(AvatarImageUtil.getMediaFile(postingOwnerAvatar));
            story.setRemotePostingAvatarShape(postingOwnerAvatar.getShape());
        }
        story.setRemotePostingId(remotePostingId);
        story.setRemoteCommentId(remoteCommentId);
        story.setSummaryData(buildCommentSummary(postingInfo, commentInfo));
        story.setPublishedAt(Util.now());
        updateMoment(story);
        story = storyRepository.save(story);
        storyAdded(story);
    }

    private static StorySummaryData buildPostSummary(String postingHeading) {
        StorySummaryData summaryData = new StorySummaryData();
        summaryData.setPosting(StorySummaryEntryUtil.build(null, null, null, postingHeading));
        return summaryData;
    }

    private static StorySummaryData buildCommentSummary(PostingInfo postingInfo, CommentInfo commentInfo) {
        StorySummaryData summaryData = new StorySummaryData();
        summaryData.setPosting(StorySummaryEntryUtil.build(
            postingInfo.getOwnerName(),
            postingInfo.getOwnerFullName(),
            postingInfo.getOwnerGender(),
            postingInfo.getHeading()
        ));
        summaryData.setComment(StorySummaryEntryUtil.build(null, null, null, commentInfo.getHeading()));
        return summaryData;
    }

}
