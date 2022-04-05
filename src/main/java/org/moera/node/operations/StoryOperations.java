package org.moera.node.operations;

import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;
import javax.inject.Inject;

import org.moera.node.data.Entry;
import org.moera.node.data.Feed;
import org.moera.node.data.Story;
import org.moera.node.data.StoryRepository;
import org.moera.node.data.StoryType;
import org.moera.node.domain.Domains;
import org.moera.node.global.UniversalContext;
import org.moera.node.liberin.Liberin;
import org.moera.node.liberin.LiberinManager;
import org.moera.node.liberin.model.StoryAddedLiberin;
import org.moera.node.liberin.model.StoryDeletedLiberin;
import org.moera.node.model.FeedStatus;
import org.moera.node.model.StoryAttributes;
import org.moera.node.util.MomentFinder;
import org.moera.node.util.Transaction;
import org.moera.node.util.Util;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;

@Component
public class StoryOperations {

    private static final Timestamp PINNED_TIME = Util.toTimestamp(9000000000000L); // 9E+12

    @Inject
    private UniversalContext universalContext;

    @Inject
    private Domains domains;

    @Inject
    private StoryRepository storyRepository;

    @Inject
    private LiberinManager liberinManager;

    @Inject
    private PlatformTransactionManager txManager;

    private final MomentFinder momentFinder = new MomentFinder();

    public void updateMoment(Story story) {
        updateMoment(story, universalContext.nodeId());
    }

    public void updateMoment(Story story, UUID nodeId) {
        story.setMoment(momentFinder.find(
                moment -> storyRepository.countMoments(nodeId, story.getFeedName(), moment) == 0,
                !story.isPinned() ? story.getPublishedAt() : PINNED_TIME));
    }

    public void publish(Entry posting, List<StoryAttributes> publications) {
        publish(posting, publications, universalContext.nodeId(), universalContext::send);
    }

    public void publish(Entry posting, List<StoryAttributes> publications, UUID nodeId,
                        Consumer<Liberin> liberinSender) {
        if (publications == null) {
            return;
        }
        for (StoryAttributes publication : publications) {
            Story story = new Story(UUID.randomUUID(), nodeId, StoryType.POSTING_ADDED);
            story.setEntry(posting);
            story.setFeedName(Feed.TIMELINE);
            publication.toStory(story);
            updateMoment(story, nodeId);
            story = storyRepository.saveAndFlush(story);
            posting.addStory(story);

            liberinSender.accept(new StoryAddedLiberin(story).withNodeId(nodeId));
        }
    }

    public FeedStatus getFeedStatus(String feedName, boolean isAdmin) {
        return getFeedStatus(feedName, universalContext.nodeId(), isAdmin);
    }

    public FeedStatus getFeedStatus(String feedName, UUID nodeId, boolean isAdmin) {
        int total = storyRepository.countInFeed(nodeId, feedName);
        int totalPinned = storyRepository.countPinned(nodeId, feedName);

        if (isAdmin) {
            int notViewed = storyRepository.countNotViewed(nodeId, feedName);
            int notRead = storyRepository.countNotRead(nodeId, feedName);
            Long notViewedMoment = storyRepository.findNotViewedMoment(nodeId, feedName);

            return new FeedStatus(total, totalPinned, notViewed, notRead, notViewedMoment);
        } else {
            return new FeedStatus(total, totalPinned);
        }
    }

    public void unpublish(UUID entryId) {
        unpublish(entryId, universalContext.nodeId(), universalContext::send);
    }

    public void unpublish(UUID entryId, UUID nodeId, Consumer<Liberin> liberinSender) {
        storyRepository.findByEntryId(nodeId, entryId).stream()
                .filter(story -> story.getFeedName() != null)
                .forEach(story -> liberinSender.accept(new StoryDeletedLiberin(story)));
        storyRepository.deleteByEntryId(nodeId, entryId);
    }

    @Scheduled(fixedDelayString = "P1D")
    public void purgeExpired() throws Throwable {
        for (String domainName : domains.getAllDomainNames()) {
            UUID nodeId = domains.getDomainNodeId(domainName);
            purgeExpired(nodeId, domainName, Feed.INSTANT, "instants.lifetime", false, true);
            purgeExpired(nodeId, domainName, Feed.INSTANT, "instants.viewed.lifetime", true, true);
            purgeExpired(nodeId, domainName, Feed.NEWS, "news.lifetime", false,
                    domains.getDomainOptions(nodeId).getBool("news.purge-pinned"));
        }
    }

    private void purgeExpired(UUID nodeId, String domainName, String feedName, String optionName,
                              boolean viewed, boolean purgePinned) throws Throwable {
        Duration lifetime = domains.getDomainOptions(domainName).getDuration(optionName).getDuration();
        Timestamp createdBefore = Timestamp.from(Instant.now().minus(lifetime));
        List<Liberin> liberins = new ArrayList<>();
        Transaction.execute(txManager, () -> {
            List<Story> stories = viewed
                ? storyRepository.findExpiredViewed(nodeId, feedName, createdBefore)
                : storyRepository.findExpired(nodeId, feedName, createdBefore);
            stories.forEach(story -> {
                if (story.isPinned() && !purgePinned) {
                    return;
                }
                storyRepository.delete(story);
                liberins.add(new StoryDeletedLiberin(story).withNodeId(nodeId));
            });
            return null;
        });
        liberins.forEach(liberinManager::send);
    }

}
