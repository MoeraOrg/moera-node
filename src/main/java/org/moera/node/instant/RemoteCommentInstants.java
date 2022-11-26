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
import org.moera.node.data.Story;
import org.moera.node.data.StoryRepository;
import org.moera.node.data.StoryType;
import org.moera.node.data.SubscriptionReason;
import org.moera.node.model.AvatarImage;
import org.moera.node.model.StorySummaryData;
import org.moera.node.model.StorySummaryEntry;
import org.moera.node.util.Util;
import org.springframework.stereotype.Component;

@Component
public class RemoteCommentInstants extends InstantsCreator {

    private static final Duration GROUP_PERIOD = Duration.of(6, ChronoUnit.HOURS);

    @Inject
    private StoryRepository storyRepository;

    public void added(String nodeName, String postingOwnerName, String postingOwnerFullName, String postingOwnerGender,
                      AvatarImage postingOwnerAvatar, String postingId, String postingHeading, String commentOwnerName,
                      String commentOwnerFullName, String commentOwnerGender, AvatarImage commentOwnerAvatar,
                      String commentId, String commentHeading, SubscriptionReason reason) {
        if (commentOwnerName.equals(nodeName())) {
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
        if (story == null || story.getCreatedAt().toInstant().plus(GROUP_PERIOD).isBefore(Instant.now())) {
            isNewStory = true;
            story = new Story(UUID.randomUUID(), nodeId(), StoryType.REMOTE_COMMENT_ADDED);
            story.setFeedName(Feed.INSTANT);
            story.setRemoteNodeName(nodeName);
            story.setRemotePostingNodeName(postingOwnerName);
            story.setRemotePostingFullName(postingOwnerFullName);
            if (postingOwnerAvatar != null) {
                story.setRemotePostingAvatarMediaFile(postingOwnerAvatar.getMediaFile());
                story.setRemotePostingAvatarShape(postingOwnerAvatar.getShape());
            }
            story.setRemotePostingId(postingId);
            story.setRemoteHeading(postingHeading);
            story.setSummaryData(buildPostingSummary(postingOwnerGender, postingHeading));
            story.setMoment(0L);
            story = storyRepository.save(story);
        }

        Story substory = new Story(UUID.randomUUID(), nodeId(), StoryType.REMOTE_COMMENT_ADDED);
        substory.setRemoteNodeName(nodeName);
        substory.setRemotePostingId(postingId);
        substory.setRemoteOwnerName(commentOwnerName);
        substory.setRemoteOwnerFullName(commentOwnerFullName);
        if (commentOwnerAvatar != null) {
            substory.setRemoteOwnerAvatarMediaFile(commentOwnerAvatar.getMediaFile());
            substory.setRemoteOwnerAvatarShape(commentOwnerAvatar.getShape());
        }
        substory.setRemoteCommentId(commentId);
        substory.setRemoteHeading(commentHeading);
        substory.setSummaryData(buildCommentSummary(commentOwnerGender, commentHeading));
        substory.setMoment(0L);
        substory = storyRepository.save(substory);
        story.addSubstory(substory);

        updated(story, reason, isNewStory, true);
    }

    private static StorySummaryData buildPostingSummary(String gender, String heading) {
        StorySummaryData summaryData = new StorySummaryData();
        summaryData.setPosting(new StorySummaryEntry(null, null, gender, heading));
        return summaryData;
    }

    private static StorySummaryData buildCommentSummary(String gender, String heading) {
        StorySummaryData summaryData = new StorySummaryData();
        summaryData.setComment(new StorySummaryEntry(null, null, gender, heading));
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
        if (stories.size() == 0) {
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
        story.setRemoteCommentId(stories.get(0).getRemoteCommentId());
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
        comments.add(new StorySummaryEntry(
                firstStory.getRemoteOwnerName(), firstStory.getRemoteOwnerFullName(),
                firstStory.getSummaryData().getComment().getOwnerGender(), firstStory.getRemoteHeading()));
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
                            secondStory.getSummaryData().getComment().getOwnerGender(), secondStory.getRemoteHeading()));
                }
            }
            summaryData.setTotalComments(names.size());
        } else {
            summaryData.setTotalComments(1);
        }
        summaryData.setComments(comments);
        summaryData.setPosting(new StorySummaryEntry(
                story.getRemotePostingNodeName(), story.getRemotePostingFullName(),
                story.getSummaryData().getPosting().getOwnerGender(), story.getRemoteHeading()));
        summaryData.setSubscriptionReason(reason);
        return summaryData;
    }

}
