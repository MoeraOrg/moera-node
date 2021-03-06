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
import org.moera.node.model.CommentInfo;
import org.moera.node.model.PostingInfo;
import org.moera.node.model.event.StoryAddedEvent;
import org.moera.node.model.event.StoryDeletedEvent;
import org.moera.node.model.event.StoryUpdatedEvent;
import org.moera.node.operations.StoryOperations;
import org.moera.node.util.Util;
import org.springframework.stereotype.Component;

@Component
public class CommentReactionInstants extends InstantsCreator {

    private static final Duration GROUP_PERIOD = Duration.of(6, ChronoUnit.HOURS);

    @Inject
    private StoryRepository storyRepository;

    @Inject
    private StoryOperations storyOperations;

    public void added(String nodeName, String fullName, AvatarImage avatar, String postingId, String commentId,
                      String ownerName, String ownerFullName, AvatarImage ownerAvatar, String commentHeading,
                      boolean negative, int emoji) {
        if (ownerName.equals(nodeName())) {
            return;
        }

        StoryType storyType = negative ? StoryType.COMMENT_REACTION_ADDED_NEGATIVE
                : StoryType.COMMENT_REACTION_ADDED_POSITIVE;

        boolean isNewStory = false;
        Story story = storyRepository.findFullByRemotePostingAndCommentId(
                nodeId(), Feed.INSTANT, storyType, nodeName, postingId, commentId).stream()
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
            story.setRemoteCommentId(commentId);
            story.setRemoteHeading(commentHeading);
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
        story.setRemoteCommentId(commentId);
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
        feedStatusUpdated();
    }

    public void deleted(String nodeName, String postingId, String commentId, String ownerName, boolean negative) {
        if (ownerName.equals(nodeName())) {
            return;
        }

        StoryType storyType = negative ? StoryType.COMMENT_REACTION_ADDED_NEGATIVE
                : StoryType.COMMENT_REACTION_ADDED_POSITIVE;
        List<Story> stories = storyRepository.findFullByRemotePostingAndCommentId(
                nodeId(), Feed.INSTANT, storyType, nodeName, postingId, commentId);
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
        feedStatusUpdated();
    }

    public void deletedAll(String nodeName, String postingId, String commentId) {
        storyRepository.deleteByRemotePostingAndCommentId(nodeId(), Feed.INSTANT,
                StoryType.COMMENT_REACTION_ADDED_POSITIVE, nodeName, postingId, commentId);
        storyRepository.deleteByRemotePostingAndCommentId(nodeId(), Feed.INSTANT,
                StoryType.COMMENT_REACTION_ADDED_NEGATIVE, nodeName, postingId, commentId);
        feedStatusUpdated();
    }

    private void updated(Story story, boolean isNew, boolean isAdded) {
        List<Story> stories = story.getSubstories().stream()
                .sorted(Comparator.comparing(Story::getCreatedAt).reversed())
                .collect(Collectors.toList());
        if (stories.size() == 0) {
            storyRepository.delete(story);
            if (!isNew) {
                send(new StoryDeletedEvent(story, true));
            }
            deletePush(story.getId());
            return;
        }

        story.setSummary(buildAddedSummary(story, stories));
        story.setRemoteOwnerAvatarMediaFile(stories.get(0).getRemoteOwnerAvatarMediaFile());
        story.setRemoteOwnerAvatarShape(stories.get(0).getRemoteOwnerAvatarShape());
        story.setPublishedAt(Util.now());
        if (isAdded) {
            story.setRead(false);
            story.setViewed(false);
        }
        storyOperations.updateMoment(story);
        send(isNew ? new StoryAddedEvent(story, true) : new StoryUpdatedEvent(story, true));
        sendPush(story);
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
        buf.append(story.getStoryType() == StoryType.COMMENT_REACTION_ADDED_POSITIVE ? " supported" : " opposed");
        buf.append(" your comment \"");
        buf.append(Util.he(story.getRemoteHeading()));
        buf.append("\" on ");
        if (Objects.equals(story.getRemoteNodeName(), nodeName())) {
            buf.append("your");
        } else if (stories.size() == 1 && Objects.equals(story.getRemoteNodeName(), firstName)) {
            buf.append("their");
        } else {
            buf.append(formatNodeName(story.getRemoteNodeName(), story.getRemoteFullName()));
        }
        buf.append(" post");
        return buf.toString();
    }

    private static String buildSummary(String ownerName, String ownerFullName, int emoji) {
        return String.valueOf(Character.toChars(emoji)) + ' ' + formatNodeName(ownerName, ownerFullName);
    }

    public void addingFailed(String postingId, PostingInfo postingInfo, String commentId, CommentInfo commentInfo) {
        String postingOwnerName = postingInfo != null ? postingInfo.getOwnerName() : "";
        String postingOwnerFullName = postingInfo != null ? postingInfo.getOwnerFullName() : null;
        AvatarImage postingOwnerAvatar = postingInfo != null ? postingInfo.getOwnerAvatar() : null;
        String postingHeading = postingInfo != null ? postingInfo.getHeading() : "";
        String commentOwnerName = commentInfo != null ? commentInfo.getOwnerName() : "";
        String commentOwnerFullName = commentInfo != null ? commentInfo.getOwnerFullName() : null;
        AvatarImage commentOwnerAvatar = commentInfo != null ? commentInfo.getOwnerAvatar() : null;
        String commentHeading = commentInfo != null ? commentInfo.getHeading() : "";

        Story story = new Story(UUID.randomUUID(), nodeId(), StoryType.COMMENT_TASK_FAILED);
        story.setFeedName(Feed.INSTANT);
        story.setRemoteNodeName(postingOwnerName);
        story.setRemoteFullName(postingOwnerFullName);
        if (postingOwnerAvatar != null) {
            story.setRemoteAvatarMediaFile(postingOwnerAvatar.getMediaFile());
            story.setRemoteAvatarShape(postingOwnerAvatar.getShape());
        }
        story.setRemotePostingId(postingId);
        story.setRemoteOwnerName(commentOwnerName);
        story.setRemoteOwnerFullName(commentOwnerFullName);
        if (commentOwnerAvatar != null) {
            story.setRemoteOwnerAvatarMediaFile(commentOwnerAvatar.getMediaFile());
            story.setRemoteOwnerAvatarShape(commentOwnerAvatar.getShape());
        }
        story.setRemoteCommentId(commentId);
        story.setSummary(buildAddingFailedSummary(postingOwnerName, postingOwnerFullName, postingHeading,
                commentOwnerName, commentOwnerFullName, commentHeading));
        story.setPublishedAt(Util.now());
        updateMoment(story);
        story = storyRepository.save(story);
        send(new StoryAddedEvent(story, true));
        sendPush(story);
        feedStatusUpdated();
    }

    private static String buildAddingFailedSummary(String postingOwnerName, String postingOwnerFullName,
                                                   String postingHeading,
                                                   String commentOwnerName, String commentOwnerFullName,
                                                   String commentHeading) {
        return String.format("Failed to sign a reaction to %s comment \"%s\" to %s post \"%s\"",
                formatNodeName(commentOwnerName, commentOwnerFullName), Util.he(commentHeading),
                formatNodeName(postingOwnerName, postingOwnerFullName), Util.he(postingHeading));
    }

}
