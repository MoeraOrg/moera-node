package org.moera.node.rest;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import javax.inject.Inject;

import org.moera.node.data.Feed;
import org.moera.node.data.Posting;
import org.moera.node.data.Reaction;
import org.moera.node.data.Story;
import org.moera.node.data.StoryRepository;
import org.moera.node.data.StoryType;
import org.moera.node.event.model.FeedStatusUpdatedEvent;
import org.moera.node.event.model.StoryAddedEvent;
import org.moera.node.event.model.StoryDeletedEvent;
import org.moera.node.event.model.StoryUpdatedEvent;
import org.moera.node.global.RequestContext;
import org.moera.node.util.Util;
import org.springframework.stereotype.Component;

@Component
public class InstantOperations {

    private static final Duration REACTION_GROUP_PERIOD = Duration.of(6, ChronoUnit.HOURS);

    @Inject
    private RequestContext requestContext;

    @Inject
    private StoryRepository storyRepository;

    @Inject
    private StoryOperations storyOperations;

    public void reactionAdded(Posting posting, Reaction reaction) {
        if (reaction.getOwnerName().equals(requestContext.getClientName())) {
            return;
        }

        StoryType storyType = reaction.isNegative()
                ? StoryType.REACTION_ADDED_NEGATIVE : StoryType.REACTION_ADDED_POSITIVE;
        boolean isNewStory = false;
        Story story = storyRepository.findByFeedAndTypeAndEntryId(
                requestContext.nodeId(), Feed.INSTANT, storyType, posting.getId());
        if (story == null || story.getCreatedAt().toInstant().plus(REACTION_GROUP_PERIOD).isBefore(Instant.now())) {
            isNewStory = true;
            story = new Story(UUID.randomUUID(), requestContext.nodeId(), storyType, posting);
            story.setFeedName(Feed.INSTANT);
            story.setMoment(0L);
            story = storyRepository.save(story);
        }
        story.getReactions().add(reaction);
        reactionsUpdated(story, isNewStory);
        feedStatusUpdated();
    }

    public void reactionDeleted(Reaction reaction) {
        StoryType storyType = reaction.isNegative()
                ? StoryType.REACTION_ADDED_NEGATIVE : StoryType.REACTION_ADDED_POSITIVE;
        reaction.getStories().stream()
                .filter(t -> t.getStoryType() == storyType)
                .forEach(t -> reactionsUpdated(t, false));
        feedStatusUpdated();
    }

    public void reactionsDeletedAll(UUID postingId) {
        storyRepository.deleteByFeedAndTypeAndEntryId(
                requestContext.nodeId(), Feed.INSTANT, StoryType.REACTION_ADDED_POSITIVE, postingId);
        storyRepository.deleteByFeedAndTypeAndEntryId(
                requestContext.nodeId(), Feed.INSTANT, StoryType.REACTION_ADDED_NEGATIVE, postingId);
        feedStatusUpdated();
    }

    private void reactionsUpdated(Story story, boolean isNew) {
        List<Reaction> reactions = story.getReactions().stream()
                .filter(r -> r.getDeletedAt() == null)
                .sorted(Comparator.comparing(Reaction::getCreatedAt))
                .collect(Collectors.toList());
        if (reactions.size() == 0) {
            storyRepository.delete(story);
            if (!isNew) {
                requestContext.send(new StoryDeletedEvent(story, true));
            }
            return;
        }

        story.setSummary(buildReactionAddedSummary(story, reactions));
        story.setPublishedAt(Util.now());
        storyOperations.updateMoment(story);
        requestContext.send(isNew ? new StoryAddedEvent(story, true) : new StoryUpdatedEvent(story, true));
    }

    private String buildReactionAddedSummary(Story story, List<Reaction> reactions) {
        StringBuilder buf = new StringBuilder();
        buf.append("<b>");
        buf.append(reactions.get(0).getOwnerName());
        buf.append("</b>");
        if (reactions.size() > 1) {
            buf.append(reactions.size() == 2 ? " and" : ",");
            buf.append(" <b>");
            buf.append(reactions.get(1).getOwnerName());
            buf.append("</b>");
        }
        if (reactions.size() > 2) {
            buf.append(" and ");
            buf.append(reactions.size() - 2);
            buf.append(reactions.size() == 3 ? " other" : " others");
        }
        buf.append(story.getStoryType() == StoryType.REACTION_ADDED_POSITIVE ? " supported" : " opposed");
        buf.append(" your post \"");
        buf.append(Util.he(story.getEntry().getCurrentRevision().getHeading()));
        buf.append('"');
        return buf.toString();
    }

    public void mentionPostingAdded(String remoteNodeName, String remotePostingId, String remotePostingHeading) {
        Story story = storyRepository.findByRemoteEntryId(
                requestContext.nodeId(), Feed.INSTANT, StoryType.MENTION_POSTING, remoteNodeName, remotePostingId);
        if (story != null) {
            return;
        }
        story = new Story(UUID.randomUUID(), requestContext.nodeId(), StoryType.MENTION_POSTING, null);
        story.setFeedName(Feed.INSTANT);
        story.setRemoteNodeName(remoteNodeName);
        story.setRemoteEntryId(remotePostingId);
        story.setSummary(buildMentionPostingSummary(story, remotePostingHeading));
        storyOperations.updateMoment(story);
        story = storyRepository.saveAndFlush(story);
        requestContext.send(new StoryAddedEvent(story, true));
        feedStatusUpdated();
    }

    public void mentionPostingDeleted(String remoteNodeName, String remotePostingId) {
        Story story = storyRepository.findByRemoteEntryId(
                requestContext.nodeId(), Feed.INSTANT, StoryType.MENTION_POSTING, remoteNodeName, remotePostingId);
        if (story == null) {
            return;
        }
        storyRepository.delete(story);
        requestContext.send(new StoryDeletedEvent(story, true));
        feedStatusUpdated();
    }

    private String buildMentionPostingSummary(Story story, String remotePostingHeading) {
        return String.format("<b>%s</b> mentioned you in a post \"%s\"",
                story.getRemoteNodeName(), Util.he(remotePostingHeading));
    }

    private void feedStatusUpdated() {
        requestContext.send(new FeedStatusUpdatedEvent(Feed.INSTANT, storyOperations.getFeedStatus(Feed.INSTANT)));
    }

}
