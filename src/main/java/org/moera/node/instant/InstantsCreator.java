package org.moera.node.instant;

import java.util.List;
import java.util.UUID;
import javax.inject.Inject;

import org.moera.node.data.BlockedOperation;
import org.moera.node.data.Feed;
import org.moera.node.data.Story;
import org.moera.node.data.StoryRepository;
import org.moera.node.data.StoryType;
import org.moera.node.global.UniversalContext;
import org.moera.node.liberin.model.FeedStatusUpdatedLiberin;
import org.moera.node.liberin.model.StoryAddedLiberin;
import org.moera.node.liberin.model.StoryDeletedLiberin;
import org.moera.node.liberin.model.StoryUpdatedLiberin;
import org.moera.node.model.FeedStatus;
import org.moera.node.operations.BlockedInstantOperations;
import org.moera.node.operations.BlockedUserOperations;
import org.moera.node.operations.StoryOperations;
import org.moera.node.util.Util;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.util.ObjectUtils;

public class InstantsCreator {

    @Inject
    protected UniversalContext universalContext;

    @Inject
    private StoryRepository storyRepository;

    @Inject
    private StoryOperations storyOperations;

    @Inject
    private BlockedInstantOperations blockedInstantOperations;

    @Inject
    private BlockedUserOperations blockedUserOperations;

    protected UUID nodeId() {
        return universalContext.nodeId();
    }

    protected String nodeName() {
        return universalContext.nodeName();
    }

    protected boolean isBlocked(StoryType storyType, UUID entryId, String remoteNodeName, String remotePostingId,
                                String remoteOwnerName) {
        boolean blocked = false;
        if (!ObjectUtils.isEmpty(remoteOwnerName)) {
            blocked |= blockedUserOperations.isBlocked(
                    nodeId(), new BlockedOperation[]{BlockedOperation.INSTANT}, remoteOwnerName, entryId,
                    remoteNodeName, remotePostingId);
        }
        blocked |= blockedInstantOperations.count(
                nodeId(), storyType, entryId, remoteNodeName, remotePostingId, remoteOwnerName) > 0;
        return blocked;
    }

    protected boolean isBlocked(StoryType storyType, UUID entryId, String remoteNodeName, String remotePostingId) {
        return isBlocked(storyType, entryId, remoteNodeName, remotePostingId, null);
    }

    protected boolean isBlocked(StoryType storyType, UUID entryId, String remoteNodeName) {
        return isBlocked(storyType, entryId, remoteNodeName, null, null);
    }

    protected boolean isBlocked(StoryType storyType, UUID entryId) {
        return isBlocked(storyType, entryId, null, null, null);
    }

    protected boolean isBlocked(StoryType storyType) {
        return isBlocked(storyType, null, null, null, null);
    }

    protected void storyAdded(Story story) {
        universalContext.send(new StoryAddedLiberin(story));
    }

    protected void storyUpdated(Story story) {
        universalContext.send(new StoryUpdatedLiberin(story));
    }

    protected void storyAddedOrUpdated(Story story, boolean isAdded) {
        if (isAdded) {
            storyAdded(story);
        } else {
            storyUpdated(story);
        }
    }

    protected void storyDeleted(Story story) {
        universalContext.send(new StoryDeletedLiberin(story));
    }

    protected void feedUpdated() {
        FeedStatus feedStatus = storyOperations.getFeedStatus(Feed.INSTANT, true);
        universalContext.send(new FeedStatusUpdatedLiberin(Feed.INSTANT, feedStatus));
    }

    protected void updateMoment(Story story) {
        storyOperations.updateMoment(story, nodeId(), findFeedPosition(story));
    }

    private long findFeedPosition(Story story) {
        long momentBase = Util.toEpochSecond(story.getPublishedAt()) * 1000;

        boolean top = true;
        PageRequest page = PageRequest.of(0, 20, Sort.Direction.DESC, "moment");
        List<Story> stories = storyRepository.findByFeed(nodeId(), story.getFeedName(), page);
        while (!stories.isEmpty()) {
            for (Story prev : stories) {
                if (prev.getId().equals(story.getId())) {
                    continue;
                }
                if (prev.isViewed() || prev.getStoryType().getPriority() <= story.getStoryType().getPriority()) {
                    return top ? momentBase : prev.getMoment();
                }
                top = false;
                momentBase = prev.getMoment() - 1000;
            }
            page = page.next();
            stories = storyRepository.findByFeed(nodeId(), story.getFeedName(), page);
        }
        return momentBase;
    }

}
