package org.moera.node.instant;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import javax.inject.Inject;

import org.moera.node.data.Comment;
import org.moera.node.data.Feed;
import org.moera.node.data.Story;
import org.moera.node.data.StoryRepository;
import org.moera.node.data.StoryType;
import org.moera.node.model.AvatarImage;
import org.moera.node.model.CommentInfo;
import org.moera.node.model.PostingInfo;
import org.moera.node.model.StorySummaryData;
import org.moera.node.model.StorySummaryEntry;
import org.moera.node.util.Util;
import org.springframework.stereotype.Component;

@Component
public class CommentInstants extends InstantsCreator {

    private static final Duration GROUP_PERIOD = Duration.of(6, ChronoUnit.HOURS);

    @Inject
    private StoryRepository storyRepository;

    public void added(Comment comment) {
        if (comment.getOwnerName().equals(nodeName())
                // 'reply-comment' instant is expected to be created for such a comment
                || comment.getRepliedTo() != null && comment.getRepliedToName().equals(nodeName())) {
            return;
        }

        boolean alreadyReported = !storyRepository.findSubsByTypeAndEntryId(nodeId(), StoryType.COMMENT_ADDED,
                comment.getId()).isEmpty();
        if (alreadyReported) {
            return;
        }

        boolean isNewStory = false;
        Story story = storyRepository.findFullByFeedAndTypeAndEntryId(nodeId(), Feed.INSTANT,
                StoryType.COMMENT_ADDED, comment.getPosting().getId()).stream().findFirst().orElse(null);
        if (story == null || story.getCreatedAt().toInstant().plus(GROUP_PERIOD).isBefore(Instant.now())) {
            isNewStory = true;
            story = new Story(UUID.randomUUID(), nodeId(), StoryType.COMMENT_ADDED);
            story.setFeedName(Feed.INSTANT);
            story.setEntry(comment.getPosting());
            story.setMoment(0L);
            story = storyRepository.save(story);
        }

        Story substory = new Story(UUID.randomUUID(), nodeId(), StoryType.COMMENT_ADDED);
        substory.setEntry(comment);
        substory.setRemoteOwnerName(comment.getOwnerName());
        substory.setRemoteOwnerFullName(comment.getOwnerFullName());
        substory.setRemoteOwnerAvatarMediaFile(comment.getOwnerAvatarMediaFile());
        substory.setRemoteOwnerAvatarShape(comment.getOwnerAvatarShape());
        substory.setMoment(0L);
        substory = storyRepository.save(substory);
        story.addSubstory(substory);

        updated(story, isNewStory, true);
    }

    public void deleted(Comment comment) {
        if (comment.getOwnerName().equals(nodeName())) {
            return;
        }

        List<Story> stories = storyRepository.findSubsByTypeAndEntryId(nodeId(), StoryType.COMMENT_ADDED,
                comment.getId());
        for (Story substory : stories) {
            Story story = substory.getParent();
            story.removeSubstory(substory);
            storyRepository.delete(substory);
            updated(story, false, false);
        }
    }

    private void updated(Story story, boolean isNew, boolean isAdded) {
        List<Story> stories = story.getSubstories().stream()
                .sorted(Comparator.comparing(Story::getCreatedAt).reversed())
                .collect(Collectors.toList());
        if (stories.size() == 0) {
            storyRepository.delete(story);
            if (!isNew) {
                storyDeleted(story);
            }
            return;
        }

        story.setSummaryData(buildAddedSummary(story, stories));
        story.setRemoteCommentId(stories.get(0).getEntry().getId().toString());
        story.setRemoteOwnerName(stories.get(0).getRemoteOwnerName());
        story.setRemoteOwnerFullName(stories.get(0).getRemoteOwnerFullName());
        story.setRemoteOwnerAvatarMediaFile(stories.get(0).getRemoteOwnerAvatarMediaFile());
        story.setRemoteOwnerAvatarShape(stories.get(0).getRemoteOwnerAvatarShape());
        story.setPublishedAt(Util.now());
        if (isAdded) {
            story.setRead(false);
            story.setViewed(false);
        }
        updateMoment(story);
        storyAddedOrUpdated(story, isNew);
    }

    private static StorySummaryData buildAddedSummary(Story story, List<Story> stories) {
        StorySummaryData summaryData = new StorySummaryData();
        List<StorySummaryEntry> comments = new ArrayList<>();
        summaryData.setComments(comments);
        Story firstStory = stories.get(0);
        comments.add(new StorySummaryEntry(firstStory.getRemoteOwnerName(), firstStory.getRemoteOwnerFullName(), null));
        if (stories.size() > 1) { // just for optimization
            var names = stories.stream().map(Story::getRemoteOwnerName).collect(Collectors.toSet());
            Story secondStory = stories.stream()
                    .filter(t -> !t.getRemoteOwnerName().equals(firstStory.getRemoteOwnerName()))
                    .findFirst()
                    .orElse(null);
            if (secondStory != null) {
                comments.add(new StorySummaryEntry(
                        secondStory.getRemoteOwnerName(), secondStory.getRemoteOwnerFullName(), null));
            }
            summaryData.setTotalComments(names.size());
        } else {
            summaryData.setTotalComments(1);
        }
        summaryData.setPosting(new StorySummaryEntry(null, null, story.getEntry().getCurrentRevision().getHeading()));
        return summaryData;
    }

    public void addingFailed(String remoteNodeName, String remotePostingId, PostingInfo postingInfo) {
        String postingOwnerName = postingInfo != null ? postingInfo.getOwnerName() : "";
        String postingOwnerFullName = postingInfo != null ? postingInfo.getOwnerFullName() : null;
        AvatarImage postingOwnerAvatar = postingInfo != null ? postingInfo.getOwnerAvatar() : null;
        String postingHeading = postingInfo != null ? postingInfo.getHeading() : "";

        Story story = new Story(UUID.randomUUID(), nodeId(), StoryType.COMMENT_POST_TASK_FAILED);
        story.setFeedName(Feed.INSTANT);
        story.setRemoteNodeName(remoteNodeName);
        story.setRemotePostingNodeName(postingOwnerName);
        story.setRemotePostingFullName(postingOwnerFullName);
        if (postingOwnerAvatar != null) {
            story.setRemotePostingAvatarMediaFile(postingOwnerAvatar.getMediaFile());
            story.setRemotePostingAvatarShape(postingOwnerAvatar.getShape());
        }
        story.setRemotePostingId(remotePostingId);
        story.setSummaryData(buildAddingFailedSummary(postingOwnerName, postingOwnerFullName, postingHeading));
        story.setPublishedAt(Util.now());
        updateMoment(story);
        story = storyRepository.save(story);
        storyAdded(story);
    }

    public void updateFailed(String remoteNodeName, String remotePostingId, PostingInfo postingInfo,
                             String remoteCommentId, CommentInfo commentInfo) {
        String postingOwnerName = postingInfo != null ? postingInfo.getOwnerName() : "";
        String postingOwnerFullName = postingInfo != null ? postingInfo.getOwnerFullName() : null;
        AvatarImage postingOwnerAvatar = postingInfo != null ? postingInfo.getOwnerAvatar() : null;
        String postingHeading = postingInfo != null ? postingInfo.getHeading() : "";
        String commentHeading = commentInfo != null ? commentInfo.getHeading() : "";

        Story story = new Story(UUID.randomUUID(), nodeId(), StoryType.COMMENT_UPDATE_TASK_FAILED);
        story.setFeedName(Feed.INSTANT);
        story.setRemoteNodeName(remoteNodeName);
        story.setRemotePostingNodeName(postingOwnerName);
        story.setRemotePostingFullName(postingOwnerFullName);
        if (postingOwnerAvatar != null) {
            story.setRemotePostingAvatarMediaFile(postingOwnerAvatar.getMediaFile());
            story.setRemotePostingAvatarShape(postingOwnerAvatar.getShape());
        }
        story.setRemotePostingId(remotePostingId);
        story.setRemoteCommentId(remoteCommentId);
        story.setSummaryData(
                buildUpdateFailedSummary(postingOwnerName, postingOwnerFullName, postingHeading, commentHeading));
        story.setPublishedAt(Util.now());
        updateMoment(story);
        story = storyRepository.save(story);
        storyAdded(story);
    }

    private static StorySummaryData buildAddingFailedSummary(String nodeName, String fullName, String postingHeading) {
        StorySummaryData summaryData = new StorySummaryData();
        summaryData.setPosting(new StorySummaryEntry(nodeName, fullName, postingHeading));
        return summaryData;
    }

    private static StorySummaryData buildUpdateFailedSummary(String nodeName, String fullName, String postingHeading,
                                                             String commentHeading) {
        StorySummaryData summaryData = new StorySummaryData();
        summaryData.setPosting(new StorySummaryEntry(nodeName, fullName, postingHeading));
        summaryData.setComment(new StorySummaryEntry(null, null, commentHeading));
        return summaryData;
    }

}
