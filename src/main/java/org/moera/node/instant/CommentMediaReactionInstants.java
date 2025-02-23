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
import jakarta.inject.Inject;

import org.moera.lib.node.types.AvatarImage;
import org.moera.lib.node.types.CommentInfo;
import org.moera.lib.node.types.PostingInfo;
import org.moera.lib.node.types.StorySummaryData;
import org.moera.lib.node.types.StorySummaryReaction;
import org.moera.lib.node.types.StoryType;
import org.moera.node.data.Feed;
import org.moera.node.data.Story;
import org.moera.node.data.StoryRepository;
import org.moera.node.model.AvatarImageUtil;
import org.moera.node.model.StorySummaryEntryUtil;
import org.moera.node.model.StorySummaryReactionUtil;
import org.moera.node.util.Util;
import org.springframework.stereotype.Component;

@Component
public class CommentMediaReactionInstants extends InstantsCreator {

    private static final Duration GROUP_PERIOD = Duration.of(6, ChronoUnit.HOURS);

    @Inject
    private StoryRepository storyRepository;

    public void added(
        String nodeName,
        String parentPostingNodeName,
        String parentPostingFullName,
        String parentPostingGender,
        AvatarImage parentPostingAvatar,
        String mediaPostingId,
        String parentPostingId,
        String parentCommentId,
        String parentMediaId,
        String reactionNodeName,
        String reactionFullName,
        String reactionGender,
        AvatarImage reactionAvatar,
        String commentHeading,
        boolean reactionNegative,
        int reactionEmoji
    ) {
        if (reactionNodeName.equals(nodeName())) {
            return;
        }

        StoryType storyType = reactionNegative
            ? StoryType.COMMENT_MEDIA_REACTION_ADDED_NEGATIVE
            : StoryType.COMMENT_MEDIA_REACTION_ADDED_POSITIVE;

        if (isBlocked(storyType, null, parentPostingNodeName, parentPostingId, reactionNodeName)) {
            return;
        }

        boolean isNewStory = false;
        Story story = storyRepository.findFullByRemotePostingId(
            nodeId(), Feed.INSTANT, storyType, nodeName, mediaPostingId
        ).stream().findFirst().orElse(null);
        if (
            story == null
            || story.isViewed() && story.getCreatedAt().toInstant().plus(GROUP_PERIOD).isBefore(Instant.now())
        ) {
            isNewStory = true;
            story = new Story(UUID.randomUUID(), nodeId(), storyType);
            story.setFeedName(Feed.INSTANT);
            story.setRemoteNodeName(nodeName);
            story.setRemotePostingNodeName(parentPostingNodeName);
            story.setRemotePostingFullName(parentPostingFullName);
            if (parentPostingAvatar != null) {
                story.setRemotePostingAvatarMediaFile(AvatarImageUtil.getMediaFile(parentPostingAvatar));
                story.setRemotePostingAvatarShape(parentPostingAvatar.getShape());
            }
            story.setRemotePostingId(mediaPostingId);
            story.setRemoteParentPostingId(parentPostingId);
            story.setRemoteParentCommentId(parentCommentId);
            story.setRemoteParentMediaId(parentMediaId);
            story.setSummaryData(buildParentPostingSummary(parentPostingGender, commentHeading));
            story.setMoment(0L);
            story = storyRepository.save(story);
        }

        Story substory = new Story(UUID.randomUUID(), nodeId(), storyType);
        substory.setRemoteOwnerName(reactionNodeName);
        substory.setRemoteOwnerFullName(reactionFullName);
        if (reactionAvatar != null) {
            substory.setRemoteOwnerAvatarMediaFile(AvatarImageUtil.getMediaFile(reactionAvatar));
            substory.setRemoteOwnerAvatarShape(reactionAvatar.getShape());
        }
        substory.setSummaryData(buildReactionSummary(reactionGender, reactionEmoji));
        substory.setMoment(0L);
        substory = storyRepository.save(substory);
        story.addSubstory(substory);

        updated(story, isNewStory, true);
    }

    private static StorySummaryData buildParentPostingSummary(String gender, String commentHeading) {
        StorySummaryData summaryData = new StorySummaryData();
        summaryData.setParentPosting(StorySummaryEntryUtil.build(null, null, gender, null));
        summaryData.setComment(StorySummaryEntryUtil.build(null, null, null, commentHeading));
        return summaryData;
    }

    private static StorySummaryData buildReactionSummary(String gender, int emoji) {
        StorySummaryData summaryData = new StorySummaryData();
        summaryData.setReaction(StorySummaryReactionUtil.build(null, null, gender, emoji));
        return summaryData;
    }

    public void deleted(String nodeName, String postingId, String ownerName, boolean negative) {
        if (ownerName.equals(nodeName())) {
            return;
        }

        StoryType storyType = negative
            ? StoryType.COMMENT_MEDIA_REACTION_ADDED_NEGATIVE
            : StoryType.COMMENT_MEDIA_REACTION_ADDED_POSITIVE;
        List<Story> stories = storyRepository.findFullByRemotePostingId(
            nodeId(), Feed.INSTANT, storyType, nodeName, postingId
        );
        for (Story story : stories) {
            Story substory = story.getSubstories().stream()
                .filter(t -> Objects.equals(t.getRemoteOwnerName(), ownerName))
                .findAny()
                .orElse(null);
            if (substory != null) {
                story.removeSubstory(substory);
                storyRepository.delete(substory);
                updated(story, false, false);
            }
        }
    }

    public void deletedAll(String nodeName, String postingId) {
        storyRepository.deleteByRemotePostingId(
            nodeId(), Feed.INSTANT, StoryType.COMMENT_MEDIA_REACTION_ADDED_POSITIVE, nodeName, postingId
        );
        storyRepository.deleteByRemotePostingId(
            nodeId(), Feed.INSTANT, StoryType.COMMENT_MEDIA_REACTION_ADDED_NEGATIVE, nodeName, postingId
        );
        feedUpdated();
    }

    private void updated(Story story, boolean isNew, boolean isAdded) {
        List<Story> substories = story.getSubstories().stream()
            .sorted(Comparator.comparing(Story::getCreatedAt).reversed())
            .collect(Collectors.toList());
        if (substories.isEmpty()) {
            storyRepository.delete(story);
            if (!isNew) {
                storyDeleted(story);
            }
            return;
        }

        story.setSummaryData(buildAddedSummary(story, substories));
        story.setRemoteOwnerName(substories.get(0).getRemoteOwnerName());
        story.setRemoteOwnerFullName(substories.get(0).getRemoteOwnerFullName());
        story.setRemoteOwnerAvatarMediaFile(substories.get(0).getRemoteOwnerAvatarMediaFile());
        story.setRemoteOwnerAvatarShape(substories.get(0).getRemoteOwnerAvatarShape());
        story.setPublishedAt(Util.now());
        if (isAdded) {
            story.setRead(false);
            story.setViewed(false);
        }
        updateMoment(story);
        storyAddedOrUpdated(story, isNew);
    }

    private StorySummaryData buildAddedSummary(Story story, List<Story> substories) {
        StorySummaryData summaryData = new StorySummaryData();
        List<StorySummaryReaction> reactions = new ArrayList<>();
        for (int i = 0; i < 2 && i < substories.size(); i++) {
            Story substory = substories.get(i);
            StorySummaryReaction reaction = substory.getSummaryData().getReaction();
            reactions.add(StorySummaryReactionUtil.build(
                substory.getRemoteOwnerName(),
                substory.getRemoteOwnerFullName(),
                reaction.getOwnerGender(),
                reaction.getEmoji()
            ));
        }
        summaryData.setReactions(reactions);
        summaryData.setTotalReactions(substories.size());
        summaryData.setComment(StorySummaryEntryUtil.build(
            null, null, null, story.getSummaryData().getComment().getHeading()
        ));
        summaryData.setPosting(StorySummaryEntryUtil.build(
            story.getRemotePostingNodeName(),
            story.getRemotePostingFullName(),
            story.getSummaryData().getParentPosting().getOwnerGender(),
            null
        ));
        return summaryData;
    }

    public void addingFailed(
        String nodeName,
        String mediaPostingId,
        String parentPostingId,
        String parentCommentId,
        String parentMediaId,
        PostingInfo parentPostingInfo,
        CommentInfo parentCommentInfo
    ) {
        if (isBlocked(StoryType.COMMENT_MEDIA_REACTION_FAILED, null, nodeName, parentPostingId)) {
            return;
        }

        String postingOwnerName = parentPostingInfo != null ? parentPostingInfo.getOwnerName() : "";
        String postingOwnerFullName = parentPostingInfo != null ? parentPostingInfo.getOwnerFullName() : null;
        String postingOwnerGender = parentPostingInfo != null ? parentPostingInfo.getOwnerGender() : null;
        AvatarImage postingOwnerAvatar = parentPostingInfo != null ? parentPostingInfo.getOwnerAvatar() : null;
        String postingHeading = parentPostingInfo != null ? parentPostingInfo.getHeading() : "";
        String commentOwnerName = parentCommentInfo != null ? parentCommentInfo.getOwnerName() : "";
        String commentOwnerFullName = parentCommentInfo != null ? parentCommentInfo.getOwnerFullName() : null;
        String commentOwnerGender = parentCommentInfo != null ? parentCommentInfo.getOwnerGender() : null;
        AvatarImage commentOwnerAvatar = parentCommentInfo != null ? parentCommentInfo.getOwnerAvatar() : null;
        String commentHeading = parentCommentInfo != null ? parentCommentInfo.getHeading() : "";

        Story story = new Story(UUID.randomUUID(), nodeId(), StoryType.COMMENT_MEDIA_REACTION_FAILED);
        story.setFeedName(Feed.INSTANT);
        story.setRemoteNodeName(nodeName);
        story.setRemotePostingNodeName(postingOwnerName);
        story.setRemotePostingFullName(postingOwnerFullName);
        if (postingOwnerAvatar != null) {
            story.setRemotePostingAvatarMediaFile(AvatarImageUtil.getMediaFile(postingOwnerAvatar));
            story.setRemotePostingAvatarShape(postingOwnerAvatar.getShape());
        }
        story.setRemotePostingId(mediaPostingId);
        story.setRemoteOwnerName(commentOwnerName);
        story.setRemoteOwnerFullName(commentOwnerFullName);
        if (commentOwnerAvatar != null) {
            story.setRemoteOwnerAvatarMediaFile(AvatarImageUtil.getMediaFile(commentOwnerAvatar));
            story.setRemoteOwnerAvatarShape(commentOwnerAvatar.getShape());
        }
        story.setRemoteParentPostingId(parentPostingId);
        story.setRemoteParentCommentId(parentCommentId);
        story.setRemoteParentMediaId(parentMediaId);
        story.setSummaryData(buildAddingFailedSummary(
            postingOwnerName,
            postingOwnerFullName,
            postingOwnerGender,
            postingHeading,
            commentOwnerName,
            commentOwnerFullName,
            commentOwnerGender,
            commentHeading
        ));
        story.setPublishedAt(Util.now());
        updateMoment(story);
        story = storyRepository.save(story);
        storyAdded(story);
    }

    private static StorySummaryData buildAddingFailedSummary(
        String postingOwnerName,
        String postingOwnerFullName,
        String postingOwnerGender,
        String postingHeading,
        String commentOwnerName,
        String commentOwnerFullName,
        String commentOwnerGender,
        String commentHeading
    ) {
        StorySummaryData summaryData = new StorySummaryData();
        summaryData.setPosting(StorySummaryEntryUtil.build(
            postingOwnerName, postingOwnerFullName, postingOwnerGender, postingHeading
        ));
        summaryData.setComment(StorySummaryEntryUtil.build(
            commentOwnerName, commentOwnerFullName, commentOwnerGender, commentHeading
        ));
        return summaryData;
    }

}
