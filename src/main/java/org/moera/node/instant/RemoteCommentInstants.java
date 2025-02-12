package org.moera.node.instant;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import jakarta.inject.Inject;

import org.moera.lib.node.types.AvatarImage;
import org.moera.lib.node.types.SheriffMark;
import org.moera.lib.node.types.StorySummaryData;
import org.moera.lib.node.types.StorySummaryEntry;
import org.moera.lib.node.types.StoryType;
import org.moera.lib.node.types.SubscriptionReason;
import org.moera.node.data.Feed;
import org.moera.node.data.Story;
import org.moera.node.data.StoryRepository;
import org.moera.node.model.AvatarImageUtil;
import org.moera.node.model.StorySummaryEntryUtil;
import org.moera.node.util.Util;
import org.springframework.stereotype.Component;

@Component
public class RemoteCommentInstants extends InstantsCreator {

    private static final Duration GROUP_PERIOD = Duration.of(6, ChronoUnit.HOURS);

    @Inject
    private StoryRepository storyRepository;

    public void added(String nodeName, String postingOwnerName, String postingOwnerFullName, String postingOwnerGender,
                      AvatarImage postingOwnerAvatar, String postingId, String postingHeading,
                      List<String> postingSheriffs, List<SheriffMark> postingSheriffMarks, String commentOwnerName,
                      String commentOwnerFullName, String commentOwnerGender, AvatarImage commentOwnerAvatar,
                      String commentId, String commentHeading, List<SheriffMark> commentSheriffMarks,
                      SubscriptionReason reason) {
        if (commentOwnerName.equals(nodeName())) {
            return;
        }

        if (isBlocked(StoryType.REMOTE_COMMENT_ADDED, null, nodeName, postingId, commentOwnerName)) {
            return;
        }

        boolean alreadyReported = !storyRepository.findSubsByRemotePostingAndCommentId(nodeId(),
                StoryType.REMOTE_COMMENT_ADDED, nodeName, postingId, commentId).isEmpty();
        if (alreadyReported) {
            return;
        }

        boolean isNewStory = false;
        Story story = storyRepository.findFullByRemotePostingId(nodeId(), Feed.INSTANT, StoryType.REMOTE_COMMENT_ADDED,
                nodeName, postingId).stream().findFirst().orElse(null);
        if (story == null
                || story.isRead()
                || story.isViewed() && story.getCreatedAt().toInstant().plus(GROUP_PERIOD).isBefore(Instant.now())) {
            isNewStory = true;
            story = new Story(UUID.randomUUID(), nodeId(), StoryType.REMOTE_COMMENT_ADDED);
            story.setFeedName(Feed.INSTANT);
            story.setRemoteNodeName(nodeName);
            story.setRemotePostingNodeName(postingOwnerName);
            story.setRemotePostingFullName(postingOwnerFullName);
            if (postingOwnerAvatar != null) {
                story.setRemotePostingAvatarMediaFile(AvatarImageUtil.getMediaFile(postingOwnerAvatar));
                story.setRemotePostingAvatarShape(postingOwnerAvatar.getShape());
            }
            story.setRemotePostingId(postingId);
            story.setSummaryData(buildPostingSummary(
                    postingOwnerGender, postingHeading, postingSheriffs, postingSheriffMarks));
            story.setMoment(0L);
            story = storyRepository.save(story);
        }

        Story substory = new Story(UUID.randomUUID(), nodeId(), StoryType.REMOTE_COMMENT_ADDED);
        substory.setRemoteNodeName(nodeName);
        substory.setRemotePostingId(postingId);
        substory.setRemoteOwnerName(commentOwnerName);
        substory.setRemoteOwnerFullName(commentOwnerFullName);
        if (commentOwnerAvatar != null) {
            substory.setRemoteOwnerAvatarMediaFile(AvatarImageUtil.getMediaFile(commentOwnerAvatar));
            substory.setRemoteOwnerAvatarShape(commentOwnerAvatar.getShape());
        }
        substory.setRemoteCommentId(commentId);
        substory.setSummaryData(buildCommentSummary(commentOwnerGender, commentHeading, commentSheriffMarks));
        substory.setMoment(0L);
        substory = storyRepository.save(substory);
        story.addSubstory(substory);

        updated(story, reason, isNewStory, true);
    }

    private static StorySummaryData buildPostingSummary(String gender, String heading, List<String> sheriffs,
                                                        List<SheriffMark> sheriffMarks) {
        StorySummaryData summaryData = new StorySummaryData();
        summaryData.setPosting(StorySummaryEntryUtil.build(null, null, gender, heading, sheriffs, sheriffMarks));
        return summaryData;
    }

    private static StorySummaryData buildCommentSummary(String gender, String heading, List<SheriffMark> sheriffMarks) {
        StorySummaryData summaryData = new StorySummaryData();
        summaryData.setComment(StorySummaryEntryUtil.build(null, null, gender, heading, null, sheriffMarks));
        return summaryData;
    }

    public void deleted(String nodeName, String postingId, String commentOwnerName, String commentId,
                        SubscriptionReason reason) {
        if (commentOwnerName.equals(nodeName())) {
            return;
        }

        List<Story> stories = storyRepository.findSubsByRemotePostingAndCommentId(nodeId(),
                StoryType.REMOTE_COMMENT_ADDED, nodeName, postingId, commentId);
        for (Story substory : stories) {
            Story story = substory.getParent();
            story.removeSubstory(substory);
            storyRepository.delete(substory);
            updated(story, reason, false, false);
        }
    }

    private void updated(Story story, SubscriptionReason reason, boolean isNew, boolean isAdded) {
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

        story.setSummaryData(buildAddedSummary(story, stories, reason));
        story.setRemoteOwnerName(stories.get(0).getRemoteOwnerName());
        story.setRemoteOwnerFullName(stories.get(0).getRemoteOwnerFullName());
        story.setRemoteOwnerAvatarMediaFile(stories.get(0).getRemoteOwnerAvatarMediaFile());
        story.setRemoteOwnerAvatarShape(stories.get(0).getRemoteOwnerAvatarShape());
        story.setRemoteCommentId(stories.get(stories.size() - 1).getRemoteCommentId());
        story.setPublishedAt(Util.now());
        if (isAdded) {
            story.setRead(false);
            story.setViewed(false);
        }
        updateMoment(story);
        storyAddedOrUpdated(story, isNew);
    }

    private static StorySummaryData buildAddedSummary(Story story, List<Story> stories, SubscriptionReason reason) {
        StorySummaryData summaryData = new StorySummaryData();
        List<StorySummaryEntry> comments = new ArrayList<>();
        Story firstStory = stories.get(0);
        comments.add(StorySummaryEntryUtil.build(
            firstStory.getRemoteOwnerName(),
            firstStory.getRemoteOwnerFullName(),
            firstStory.getSummaryData().getComment().getOwnerGender(),
            firstStory.getSummaryData().getComment().getHeading(),
            firstStory.getSummaryData().getComment().getSheriffs(),
            firstStory.getSummaryData().getComment().getSheriffMarks()
        ));
        if (stories.size() > 1) { // just for optimization
            var names = stories.stream().map(Story::getRemoteOwnerName).collect(Collectors.toSet());
            if (names.size() > 1) {
                Story secondStory = stories.stream()
                        .filter(t -> !t.getRemoteOwnerName().equals(firstStory.getRemoteOwnerName()))
                        .findFirst()
                        .orElse(null);
                if (secondStory != null) {
                    comments.add(StorySummaryEntryUtil.build(
                        secondStory.getRemoteOwnerName(),
                        secondStory.getRemoteOwnerFullName(),
                        secondStory.getSummaryData().getComment().getOwnerGender(),
                        secondStory.getSummaryData().getComment().getHeading(),
                        secondStory.getSummaryData().getComment().getSheriffs(),
                        secondStory.getSummaryData().getComment().getSheriffMarks()
                    ));
                }
            }
            summaryData.setTotalComments(names.size());
        } else {
            summaryData.setTotalComments(1);
        }
        summaryData.setComments(comments);
        summaryData.setPosting(StorySummaryEntryUtil.build(
            story.getRemotePostingNodeName(),
            story.getRemotePostingFullName(),
            story.getSummaryData().getPosting().getOwnerGender(),
            story.getSummaryData().getPosting().getHeading(),
            story.getSummaryData().getPosting().getSheriffs(),
            story.getSummaryData().getPosting().getSheriffMarks()
        ));
        summaryData.setSubscriptionReason(reason);
        return summaryData;
    }

}
