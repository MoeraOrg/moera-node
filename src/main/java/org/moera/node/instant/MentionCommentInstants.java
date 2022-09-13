package org.moera.node.instant;

import java.util.UUID;
import javax.inject.Inject;

import org.moera.node.data.Feed;
import org.moera.node.data.Story;
import org.moera.node.data.StoryRepository;
import org.moera.node.data.StoryType;
import org.moera.node.model.AvatarImage;
import org.moera.node.model.StorySummaryData;
import org.moera.node.model.StorySummaryEntry;
import org.moera.node.operations.StoryOperations;
import org.springframework.stereotype.Component;

@Component
public class MentionCommentInstants extends InstantsCreator {

    @Inject
    private StoryRepository storyRepository;

    @Inject
    private StoryOperations storyOperations;

    public void added(String nodeName, String postingOwnerName, String postingOwnerFullName, AvatarImage postingAvatar,
                      String postingId, String postingHeading, String commentOwnerName, String commentOwnerFullName,
                      AvatarImage commentOwnerAvatar, String commentId, String commentHeading) {
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
            story.setRemotePostingAvatarMediaFile(postingAvatar.getMediaFile());
            story.setRemotePostingAvatarShape(postingAvatar.getShape());
        }
        story.setRemotePostingId(postingId);
        story.setRemoteOwnerName(commentOwnerName);
        story.setRemoteOwnerFullName(commentOwnerFullName);
        if (commentOwnerAvatar != null) {
            story.setRemoteOwnerAvatarMediaFile(commentOwnerAvatar.getMediaFile());
            story.setRemoteOwnerAvatarShape(commentOwnerAvatar.getShape());
        }
        story.setRemoteCommentId(commentId);
        story.setSummaryData(buildSummary(story, postingHeading, commentHeading));
        storyOperations.updateMoment(story);
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

    private StorySummaryData buildSummary(Story story, String postingHeading, String commentHeading) {
        StorySummaryData summaryData = new StorySummaryData();
        summaryData.setPosting(new StorySummaryEntry(story.getRemotePostingNodeName(), story.getRemotePostingFullName(),
                postingHeading));
        summaryData.setComment(new StorySummaryEntry(story.getRemoteOwnerName(), story.getRemoteOwnerFullName(),
                commentHeading));
        return summaryData;
    }

}
