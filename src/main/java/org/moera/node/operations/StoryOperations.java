package org.moera.node.operations;

import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import jakarta.persistence.PersistenceContext;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.jpa.impl.JPAQueryFactory;
import org.moera.lib.node.types.FeedStatus;
import org.moera.lib.node.types.Scope;
import org.moera.lib.node.types.StoryAttributes;
import org.moera.lib.node.types.StoryType;
import org.moera.lib.node.types.principal.Principal;
import org.moera.lib.node.types.principal.PrincipalFilter;
import org.moera.node.auth.AuthenticationException;
import org.moera.node.data.Entry;
import org.moera.node.data.EntryType;
import org.moera.node.data.Feed;
import org.moera.node.data.MediaFileOwnerRepository;
import org.moera.node.data.Posting;
import org.moera.node.data.PostingRepository;
import org.moera.node.data.QEntry;
import org.moera.node.data.QStory;
import org.moera.node.data.Story;
import org.moera.node.data.StoryRepository;
import org.moera.node.domain.Domains;
import org.moera.node.global.RequestCounter;
import org.moera.node.global.UniversalContext;
import org.moera.node.liberin.Liberin;
import org.moera.node.liberin.LiberinManager;
import org.moera.node.liberin.model.PostingPublishedAtUpdatedLiberin;
import org.moera.node.liberin.model.StoryAddedLiberin;
import org.moera.node.liberin.model.StoryDeletedLiberin;
import org.moera.node.liberin.model.StoryUpdatedLiberin;
import org.moera.node.model.FeedStatusUtil;
import org.moera.node.model.StoryAttributesUtil;
import org.moera.node.userlist.SheriffUserListOperations;
import org.moera.node.util.MomentFinder;
import org.moera.node.util.SafeInteger;
import org.moera.node.util.Transaction;
import org.moera.node.util.Util;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.util.ObjectUtils;

@Component
public class StoryOperations {

    private static final Logger log = LoggerFactory.getLogger(StoryOperations.class);

    private static final int DELETE_BATCH_SIZE = 500;

    private static final Timestamp PINNED_TIME = Util.toTimestamp(9000000000000L); // 9E+12

    @Inject
    private RequestCounter requestCounter;

    @Inject
    private UniversalContext universalContext;

    @Inject
    private Domains domains;

    @Inject
    private StoryRepository storyRepository;

    @Inject
    private PostingRepository postingRepository;

    @Inject
    @Lazy
    private PostingOperations postingOperations;

    @Inject
    private MediaFileOwnerRepository mediaFileOwnerRepository;

    @Inject
    private SheriffUserListOperations sheriffUserListOperations;

    @Inject
    @Lazy
    private LiberinManager liberinManager;

    @Inject
    private Transaction tx;

    @Inject
    @PersistenceContext
    private EntityManager entityManager;

    private final MomentFinder momentFinder = new MomentFinder();

    public static Long findEarliestPublishedAt(Collection<Story> stories) {
        if (ObjectUtils.isEmpty(stories)) {
            return null;
        }
        return stories.stream()
            .filter(story -> Feed.TIMELINE.equals(story.getFeedName()))
            .map(Story::getPublishedAt)
            .map(Util::toEpochSecond)
            .min(Long::compareTo)
            .orElse(null);
    }

    public void updateMoment(Story story, UUID nodeId) {
        story.setMoment(momentFinder.find(
            moment -> storyRepository.countMoments(nodeId, story.getFeedName(), moment) == 0,
            !story.isPinned() ? story.getPublishedAt() : PINNED_TIME
        ));
    }

    public void updateMoment(Story story, UUID nodeId, long momentBase, long momentLimit) {
        story.setMoment(momentFinder.find(
            moment -> storyRepository.countMoments(nodeId, story.getFeedName(), moment) == 0,
            momentBase,
            momentLimit
        ));
    }

    public void publish(Entry entry, List<StoryAttributes> publications) {
        if (ObjectUtils.isEmpty(publications)) {
            return;
        }

        boolean publishingInTimeline = entry.getEntryType() == EntryType.POSTING
            && publications.stream().anyMatch(p -> p.getFeedName() == null || Feed.TIMELINE.equals(p.getFeedName()));
        List<Story> postingStories = publishingInTimeline
            ? findPostingPublications(universalContext.nodeId(), entry)
            : List.of();
        Long earliestPublishedAt = findEarliestPublishedAt(postingStories);

        for (StoryAttributes publication : publications) {
            Story story = new Story(UUID.randomUUID(), universalContext.nodeId(), StoryType.POSTING_ADDED);
            story.setEntry(entry);
            if (entry.getEntryType() == EntryType.POSTING && !entry.isOriginal()) {
                story.setRemoteNodeName(entry.getReceiverName());
                story.setRemotePostingId(entry.getReceiverEntryId());
            }
            story.setFeedName(Feed.TIMELINE);
            StoryAttributesUtil.toStory(publication, story);
            updateMoment(story, universalContext.nodeId());
            story = storyRepository.saveAndFlush(story);
            entry.addStory(story);
            if (publishingInTimeline) {
                postingStories.add(story);
            }

            sheriffUserListOperations.sheriffListReference(story);

            universalContext.send(new StoryAddedLiberin(story));
        }

        notifyPostingPublishedAtChanged(entry, earliestPublishedAt, postingStories);
    }

    public void publish(Entry entry, List<StoryAttributes> publications, boolean allowMultiple) {
        var newPublications = publications.stream().filter(p ->
            p.getFeedName() != null
            && (
                allowMultiple
                || storyRepository.countByFeedAndTypeAndEntryId(
                    universalContext.nodeId(), p.getFeedName(), StoryType.POSTING_ADDED, entry.getId()
                ) <= 0
            )
        ).toList();

        if (!newPublications.isEmpty()) {
            publish(entry, newPublications);
        }
    }

    public void update(Story story, StoryAttributes attributes) {
        List<Story> publications = attributes.getPublishAt() != null ? findPostingPublications(story) : List.of();
        Long earliestPublishedAt = findEarliestPublishedAt(publications);
        Principal viewPrincipal = story.getViewPrincipal();
        PrincipalFilter previousViewFilter = story.viewPrincipalFilter();
        StoryAttributesUtil.toStory(attributes, story);
        if (attributes.getPublishAt() != null || attributes.getPinned() != null) {
            updateMoment(story, story.getNodeId());
        }
        notifyPostingPublishedAtChanged(story.getEntry(), earliestPublishedAt, publications);
        universalContext.send(new StoryUpdatedLiberin(
            story.getId(), !viewPrincipal.equals(story.getViewPrincipal()) ? previousViewFilter : null
        ));
    }

    public FeedStatus getFeedStatus(String feedName, boolean isAdmin) {
        return getFeedStatus(feedName, universalContext.nodeId(), isAdmin);
    }

    public FeedStatus getFeedStatus(String feedName, UUID nodeId, boolean isAdmin) {
        int total = storyRepository.countInFeed(nodeId, feedName);
        int totalPinned = storyRepository.countPinned(nodeId, feedName);
        Long lastMoment = storyRepository.findLastMoment(nodeId, feedName);

        if (isAdmin) {
            int notViewed = storyRepository.countNotViewed(nodeId, feedName);
            int notRead = storyRepository.countNotRead(nodeId, feedName);
            Long notViewedMoment = storyRepository.findNotViewedMoment(nodeId, feedName);
            notViewedMoment = notViewedMoment != null ? notViewedMoment : SafeInteger.MAX_VALUE;
            Long notReadMoment = storyRepository.findNotReadMoment(nodeId, feedName);
            notReadMoment = notReadMoment != null ? notReadMoment : SafeInteger.MAX_VALUE;

            return FeedStatusUtil.build(
                total, totalPinned, lastMoment, notViewed, notRead, notViewedMoment, notReadMoment
            );
        } else {
            return FeedStatusUtil.build(total, totalPinned, lastMoment);
        }
    }

    public void unpublish(UUID entryId) {
        storyRepository.findByEntryId(universalContext.nodeId(), entryId).stream()
            .filter(story -> story.getFeedName() != null)
            .peek(story -> universalContext.send(new StoryDeletedLiberin(story, true)))
            .forEach(storyRepository::delete);
    }

    public void delete(Story story) {
        List<Story> publications = findPostingPublications(story);
        Long earliestPublishedAt = findEarliestPublishedAt(publications);
        storyRepository.delete(story);
        List<Story> remaining = publications.stream()
            .filter(publication -> !publication.getId().equals(story.getId()))
            .toList();
        notifyPostingPublishedAtChanged(story.getEntry(), earliestPublishedAt, remaining);
    }

    private List<Story> findPostingPublications(Story story) {
        if (
            Feed.TIMELINE.equals(story.getFeedName())
            && story.getEntry() != null
            && story.getEntry().getEntryType() == EntryType.POSTING
            && story.getParent() == null
        ) {
            return findPostingPublications(story.getNodeId(), story.getEntry());
        }
        return List.of();
    }

    private List<Story> findPostingPublications(UUID nodeId, Entry entry) {
        entityManager.lock(entry, LockModeType.PESSIMISTIC_WRITE);
        List<Story> stories = new ArrayList<>(storyRepository.findByEntryId(nodeId, entry.getId()));
        stories.forEach(entityManager::refresh);
        return stories;
    }

    private void notifyPostingPublishedAtChanged(
        Entry posting, Long previousPublishedAt, Collection<Story> publications
    ) {
        if (!Objects.equals(previousPublishedAt, findEarliestPublishedAt(publications))) {
            universalContext.send(new PostingPublishedAtUpdatedLiberin(posting.getId()));
        }
    }

    public void deleteSingle(Story story) {
        Entry entry = story.getEntry();
        boolean hasPosting = entry != null && entry.getEntryType() == EntryType.POSTING;
        if (hasPosting) {
            entityManager.lock(entry, LockModeType.PESSIMISTIC_WRITE);
        }

        delete(story);

        if (hasPosting) {
            List<Posting> postings = postingRepository.findUnlinked(universalContext.nodeId(), List.of(entry.getId()));
            postings.forEach(this::checkPostingDeletionAllowed);
            postingOperations.deletePostings(postings);
        }
    }

    public void deleteMultiple(
        String feedName,
        StoryType type,
        String receiver,
        String receiverid,
        UUID postingId,
        Boolean recommended
    ) {
        boolean singlePosting = postingId != null || !ObjectUtils.isEmpty(receiver) && !ObjectUtils.isEmpty(receiverid);

        while (true) {
            List<Story> stories = findStoriesToDelete(feedName, type, receiver, receiverid, postingId, recommended);
            if (stories.isEmpty()) {
                break;
            }

            for (Story story : stories) {
                if (story.getEntry() != null) {
                    // Serialize removal of references to the same entry from concurrent feed requests.
                    entityManager.lock(story.getEntry(), LockModeType.PESSIMISTIC_WRITE);
                }
                delete(story);
                universalContext.send(new StoryDeletedLiberin(story));
            }

            if (!singlePosting) {
                List<UUID> postingIds = stories.stream()
                    .map(Story::getEntry)
                    .filter(Objects::nonNull)
                    .map(Entry::getId)
                    .distinct()
                    .toList();
                if (!postingIds.isEmpty()) {
                    // Background cleanup would also delete these postings after losing their last reference.
                    postingRepository.findUnlinked(universalContext.nodeId(), postingIds)
                        .forEach(this::checkPostingDeletionAllowed);
                }
            }
        }

        if (singlePosting) {
            List<Posting> postings = postingRepository.findUnlinked(
                universalContext.nodeId(),
                ObjectUtils.isEmpty(receiver) ? null : receiver,
                ObjectUtils.isEmpty(receiverid) ? null : receiverid,
                postingId
            );
            postings.forEach(this::checkPostingDeletionAllowed);
            postingOperations.deletePostings(postings);
        }
    }

    private void checkPostingDeletionAllowed(Posting posting) {
        Scope clientScope = Objects.equals(universalContext.getClientName(Scope.IDENTIFY), posting.getOwnerName())
            ? Scope.DELETE_OWN_CONTENT
            : Scope.DELETE_OTHERS_CONTENT;
        Scope adminScope = Objects.equals(posting.getOwnerName(), universalContext.nodeName())
            ? Scope.DELETE_OWN_CONTENT
            : Scope.DELETE_OTHERS_CONTENT;
        if (!universalContext.isPrincipal(posting.getDeleteE(), clientScope, adminScope)) {
            throw new AuthenticationException();
        }
    }

    private List<Story> findStoriesToDelete(
        String feedName,
        StoryType type,
        String receiverName,
        String receiverPostingId,
        UUID postingId,
        Boolean recommended
    ) {
        QStory story = QStory.story;
        QEntry entry = QEntry.entry;
        var query = new JPAQueryFactory(entityManager).selectFrom(story);
        if (
            !ObjectUtils.isEmpty(receiverName)
            || !ObjectUtils.isEmpty(receiverPostingId)
            || postingId != null
            || recommended != null
        ) {
            query = query.leftJoin(story.entry, entry);
        }
        BooleanBuilder where = new BooleanBuilder();
        where.and(story.nodeId.eq(universalContext.nodeId()));
        if (feedName != null) {
            where.and(story.feedName.eq(feedName));
        }
        if (type != null) {
            where.and(story.storyType.eq(type));
        }
        if (!ObjectUtils.isEmpty(receiverName)) {
            where.and(entry.receiverName.eq(receiverName));
        }
        if (!ObjectUtils.isEmpty(receiverPostingId)) {
            where.and(entry.receiverEntryId.eq(receiverPostingId));
        }
        if (postingId != null) {
            where.and(entry.id.eq(postingId));
        }
        if (recommended != null) {
            where.and(entry.recommended.eq(recommended));
        }
        return query.where(where).limit(DELETE_BATCH_SIZE).fetch();
    }

    @Scheduled(fixedDelayString = "P1D")
    public void purgeExpired() {
        try (var ignored = requestCounter.allot()) {
            log.info("Purging old stories");

            for (String domainName : domains.getAllDomainNames()) {
                UUID nodeId = domains.getDomainNodeId(domainName);
                purgeExpired(nodeId, domainName, Feed.INSTANT, "instants.lifetime", storyRepository::findExpired, true);
                purgeExpired(
                    nodeId, domainName, Feed.INSTANT, "instants.viewed.lifetime", storyRepository::findExpiredViewed,
                    true
                );
                boolean purgePinned = domains.getDomainOptions(nodeId).getBool("news.purge-pinned");
                purgeExpired(nodeId, domainName, Feed.NEWS, "news.lifetime", storyRepository::findExpired, purgePinned);
                purgeExpired(
                    nodeId, domainName, Feed.NEWS, "recommendations.lifetime", storyRepository::findExpiredRecommended,
                    purgePinned
                );
                purgeExpired(nodeId, domainName, Feed.EXPLORE, "explore.lifetime", storyRepository::findExpired, true);
            }
        }
    }

    private interface ExpiredFinder {

        List<Story> find(UUID nodeId, String feedName, Timestamp createdBefore);

    }

    private void purgeExpired(
        UUID nodeId, String domainName, String feedName, String optionName, ExpiredFinder finder, boolean purgePinned
    ) {
        Duration lifetime = domains.getDomainOptions(domainName).getDuration(optionName).getDuration();
        Timestamp createdBefore = Timestamp.from(Instant.now().minus(lifetime));
        List<Liberin> liberins = new ArrayList<>();
        tx.executeWrite(() -> {
            mediaFileOwnerRepository.lockExclusive();
            List<Story> stories = finder.find(nodeId, feedName, createdBefore);
            stories.forEach(story -> {
                if (story.isPinned() && !purgePinned) {
                    return;
                }
                storyRepository.delete(story);
                liberins.add(new StoryDeletedLiberin(story).withNodeId(nodeId));
            });
        });
        liberins.forEach(liberinManager::send);
    }

}
