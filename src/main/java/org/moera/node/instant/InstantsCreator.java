package org.moera.node.instant;

import java.util.List;
import java.util.UUID;
import jakarta.inject.Inject;

import org.moera.lib.node.types.BlockedOperation;
import org.moera.lib.node.types.StoryType;
import org.moera.lib.util.LogUtil;
import org.moera.node.data.Feed;
import org.moera.node.data.Story;
import org.moera.node.data.StoryRepository;
import org.moera.node.global.UniversalContext;
import org.moera.node.liberin.model.FeedStatusUpdatedLiberin;
import org.moera.node.liberin.model.StoryAddedLiberin;
import org.moera.node.liberin.model.StoryDeletedLiberin;
import org.moera.node.liberin.model.StoryUpdatedLiberin;
import org.moera.node.model.FeedStatus;
import org.moera.node.model.StoryTypeUtil;
import org.moera.node.operations.BlockedInstantOperations;
import org.moera.node.operations.BlockedUserOperations;
import org.moera.node.operations.StoryOperations;
import org.moera.node.util.SafeInteger;
import org.moera.node.util.Util;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.util.Pair;
import org.springframework.util.ObjectUtils;

public class InstantsCreator {

    private static final Logger log = LoggerFactory.getLogger(InstantsCreator.class);

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

    protected boolean isBlocked(
        StoryType storyType, UUID entryId, String remoteNodeName, String remotePostingId, String remoteOwnerName
    ) {
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
        if (universalContext.getOptions().getBool("instants.prioritize")) {
            log.debug("Finding position for story {}, node {}",
                    LogUtil.format(story.getId()), LogUtil.format(story.getNodeId()));
            Pair<Long, Long> feedPosition = findFeedPosition(story);
            storyOperations.updateMoment(story, nodeId(), feedPosition.getFirst(), feedPosition.getSecond());
            log.debug("Final position is {}", story.getMoment());
        } else {
            storyOperations.updateMoment(story, nodeId());
        }
    }

    private Pair<Long, Long> findFeedPosition(Story story) {
        long momentBase = Util.toEpochSecond(story.getPublishedAt()) * 1000;
        log.debug("Moment base is {}", momentBase);

        boolean top = true;
        PageRequest page = PageRequest.of(0, 20, Sort.Direction.DESC, "moment");
        List<Story> stories = storyRepository.findByFeed(nodeId(), story.getFeedName(), page);
        log.debug("Fetched {} stories", stories.size());
        while (!stories.isEmpty()) {
            for (Story prev : stories) {
                log.debug(
                    "Probing story {}, type {}, priority {}, viewed {}",
                    LogUtil.format(prev.getId()), LogUtil.format(prev.getStoryType().getValue()),
                    StoryTypeUtil.priority(prev.getStoryType()), LogUtil.format(prev.isViewed())
                );
                if (prev.getId().equals(story.getId())) {
                    log.debug("Skipping ourselves");
                    continue;
                }
                if (
                    prev.isViewed()
                    || StoryTypeUtil.priority(prev.getStoryType()) <= StoryTypeUtil.priority(story.getStoryType())
                ) {
                    log.debug(
                        "Found position: {}..{}",
                        top ? momentBase : prev.getMoment(),
                        top ? SafeInteger.MAX_VALUE : momentBase
                    );
                    return top ? Pair.of(momentBase, SafeInteger.MAX_VALUE) : Pair.of(prev.getMoment(), momentBase);
                }
                top = false;
                momentBase = prev.getMoment();
                log.debug("Next moment base is {}", momentBase);
            }
            page = page.next();
            stories = storyRepository.findByFeed(nodeId(), story.getFeedName(), page);
            log.debug("Fetched next {} stories", stories.size());
        }
        log.debug("Found nothing, placing at {}..{}", momentBase - 2000, momentBase);
        return Pair.of(momentBase - 2000, momentBase);
    }

}
