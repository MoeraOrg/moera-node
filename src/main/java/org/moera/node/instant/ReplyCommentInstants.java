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
import org.moera.node.model.event.StoryAddedEvent;
import org.moera.node.model.event.StoryDeletedEvent;
import org.moera.node.model.event.StoryUpdatedEvent;
import org.moera.node.operations.StoryOperations;
import org.moera.node.util.Util;
import org.springframework.stereotype.Component;

@Component
public class ReplyCommentInstants extends InstantsCreator {

    private static final Duration GROUP_PERIOD = Duration.of(6, ChronoUnit.HOURS);

    @Inject
    private StoryRepository storyRepository;

    @Inject
    private StoryOperations storyOperations;

    public void added(String nodeName, String fullName, AvatarImage avatar, String postingId, String commentId,
                      String repliedToId, String commentOwnerName, String commentOwnerFullName,
                      AvatarImage commentOwnerAvatar, String postingHeading, String repliedToHeading) {
        if (commentOwnerName.equals(nodeName())) {
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
        if (story == null || story.getCreatedAt().toInstant().plus(GROUP_PERIOD).isBefore(Instant.now())) {
            isNewStory = true;
            story = new Story(UUID.randomUUID(), nodeId(), StoryType.REPLY_COMMENT);
            story.setFeedName(Feed.INSTANT);
            story.setRemoteNodeName(nodeName);
            story.setRemoteFullName(fullName);
            if (avatar != null) {
                story.setRemoteAvatarMediaFile(avatar.getMediaFile());
                story.setRemoteAvatarShape(avatar.getShape());
            }
            story.setRemotePostingId(postingId);
            story.setRemoteHeading(postingHeading);
            story.setRemoteRepliedToId(repliedToId);
            story.setRemoteRepliedToHeading(repliedToHeading);
            story.setMoment(0L);
            story = storyRepository.save(story);
        }

        Story substory = new Story(UUID.randomUUID(), nodeId(), StoryType.REPLY_COMMENT);
        substory.setRemoteNodeName(nodeName);
        substory.setRemoteFullName(fullName);
        if (avatar != null) {
            substory.setRemoteAvatarMediaFile(avatar.getMediaFile());
            substory.setRemoteAvatarShape(avatar.getShape());
        }
        substory.setRemotePostingId(postingId);
        substory.setRemoteCommentId(commentId);
        substory.setRemoteOwnerName(commentOwnerName);
        substory.setRemoteOwnerFullName(commentOwnerFullName);
        if (avatar != null) {
            substory.setRemoteOwnerAvatarMediaFile(commentOwnerAvatar.getMediaFile());
            substory.setRemoteOwnerAvatarShape(commentOwnerAvatar.getShape());
        }
        substory.setMoment(0L);
        substory = storyRepository.save(substory);
        story.addSubstory(substory);

        updated(story, isNewStory, true);
        feedStatusUpdated();
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
            webPushDeleted(story.getId());
            return;
        }

        story.setSummary(buildAddedSummary(story, stories));
        story.setRemoteCommentId(stories.get(0).getRemoteCommentId());
        story.setRemoteOwnerAvatarMediaFile(stories.get(0).getRemoteOwnerAvatarMediaFile());
        story.setRemoteOwnerAvatarShape(stories.get(0).getRemoteOwnerAvatarShape());
        story.setPublishedAt(Util.now());
        if (isAdded) {
            story.setRead(false);
            story.setViewed(false);
        }
        storyOperations.updateMoment(story);
        send(isNew ? new StoryAddedEvent(story, true) : new StoryUpdatedEvent(story, true));
        webPush(story);
    }

    private String buildAddedSummary(Story story, List<Story> stories) {
        StringBuilder buf = new StringBuilder();
        Story firstStory = stories.get(0);
        buf.append(formatNodeName(firstStory.getRemoteOwnerName(), firstStory.getRemoteOwnerFullName()));
        if (stories.size() > 1) { // just for optimization
            var names = stories.stream().map(Story::getRemoteOwnerName).collect(Collectors.toSet());
            if (names.size() > 1) {
                buf.append(names.size() == 2 ? " and " : ", ");
                Story secondStory = stories.stream()
                        .filter(t -> !t.getRemoteOwnerName().equals(firstStory.getRemoteOwnerName()))
                        .findFirst()
                        .orElse(null);
                if (secondStory != null) {
                    buf.append(formatNodeName(secondStory.getRemoteOwnerName(), secondStory.getRemoteOwnerFullName()));
                }
            }
            if (names.size() > 2) {
                buf.append(" and ");
                buf.append(names.size() - 2);
                buf.append(names.size() == 3 ? " other" : " others");
            }
        }
        buf.append(" replied to your comment \"");
        buf.append(story.getRemoteRepliedToHeading());
        buf.append("\" on ");
        if (Objects.equals(story.getRemoteNodeName(), nodeName())) {
            buf.append("your");
        } else {
            buf.append(stories.size() == 1 && Objects.equals(story.getRemoteNodeName(), firstStory.getRemoteOwnerName())
                    ? "their" : formatNodeName(story.getRemoteNodeName(), story.getRemoteFullName()));
        }
        buf.append(" post");
        return buf.toString();
    }

}
