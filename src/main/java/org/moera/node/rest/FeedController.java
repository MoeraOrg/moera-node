package org.moera.node.rest;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.inject.Inject;
import javax.transaction.Transactional;
import javax.validation.Valid;

import com.querydsl.core.BooleanBuilder;
import org.moera.commons.util.LogUtil;
import org.moera.node.auth.Admin;
import org.moera.node.auth.AuthenticationException;
import org.moera.node.auth.principal.Principal;
import org.moera.node.data.Feed;
import org.moera.node.data.OwnReaction;
import org.moera.node.data.OwnReactionRepository;
import org.moera.node.data.Posting;
import org.moera.node.data.QStory;
import org.moera.node.data.ReactionRepository;
import org.moera.node.data.Story;
import org.moera.node.data.StoryRepository;
import org.moera.node.data.Subscriber;
import org.moera.node.data.SubscriberRepository;
import org.moera.node.data.Subscription;
import org.moera.node.data.SubscriptionRepository;
import org.moera.node.data.SubscriptionType;
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
import org.moera.node.model.PostingSubscriptionsInfo;
import org.moera.node.model.StoryInfo;
import org.moera.node.model.ValidationFailure;
import org.moera.node.operations.PostingOperations;
import org.moera.node.operations.StoryOperations;
import org.moera.node.push.PushContent;
import org.moera.node.push.PushService;
import org.moera.node.util.SafeInteger;
import org.moera.node.util.Transaction;
import org.moera.node.util.Util;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.transaction.PlatformTransactionManager;
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
    private SubscriberRepository subscriberRepository;

    @Inject
    private SubscriptionRepository subscriptionRepository;

    @Inject
    private StoryOperations storyOperations;

    @Inject
    private PlatformTransactionManager txManager;

    @Inject
    private PushService pushService;

    @GetMapping
    @Transactional
    public Collection<FeedInfo> getAll() {
        log.info("GET /feeds");

        Collection<FeedInfo> feeds = Feed.getAllStandard(requestContext.isAdmin())
                .stream()
                .map(FeedInfo::clone)
                .peek(this::fillFeedTotals)
                .collect(Collectors.toList());

        String clientName = requestContext.getClientName();
        if (!requestContext.isAdmin() && !ObjectUtils.isEmpty(clientName)) {
            Map<String, UUID> subscriberIds =
                subscriberRepository.findByNameAndType(requestContext.nodeId(), clientName, SubscriptionType.FEED)
                        .stream()
                        .collect(Collectors.toMap(Subscriber::getFeedName, Subscriber::getId, (id1, id2) -> id1));
            feeds.forEach(feedInfo -> {
                int total = storyRepository.countInFeed(requestContext.nodeId(), feedInfo.getFeedName());
                feedInfo.setTotal(total);
                UUID subscriberId = subscriberIds.get(feedInfo.getFeedName());
                if (subscriberId != null) {
                    feedInfo.setSubscriberId(subscriberId.toString());
                }
            });
        }

        return feeds;
    }

    @GetMapping("/{feedName}")
    @Transactional
    public FeedInfo get(@PathVariable String feedName) {
        log.info("GET /feeds/{feedName} (feedName = {})", LogUtil.format(feedName));

        if (!Feed.isStandard(feedName) || !Feed.isReadable(feedName, requestContext.isAdmin())) {
            throw new ObjectNotFoundFailure("feed.not-found");
        }

        FeedInfo feedInfo = Feed.getStandard(feedName).clone();
        fillFeedTotals(feedInfo);

        String clientName = requestContext.getClientName();
        if (!requestContext.isAdmin() && !ObjectUtils.isEmpty(clientName)) {
            subscriberRepository.findByNameAndType(requestContext.nodeId(), clientName, SubscriptionType.FEED)
                    .stream()
                    .filter(s -> s.getFeedName().equals(feedName))
                    .findFirst()
                    .ifPresent(subscriber -> feedInfo.setSubscriberId(subscriber.getId().toString()));
        }

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
        if (Feed.isAdmin(feedName) && !requestContext.isAdmin()) {
            throw new AuthenticationException();
        }

        return storyOperations.getFeedStatus(feedName, requestContext.isAdmin());
    }

    @PutMapping("/{feedName}/status")
    @Admin
    @Transactional
    public FeedStatus putStatus(@PathVariable String feedName, @Valid @RequestBody FeedStatusChange change)
            throws Throwable {
        log.info("PUT /feeds/{feedName}/status (feedName = {}, viewed = {}, read = {}, before = {})",
                LogUtil.format(feedName), LogUtil.format(change.getViewed()), LogUtil.format(change.getRead()),
                LogUtil.format(change.getBefore()));

        if (!Feed.isStandard(feedName)) {
            throw new ObjectNotFoundFailure("feed.not-found");
        }

        Set<Story> instantsUpdated = new HashSet<>();
        Transaction.execute(txManager, () -> {
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
            return null;
        });

        if (!instantsUpdated.isEmpty()) {
            if (change.getViewed()) {
                instantsUpdated.stream()
                        .map(Story::getId)
                        .map(PushContent::storyDeleted)
                        .forEach(content -> pushService.send(requestContext.nodeId(), content));
            } else {
                instantsUpdated.stream()
                        .map(PushContent::storyAdded)
                        .forEach(content -> pushService.send(requestContext.nodeId(), content));
            }
        }

        FeedStatus feedStatus = storyOperations.getFeedStatus(feedName, true);

        requestContext.send(new FeedStatusUpdatedLiberin(feedName, feedStatus, change));

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

        if (!Feed.isStandard(feedName) || !Feed.isReadable(feedName, requestContext.isAdmin())) {
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
            Page<Story> page = findSlice(requestContext.nodeId(), feedName, SafeInteger.MIN_VALUE, sliceBefore,
                    limit + 1, Sort.Direction.DESC);
            if (page.getNumberOfElements() < limit + 1) {
                sliceInfo.setAfter(SafeInteger.MIN_VALUE);
            } else {
                sliceInfo.setAfter(page.getContent().get(limit).getMoment());
            }
            fillSlice(sliceInfo, feedName, limit);
            sliceBefore = sliceInfo.getAfter();
        } while (sliceBefore > SafeInteger.MIN_VALUE && sliceInfo.getStories().size() < limit / 2);
        return sliceInfo;
    }

    private FeedSliceInfo getStoriesAfter(String feedName, long after, int limit) {
        FeedSliceInfo sliceInfo = new FeedSliceInfo();
        sliceInfo.setAfter(after);
        long sliceAfter = after;
        do {
            Page<Story> page = findSlice(requestContext.nodeId(), feedName, sliceAfter, SafeInteger.MAX_VALUE,
                    limit + 1, Sort.Direction.ASC);
            if (page.getNumberOfElements() < limit + 1) {
                sliceInfo.setBefore(SafeInteger.MAX_VALUE);
            } else {
                sliceInfo.setBefore(page.getContent().get(limit - 1).getMoment());
            }
            fillSlice(sliceInfo, feedName, limit);
            sliceAfter = sliceInfo.getBefore();
        } while (sliceAfter < SafeInteger.MAX_VALUE && sliceInfo.getStories().size() < limit / 2);
        return sliceInfo;
    }

    private Page<Story> findSlice(UUID nodeId, String feedName, long afterMoment, long beforeMoment, int limit,
                                  Sort.Direction direction) {
        QStory story = QStory.story;
        BooleanBuilder where = new BooleanBuilder();
        where.and(story.nodeId.eq(nodeId))
                .and(story.feedName.eq(feedName))
                .and(story.moment.gt(afterMoment))
                .and(story.moment.loe(beforeMoment));
        if (!requestContext.isAdmin()) {
            var viewPrincipal = story.entry.viewPrincipal;
            BooleanBuilder visibility = new BooleanBuilder();
            visibility.or(viewPrincipal.eq(Principal.PUBLIC));
            if (!ObjectUtils.isEmpty(requestContext.getClientName())) {
                visibility.or(viewPrincipal.eq(Principal.SIGNED));
                BooleanBuilder priv = new BooleanBuilder();
                priv.and(viewPrincipal.eq(Principal.PRIVATE));
                priv.and(story.entry.ownerName.eq(requestContext.getClientName()));
                visibility.or(priv);
            }
            where.and(visibility);
        }
        return storyRepository.findAll(where, PageRequest.of(0, limit + 1, direction, "moment"));
    }

    private void fillSlice(FeedSliceInfo sliceInfo, String feedName, int limit) {
        List<StoryInfo> stories = storyRepository.findInRange(
                requestContext.nodeId(), feedName, sliceInfo.getAfter(), sliceInfo.getBefore())
                .stream()
                .map(this::buildStoryInfo)
                .filter(this::isStoryVisible)
                .sorted(Comparator.comparing(StoryInfo::getMoment).reversed())
                .collect(Collectors.toList());
        String clientName = requestContext.getClientName();
        if (!ObjectUtils.isEmpty(clientName)) {
            Map<String, PostingInfo> postingMap = stories.stream()
                    .map(StoryInfo::getPosting)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toMap(PostingInfo::getId, Function.identity(), (p1, p2) -> p1));
            reactionRepository.findByStoriesInRangeAndOwner(
                    requestContext.nodeId(), feedName, sliceInfo.getAfter(), sliceInfo.getBefore(), clientName)
                    .stream()
                    .map(ClientReactionInfo::new)
                    .filter(r -> postingMap.containsKey(r.getEntryId()))
                    .forEach(r -> postingMap.get(r.getEntryId()).setClientReaction(r));
            fillRemoteInfo(stories, postingMap);
            if (requestContext.isAdmin()) {
                fillOwnInfo(stories, postingMap);
            }
        }
        sliceInfo.getStories().addAll(stories);
        if (sliceInfo.getStories().size() > limit) {
            sliceInfo.getStories().remove(limit);
        }
    }

    private void fillRemoteInfo(List<StoryInfo> stories, Map<String, PostingInfo> postingMap) {
        if (ObjectUtils.isEmpty(requestContext.getClientName())) {
            return;
        }
        List<PostingInfo> postings = stories.stream()
                .map(StoryInfo::getPosting)
                .filter(Objects::nonNull)
                .filter(PostingInfo::isOriginal)
                .collect(Collectors.toList());
        List<UUID> postingIds = postingMap.values().stream()
                .filter(PostingInfo::isOriginal)
                .map(PostingInfo::getId)
                .map(UUID::fromString)
                .collect(Collectors.toList());
        if (!postingIds.isEmpty()) {
            fillSubscribers(postings, postingIds);
        }
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

    private void fillSubscribers(List<PostingInfo> postings, List<UUID> postingIds) {
        List<Subscriber> allSubscribers = subscriberRepository.findAllByPostingIds(
                requestContext.nodeId(), requestContext.getClientName(), postingIds);
        Map<String, List<Subscriber>> subscriberMap = new HashMap<>();
        for (Subscriber subscriber : allSubscribers) {
            subscriberMap
                    .computeIfAbsent(subscriber.getEntry().getId().toString(), key -> new ArrayList<>())
                    .add(subscriber);
        }
        postings.forEach(posting ->
                posting.setSubscriptions(PostingSubscriptionsInfo.fromSubscribers(subscriberMap.get(posting.getId()))));
    }

    private void fillOwnInfo(List<StoryInfo> stories, Map<String, PostingInfo> postingMap) {
        List<PostingInfo> postings = stories.stream()
                .map(StoryInfo::getPosting)
                .filter(Objects::nonNull)
                .filter(p -> !p.isOriginal())
                .collect(Collectors.toList());
        List<String> remotePostingIds = postingMap.values().stream()
                .filter(p -> !p.isOriginal())
                .map(PostingInfo::getReceiverPostingId)
                .collect(Collectors.toList());
        if (!remotePostingIds.isEmpty()) {
            fillOwnReactions(postings, remotePostingIds);
            fillSubscriptions(postings, remotePostingIds);
        }
    }

    private void fillOwnReactions(List<PostingInfo> postings, List<String> remotePostingIds) {
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

    private void fillSubscriptions(List<PostingInfo> postings, List<String> remotePostingIds) {
        List<Subscription> allSubscriptions = subscriptionRepository
                .findAllByRemotePostingIds(requestContext.nodeId(), remotePostingIds);
        Map<String, List<Subscription>> subscriptionMap = new HashMap<>();
        for (Subscription subscription : allSubscriptions) {
            subscriptionMap
                    .computeIfAbsent(subscription.getRemoteEntryId(), key -> new ArrayList<>())
                    .add(subscription);
        }
        postings.forEach(posting -> {
            List<Subscription> subscriptions = subscriptionMap.get(posting.getReceiverPostingId());
            if (subscriptions != null) {
                subscriptions = subscriptions
                        .stream()
                        .filter(sb -> sb.getRemoteNodeName().equals(posting.getReceiverName()))
                        .collect(Collectors.toList());
            }
            posting.setSubscriptions(PostingSubscriptionsInfo.fromSubscriptions(subscriptions));
        });
    }

    private StoryInfo buildStoryInfo(Story story) {
        return StoryInfo.build(
                story,
                requestContext.isAdmin(),
                t -> {
                    Posting posting = (Posting) t.getEntry();
                    return new PostingInfo(posting, requestContext);
                }
        );
    }

    private boolean isStoryVisible(StoryInfo storyInfo) {
        PostingInfo postingInfo = storyInfo.getPosting();
        if (postingInfo == null) {
            return true;
        }
        Principal viewPrincipal = postingInfo.getOperations() != null
                ? postingInfo.getOperations().getOrDefault("view", Principal.PUBLIC)
                : Principal.PUBLIC;
        return requestContext.isPrincipal(viewPrincipal.withOwner(postingInfo.getOwnerName()));
    }

}
