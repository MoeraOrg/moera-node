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
import org.moera.node.data.Story;
import org.moera.node.data.StoryRepository;
import org.moera.node.data.StoryType;
import org.moera.node.model.AvatarImage;
import org.moera.node.model.PostingInfo;
import org.moera.node.operations.StoryOperations;
import org.moera.node.util.Util;
import org.springframework.stereotype.Component;

@Component
public class PostingMediaReactionInstants extends InstantsCreator {

    private static final Duration GROUP_PERIOD = Duration.of(6, ChronoUnit.HOURS);

    @Inject
    private StoryRepository storyRepository;

    @Inject
    private StoryOperations storyOperations;

    public void added(String nodeName, String fullName, AvatarImage avatar, String postingId, String parentPostingId,
                      String parentMediaId, String ownerName, String ownerFullName, AvatarImage ownerAvatar,
                      String postingHeading, boolean negative, int emoji) {
        if (ownerName.equals(nodeName())) {
            return;
        }

        StoryType storyType = negative ? StoryType.POSTING_MEDIA_REACTION_ADDED_NEGATIVE
                : StoryType.POSTING_MEDIA_REACTION_ADDED_POSITIVE;

        boolean isNewStory = false;
        Story story = storyRepository.findFullByRemotePostingId(
                        nodeId(), Feed.INSTANT, storyType, nodeName, postingId).stream()
                .findFirst().orElse(null);
        if (story == null || story.getCreatedAt().toInstant().plus(GROUP_PERIOD).isBefore(Instant.now())) {
            isNewStory = true;
            story = new Story(UUID.randomUUID(), nodeId(), storyType);
            story.setFeedName(Feed.INSTANT);
            story.setRemoteNodeName(nodeName);
            story.setRemoteFullName(fullName);
            if (avatar != null) {
                story.setRemoteAvatarMediaFile(avatar.getMediaFile());
                story.setRemoteAvatarShape(avatar.getShape());
            }
            story.setRemotePostingId(postingId);
            story.setRemoteHeading(postingHeading);
            story.setRemoteParentPostingId(parentPostingId);
            story.setRemoteParentMediaId(parentMediaId);
            story.setMoment(0L);
            story = storyRepository.save(story);
        }

        Story substory = new Story(UUID.randomUUID(), nodeId(), storyType);
        story.setRemoteNodeName(nodeName);
        story.setRemoteFullName(fullName);
        if (avatar != null) {
            story.setRemoteAvatarMediaFile(avatar.getMediaFile());
            story.setRemoteAvatarShape(avatar.getShape());
        }
        story.setRemotePostingId(postingId);
        substory.setRemoteOwnerName(ownerName);
        substory.setRemoteOwnerFullName(ownerFullName);
        if (ownerAvatar != null) {
            substory.setRemoteOwnerAvatarMediaFile(ownerAvatar.getMediaFile());
            substory.setRemoteOwnerAvatarShape(ownerAvatar.getShape());
        }
        substory.setSummary(buildSummary(ownerName, ownerFullName, emoji));
        substory.setMoment(0L);
        substory = storyRepository.save(substory);
        story.addSubstory(substory);

        updated(story, isNewStory, true);
    }

    private static String buildSummary(String ownerName, String ownerFullName, int emoji) {
        return String.valueOf(Character.toChars(emoji)) + ' ' + formatNodeName(ownerName, ownerFullName);
    }

    public void deleted(String nodeName, String postingId, String ownerName, boolean negative) {
        if (ownerName.equals(nodeName())) {
            return;
        }

        StoryType storyType = negative ? StoryType.POSTING_MEDIA_REACTION_ADDED_NEGATIVE
                : StoryType.POSTING_MEDIA_REACTION_ADDED_POSITIVE;
        List<Story> stories = storyRepository.findFullByRemotePostingId(
                nodeId(), Feed.INSTANT, storyType, nodeName, postingId);
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
        storyRepository.deleteByRemotePostingId(nodeId(), Feed.INSTANT,
                StoryType.POSTING_MEDIA_REACTION_ADDED_POSITIVE, nodeName, postingId);
        storyRepository.deleteByRemotePostingId(nodeId(), Feed.INSTANT,
                StoryType.POSTING_MEDIA_REACTION_ADDED_NEGATIVE, nodeName, postingId);
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
        story.setRemoteOwnerName(stories.get(0).getRemoteOwnerName());
        story.setRemoteOwnerFullName(stories.get(0).getRemoteOwnerFullName());
        story.setRemoteOwnerAvatarMediaFile(stories.get(0).getRemoteOwnerAvatarMediaFile());
        story.setRemoteOwnerAvatarShape(stories.get(0).getRemoteOwnerAvatarShape());
        story.setPublishedAt(Util.now());
        if (isAdded) {
            story.setRead(false);
            story.setViewed(false);
        }
        storyOperations.updateMoment(story);
        storyAddedOrUpdated(story, isNew);
    }

    private String buildAddedSummary(Story story, List<Story> stories) {
        StringBuilder buf = new StringBuilder();
        String firstName = stories.get(0).getRemoteOwnerName();
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
        buf.append(story.getStoryType() == StoryType.POSTING_MEDIA_REACTION_ADDED_POSITIVE ? " supported" : " opposed");
        buf.append(" a media in your post \"");
        buf.append(Util.he(story.getRemoteHeading()));
        buf.append("\" in ");
        if (Objects.equals(story.getRemoteNodeName(), nodeName())) {
            buf.append("your");
        } else if (stories.size() == 1 && Objects.equals(story.getRemoteNodeName(), firstName)) {
            buf.append("their");
        } else {
            buf.append(formatNodeName(story.getRemoteNodeName(), story.getRemoteFullName()));
        }
        buf.append(" blog");
        return buf.toString();
    }

    public void addingFailed(String postingId, String parentPostingId, String parentMediaId,
                             PostingInfo parentPostingInfo) {
        String parentOwnerName = parentPostingInfo != null ? parentPostingInfo.getOwnerName() : "";
        String parentOwnerFullName = parentPostingInfo != null ? parentPostingInfo.getOwnerFullName() : null;
        AvatarImage parentOwnerAvatar = parentPostingInfo != null ? parentPostingInfo.getOwnerAvatar() : null;
        String parentHeading = parentPostingInfo != null ? parentPostingInfo.getHeading() : "";

        Story story = new Story(UUID.randomUUID(), nodeId(), StoryType.POSTING_MEDIA_REACTION_FAILED);
        story.setFeedName(Feed.INSTANT);
        story.setRemoteNodeName(parentOwnerName);
        story.setRemoteFullName(parentOwnerFullName);
        if (parentOwnerAvatar != null) {
            story.setRemoteAvatarMediaFile(parentOwnerAvatar.getMediaFile());
            story.setRemoteAvatarShape(parentOwnerAvatar.getShape());
        }
        story.setRemotePostingId(postingId);
        story.setRemoteParentPostingId(parentPostingId);
        story.setRemoteParentMediaId(parentMediaId);
        story.setSummary(buildAddingFailedSummary(parentOwnerName, parentOwnerFullName, parentHeading));
        story.setPublishedAt(Util.now());
        updateMoment(story);
        story = storyRepository.save(story);
        storyAdded(story);
    }

    private static String buildAddingFailedSummary(String nodeName, String fullName, String postingHeading) {
        return String.format("Failed to sign a reaction to a media in %s post \"%s\"",
                formatNodeName(nodeName, fullName), Util.he(postingHeading));
    }

}
