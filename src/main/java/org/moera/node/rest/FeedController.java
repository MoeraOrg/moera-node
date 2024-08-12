package org.moera.node.rest;

import java.sql.Timestamp;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.transaction.Transactional;
import javax.validation.Valid;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.Order;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.Predicate;
import com.querydsl.jpa.impl.JPAQueryFactory;
import org.moera.commons.util.LogUtil;
import org.moera.node.auth.Admin;
import org.moera.node.auth.AuthenticationException;
import org.moera.node.auth.Scope;
import org.moera.node.auth.principal.Principal;
import org.moera.node.data.BlockedByUser;
import org.moera.node.data.BlockedOperation;
import org.moera.node.data.BlockedUser;
import org.moera.node.data.Feed;
import org.moera.node.data.OwnReaction;
import org.moera.node.data.OwnReactionRepository;
import org.moera.node.data.QEntry;
import org.moera.node.data.QEntryRevision;
import org.moera.node.data.QStory;
import org.moera.node.data.ReactionRepository;
import org.moera.node.data.Story;
import org.moera.node.data.StoryRepository;
import org.moera.node.global.ApiController;
import org.moera.node.global.NoCache;
import org.moera.node.global.RequestContext;
import org.moera.node.liberin.model.FeedStatusUpdatedLiberin;
import org.moera.node.liberin.model.FeedStoriesReadLiberin;
import org.moera.node.model.ClientReactionInfo;
import org.moera.node.model.FeedInfo;
import org.moera.node.model.FeedSliceInfo;
import org.moera.node.model.FeedStatus;
import org.moera.node.model.FeedStatusChange;
import org.moera.node.model.ObjectNotFoundFailure;
import org.moera.node.model.PostingInfo;
import org.moera.node.model.RemotePosting;
import org.moera.node.model.StoryInfo;
import org.moera.node.model.ValidationFailure;
import org.moera.node.operations.BlockedByUserOperations;
import org.moera.node.operations.BlockedUserOperations;
import org.moera.node.operations.EntryOperations;
import org.moera.node.operations.PostingOperations;
import org.moera.node.operations.StoryOperations;
import org.moera.node.operations.UserListOperations;
import org.moera.node.util.SafeInteger;
import org.moera.node.util.Transaction;
import org.moera.node.util.Util;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Sort;
import org.springframework.util.ObjectUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@ApiController
@RequestMapping("/moera/api/feeds")
@NoCache
public class FeedController {

    private static final Logger log = LoggerFactory.getLogger(FeedController.class);

    @Inject
    private RequestContext requestContext;

    @Inject
    private StoryRepository storyRepository;

    @Inject
    private ReactionRepository reactionRepository;

    @Inject
    private OwnReactionRepository ownReactionRepository;

    @Inject
    private StoryOperations storyOperations;

    @Inject
    private EntryOperations entryOperations;

    @Inject
    private BlockedUserOperations blockedUserOperations;

    @Inject
    private BlockedByUserOperations blockedByUserOperations;

    @Inject
    private UserListOperations userListOperations;

    @Inject
    private Transaction tx;

    @Inject
    @PersistenceContext
    private EntityManager entityManager;

    @GetMapping
    @Transactional
    public Collection<FeedInfo> getAll() {
        log.info("GET /feeds");

        return Feed.getAllStandard(requestContext.isAdmin(Scope.VIEW_FEEDS))
                .stream()
                .map(FeedInfo::clone)
                .peek(this::fillFeedTotals)
                .peek(fi -> fi.fillSheriffs(requestContext.getOptions()))
                .collect(Collectors.toList());
    }

    @GetMapping("/{feedName}")
    @Transactional
    public FeedInfo get(@PathVariable String feedName) {
        log.info("GET /feeds/{feedName} (feedName = {})", LogUtil.format(feedName));

        if (!Feed.isStandard(feedName) || !Feed.isReadable(feedName, requestContext.isAdmin(Scope.VIEW_FEEDS))) {
            throw new ObjectNotFoundFailure("feed.not-found");
        }

        FeedInfo feedInfo = Feed.getStandard(feedName).clone();
        fillFeedTotals(feedInfo);
        feedInfo.fillSheriffs(requestContext.getOptions());

        return feedInfo;
    }

    private void fillFeedTotals(FeedInfo feedInfo) {
        int total = storyRepository.countInFeed(requestContext.nodeId(), feedInfo.getFeedName());
        feedInfo.setTotal(total);
        Timestamp lastCreatedAt = storyRepository.findLastCreatedAt(requestContext.nodeId(), feedInfo.getFeedName());
        feedInfo.setLastCreatedAt(Util.toEpochSecond(lastCreatedAt));
        Timestamp firstCreatedAt = storyRepository.findFirstCreatedAt(requestContext.nodeId(), feedInfo.getFeedName());
        feedInfo.setFirstCreatedAt(Util.toEpochSecond(firstCreatedAt));
    }

    @GetMapping("/{feedName}/status")
    @Transactional
    public FeedStatus getStatus(@PathVariable String feedName) {
        log.info("GET /feeds/{feedName}/status (feedName = {})", LogUtil.format(feedName));

        if (!Feed.isStandard(feedName)) {
            throw new ObjectNotFoundFailure("feed.not-found");
        }
        if (Feed.isAdmin(feedName) && !requestContext.isAdmin(Scope.VIEW_FEEDS)) {
            throw new AuthenticationException();
        }

        return storyOperations.getFeedStatus(feedName, requestContext.isAdmin(Scope.VIEW_FEEDS));
    }

    @PutMapping("/{feedName}/status")
    @Admin(Scope.UPDATE_FEEDS)
    @Transactional
    public FeedStatus putStatus(@PathVariable String feedName, @Valid @RequestBody FeedStatusChange change) {
        log.info("PUT /feeds/{feedName}/status (feedName = {}, viewed = {}, read = {}, before = {})",
                LogUtil.format(feedName), LogUtil.format(change.getViewed()), LogUtil.format(change.getRead()),
                LogUtil.format(change.getBefore()));

        if (!Feed.isStandard(feedName)) {
            throw new ObjectNotFoundFailure("feed.not-found");
        }

        Set<Story> instantsUpdated = new HashSet<>();
        tx.executeWrite(() -> {
            if (change.getViewed() != null) {
                if (feedName.equals(Feed.INSTANT)) {
                    instantsUpdated.addAll(storyRepository.findViewed(requestContext.nodeId(), feedName,
                            !change.getViewed(), change.getBefore()));
                }
                storyRepository.updateViewed(requestContext.nodeId(), feedName, change.getViewed(),
                        change.getBefore(), !change.getViewed());
            }
            if (change.getRead() != null) {
                storyRepository.updateRead(requestContext.nodeId(), feedName, change.getRead(),
                        change.getBefore(), !change.getRead());
            }
        });

        FeedStatus feedStatus = storyOperations.getFeedStatus(feedName, true);

        requestContext.send(new FeedStatusUpdatedLiberin(feedName, feedStatus, change, instantsUpdated));

        return feedStatus;
    }

    @GetMapping("/{feedName}/stories")
    @Transactional
    public FeedSliceInfo getStories(
            @PathVariable String feedName,
            @RequestParam(required = false) Long before,
            @RequestParam(required = false) Long after,
            @RequestParam(required = false) Integer limit) {

        log.info("GET /feeds/{feedName}/stories (feedName = {}, before = {}, after = {}, limit = {})",
                LogUtil.format(feedName), LogUtil.format(before), LogUtil.format(after), LogUtil.format(limit));

        if (!Feed.isStandard(feedName) || !Feed.isReadable(feedName, requestContext.isAdmin(Scope.VIEW_FEEDS))) {
            throw new ObjectNotFoundFailure("feed.not-found");
        }
        if (before != null && after != null) {
            throw new ValidationFailure("feed.before-after-exclusive");
        }

        limit = limit != null && limit <= PostingOperations.MAX_POSTINGS_PER_REQUEST
                ? limit : PostingOperations.MAX_POSTINGS_PER_REQUEST;
        if (limit < 0) {
            throw new ValidationFailure("limit.invalid");
        }

        FeedSliceInfo sliceInfo;
        if (after == null) {
            before = before != null ? before : SafeInteger.MAX_VALUE;
            sliceInfo = getStoriesBefore(feedName, before, limit);
        } else {
            sliceInfo = getStoriesAfter(feedName, after, limit);
        }
        calcSliceTotals(sliceInfo, feedName);

        requestContext.send(new FeedStoriesReadLiberin(feedName, before, after, limit));

        return sliceInfo;
    }

    private FeedSliceInfo getStoriesBefore(String feedName, long before, int limit) {
        FeedSliceInfo sliceInfo = new FeedSliceInfo();
        sliceInfo.setBefore(before);
        long sliceBefore = before;
        do {
            List<Long> slice = findSlice(requestContext.nodeId(), feedName, SafeInteger.MIN_VALUE, sliceBefore,
                    limit + 1, Sort.Direction.DESC);
            if (slice.size() < limit + 1) {
                sliceInfo.setAfter(SafeInteger.MIN_VALUE);
            } else {
                sliceInfo.setAfter(slice.get(limit));
            }
            fillSlice(sliceInfo, feedName);
            sliceBefore = sliceInfo.getAfter();
        } while (sliceBefore > SafeInteger.MIN_VALUE && sliceInfo.getStories().size() < limit / 2);
        return sliceInfo;
    }

    private FeedSliceInfo getStoriesAfter(String feedName, long after, int limit) {
        FeedSliceInfo sliceInfo = new FeedSliceInfo();
        sliceInfo.setAfter(after);
        long sliceAfter = after;
        do {
            List<Long> slice = findSlice(requestContext.nodeId(), feedName, sliceAfter, SafeInteger.MAX_VALUE,
                    limit + 1, Sort.Direction.ASC);
            if (slice.size() < limit + 1) {
                sliceInfo.setBefore(SafeInteger.MAX_VALUE);
            } else {
                sliceInfo.setBefore(slice.get(limit - 1));
            }
            fillSlice(sliceInfo, feedName);
            sliceAfter = sliceInfo.getBefore();
        } while (sliceAfter < SafeInteger.MAX_VALUE && sliceInfo.getStories().size() < limit / 2);
        return sliceInfo;
    }

    private List<Long> findSlice(UUID nodeId, String feedName, long afterMoment, long beforeMoment, int limit,
                                 Sort.Direction direction) {
        QStory story = QStory.story;
        return new JPAQueryFactory(entityManager)
                .select(story.moment)
                .from(story)
                .where(storyFilter(nodeId, feedName, afterMoment, beforeMoment))
                .orderBy(new OrderSpecifier<>(direction.isAscending() ? Order.ASC : Order.DESC, story.moment))
                .limit(limit + 1)
                .fetch();
    }

    private void fillSlice(FeedSliceInfo sliceInfo, String feedName) {
        QStory story = QStory.story;
        QEntry entry = QEntry.entry;
        QEntryRevision currentRevision = QEntryRevision.entryRevision;
        List<StoryInfo> stories = new JPAQueryFactory(entityManager)
                .selectFrom(story)
                .distinct()
                .leftJoin(story.remoteAvatarMediaFile).fetchJoin()
                .leftJoin(story.remoteOwnerAvatarMediaFile).fetchJoin()
                .leftJoin(story.entry, entry).fetchJoin()
                .leftJoin(entry.currentRevision, currentRevision).fetchJoin()
                .leftJoin(entry.reactionTotals).fetchJoin()
                .leftJoin(entry.sources).fetchJoin()
                .leftJoin(entry.ownerAvatarMediaFile).fetchJoin()
                .leftJoin(entry.blockedInstants).fetchJoin()
                .where(storyFilter(requestContext.nodeId(), feedName, sliceInfo.getAfter(), sliceInfo.getBefore()))
                .fetch()
                .stream()
                .map(this::buildStoryInfo)
                // This should be unnecessary, but let it be for reliability
                .filter(this::isStoryVisible)
                .sorted(Comparator.comparing(StoryInfo::getMoment).reversed())
                .toList();

        Map<String, PostingInfo> postingMap = stories.stream()
                .map(StoryInfo::getPosting)
                .filter(Objects::nonNull)
                .collect(Collectors.toMap(PostingInfo::getId, Function.identity(), (p1, p2) -> p1));
        if (!requestContext.isOwner()) {
            String clientName = requestContext.getClientName(Scope.IDENTIFY);
            if (!ObjectUtils.isEmpty(clientName)) {
                fillReactions(sliceInfo, feedName, postingMap, clientName,
                        requestContext.hasClientScope(Scope.VIEW_CONTENT));
                fillBlocked(clientName, postingMap);
            }
        } else {
            fillReactions(sliceInfo, feedName, postingMap, requestContext.nodeName(),
                    requestContext.isAdmin(Scope.VIEW_CONTENT));
            fillOwnInfo(stories, postingMap);
        }

        userListOperations.fillSheriffListMarks(feedName, stories);

        sliceInfo.getStories().addAll(stories);
    }

    private void fillReactions(FeedSliceInfo sliceInfo, String feedName, Map<String, PostingInfo> postingMap,
                               String clientName, boolean viewContent) {
        reactionRepository.findByStoriesInRangeAndOwner(
                requestContext.nodeId(), feedName, sliceInfo.getAfter(), sliceInfo.getBefore(), clientName)
                .stream()
                .filter(r -> r.getViewE().isPublic() || viewContent)
                .map(ClientReactionInfo::new)
                .filter(r -> postingMap.containsKey(r.getEntryId()))
                .forEach(r -> postingMap.get(r.getEntryId()).setClientReaction(r));
    }

    private Predicate storyFilter(UUID nodeId, String feedName, long afterMoment, long beforeMoment) {
        QStory story = QStory.story;
        BooleanBuilder where = new BooleanBuilder();
        where.and(story.nodeId.eq(nodeId))
                .and(story.feedName.eq(feedName))
                .and(story.moment.gt(afterMoment))
                .and(story.moment.loe(beforeMoment));
        if (!requestContext.isAdmin(Scope.VIEW_CONTENT)) {
            var viewPrincipal = story.entry.viewPrincipal;
            BooleanBuilder visibility = new BooleanBuilder();
            visibility.or(viewPrincipal.eq(Principal.PUBLIC));
            String clientName = requestContext.getClientName(Scope.VIEW_CONTENT);
            if (!ObjectUtils.isEmpty(clientName)) {
                visibility.or(viewPrincipal.eq(Principal.SIGNED));
                BooleanBuilder priv = new BooleanBuilder();
                priv.and(viewPrincipal.eq(Principal.PRIVATE));
                priv.and(story.entry.ownerName.eq(clientName));
                visibility.or(priv);
            }
            if (requestContext.isSubscribedToClient(Scope.VIEW_CONTENT)) {
                visibility.or(viewPrincipal.eq(Principal.SUBSCRIBED));
            }
            String[] friendGroups = requestContext.getFriendGroups(Scope.VIEW_CONTENT);
            if (friendGroups != null) {
                for (String friendGroupName : friendGroups) {
                    visibility.or(viewPrincipal.eq(Principal.ofFriendGroup(friendGroupName)));
                }
            }
            where.and(visibility);
        }
        return where;
    }

    private void calcSliceTotals(FeedSliceInfo sliceInfo, String feedName) {
        int total = storyRepository.countInFeed(requestContext.nodeId(), feedName);
        if (sliceInfo.getAfter() <= SafeInteger.MIN_VALUE) {
            sliceInfo.setTotalInPast(0);
            sliceInfo.setTotalInFuture(total - sliceInfo.getStories().size());
        } else if (sliceInfo.getBefore() >= SafeInteger.MAX_VALUE) {
            sliceInfo.setTotalInFuture(0);
            sliceInfo.setTotalInPast(total - sliceInfo.getStories().size());
        } else {
            int totalInFuture = storyRepository.countInRange(requestContext.nodeId(), feedName,
                    sliceInfo.getBefore(), SafeInteger.MAX_VALUE);
            sliceInfo.setTotalInFuture(totalInFuture);
            sliceInfo.setTotalInPast(total - totalInFuture - sliceInfo.getStories().size());
        }
    }

    private void fillBlocked(String clientName, Map<String, PostingInfo> postingMap) {
        List<BlockedUser> blockedUsers = blockedUserOperations.search(
                requestContext.nodeId(),
                new BlockedOperation[]{BlockedOperation.COMMENT, BlockedOperation.REACTION},
                clientName,
                postingMap.keySet().stream().map(UUID::fromString).collect(Collectors.toList()),
                null,
                null,
                false
        );
        for (BlockedUser blockedUser : blockedUsers) {
            if (blockedUser.isGlobal()) {
                postingMap.values().forEach(p -> p.putBlockedOperation(blockedUser.getBlockedOperation()));
            } else {
                PostingInfo postingInfo = postingMap.get(blockedUser.getEntry().getId().toString());
                if (postingInfo != null) {
                    postingInfo.putBlockedOperation(blockedUser.getBlockedOperation());
                }
            }
        }
    }

    private void fillOwnInfo(List<StoryInfo> stories, Map<String, PostingInfo> postingMap) {
        List<PostingInfo> postings = stories.stream()
                .map(StoryInfo::getPosting)
                .filter(Objects::nonNull)
                .filter(p -> !p.isOriginal())
                .collect(Collectors.toList());
        List<RemotePosting> remotePostings = postingMap.values().stream()
                .filter(p -> !p.isOriginal())
                .map(p -> new RemotePosting(p.getReceiverName(), p.getReceiverPostingId()))
                .collect(Collectors.toList());
        if (!remotePostings.isEmpty()) {
            // TODO to see public reactions, we need to store the reaction's view principal in OwnReaction
            if (requestContext.isAdmin(Scope.VIEW_CONTENT)) {
                fillOwnReactions(postings, remotePostings);
            }
            fillBlockedBy(postings, remotePostings);
        }
    }

    private void fillOwnReactions(List<PostingInfo> postings, List<RemotePosting> remotePostings) {
        List<String> remotePostingIds = remotePostings.stream()
                .map(RemotePosting::getPostingId)
                .collect(Collectors.toList());
        Map<String, OwnReaction> ownReactions = ownReactionRepository
                .findAllByRemotePostingIds(requestContext.nodeId(), remotePostingIds)
                .stream()
                .collect(Collectors.toMap(
                        OwnReaction::getRemotePostingId, Function.identity(), (p1, p2) -> p1));
        postings.forEach(posting -> {
            OwnReaction ownReaction = ownReactions.get(posting.getReceiverPostingId());
            if (ownReaction != null
                    && ownReaction.getRemoteNodeName().equals(posting.getReceiverName())
                    && ownReaction.getRemotePostingId().equals(posting.getReceiverPostingId())) {
                posting.setClientReaction(new ClientReactionInfo(ownReaction));
            }
        });
    }

    private void fillBlockedBy(List<PostingInfo> postings, List<RemotePosting> remotePostings) {
        List<BlockedByUser> blockedByUsers = blockedByUserOperations.search(
                requestContext.nodeId(),
                new BlockedOperation[]{BlockedOperation.COMMENT, BlockedOperation.REACTION},
                remotePostings.toArray(RemotePosting[]::new),
                false);
        if (blockedByUsers.isEmpty()) {
            return;
        }
        for (BlockedByUser blockedByUser : blockedByUsers) {
            for (PostingInfo posting : postings) {
                if (blockedByUser.getRemoteNodeName().equals(posting.getReceiverName())
                        && (blockedByUser.isGlobal()
                            || blockedByUser.getRemotePostingId().equals(posting.getReceiverPostingId()))) {
                    posting.putBlockedOperation(blockedByUser.getBlockedOperation());
                }
            }
        }
    }

    private StoryInfo buildStoryInfo(Story story) {
        return StoryInfo.build(
                story,
                requestContext.isAdmin(Scope.VIEW_FEEDS),
                t -> new PostingInfo(
                        t.getEntry(), List.of(t), entryOperations, requestContext, requestContext.getOptions())
        );
    }

    private boolean isStoryVisible(StoryInfo storyInfo) {
        PostingInfo postingInfo = storyInfo.getPosting();
        if (postingInfo == null) {
            return true;
        }
        Principal viewPrincipal = postingInfo.getOperations() != null
                ? postingInfo.getOperations().getOrDefault("view", Principal.PUBLIC)
                : Principal.PUBLIC; // FIXME other types of stories may be invisible by default and require VIEW_FEEDS
        return requestContext.isPrincipal(viewPrincipal.withOwner(postingInfo.getOwnerName()), Scope.VIEW_CONTENT);
    }

}
