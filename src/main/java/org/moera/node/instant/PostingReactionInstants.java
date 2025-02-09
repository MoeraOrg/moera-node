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

import org.moera.node.data.Feed;
import org.moera.node.data.Posting;
import org.moera.node.data.Story;
import org.moera.node.data.StoryRepository;
import org.moera.node.data.StoryType;
import org.moera.node.model.AvatarImage;
import org.moera.node.model.PostingInfo;
import org.moera.node.model.StorySummaryData;
import org.moera.node.model.StorySummaryEntry;
import org.moera.node.model.StorySummaryReaction;
import org.moera.node.util.Util;
import org.springframework.stereotype.Component;

@Component
public class PostingReactionInstants extends InstantsCreator {

    private static final Duration GROUP_PERIOD = Duration.of(6, ChronoUnit.HOURS);

    @Inject
    private StoryRepository storyRepository;

    public void added(Posting posting, String ownerName, String ownerFullName, String ownerGender,
                      AvatarImage ownerAvatar, boolean negative, int emoji) {
        if (ownerName.equals(nodeName())) {
            return;
        }

        StoryType storyType = negative ? StoryType.REACTION_ADDED_NEGATIVE : StoryType.REACTION_ADDED_POSITIVE;

        if (isBlocked(storyType, posting.getId(), null, null, ownerName)) {
            return;
        }

        boolean isNewStory = false;
        Story story = storyRepository.findFullByFeedAndTypeAndEntryId(
                nodeId(), Feed.INSTANT, storyType, posting.getId()).stream().findFirst().orElse(null);
        if (story == null
                || story.isViewed() && story.getCreatedAt().toInstant().plus(GROUP_PERIOD).isBefore(Instant.now())) {
            isNewStory = true;
            story = new Story(UUID.randomUUID(), nodeId(), storyType);
            story.setFeedName(Feed.INSTANT);
            story.setEntry(posting);
            story.setMoment(0L);
            story = storyRepository.save(story);
        }

        Story substory = new Story(UUID.randomUUID(), nodeId(), storyType);
        substory.setEntry(posting);
        substory.setRemoteOwnerName(ownerName);
        substory.setRemoteOwnerFullName(ownerFullName);
        if (ownerAvatar != null) {
            substory.setRemoteOwnerAvatarMediaFile(ownerAvatar.getMediaFile());
            substory.setRemoteOwnerAvatarShape(ownerAvatar.getShape());
        }
        substory.setSummaryData(buildReactionSummary(ownerGender, emoji));
        substory.setMoment(0L);
        substory = storyRepository.save(substory);
        story.addSubstory(substory);

        updated(story, isNewStory, true);
    }

    private static StorySummaryData buildReactionSummary(String gender, int emoji) {
        StorySummaryData summaryData = new StorySummaryData();
        summaryData.setReaction(new StorySummaryReaction(null, null, gender, emoji));
        return summaryData;
    }

    public void deleted(UUID postingId, String ownerName, boolean negative) {
        if (ownerName.equals(nodeName())) {
            return;
        }

        StoryType storyType = negative ? StoryType.REACTION_ADDED_NEGATIVE : StoryType.REACTION_ADDED_POSITIVE;
        List<Story> stories = storyRepository.findFullByFeedAndTypeAndEntryId(
                nodeId(), Feed.INSTANT, storyType, postingId);
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

    public void deletedAll(UUID postingId) {
        storyRepository.deleteByFeedAndTypeAndEntryId(
                nodeId(), Feed.INSTANT, StoryType.REACTION_ADDED_POSITIVE, postingId);
        storyRepository.deleteByFeedAndTypeAndEntryId(
                nodeId(), Feed.INSTANT, StoryType.REACTION_ADDED_NEGATIVE, postingId);
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
        summaryData.setPosting(new StorySummaryEntry(
                story.getRemotePostingNodeName(), story.getRemotePostingFullName(), story.getEntry().getOwnerGender(),
                story.getEntry().getCurrentRevision().getHeading()));
        return summaryData;
    }

    public void addingFailed(String nodeName, String postingId, PostingInfo postingInfo) {
        if (isBlocked(StoryType.POSTING_REACTION_TASK_FAILED, null, nodeName, postingId)) {
            return;
        }

        String postingOwnerName = postingInfo != null ? postingInfo.getOwnerName() : "";
        String postingOwnerFullName = postingInfo != null ? postingInfo.getOwnerFullName() : null;
        String postingOwnerGender = postingInfo != null ? postingInfo.getOwnerGender() : null;
        AvatarImage postingOwnerAvatar = postingInfo != null ? postingInfo.getOwnerAvatar() : null;
        String postingHeading = postingInfo != null ? postingInfo.getHeading() : "";

        Story story = new Story(UUID.randomUUID(), nodeId(), StoryType.POSTING_REACTION_TASK_FAILED);
        story.setFeedName(Feed.INSTANT);
        story.setRemoteNodeName(nodeName);
        story.setRemotePostingNodeName(postingOwnerName);
        story.setRemotePostingFullName(postingOwnerFullName);
        if (postingOwnerAvatar != null) {
            story.setRemotePostingAvatarMediaFile(postingOwnerAvatar.getMediaFile());
            story.setRemotePostingAvatarShape(postingOwnerAvatar.getShape());
        }
        story.setRemotePostingId(postingId);
        story.setSummaryData(buildAddingFailedSummary(
                postingOwnerName, postingOwnerFullName, postingOwnerGender, postingHeading));
        story.setPublishedAt(Util.now());
        updateMoment(story);
        story = storyRepository.save(story);
        storyAdded(story);
    }

    private static StorySummaryData buildAddingFailedSummary(String nodeName, String fullName, String gender,
                                                             String postingHeading) {
        StorySummaryData summaryData = new StorySummaryData();
        summaryData.setPosting(new StorySummaryEntry(nodeName, fullName, gender, postingHeading));
        return summaryData;
    }

}
