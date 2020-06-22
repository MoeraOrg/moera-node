package org.moera.node.operations;

import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import javax.inject.Inject;
import javax.transaction.Transactional;

import org.moera.node.data.Feed;
import org.moera.node.data.Posting;
import org.moera.node.data.Reaction;
import org.moera.node.data.Story;
import org.moera.node.data.StoryRepository;
import org.moera.node.data.StoryType;
import org.moera.node.data.Subscriber;
import org.moera.node.domain.Domains;
import org.moera.node.event.EventManager;
import org.moera.node.model.event.FeedStatusUpdatedEvent;
import org.moera.node.model.event.StoryAddedEvent;
import org.moera.node.model.event.StoryDeletedEvent;
import org.moera.node.model.event.StoryUpdatedEvent;
import org.moera.node.global.RequestContext;
import org.moera.node.naming.NodeName;
import org.moera.node.naming.RegisteredName;
import org.moera.node.util.Util;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class InstantOperations {

    private static final Duration REACTION_GROUP_PERIOD = Duration.of(6, ChronoUnit.HOURS);

    @Inject
    private RequestContext requestContext;

    @Inject
    private Domains domains;

    @Inject
    private StoryRepository storyRepository;

    @Inject
    private StoryOperations storyOperations;

    @Inject
    private EventManager eventManager;

    public void reactionAdded(Posting posting, Reaction reaction) {
        if (reaction.getOwnerName().equals(requestContext.nodeName())) {
            return;
        }

        StoryType storyType = reaction.isNegative()
                ? StoryType.REACTION_ADDED_NEGATIVE : StoryType.REACTION_ADDED_POSITIVE;
        boolean isNewStory = false;
        Story story = storyRepository.findByFeedAndTypeAndEntryId(
                requestContext.nodeId(), Feed.INSTANT, storyType, posting.getId()).stream().findFirst().orElse(null);
        if (story == null || story.getCreatedAt().toInstant().plus(REACTION_GROUP_PERIOD).isBefore(Instant.now())) {
            isNewStory = true;
            story = new Story(UUID.randomUUID(), requestContext.nodeId(), storyType, posting);
            story.setFeedName(Feed.INSTANT);
            story.setMoment(0L);
            story = storyRepository.save(story);
        }
        story.addReaction(reaction);
        reactionsUpdated(story, isNewStory, true);
        feedStatusUpdated();
    }

    public void reactionDeleted(Reaction reaction) {
        if (reaction.getOwnerName().equals(requestContext.nodeName())) {
            return;
        }

        StoryType storyType = reaction.isNegative()
                ? StoryType.REACTION_ADDED_NEGATIVE : StoryType.REACTION_ADDED_POSITIVE;
        reaction.getStories().stream()
                .filter(t -> t.getStoryType() == storyType)
                .forEach(t -> reactionsUpdated(t, false, false));
        feedStatusUpdated();
    }

    public void reactionsDeletedAll(UUID postingId) {
        storyRepository.deleteByFeedAndTypeAndEntryId(
                requestContext.nodeId(), Feed.INSTANT, StoryType.REACTION_ADDED_POSITIVE, postingId);
        storyRepository.deleteByFeedAndTypeAndEntryId(
                requestContext.nodeId(), Feed.INSTANT, StoryType.REACTION_ADDED_NEGATIVE, postingId);
        feedStatusUpdated();
    }

    private void reactionsUpdated(Story story, boolean isNew, boolean isAdded) {
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
        if (isAdded) {
            story.setRead(false);
            story.setViewed(false);
        }
        storyOperations.updateMoment(story);
        requestContext.send(isNew ? new StoryAddedEvent(story, true) : new StoryUpdatedEvent(story, true));
    }

    private String buildReactionAddedSummary(Story story, List<Reaction> reactions) {
        StringBuilder buf = new StringBuilder();
        appendReaction(buf, reactions.get(0));
        if (reactions.size() > 1) {
            buf.append(reactions.size() == 2 ? " and " : ", ");
            appendReaction(buf, reactions.get(1));
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

    private static void appendReaction(StringBuilder buf, Reaction reaction) {
        buf.append(Character.toChars(reaction.getEmoji()));
        buf.append(' ');
        buf.append(formatNodeName(reaction.getOwnerName()));
    }

    public void mentionPostingAdded(String remoteNodeName, String remotePostingId, String remotePostingHeading) {
        Story story = findMentionPostingStory(remoteNodeName, remotePostingId);
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
        Story story = findMentionPostingStory(remoteNodeName, remotePostingId);
        if (story == null) {
            return;
        }
        storyRepository.delete(story);
        requestContext.send(new StoryDeletedEvent(story, true));
        feedStatusUpdated();
    }

    private Story findMentionPostingStory(String remoteNodeName, String remotePostingId) {
        return storyRepository.findByRemoteEntryId(requestContext.nodeId(), Feed.INSTANT, StoryType.MENTION_POSTING,
                remoteNodeName, remotePostingId).stream().findFirst().orElse(null);
    }

    private String buildMentionPostingSummary(Story story, String remotePostingHeading) {
        return String.format("<b>%s</b> mentioned you in a post \"%s\"",
                formatNodeName(story.getRemoteNodeName()), Util.he(remotePostingHeading));
    }

    public void subscriberAdded(Subscriber subscriber) {
        Story story = findSubscriberDeletedStory(subscriber.getRemoteNodeName());
        if (story != null && !story.isRead()) {
            storyRepository.delete(story);
            requestContext.send(new StoryDeletedEvent(story, true));
        }

        story = new Story(UUID.randomUUID(), requestContext.nodeId(), StoryType.SUBSCRIBER_ADDED, null);
        story.setFeedName(Feed.INSTANT);
        story.setRemoteNodeName(subscriber.getRemoteNodeName());
        story.setSummary(buildSubscriberAddedSummary(subscriber));
        storyOperations.updateMoment(story);
        story = storyRepository.saveAndFlush(story);
        requestContext.send(new StoryAddedEvent(story, true));
        feedStatusUpdated();
    }

    private String buildSubscriberAddedSummary(Subscriber subscriber) {
        return String.format("%s subscribed to your %s",
                formatNodeName(subscriber.getRemoteNodeName()),
                Feed.getStandard(subscriber.getFeedName()).getTitle());
    }

    public void subscriberDeleted(Subscriber subscriber) {
        Story story = findSubscriberAddedStory(subscriber.getRemoteNodeName());
        if (story != null && !story.isRead()) {
            storyRepository.delete(story);
            requestContext.send(new StoryDeletedEvent(story, true));
        }

        story = new Story(UUID.randomUUID(), requestContext.nodeId(), StoryType.SUBSCRIBER_DELETED, null);
        story.setFeedName(Feed.INSTANT);
        story.setRemoteNodeName(subscriber.getRemoteNodeName());
        story.setSummary(buildSubscriberDeletedSummary(subscriber));
        storyOperations.updateMoment(story);
        story = storyRepository.saveAndFlush(story);
        requestContext.send(new StoryAddedEvent(story, true));
        feedStatusUpdated();
    }

    private String buildSubscriberDeletedSummary(Subscriber subscriber) {
        return String.format("%s unsubscribed from your %s",
                formatNodeName(subscriber.getRemoteNodeName()),
                Feed.getStandard(subscriber.getFeedName()).getTitle());
    }

    private Story findSubscriberAddedStory(String remoteNodeName) {
        return storyRepository.findByRemoteNodeName(requestContext.nodeId(), Feed.INSTANT, StoryType.SUBSCRIBER_ADDED,
                remoteNodeName).stream().findFirst().orElse(null);
    }

    private Story findSubscriberDeletedStory(String remoteNodeName) {
        return storyRepository.findByRemoteNodeName(requestContext.nodeId(), Feed.INSTANT, StoryType.SUBSCRIBER_DELETED,
                remoteNodeName).stream().findFirst().orElse(null);
    }

    private static String formatNodeName(String name) {
        NodeName nodeName = NodeName.parse(name);
        if (nodeName instanceof RegisteredName) {
            RegisteredName registeredName = (RegisteredName) nodeName;
            if (registeredName.getGeneration() != null) {
                return String.format("<span class=\"node-name\">%s<span class=\"generation\">%d</span></span>",
                        registeredName.getName(), registeredName.getGeneration());
            }
        }
        return String.format("<span class=\"node-name\">%s</span>", name);
    }

    private void feedStatusUpdated() {
        requestContext.send(new FeedStatusUpdatedEvent(Feed.INSTANT, storyOperations.getFeedStatus(Feed.INSTANT)));
    }

    @Scheduled(fixedDelayString = "P1D")
    @Transactional
    public void purgeExpired() {
        for (String domainName : domains.getAllDomainNames()) {
            UUID nodeId = domains.getDomainNodeId(domainName);
            Duration lifetime = domains.getDomainOptions(domainName).getDuration("instants.lifetime");
            Timestamp createdBefore = Timestamp.from(Instant.now().minus(lifetime));
            storyRepository.findExpired(nodeId, "instants", createdBefore).forEach(story -> {
                storyRepository.delete(story);
                eventManager.send(nodeId, new StoryDeletedEvent(story, true));
            });
        }
    }

}
