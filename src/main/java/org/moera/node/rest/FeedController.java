package org.moera.node.rest;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.inject.Inject;
import javax.transaction.Transactional;
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
import org.moera.node.global.RequestContext;
import org.moera.node.model.ClientReactionInfo;
import org.moera.node.model.FeedInfo;
import org.moera.node.model.FeedSliceInfo;
import org.moera.node.model.FeedStatus;
import org.moera.node.model.FeedStatusChange;
import org.moera.node.model.ObjectNotFoundFailure;
import org.moera.node.model.PostingInfo;
import org.moera.node.model.StoryInfo;
import org.moera.node.model.ValidationFailure;
import org.moera.node.model.event.FeedStatusUpdatedEvent;
import org.moera.node.model.event.StoriesStatusUpdatedEvent;
import org.moera.node.operations.PostingOperations;
import org.moera.node.operations.StoryOperations;
import org.moera.node.util.SafeInteger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@ApiController
@RequestMapping("/moera/api/feeds")
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
    @Admin
    public FeedStatus getStatus(@PathVariable String feedName) {
        log.info("GET /feeds/{feedName}/status (feedName = {})", LogUtil.format(feedName));

        if (!Feed.isStandard(feedName)) {
            throw new ObjectNotFoundFailure("feed.not-found");
        }

        return storyOperations.getFeedStatus(feedName);
    }

    @PutMapping("/{feedName}/status")
    @Admin
    @Transactional
    public FeedStatus putStatus(@PathVariable String feedName, @Valid @RequestBody FeedStatusChange change) {
        log.info("PUT /feeds/{feedName}/status (feedName = {}, viewed = {}, read = {}, before = {})",
                LogUtil.format(feedName), LogUtil.format(change.getViewed()), LogUtil.format(change.getRead()),
                LogUtil.format(change.getBefore()));

        if (!Feed.isStandard(feedName)) {
            throw new ObjectNotFoundFailure("feed.not-found");
        }

        if (change.getViewed() != null) {
            storyRepository.updateViewed(requestContext.nodeId(), feedName, change.getViewed(), change.getBefore());
        }
        if (change.getRead() != null) {
            storyRepository.updateRead(requestContext.nodeId(), feedName, change.getRead(), change.getBefore());
        }

        FeedStatus feedStatus = storyOperations.getFeedStatus(feedName);
        requestContext.send(new FeedStatusUpdatedEvent(feedName, feedStatus));
        requestContext.send(new StoriesStatusUpdatedEvent(feedName, change));

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
        if (after == null) {
            before = before != null ? before : SafeInteger.MAX_VALUE;
            return getStoriesBefore(feedName, before, limit);
        } else {
            return getStoriesAfter(feedName, after, limit);
        }
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
            if (requestContext.isAdmin()) {
                fillOwnInfo(stories, postingMap);
            }
        }
        if (stories.size() > limit) {
            stories.remove(limit);
        }
        sliceInfo.setStories(stories);
    }

    private void fillOwnInfo(List<StoryInfo> stories, Map<String, PostingInfo> postingMap) {
        List<String> remotePostingIds = postingMap.values().stream()
                .filter(p -> !p.isOriginal())
                .map(PostingInfo::getReceiverPostingId)
                .collect(Collectors.toList());
        if (!remotePostingIds.isEmpty()) {
            fillOwnReactions(stories, remotePostingIds);
            fillSubscriptions(stories, remotePostingIds);
        }
    }

    private void fillOwnReactions(List<StoryInfo> stories, List<String> remotePostingIds) {
        Map<String, OwnReaction> ownReactions = ownReactionRepository
                .findAllByRemotePostingIds(requestContext.nodeId(), remotePostingIds)
                .stream()
                .collect(Collectors.toMap(
                        OwnReaction::getRemotePostingId, Function.identity(), (p1, p2) -> p1));
        stories.stream()
                .map(StoryInfo::getPosting)
                .filter(Objects::nonNull)
                .filter(p -> !p.isOriginal())
                .forEach(posting -> {
                    OwnReaction ownReaction = ownReactions.get(posting.getReceiverPostingId());
                    if (ownReaction != null
                            && ownReaction.getRemoteNodeName().equals(posting.getReceiverName())
                            && ownReaction.getRemotePostingId().equals(posting.getReceiverPostingId())) {
                        posting.setClientReaction(new ClientReactionInfo(ownReaction));
                    }
                });
    }

    private void fillSubscriptions(List<StoryInfo> stories, List<String> remotePostingIds) {
        Map<String, Subscription> subscriptionMap = subscriptionRepository
                .findAllByRemotePostingIds(requestContext.nodeId(), remotePostingIds)
                .stream()
                .filter(sr -> sr.getSubscriptionType() == SubscriptionType.POSTING_COMMENTS)
                .collect(Collectors.toMap(
                        Subscription::getRemoteEntryId, Function.identity(), (p1, p2) -> p1));
        stories.stream()
                .map(StoryInfo::getPosting)
                .filter(Objects::nonNull)
                .filter(p -> !p.isOriginal())
                .forEach(posting -> {
                    Subscription subscription = subscriptionMap.get(posting.getReceiverPostingId());
                    if (subscription != null
                            && subscription.getRemoteNodeName().equals(posting.getReceiverName())
                            && subscription.getRemoteEntryId().equals(posting.getReceiverPostingId())) {
                        posting.getSubscriptions().setSubscriberId(SubscriptionType.POSTING_COMMENTS,
                                subscription.getRemoteSubscriberId());
                    }
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
