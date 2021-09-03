package org.moera.node.rest;

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
import javax.validation.Valid;

import org.moera.commons.util.LogUtil;
import org.moera.node.auth.Admin;
import org.moera.node.data.Feed;
import org.moera.node.data.OwnReaction;
import org.moera.node.data.OwnReactionRepository;
import org.moera.node.data.Posting;
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
import org.moera.node.model.event.FeedStatusUpdatedEvent;
import org.moera.node.model.event.StoriesStatusUpdatedEvent;
import org.moera.node.operations.PostingOperations;
import org.moera.node.operations.StoryOperations;
import org.moera.node.push.PushContent;
import org.moera.node.push.PushService;
import org.moera.node.util.SafeInteger;
import org.moera.node.util.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.util.StringUtils;
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

    private static Logger log = LoggerFactory.getLogger(FeedController.class);

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
    public Collection<FeedInfo> getAll() {
        log.info("GET /feeds");

        Collection<FeedInfo> feeds = Feed.getAllStandard(requestContext.isAdmin());
        String clientName = requestContext.getClientName();
        if (!requestContext.isAdmin() && !StringUtils.isEmpty(clientName)) {
            Map<String, UUID> subscriberIds =
                subscriberRepository.findByType(requestContext.nodeId(), clientName, SubscriptionType.FEED)
                        .stream()
                        .collect(Collectors.toMap(Subscriber::getFeedName, Subscriber::getId, (id1, id2) -> id1));
            feeds = feeds.stream().map(feedInfo -> {
                UUID subscriberId = subscriberIds.get(feedInfo.getFeedName());
                if (subscriberId != null) {
                    feedInfo = feedInfo.clone();
                    feedInfo.setSubscriberId(subscriberId.toString());
                }
                return feedInfo;
            }).collect(Collectors.toList());
        }
        return feeds;
    }

    @GetMapping("/{feedName}")
    public FeedInfo get(@PathVariable String feedName) {
        log.info("GET /feeds/{feedName} (feedName = {})", LogUtil.format(feedName));

        if (!Feed.isStandard(feedName) || !Feed.isReadable(feedName, requestContext.isAdmin())) {
            throw new ObjectNotFoundFailure("feed.not-found");
        }
        FeedInfo feedInfo = Feed.getStandard(feedName);
        String clientName = requestContext.getClientName();
        if (!requestContext.isAdmin() && !StringUtils.isEmpty(clientName)) {
            Subscriber subscriber =
                    subscriberRepository.findByType(requestContext.nodeId(), clientName, SubscriptionType.FEED)
                            .stream()
                            .filter(s -> s.getFeedName().equals(feedName))
                            .findFirst()
                            .orElse(null);
            if (subscriber != null) {
                feedInfo = feedInfo.clone();
                feedInfo.setSubscriberId(subscriber.getId().toString());
            }
        }
        return feedInfo;
    }

    @GetMapping("/{feedName}/status")
    public FeedStatus getStatus(@PathVariable String feedName) {
        log.info("GET /feeds/{feedName}/status (feedName = {})", LogUtil.format(feedName));

        if (!Feed.isStandard(feedName)) {
            throw new ObjectNotFoundFailure("feed.not-found");
        }

        return storyOperations.getFeedStatus(feedName, requestContext.isAdmin());
    }

    @PutMapping("/{feedName}/status")
    @Admin
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
        requestContext.send(new FeedStatusUpdatedEvent(feedName, feedStatus, true));
        requestContext.send(new StoriesStatusUpdatedEvent(feedName, change));
        pushService.send(requestContext.nodeId(), PushContent.feedUpdated(feedName, feedStatus));

        return feedStatus;
    }

    @GetMapping("/{feedName}/stories")
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

        return sliceInfo;
    }

    private FeedSliceInfo getStoriesBefore(String feedName, long before, int limit) {
        Page<Story> page = storyRepository.findSlice(requestContext.nodeId(), feedName, SafeInteger.MIN_VALUE, before,
                PageRequest.of(0, limit + 1, Sort.Direction.DESC, "moment"));
        FeedSliceInfo sliceInfo = new FeedSliceInfo();
        sliceInfo.setBefore(before);
        if (page.getNumberOfElements() < limit + 1) {
            sliceInfo.setAfter(SafeInteger.MIN_VALUE);
        } else {
            sliceInfo.setAfter(page.getContent().get(limit).getMoment());
        }
        fillSlice(sliceInfo, feedName, limit);
        return sliceInfo;
    }

    private FeedSliceInfo getStoriesAfter(String feedName, long after, int limit) {
        Page<Story> page = storyRepository.findSlice(requestContext.nodeId(), feedName, after, SafeInteger.MAX_VALUE,
                PageRequest.of(0, limit + 1, Sort.Direction.ASC, "moment"));
        FeedSliceInfo sliceInfo = new FeedSliceInfo();
        sliceInfo.setAfter(after);
        if (page.getNumberOfElements() < limit + 1) {
            sliceInfo.setBefore(SafeInteger.MAX_VALUE);
        } else {
            sliceInfo.setBefore(page.getContent().get(limit - 1).getMoment());
        }
        fillSlice(sliceInfo, feedName, limit);
        return sliceInfo;
    }

    private void fillSlice(FeedSliceInfo sliceInfo, String feedName, int limit) {
        List<StoryInfo> stories = storyRepository.findInRange(
                requestContext.nodeId(), feedName, sliceInfo.getAfter(), sliceInfo.getBefore())
                .stream()
                .map(this::buildStoryInfo)
                .sorted(Comparator.comparing(StoryInfo::getMoment).reversed())
                .collect(Collectors.toList());
        String clientName = requestContext.getClientName();
        if (!StringUtils.isEmpty(clientName)) {
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
        if (stories.size() > limit) {
            stories.remove(limit);
        }
        sliceInfo.setStories(stories);
    }

    private void fillRemoteInfo(List<StoryInfo> stories, Map<String, PostingInfo> postingMap) {
        if (StringUtils.isEmpty(requestContext.getClientName())) {
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
                    return new PostingInfo(posting,
                            requestContext.isAdmin() || requestContext.isClient(posting.getOwnerName()));
                }
        );
    }

}
