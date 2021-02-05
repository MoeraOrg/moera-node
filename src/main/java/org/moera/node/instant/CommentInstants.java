package org.moera.node.instant;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import javax.inject.Inject;

import org.moera.node.data.Comment;
import org.moera.node.data.Feed;
import org.moera.node.data.Story;
import org.moera.node.data.StoryRepository;
import org.moera.node.data.StoryType;
import org.moera.node.model.CommentInfo;
import org.moera.node.model.PostingInfo;
import org.moera.node.model.event.StoryAddedEvent;
import org.moera.node.model.event.StoryDeletedEvent;
import org.moera.node.model.event.StoryUpdatedEvent;
import org.moera.node.util.Util;
import org.springframework.stereotype.Component;

@Component
public class CommentInstants extends InstantsCreator {

    private static final Duration GROUP_PERIOD = Duration.of(6, ChronoUnit.HOURS);

    @Inject
    private StoryRepository storyRepository;

    public void added(Comment comment) {
        if (comment.getOwnerName().equals(nodeName())
                // 'reply-comment' instant is expected to be created for such a comment
                || comment.getRepliedTo() != null && comment.getRepliedToName().equals(nodeName())) {
            return;
        }

        boolean alreadyReported = !storyRepository.findSubsByTypeAndEntryId(nodeId(), StoryType.COMMENT_ADDED,
                comment.getId()).isEmpty();
        if (alreadyReported) {
            return;
        }

        boolean isNewStory = false;
        Story story = storyRepository.findFullByFeedAndTypeAndEntryId(nodeId(), Feed.INSTANT,
                StoryType.COMMENT_ADDED, comment.getPosting().getId()).stream().findFirst().orElse(null);
        if (story == null || story.getCreatedAt().toInstant().plus(GROUP_PERIOD).isBefore(Instant.now())) {
            isNewStory = true;
            story = new Story(UUID.randomUUID(), nodeId(), StoryType.COMMENT_ADDED);
            story.setFeedName(Feed.INSTANT);
            story.setEntry(comment.getPosting());
            story.setMoment(0L);
            story = storyRepository.save(story);
        }

        Story substory = new Story(UUID.randomUUID(), nodeId(), StoryType.COMMENT_ADDED);
        substory.setEntry(comment);
        substory.setRemoteOwnerName(comment.getOwnerName());
        substory.setRemoteOwnerFullName(comment.getOwnerFullName());
        substory.setMoment(0L);
        substory = storyRepository.save(substory);
        story.addSubstory(substory);

        updated(story, isNewStory, true);
        feedStatusUpdated();
    }

    public void deleted(Comment comment) {
        if (comment.getOwnerName().equals(nodeName())) {
            return;
        }

        List<Story> stories = storyRepository.findSubsByTypeAndEntryId(nodeId(), StoryType.COMMENT_ADDED,
                comment.getId());
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
        story.setRemoteCommentId(stories.get(0).getEntry().getId().toString());
        story.setPublishedAt(Util.now());
        if (isAdded) {
            story.setRead(false);
            story.setViewed(false);
        }
        updateMoment(story);
        send(isNew ? new StoryAddedEvent(story, true) : new StoryUpdatedEvent(story, true));
        webPush(story);
    }

    private static String buildAddedSummary(Story story, List<Story> stories) {
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
        buf.append(" commented on your post \"");
        buf.append(Util.he(story.getEntry().getCurrentRevision().getHeading()));
        buf.append('"');
        return buf.toString();
    }

    public void addingFailed(String postingId, PostingInfo postingInfo) {
        String postingOwnerName = postingInfo != null ? postingInfo.getOwnerName() : "";
        String postingOwnerFullName = postingInfo != null ? postingInfo.getOwnerFullName() : null;
        String postingHeading = postingInfo != null ? postingInfo.getHeading() : "";

        Story story = new Story(UUID.randomUUID(), nodeId(), StoryType.POSTING_TASK_FAILED);
        story.setFeedName(Feed.INSTANT);
        story.setRemoteNodeName(postingOwnerName);
        story.setRemoteFullName(postingOwnerFullName);
        story.setRemotePostingId(postingId);
        story.setSummary(buildAddingFailedSummary(postingOwnerName, postingOwnerFullName, postingHeading));
        story.setPublishedAt(Util.now());
        updateMoment(story);
        story = storyRepository.save(story);
        send(new StoryAddedEvent(story, true));
        webPush(story);
        feedStatusUpdated();
    }

    public void updateFailed(String postingId, PostingInfo postingInfo, String commentId, CommentInfo commentInfo) {
        String postingOwnerName = postingInfo != null ? postingInfo.getOwnerName() : "";
        String postingOwnerFullName = postingInfo != null ? postingInfo.getOwnerFullName() : null;
        String postingHeading = postingInfo != null ? postingInfo.getHeading() : "";
        String commentHeading = commentInfo != null ? commentInfo.getHeading() : "";

        Story story = new Story(UUID.randomUUID(), nodeId(), StoryType.COMMENT_TASK_FAILED);
        story.setFeedName(Feed.INSTANT);
        story.setRemoteNodeName(postingOwnerName);
        story.setRemoteFullName(postingOwnerFullName);
        story.setRemotePostingId(postingId);
        story.setRemoteCommentId(commentId);
        story.setSummary(
                buildUpdateFailedSummary(postingOwnerName, postingOwnerFullName, postingHeading, commentHeading));
        story.setPublishedAt(Util.now());
        updateMoment(story);
        story = storyRepository.save(story);
        send(new StoryAddedEvent(story, true));
        webPush(story);
        feedStatusUpdated();
    }

    private static String buildAddingFailedSummary(String nodeName, String fullName, String postingHeading) {
        return String.format("Failed to add a comment to %s post \"%s\"",
                formatNodeName(nodeName, fullName), Util.he(postingHeading));
    }

    private static String buildUpdateFailedSummary(String nodeName, String fullName, String postingHeading,
                                                   String commentHeading) {
        return String.format("Failed to sign the comment \"%s\" to %s post \"%s\"",
                Util.he(commentHeading), formatNodeName(nodeName, fullName), Util.he(postingHeading));
    }

}
