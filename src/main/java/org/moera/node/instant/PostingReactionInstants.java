package org.moera.node.instant;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;
import javax.inject.Inject;

import org.moera.node.data.Feed;
import org.moera.node.data.Posting;
import org.moera.node.data.Story;
import org.moera.node.data.StoryRepository;
import org.moera.node.data.StoryType;
import org.moera.node.model.AvatarImage;
import org.moera.node.model.PostingInfo;
import org.moera.node.operations.StoryOperations;
import org.moera.node.util.Util;
import org.springframework.stereotype.Component;

@Component
public class PostingReactionInstants extends InstantsCreator {

    private static final Duration GROUP_PERIOD = Duration.of(6, ChronoUnit.HOURS);

    @Inject
    private StoryRepository storyRepository;

    @Inject
    private StoryOperations storyOperations;

    public void added(Posting posting, String ownerName, String ownerFullName, AvatarImage ownerAvatar,
                      boolean negative, int emoji) {
        if (ownerName.equals(nodeName())) {
            return;
        }

        StoryType storyType = negative ? StoryType.REACTION_ADDED_NEGATIVE : StoryType.REACTION_ADDED_POSITIVE;

        boolean isNewStory = false;
        Story story = storyRepository.findFullByFeedAndTypeAndEntryId(
                nodeId(), Feed.INSTANT, storyType, posting.getId()).stream().findFirst().orElse(null);
        if (story == null || story.getCreatedAt().toInstant().plus(GROUP_PERIOD).isBefore(Instant.now())) {
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
        substory.setSummary(buildSubstorySummary(ownerName, ownerFullName, emoji));
        substory.setMoment(0L);
        substory = storyRepository.save(substory);
        story.addSubstory(substory);

        updated(story, isNewStory, true);
    }

    private static String buildSubstorySummary(String ownerName, String ownerFullName, int emoji) {
        return String.valueOf(Character.toChars(emoji)) + ' ' + formatNodeName(ownerName, ownerFullName);
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
        if (stories.size() == 0) {
            storyRepository.delete(story);
            if (!isNew) {
                storyDeleted(story);
            }
            return;
        }

        story.setSummary(buildAddedSummary(story, stories));
        story.setRemoteOwnerAvatarMediaFile(stories.get(0).getRemoteOwnerAvatarMediaFile());
        story.setRemoteOwnerAvatarShape(stories.get(0).getRemoteOwnerAvatarShape());
        story.setRemoteOwnerName(stories.get(0).getRemoteOwnerName());
        story.setPublishedAt(Util.now());
        if (isAdded) {
            story.setRead(false);
            story.setViewed(false);
        }
        storyOperations.updateMoment(story);
        storyAddedOrUpdated(story, isNew);
    }

    private static String buildAddedSummary(Story story, List<Story> stories) {
        StringBuilder buf = new StringBuilder();
        buf.append(stories.get(0).getSummary());
        if (stories.size() > 1) {
            buf.append(stories.size() == 2 ? " and " : ", ");
            buf.append(stories.get(1).getSummary());
        }
        if (stories.size() > 2) {
            buf.append(" and ");
            buf.append(stories.size() - 2);
            buf.append(stories.size() == 3 ? " other" : " others");
        }
        buf.append(story.getStoryType() == StoryType.REACTION_ADDED_POSITIVE ? " supported" : " opposed");
        buf.append(" your post \"");
        buf.append(Util.he(story.getEntry().getCurrentRevision().getHeading()));
        buf.append('"');
        return buf.toString();
    }

    public void addingFailed(String postingId, PostingInfo postingInfo) {
        String postingOwnerName = postingInfo != null ? postingInfo.getOwnerName() : "";
        String postingOwnerFullName = postingInfo != null ? postingInfo.getOwnerFullName() : null;
        AvatarImage postingOwnerAvatar = postingInfo != null ? postingInfo.getOwnerAvatar() : null;
        String postingHeading = postingInfo != null ? postingInfo.getHeading() : "";

        Story story = new Story(UUID.randomUUID(), nodeId(), StoryType.POSTING_REACTION_TASK_FAILED);
        story.setFeedName(Feed.INSTANT);
        story.setRemoteNodeName(postingOwnerName);
        story.setRemoteFullName(postingOwnerFullName);
        if (postingOwnerAvatar != null) {
            story.setRemoteOwnerAvatarMediaFile(postingOwnerAvatar.getMediaFile());
            story.setRemoteOwnerAvatarShape(postingOwnerAvatar.getShape());
        }
        story.setRemotePostingId(postingId);
        story.setSummary(buildAddingFailedSummary(postingOwnerName, postingOwnerFullName, postingHeading));
        story.setPublishedAt(Util.now());
        updateMoment(story);
        story = storyRepository.save(story);
        storyAdded(story);
    }

    private static String buildAddingFailedSummary(String nodeName, String fullName, String postingHeading) {
        return String.format("Failed to sign a reaction to %s post \"%s\"",
                formatNodeName(nodeName, fullName), Util.he(postingHeading));
    }

}
