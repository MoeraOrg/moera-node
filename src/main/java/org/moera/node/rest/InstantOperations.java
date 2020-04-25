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
        Story story = storyRepository.findByFeedAndTypeAndEntryId(
                requestContext.nodeId(), Feed.INSTANT, storyType, posting.getId());
        if (story == null || story.getCreatedAt().toInstant().plus(REACTION_GROUP_PERIOD).isBefore(Instant.now())) {
            story = new Story(UUID.randomUUID(), requestContext.nodeId(), storyType, posting);
            story.setFeedName(Feed.INSTANT);
            story.setMoment(0L);
            story = storyRepository.save(story);
        }
        story.getReactions().add(reaction);
        reactionsUpdated(story);
    }

    public void reactionDeleted(Reaction reaction) {
        StoryType storyType = reaction.isNegative()
                ? StoryType.REACTION_ADDED_NEGATIVE : StoryType.REACTION_ADDED_POSITIVE;
        reaction.getStories().stream()
                .filter(t -> t.getStoryType() == storyType)
                .forEach(this::reactionsUpdated);
    }

    public void reactionsDeletedAll(UUID postingId) {
        storyRepository.deleteByFeedAndTypeAndEntryId(
                requestContext.nodeId(), Feed.INSTANT, StoryType.REACTION_ADDED_POSITIVE, postingId);
        storyRepository.deleteByFeedAndTypeAndEntryId(
                requestContext.nodeId(), Feed.INSTANT, StoryType.REACTION_ADDED_NEGATIVE, postingId);
    }

    private void reactionsUpdated(Story story) {
        List<Reaction> reactions = story.getReactions().stream()
                .filter(r -> r.getDeletedAt() == null)
                .sorted(Comparator.comparing(Reaction::getCreatedAt))
                .collect(Collectors.toList());
        if (reactions.size() == 0) {
            storyRepository.delete(story);
            return;
        }

        story.setSummary(buildReactionAddedSummary(story, reactions));
        story.setPublishedAt(Util.now());
        storyOperations.updateMoment(story);
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

}
