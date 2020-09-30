package org.moera.node.instant;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import javax.inject.Inject;

import org.moera.node.data.Feed;
import org.moera.node.data.Story;
import org.moera.node.data.StoryRepository;
import org.moera.node.data.StoryType;
import org.moera.node.global.RequestContext;
import org.moera.node.model.event.StoryAddedEvent;
import org.moera.node.model.event.StoryDeletedEvent;
import org.moera.node.model.event.StoryUpdatedEvent;
import org.moera.node.operations.StoryOperations;
import org.moera.node.util.Util;
import org.springframework.stereotype.Component;

@Component
public class RemoteCommentInstants {

    private static final Duration GROUP_PERIOD = Duration.of(6, ChronoUnit.HOURS);

    @Inject
    private RequestContext requestContext;

    @Inject
    private StoryRepository storyRepository;

    @Inject
    private StoryOperations storyOperations;

    @Inject
    private InstantOperations instantOperations;

    public void added(String remoteNodeName, String remotePostingId, String remotePostingHeading,
                      String remoteOwnerName, String remoteCommentId, String remoteCommentHeading) {
        if (remoteOwnerName.equals(requestContext.nodeName())) {
            return;
        }

        boolean alreadyReported = !storyRepository.findSubsByRemotePostingAndCommentId(requestContext.nodeId(),
                StoryType.REMOTE_COMMENT_ADDED, remoteNodeName, remotePostingId, remoteCommentId).isEmpty();
        if (alreadyReported) {
            return;
        }

        boolean isNewStory = false;
        Story story = storyRepository.findFullByRemotePostingAndCommentId(requestContext.nodeId(), Feed.INSTANT,
                StoryType.REMOTE_COMMENT_ADDED, remoteNodeName, remotePostingId, remoteCommentId).stream()
                .findFirst().orElse(null);
        if (story == null || story.getCreatedAt().toInstant().plus(GROUP_PERIOD).isBefore(Instant.now())) {
            isNewStory = true;
            story = new Story(UUID.randomUUID(), requestContext.nodeId(), StoryType.REMOTE_COMMENT_ADDED);
            story.setFeedName(Feed.INSTANT);
            story.setRemoteNodeName(remoteNodeName);
            story.setRemotePostingId(remotePostingId);
            story.setRemoteHeading(remotePostingHeading);
            story.setMoment(0L);
            story = storyRepository.save(story);
        }

        Story substory = new Story(UUID.randomUUID(), requestContext.nodeId(), StoryType.REMOTE_COMMENT_ADDED);
        substory.setRemoteNodeName(remoteNodeName);
        substory.setRemotePostingId(remotePostingId);
        substory.setRemoteOwnerName(remoteOwnerName);
        substory.setRemoteCommentId(remoteCommentId);
        substory.setRemoteHeading(remoteCommentHeading);
        substory.setMoment(0L);
        substory = storyRepository.save(substory);
        story.addSubstory(substory);

        updated(story, isNewStory, true);
        instantOperations.feedStatusUpdated();
    }

    public void deleted(String remoteNodeName, String remotePostingId, String remoteOwnerName, String remoteCommentId) {
        if (remoteOwnerName.equals(requestContext.nodeName())) {
            return;
        }

        List<Story> stories = storyRepository.findSubsByRemotePostingAndCommentId(requestContext.nodeId(),
                StoryType.REMOTE_COMMENT_ADDED, remoteNodeName, remotePostingId, remoteCommentId);
        for (Story substory : stories) {
            Story story = substory.getParent();
            story.removeSubstory(substory);
            storyRepository.delete(substory);
            updated(story, false, false);
        }
        instantOperations.feedStatusUpdated();
    }

    private void updated(Story story, boolean isNew, boolean isAdded) {
        List<Story> stories = story.getSubstories().stream()
                .sorted(Comparator.comparing(Story::getCreatedAt).reversed())
                .collect(Collectors.toList());
        if (stories.size() == 0) {
            storyRepository.delete(story);
            if (!isNew) {
                requestContext.send(new StoryDeletedEvent(story, true));
            }
            return;
        }

        story.setSummary(buildAddedSummary(story, stories));
        story.setRemoteOwnerName(stories.get(0).getRemoteOwnerName());
        story.setRemoteCommentId(stories.get(0).getRemoteCommentId());
        story.setPublishedAt(Util.now());
        if (isAdded) {
            story.setRead(false);
            story.setViewed(false);
        }
        storyOperations.updateMoment(story);
        requestContext.send(isNew ? new StoryAddedEvent(story, true) : new StoryUpdatedEvent(story, true));
    }

    private static String buildAddedSummary(Story story, List<Story> stories) {
        StringBuilder buf = new StringBuilder();
        String firstName = stories.get(0).getRemoteOwnerName();
        buf.append(InstantUtil.formatNodeName(firstName));
        if (stories.size() > 1) { // just for optimization
            var names = stories.stream().map(Story::getRemoteNodeName).collect(Collectors.toSet());
            if (names.size() > 1) {
                buf.append(names.size() == 2 ? " and " : ", ");
                String secondName = stories.stream().map(Story::getRemoteNodeName).filter(nm -> !nm.equals(firstName))
                        .findFirst().orElse("");
                buf.append(InstantUtil.formatNodeName(secondName));
            }
            if (names.size() > 2) {
                buf.append(" and ");
                buf.append(names.size() - 2);
                buf.append(names.size() == 3 ? " other" : " others");
            }
        }
        buf.append(" commented on ");
        buf.append(InstantUtil.formatNodeName(story.getRemoteNodeName()));
        buf.append(" post \"");
        buf.append(Util.he(story.getRemoteHeading()));
        buf.append('"');
        return buf.toString();
    }

}
