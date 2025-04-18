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
import org.springframework.stereotype.Component;

@Component
public class MentionCommentInstants extends InstantsCreator {

    @Inject
    private StoryRepository storyRepository;

    public void added(String nodeName, String postingOwnerName, String postingOwnerFullName, String postingOwnerGender,
                      AvatarImage postingAvatar, String postingId, String postingHeading, List<String> postingSheriffs,
                      List<SheriffMark> postingSheriffMarks, String commentOwnerName, String commentOwnerFullName,
                      String commentOwnerGender, AvatarImage commentOwnerAvatar, String commentId,
                      String commentHeading, List<SheriffMark> commentSheriffMarks) {
        if (isBlocked(StoryType.MENTION_COMMENT, null, nodeName, postingId, commentOwnerName)) {
            return;
        }
        Story story = findStory(nodeName, postingId, commentId);
        if (story != null) {
            return;
        }
        story = new Story(UUID.randomUUID(), nodeId(), StoryType.MENTION_COMMENT);
        story.setFeedName(Feed.INSTANT);
        story.setRemoteNodeName(nodeName);
        story.setRemotePostingNodeName(postingOwnerName);
        story.setRemotePostingFullName(postingOwnerFullName);
        if (postingAvatar != null) {
            story.setRemotePostingAvatarMediaFile(AvatarImageUtil.getMediaFile(postingAvatar));
            story.setRemotePostingAvatarShape(postingAvatar.getShape());
        }
        story.setRemotePostingId(postingId);
        story.setRemoteOwnerName(commentOwnerName);
        story.setRemoteOwnerFullName(commentOwnerFullName);
        if (commentOwnerAvatar != null) {
            story.setRemoteOwnerAvatarMediaFile(AvatarImageUtil.getMediaFile(commentOwnerAvatar));
            story.setRemoteOwnerAvatarShape(commentOwnerAvatar.getShape());
        }
        story.setRemoteCommentId(commentId);
        story.setSummaryData(buildSummary(
                story, postingOwnerGender, postingHeading, postingSheriffs, postingSheriffMarks, commentOwnerGender,
                commentHeading, commentSheriffMarks));
        updateMoment(story);
        story = storyRepository.saveAndFlush(story);
        storyAdded(story);
    }

    public void deleted(String nodeName, String postingId, String commentId) {
        Story story = findStory(nodeName, postingId, commentId);
        if (story == null) {
            return;
        }
        storyRepository.delete(story);
        storyDeleted(story);
    }

    private Story findStory(String nodeName, String postingId, String commentId) {
        return storyRepository.findFullByRemotePostingAndCommentId(nodeId(), Feed.INSTANT, StoryType.MENTION_COMMENT,
                nodeName, postingId, commentId).stream().findFirst().orElse(null);
    }

    private StorySummaryData buildSummary(Story story, String postingOwnerGender, String postingHeading,
                                          List<String> postingSheriffs, List<SheriffMark> postingSheriffMarks,
                                          String commentOwnerGender, String commentHeading,
                                          List<SheriffMark> commentSheriffMarks) {
        StorySummaryData summaryData = new StorySummaryData();
        summaryData.setPosting(StorySummaryEntryUtil.build(
            story.getRemotePostingNodeName(),
            story.getRemotePostingFullName(),
            postingOwnerGender,
            postingHeading,
            postingSheriffs,
            postingSheriffMarks
        ));
        summaryData.setComment(StorySummaryEntryUtil.build(
            story.getRemoteOwnerName(),
            story.getRemoteOwnerFullName(),
            commentOwnerGender,
            commentHeading,
            null,
            commentSheriffMarks
        ));
        return summaryData;
    }

}
