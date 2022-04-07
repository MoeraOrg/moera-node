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
import org.moera.node.data.SubscriptionReason;
import org.moera.node.model.AvatarImage;
import org.moera.node.operations.StoryOperations;
import org.moera.node.util.Util;
import org.springframework.stereotype.Component;

@Component
public class RemoteCommentInstants extends InstantsCreator {

    private static final Duration GROUP_PERIOD = Duration.of(6, ChronoUnit.HOURS);

    @Inject
    private StoryRepository storyRepository;

    @Inject
    private StoryOperations storyOperations;

    public void added(String remoteNodeName, String remoteFullName, AvatarImage remoteAvatar, String remotePostingId,
                      String remotePostingHeading, String remoteOwnerName, String remoteOwnerFullName,
                      AvatarImage remoteOwnerAvatar, String remoteCommentId, String remoteCommentHeading,
                      SubscriptionReason reason) {
        if (remoteOwnerName.equals(nodeName())) {
            return;
        }

        boolean alreadyReported = !storyRepository.findSubsByRemotePostingAndCommentId(nodeId(),
                StoryType.REMOTE_COMMENT_ADDED, remoteNodeName, remotePostingId, remoteCommentId).isEmpty();
        if (alreadyReported) {
            return;
        }

        boolean isNewStory = false;
        Story story = storyRepository.findFullByRemotePostingId(nodeId(), Feed.INSTANT, StoryType.REMOTE_COMMENT_ADDED,
                remoteNodeName, remotePostingId).stream()
                .findFirst().orElse(null);
        if (story == null || story.getCreatedAt().toInstant().plus(GROUP_PERIOD).isBefore(Instant.now())) {
            isNewStory = true;
            story = new Story(UUID.randomUUID(), nodeId(), StoryType.REMOTE_COMMENT_ADDED);
            story.setFeedName(Feed.INSTANT);
            story.setRemoteNodeName(remoteNodeName);
            story.setRemoteFullName(remoteFullName);
            if (remoteAvatar != null) {
                story.setRemoteAvatarMediaFile(remoteAvatar.getMediaFile());
                story.setRemoteAvatarShape(remoteAvatar.getShape());
            }
            story.setRemotePostingId(remotePostingId);
            story.setRemoteHeading(remotePostingHeading);
            story.setMoment(0L);
            story = storyRepository.save(story);
        }

        Story substory = new Story(UUID.randomUUID(), nodeId(), StoryType.REMOTE_COMMENT_ADDED);
        substory.setRemoteNodeName(remoteNodeName);
        substory.setRemoteFullName(remoteFullName);
        if (remoteAvatar != null) {
            substory.setRemoteAvatarMediaFile(remoteAvatar.getMediaFile());
            substory.setRemoteAvatarShape(remoteAvatar.getShape());
        }
        substory.setRemotePostingId(remotePostingId);
        substory.setRemoteOwnerName(remoteOwnerName);
        substory.setRemoteOwnerFullName(remoteOwnerFullName);
        if (remoteOwnerAvatar != null) {
            substory.setRemoteOwnerAvatarMediaFile(remoteOwnerAvatar.getMediaFile());
            substory.setRemoteOwnerAvatarShape(remoteOwnerAvatar.getShape());
        }
        substory.setRemoteCommentId(remoteCommentId);
        substory.setRemoteHeading(remoteCommentHeading);
        substory.setMoment(0L);
        substory = storyRepository.save(substory);
        story.addSubstory(substory);

        updated(story, reason, isNewStory, true);
    }

    public void deleted(String remoteNodeName, String remotePostingId, String remoteOwnerName, String remoteCommentId,
                        SubscriptionReason reason) {
        if (remoteOwnerName.equals(nodeName())) {
            return;
        }

        List<Story> stories = storyRepository.findSubsByRemotePostingAndCommentId(nodeId(),
                StoryType.REMOTE_COMMENT_ADDED, remoteNodeName, remotePostingId, remoteCommentId);
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

        story.setSummary(buildAddedSummary(story, stories, reason));
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
        storyOperations.updateMoment(story);
        storyAddedOrUpdated(story, isNew);
    }

    private static String buildAddedSummary(Story story, List<Story> stories, SubscriptionReason reason) {
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
        buf.append(" commented on ");
        buf.append(stories.size() == 1 && Objects.equals(story.getRemoteNodeName(), firstStory.getRemoteOwnerName())
                ? "their" : formatNodeName(story.getRemoteNodeName(), story.getRemoteFullName()));
        buf.append(" post ");
        buf.append(getReasonForHuman(reason));
        buf.append(" \"");
        buf.append(Util.he(story.getRemoteHeading()));
        buf.append('"');
        return buf.toString();
    }

    private static String getReasonForHuman(SubscriptionReason reason) {
        switch (reason) {
            default:
            case USER:
                return "you subscribed to";
            case MENTION:
                return "you have been mentioned in";
            case COMMENT:
                return "you commented";
        }
    }

}
