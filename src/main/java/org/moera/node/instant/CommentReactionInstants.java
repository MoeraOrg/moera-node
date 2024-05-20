package org.moera.node.instant;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;
import javax.inject.Inject;

import org.moera.node.data.Feed;
import org.moera.node.data.Story;
import org.moera.node.data.StoryRepository;
import org.moera.node.data.StoryType;
import org.moera.node.model.AvatarImage;
import org.moera.node.model.CommentInfo;
import org.moera.node.model.PostingInfo;
import org.moera.node.model.StorySummaryData;
import org.moera.node.model.StorySummaryEntry;
import org.moera.node.model.StorySummaryReaction;
import org.moera.node.util.Util;
import org.springframework.stereotype.Component;

@Component
public class CommentReactionInstants extends InstantsCreator {

    private static final Duration GROUP_PERIOD = Duration.of(6, ChronoUnit.HOURS);

    @Inject
    private StoryRepository storyRepository;

    public void added(String nodeName, String postingOwnerName, String postingOwnerFullName, String postingOwnerGender,
                      AvatarImage postingOwnerAvatar, String postingId, String commentId, String reactionNodeName,
                      String reactionFullName, String reactionGender, AvatarImage reactionAvatar, String commentHeading,
                      boolean reactionNegative, int reactionEmoji) {
        if (reactionNodeName.equals(nodeName())) {
            return;
        }

        StoryType storyType = reactionNegative ? StoryType.COMMENT_REACTION_ADDED_NEGATIVE
                : StoryType.COMMENT_REACTION_ADDED_POSITIVE;

        if (isBlocked(storyType, null, nodeName, postingId, reactionNodeName)) {
            return;
        }

        boolean isNewStory = false;
        Story story = storyRepository.findFullByRemotePostingAndCommentId(
                nodeId(), Feed.INSTANT, storyType, nodeName, postingId, commentId).stream()
                .findFirst().orElse(null);
        if (story == null
                || story.isViewed() && story.getCreatedAt().toInstant().plus(GROUP_PERIOD).isBefore(Instant.now())) {
            isNewStory = true;
            story = new Story(UUID.randomUUID(), nodeId(), storyType);
            story.setFeedName(Feed.INSTANT);
            story.setRemoteNodeName(nodeName);
            story.setRemotePostingNodeName(postingOwnerName);
            story.setRemotePostingFullName(postingOwnerFullName);
            if (postingOwnerAvatar != null) {
                story.setRemotePostingAvatarMediaFile(postingOwnerAvatar.getMediaFile());
                story.setRemotePostingAvatarShape(postingOwnerAvatar.getShape());
            }
            story.setRemotePostingId(postingId);
            story.setRemoteCommentId(commentId);
            story.setSummaryData(buildPostingAndCommentSummary(postingOwnerGender, commentHeading));
            story.setMoment(0L);
            story = storyRepository.save(story);
        }

        Story substory = new Story(UUID.randomUUID(), nodeId(), storyType);
        substory.setRemoteOwnerName(reactionNodeName);
        substory.setRemoteOwnerFullName(reactionFullName);
        if (reactionAvatar != null) {
            substory.setRemoteOwnerAvatarMediaFile(reactionAvatar.getMediaFile());
            substory.setRemoteOwnerAvatarShape(reactionAvatar.getShape());
        }
        substory.setSummaryData(buildReactionSummary(reactionGender, reactionEmoji));
        substory.setMoment(0L);
        substory = storyRepository.save(substory);
        story.addSubstory(substory);

        updated(story, isNewStory, true);
    }

    private static StorySummaryData buildPostingAndCommentSummary(String gender, String commentHeading) {
        StorySummaryData summaryData = new StorySummaryData();
        summaryData.setPosting(new StorySummaryEntry(null, null, gender, null));
        summaryData.setComment(new StorySummaryEntry(null, null, null, commentHeading));
        return summaryData;
    }

    private static StorySummaryData buildReactionSummary(String gender, int emoji) {
        StorySummaryData summaryData = new StorySummaryData();
        summaryData.setReaction(new StorySummaryReaction(null, null, gender, emoji));
        return summaryData;
    }

    public void deleted(String nodeName, String postingId, String commentId,
                        String reactionOwnerName, boolean reactionNegative) {
        if (reactionOwnerName.equals(nodeName())) {
            return;
        }

        StoryType storyType = reactionNegative ? StoryType.COMMENT_REACTION_ADDED_NEGATIVE
                : StoryType.COMMENT_REACTION_ADDED_POSITIVE;
        List<Story> stories = storyRepository.findFullByRemotePostingAndCommentId(
                nodeId(), Feed.INSTANT, storyType, nodeName, postingId, commentId);
        for (Story story : stories) {
            Story substory = story.getSubstories().stream()
                    .filter(t -> Objects.equals(t.getRemoteOwnerName(), reactionOwnerName))
                    .findAny()
                    .orElse(null);
            if (substory != null) {
                story.removeSubstory(substory);
                storyRepository.delete(substory);
                updated(story, false, false);
            }
        }
    }

    public void deletedAll(String nodeName, String postingId, String commentId) {
        storyRepository.deleteByRemotePostingAndCommentId(nodeId(), Feed.INSTANT,
                StoryType.COMMENT_REACTION_ADDED_POSITIVE, nodeName, postingId, commentId);
        storyRepository.deleteByRemotePostingAndCommentId(nodeId(), Feed.INSTANT,
                StoryType.COMMENT_REACTION_ADDED_NEGATIVE, nodeName, postingId, commentId);
        feedUpdated();
    }

    private void updated(Story story, boolean isNew, boolean isAdded) {
        List<Story> stories = story.getSubstories().stream()
                .sorted(Comparator.comparing(Story::getCreatedAt).reversed())
                .collect(Collectors.toList());
        if (stories.isEmpty()) {
            storyRepository.delete(story);
            if (!isNew) {
                storyDeleted(story);
            }
            return;
        }

        story.setSummaryData(buildAddedSummary(story, stories));
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

    private StorySummaryData buildAddedSummary(Story story, List<Story> stories) {
        StorySummaryData summaryData = new StorySummaryData();
        List<StorySummaryReaction> reactions = new ArrayList<>();
        for (int i = 0; i < 2 && i < stories.size(); i++) {
            Story substory = stories.get(i);
            StorySummaryReaction reaction = substory.getSummaryData().getReaction();
            reactions.add(new StorySummaryReaction(substory.getRemoteOwnerName(), substory.getRemoteOwnerFullName(),
                    reaction.getOwnerGender(), reaction.getEmoji()));
        }
        summaryData.setReactions(reactions);
        summaryData.setTotalReactions(stories.size());
        summaryData.setComment(new StorySummaryEntry(
                null, null, null, story.getSummaryData().getComment().getHeading()));
        summaryData.setPosting(new StorySummaryEntry(
                story.getRemotePostingNodeName(), story.getRemotePostingFullName(),
                story.getSummaryData().getPosting().getOwnerGender(), null));
        return summaryData;
    }

    public void addingFailed(String nodeName, String postingId, PostingInfo postingInfo, String commentId,
                             CommentInfo commentInfo) {
        if (isBlocked(StoryType.COMMENT_REACTION_TASK_FAILED, null, nodeName, postingId)) {
            return;
        }

        String postingOwnerName = postingInfo != null ? postingInfo.getOwnerName() : "";
        String postingOwnerFullName = postingInfo != null ? postingInfo.getOwnerFullName() : null;
        String postingOwnerGender = postingInfo != null ? postingInfo.getOwnerGender() : null;
        AvatarImage postingOwnerAvatar = postingInfo != null ? postingInfo.getOwnerAvatar() : null;
        String postingHeading = postingInfo != null ? postingInfo.getHeading() : "";
        String commentOwnerName = commentInfo != null ? commentInfo.getOwnerName() : "";
        String commentOwnerFullName = commentInfo != null ? commentInfo.getOwnerFullName() : null;
        String commentOwnerGender = commentInfo != null ? commentInfo.getOwnerGender() : null;
        AvatarImage commentOwnerAvatar = commentInfo != null ? commentInfo.getOwnerAvatar() : null;
        String commentHeading = commentInfo != null ? commentInfo.getHeading() : "";

        Story story = new Story(UUID.randomUUID(), nodeId(), StoryType.COMMENT_REACTION_TASK_FAILED);
        story.setFeedName(Feed.INSTANT);
        story.setRemoteNodeName(nodeName);
        story.setRemotePostingNodeName(postingOwnerName);
        story.setRemotePostingFullName(postingOwnerFullName);
        if (postingOwnerAvatar != null) {
            story.setRemotePostingAvatarMediaFile(postingOwnerAvatar.getMediaFile());
            story.setRemotePostingAvatarShape(postingOwnerAvatar.getShape());
        }
        story.setRemotePostingId(postingId);
        story.setRemoteOwnerName(commentOwnerName);
        story.setRemoteOwnerFullName(commentOwnerFullName);
        if (commentOwnerAvatar != null) {
            story.setRemoteOwnerAvatarMediaFile(commentOwnerAvatar.getMediaFile());
            story.setRemoteOwnerAvatarShape(commentOwnerAvatar.getShape());
        }
        story.setRemoteCommentId(commentId);
        story.setSummaryData(buildAddingFailedSummary(postingOwnerName, postingOwnerFullName, postingOwnerGender,
                postingHeading, commentOwnerName, commentOwnerFullName, commentOwnerGender, commentHeading));
        story.setPublishedAt(Util.now());
        updateMoment(story);
        story = storyRepository.save(story);
        storyAdded(story);
    }

    private static StorySummaryData buildAddingFailedSummary(String postingOwnerName, String postingOwnerFullName,
                                                             String postingOwnerGender, String postingHeading,
                                                             String commentOwnerName, String commentOwnerFullName,
                                                             String commentOwnerGender, String commentHeading) {
        StorySummaryData summaryData = new StorySummaryData();
        summaryData.setPosting(new StorySummaryEntry(
                postingOwnerName, postingOwnerFullName, postingOwnerGender, postingHeading));
        summaryData.setComment(new StorySummaryEntry(
                commentOwnerName, commentOwnerFullName, commentOwnerGender, commentHeading));
        return summaryData;
    }

}
