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

import org.moera.node.data.Feed;
import org.moera.node.data.SheriffMark;
import org.moera.node.data.Story;
import org.moera.node.data.StoryRepository;
import org.moera.node.data.StoryType;
import org.moera.node.model.AvatarImage;
import org.moera.node.model.StorySummaryData;
import org.moera.node.model.StorySummaryEntry;
import org.moera.node.util.Util;
import org.springframework.stereotype.Component;

@Component
public class ReplyCommentInstants extends InstantsCreator {

    private static final Duration GROUP_PERIOD = Duration.of(6, ChronoUnit.HOURS);

    @Inject
    private StoryRepository storyRepository;

    public void added(String nodeName, String postingOwnerName, String postingOwnerFullName, String postingOwnerGender,
                      AvatarImage postingOwnerAvatar, String postingHeading, List<String> postingSheriffs,
                      List<SheriffMark> postingSheriffMarks, String postingId, String commentOwnerName,
                      String commentOwnerFullName, String commentOwnerGender, AvatarImage commentOwnerAvatar,
                      List<SheriffMark> commentSheriffMarks, String commentId, String repliedToHeading,
                      String repliedToId) {
        if (commentOwnerName.equals(nodeName())) {
            return;
        }

        if (isBlocked(StoryType.REPLY_COMMENT, null, nodeName, postingId, commentOwnerName)) {
            return;
        }

        boolean alreadyReported = !storyRepository.findSubsByRemotePostingAndCommentId(nodeId(),
                StoryType.REPLY_COMMENT, nodeName, postingId, commentId).isEmpty();
        if (alreadyReported) {
            return;
        }

        boolean isNewStory = false;
        Story story = storyRepository.findFullByRemotePostingAndRepliedToId(nodeId(), Feed.INSTANT,
                StoryType.REPLY_COMMENT, nodeName, postingId, repliedToId).stream().findFirst().orElse(null);
        if (story == null
                || story.isRead()
                || story.isViewed() && story.getCreatedAt().toInstant().plus(GROUP_PERIOD).isBefore(Instant.now())) {
            isNewStory = true;
            story = new Story(UUID.randomUUID(), nodeId(), StoryType.REPLY_COMMENT);
            story.setFeedName(Feed.INSTANT);
            story.setRemoteNodeName(nodeName);
            story.setRemotePostingNodeName(postingOwnerName);
            story.setRemotePostingFullName(postingOwnerFullName);
            if (postingOwnerAvatar != null) {
                story.setRemotePostingAvatarMediaFile(postingOwnerAvatar.getMediaFile());
                story.setRemotePostingAvatarShape(postingOwnerAvatar.getShape());
            }
            story.setRemotePostingId(postingId);
            story.setRemoteRepliedToId(repliedToId);
            story.setSummaryData(buildPostingSummary(
                    postingOwnerGender, postingHeading, postingSheriffs, postingSheriffMarks, repliedToHeading));
            story.setMoment(0L);
            story = storyRepository.save(story);
        }

        Story substory = new Story(UUID.randomUUID(), nodeId(), StoryType.REPLY_COMMENT);
        substory.setRemoteNodeName(nodeName);
        substory.setRemotePostingId(postingId);
        substory.setRemoteCommentId(commentId);
        substory.setRemoteOwnerName(commentOwnerName);
        substory.setRemoteOwnerFullName(commentOwnerFullName);
        if (commentOwnerAvatar != null) {
            substory.setRemoteOwnerAvatarMediaFile(commentOwnerAvatar.getMediaFile());
            substory.setRemoteOwnerAvatarShape(commentOwnerAvatar.getShape());
        }
        substory.setSummaryData(buildCommentSummary(commentOwnerGender, commentSheriffMarks));
        substory.setMoment(0L);
        substory = storyRepository.save(substory);
        story.addSubstory(substory);

        updated(story, isNewStory, true);
    }

    private static StorySummaryData buildPostingSummary(String gender, String heading, List<String> sheriffs,
                                                        List<SheriffMark> sheriffMarks, String repliedToHeading) {
        StorySummaryData summaryData = new StorySummaryData();
        summaryData.setPosting(new StorySummaryEntry(null, null, gender, heading, sheriffs, sheriffMarks));
        summaryData.setRepliedTo(new StorySummaryEntry(null, null, null, repliedToHeading));
        return summaryData;
    }

    private static StorySummaryData buildCommentSummary(String gender, List<SheriffMark> sheriffMarks) {
        StorySummaryData summaryData = new StorySummaryData();
        summaryData.setComment(new StorySummaryEntry(null, null, gender, null, null, sheriffMarks));
        return summaryData;
    }

    public void deleted(String nodeName, String postingId, String commentId, String commentOwnerName) {
        if (commentOwnerName.equals(nodeName())) {
            return;
        }

        List<Story> stories = storyRepository.findSubsByRemotePostingAndCommentId(nodeId(), StoryType.REPLY_COMMENT,
                nodeName, postingId, commentId);
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
        if (stories.isEmpty()) {
            storyRepository.delete(story);
            if (!isNew) {
                storyDeleted(story);
            }
            return;
        }

        story.setSummaryData(buildAddedSummary(story, stories));
        story.setRemoteCommentId(stories.get(stories.size() - 1).getRemoteCommentId());
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
        List<StorySummaryEntry> comments = new ArrayList<>();
        Story firstStory = stories.get(0);
        comments.add(new StorySummaryEntry(
                firstStory.getRemoteOwnerName(), firstStory.getRemoteOwnerFullName(),
                firstStory.getSummaryData().getComment().getOwnerGender(), null, null,
                firstStory.getSummaryData().getComment().getSheriffMarks()));
        if (stories.size() > 1) { // just for optimization
            var names = stories.stream().map(Story::getRemoteOwnerName).collect(Collectors.toSet());
            if (names.size() > 1) {
                Story secondStory = stories.stream()
                        .filter(t -> !t.getRemoteOwnerName().equals(firstStory.getRemoteOwnerName()))
                        .findFirst()
                        .orElse(null);
                if (secondStory != null) {
                    comments.add(new StorySummaryEntry(
                            secondStory.getRemoteOwnerName(), secondStory.getRemoteOwnerFullName(),
                            secondStory.getSummaryData().getComment().getOwnerGender(), null, null,
                            secondStory.getSummaryData().getComment().getSheriffMarks()));
                }
            }
            summaryData.setTotalComments(names.size());
        } else {
            summaryData.setTotalComments(1);
        }
        summaryData.setComments(comments);
        summaryData.setRepliedTo(new StorySummaryEntry(
                null, null, null, story.getSummaryData().getRepliedTo().getHeading()));
        summaryData.setPosting(new StorySummaryEntry(
                story.getRemotePostingNodeName(), story.getRemotePostingFullName(),
                story.getSummaryData().getPosting().getOwnerGender(),
                story.getSummaryData().getPosting().getHeading(), story.getSummaryData().getPosting().getSheriffs(),
                story.getSummaryData().getPosting().getSheriffMarks()));
        return summaryData;
    }

}
