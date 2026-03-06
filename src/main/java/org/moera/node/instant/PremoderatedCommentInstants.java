package org.moera.node.instant;

import java.util.List;
import java.util.UUID;
import jakarta.inject.Inject;

import org.moera.lib.node.types.AvatarImage;
import org.moera.lib.node.types.SheriffMark;
import org.moera.lib.node.types.StorySummaryData;
import org.moera.lib.node.types.StoryType;
import org.moera.node.data.Feed;
import org.moera.node.data.Story;
import org.moera.node.data.StoryRepository;
import org.moera.node.model.AvatarImageUtil;
import org.moera.node.model.StorySummaryEntryUtil;
import org.moera.node.util.Util;
import org.springframework.stereotype.Component;

@Component
public class PremoderatedCommentInstants extends InstantsCreator {

    @Inject
    private StoryRepository storyRepository;

    public void accepted(
        String nodeName,
        String postingOwnerName,
        String postingOwnerFullName,
        String postingOwnerGender,
        AvatarImage postingOwnerAvatar,
        String postingHeading,
        List<String> postingSheriffs,
        List<SheriffMark> postingSheriffMarks,
        String postingId,
        String commentId,
        String commentHeading
    ) {
        decided(
            StoryType.PREMODERATED_COMMENT_ACCEPTED,
            nodeName,
            postingOwnerName,
            postingOwnerFullName,
            postingOwnerGender,
            postingOwnerAvatar,
            postingHeading,
            postingSheriffs,
            postingSheriffMarks,
            postingId,
            commentId,
            commentHeading
        );
    }

    public void rejected(
        String nodeName,
        String postingOwnerName,
        String postingOwnerFullName,
        String postingOwnerGender,
        AvatarImage postingOwnerAvatar,
        String postingHeading,
        List<String> postingSheriffs,
        List<SheriffMark> postingSheriffMarks,
        String postingId,
        String commentId,
        String commentHeading
    ) {
        decided(
            StoryType.PREMODERATED_COMMENT_REJECTED,
            nodeName,
            postingOwnerName,
            postingOwnerFullName,
            postingOwnerGender,
            postingOwnerAvatar,
            postingHeading,
            postingSheriffs,
            postingSheriffMarks,
            postingId,
            commentId,
            commentHeading
        );
    }

    private void decided(
        StoryType storyType,
        String nodeName,
        String postingOwnerName,
        String postingOwnerFullName,
        String postingOwnerGender,
        AvatarImage postingOwnerAvatar,
        String postingHeading,
        List<String> postingSheriffs,
        List<SheriffMark> postingSheriffMarks,
        String postingId,
        String commentId,
        String commentHeading
    ) {
        if (isBlocked(storyType, null, nodeName, postingId)) {
            return;
        }

        Story story = new Story(UUID.randomUUID(), nodeId(), storyType);
        story.setFeedName(Feed.INSTANT);
        story.setRemoteNodeName(nodeName);
        story.setRemotePostingNodeName(postingOwnerName);
        story.setRemotePostingFullName(postingOwnerFullName);
        if (postingOwnerAvatar != null) {
            story.setRemotePostingAvatarMediaFile(AvatarImageUtil.getMediaFile(postingOwnerAvatar));
            story.setRemotePostingAvatarShape(postingOwnerAvatar.getShape());
        }
        story.setRemotePostingId(postingId);
        story.setRemoteCommentId(commentId);
        story.setSummaryData(buildSummary(
            postingOwnerName,
            postingOwnerFullName,
            postingOwnerGender,
            postingHeading,
            postingSheriffs,
            postingSheriffMarks,
            commentHeading
        ));
        story.setPublishedAt(Util.now());
        updateMoment(story);
        story = storyRepository.save(story);
        storyAdded(story);
    }

    private static StorySummaryData buildSummary(
        String postingOwnerName,
        String postingOwnerFullName,
        String postingOwnerGender,
        String postingHeading,
        List<String> postingSheriffs,
        List<SheriffMark> postingSheriffMarks,
        String commentHeading
    ) {
        StorySummaryData summaryData = new StorySummaryData();
        summaryData.setPosting(StorySummaryEntryUtil.build(
            postingOwnerName,
            postingOwnerFullName,
            postingOwnerGender,
            postingHeading,
            postingSheriffs,
            postingSheriffMarks
        ));
        summaryData.setComment(StorySummaryEntryUtil.build(null, null, null, commentHeading));
        return summaryData;
    }

}
