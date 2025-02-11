package org.moera.node.operations;

import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import org.moera.lib.node.types.SubscriptionReason;
import org.moera.lib.node.types.SubscriptionType;
import org.moera.node.data.Comment;
import org.moera.node.data.CommentRepository;
import org.moera.node.data.Entry;
import org.moera.node.data.Feed;
import org.moera.node.data.Posting;
import org.moera.node.data.PostingRepository;
import org.moera.node.data.RemoteUserListItem;
import org.moera.node.data.RemoteUserListItemRepository;
import org.moera.node.data.Story;
import org.moera.node.data.UserList;
import org.moera.node.data.UserListItem;
import org.moera.node.data.UserListItemRepository;
import org.moera.node.data.UserSubscriptionRepository;
import org.moera.node.global.RequestCounter;
import org.moera.node.global.UniversalContext;
import org.moera.node.liberin.model.PostingUpdatedLiberin;
import org.moera.node.model.CommentInfo;
import org.moera.node.model.PostingInfo;
import org.moera.node.model.StoryInfo;
import org.moera.node.option.OptionHook;
import org.moera.node.option.OptionValueChange;
import org.moera.node.rest.task.RemoteUserListItemFetchJob;
import org.moera.node.rest.task.UserListUpdateJob;
import org.moera.node.task.Jobs;
import org.moera.node.util.SheriffUtil;
import org.moera.node.util.Util;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.util.ObjectUtils;

@Component
public class UserListOperations {

    public static final Duration ABSENT_TTL = Duration.ofDays(3);
    public static final Duration PRESENT_TTL = Duration.ofDays(30);

    private static final Logger log = LoggerFactory.getLogger(UserListOperations.class);

    @Inject
    private RequestCounter requestCounter;

    @Inject
    private UniversalContext universalContext;

    @Inject
    private UserListItemRepository userListItemRepository;

    @Inject
    private RemoteUserListItemRepository remoteUserListItemRepository;

    @Inject
    private PostingRepository postingRepository;

    @Inject
    private CommentRepository commentRepository;

    @Inject
    private UserSubscriptionRepository userSubscriptionRepository;

    @Inject
    private FeedOperations feedOperations;

    @Inject
    private SubscriptionOperations subscriptionOperations;

    @Inject
    private Jobs jobs;

    public void sheriffListReference(Story story) {
        FeedOperations.getFeedSheriffs(universalContext.getOptions(), story.getFeedName())
                .orElse(Collections.emptyList())
                .forEach(sheriffName -> sheriffListReference(story.getEntry(), sheriffName));
    }

    public void sheriffListReference(Comment comment) {
        Set<String> sheriffs = new HashSet<>();
        comment.getPosting().getStories().stream()
                .map(Story::getFeedName)
                .map(feedName -> FeedOperations.getFeedSheriffs(universalContext.getOptions(), feedName))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .forEach(sheriffs::addAll);
        sheriffs.forEach(sheriffName -> sheriffListReference(comment, sheriffName));
    }

    private void sheriffListReference(Entry entry, String sheriffName) {
        boolean included;
        if (sheriffName.equals(universalContext.nodeName())) {
            included = userListItemRepository.findByListAndNodeName(
                    universalContext.nodeId(), UserList.SHERIFF_HIDE, entry.getOwnerName()).isPresent();
        } else {
            RemoteUserListItem item = remoteUserListItemRepository.findByListAndNodeName(
                    universalContext.nodeId(), sheriffName, UserList.SHERIFF_HIDE, entry.getOwnerName()).orElse(null);
            if (item == null) {
                jobs.run(
                        RemoteUserListItemFetchJob.class,
                        new RemoteUserListItemFetchJob.Parameters(sheriffName, entry.getOwnerName(), entry.getId()),
                        universalContext.nodeId());
                return;
            }
            Duration ttl = item.isAbsent() ? ABSENT_TTL : PRESENT_TTL;
            item.setDeadline(Timestamp.from(Instant.now().plus(ttl)));
            included = !item.isAbsent();
        }
        entry.setSheriffUserListReferred(entry.isSheriffUserListReferred() || included);
    }

    public void fillSheriffListMarks(String feedName, List<StoryInfo> stories) {
        List<String> sheriffs = FeedOperations.getFeedSheriffs(universalContext.getOptions(), feedName)
                .orElse(Collections.emptyList());
        if (sheriffs.isEmpty()) {
            return;
        }
        Set<String> ownerNames = stories.stream()
                .map(StoryInfo::getPosting)
                .filter(Objects::nonNull)
                .filter(PostingInfo::isSheriffUserListReferred)
                .map(PostingInfo::getOwnerName)
                .collect(Collectors.toSet());
        ownerNames.remove(universalContext.nodeName());
        if (ownerNames.isEmpty()) {
            return;
        }
        for (String sheriff : sheriffs) {
            Set<String> markedNames;
            if (sheriff.equals(universalContext.nodeName())) {
                markedNames = userListItemRepository.findByListAndNodeNames(
                                universalContext.nodeId(), UserList.SHERIFF_HIDE, ownerNames).stream()
                        .map(UserListItem::getNodeName)
                        .collect(Collectors.toSet());
            } else {
                markedNames = remoteUserListItemRepository.findByListAndNodeNames(
                                universalContext.nodeId(), sheriff, UserList.SHERIFF_HIDE, ownerNames).stream()
                        .filter(i -> !i.isAbsent())
                        .map(RemoteUserListItem::getNodeName)
                        .collect(Collectors.toSet());
            }
            if (markedNames.isEmpty()) {
                continue;
            }
            stories.stream()
                    .map(StoryInfo::getPosting)
                    .filter(Objects::nonNull)
                    .filter(pi -> markedNames.contains(pi.getOwnerName()))
                    .forEach(pi -> SheriffUtil.addSheriffMark(pi, sheriff));
        }
    }

    public void fillSheriffListMarks(PostingInfo postingInfo) {
        if (!postingInfo.isSheriffUserListReferred()
                || ObjectUtils.isEmpty(postingInfo.getSheriffs())
                || postingInfo.getOwnerName().equals(universalContext.nodeName())) {
            return;
        }
        for (String sheriff : postingInfo.getSheriffs()) {
            boolean mark;
            if (sheriff.equals(universalContext.nodeName())) {
                mark = userListItemRepository.findByListAndNodeName(
                                universalContext.nodeId(), UserList.SHERIFF_HIDE, postingInfo.getOwnerName())
                        .isPresent();
            } else {
                mark = remoteUserListItemRepository.findByListAndNodeName(
                                universalContext.nodeId(), sheriff, UserList.SHERIFF_HIDE, postingInfo.getOwnerName())
                        .map(i -> !i.isAbsent())
                        .orElse(false);
            }
            if (mark) {
                SheriffUtil.addSheriffMark(postingInfo, sheriff);
            }
        }
    }

    public void fillSheriffListMarks(Entry posting, List<CommentInfo> comments) {
        if (posting == null || ObjectUtils.isEmpty(posting.getStories())) {
            return;
        }
        List<String> sheriffs = new ArrayList<>();
        for (Story story : posting.getStories()) {
            FeedOperations.getFeedSheriffs(universalContext.getOptions(), story.getFeedName())
                    .ifPresent(sheriffs::addAll);
        }
        if (ObjectUtils.isEmpty(sheriffs)) {
            return;
        }
        Set<String> ownerNames = comments.stream()
                .filter(Objects::nonNull)
                .filter(CommentInfo::isSheriffUserListReferred)
                .map(CommentInfo::getOwnerName)
                .collect(Collectors.toSet());
        ownerNames.remove(universalContext.nodeName());
        if (ownerNames.isEmpty()) {
            return;
        }
        for (String sheriff : sheriffs) {
            Set<String> markedNames;
            if (sheriff.equals(universalContext.nodeName())) {
                markedNames = userListItemRepository.findByListAndNodeNames(
                                universalContext.nodeId(), UserList.SHERIFF_HIDE, ownerNames).stream()
                        .map(UserListItem::getNodeName)
                        .collect(Collectors.toSet());
            } else {
                markedNames = remoteUserListItemRepository.findByListAndNodeNames(
                                universalContext.nodeId(), sheriff, UserList.SHERIFF_HIDE, ownerNames).stream()
                        .filter(i -> !i.isAbsent())
                        .map(RemoteUserListItem::getNodeName)
                        .collect(Collectors.toSet());
            }
            if (markedNames.isEmpty()) {
                continue;
            }
            comments.stream()
                    .filter(Objects::nonNull)
                    .filter(ci -> markedNames.contains(ci.getOwnerName()))
                    .forEach(ci -> SheriffUtil.addSheriffMark(ci, sheriff));
        }
    }

    public void fillSheriffListMarks(Entry posting, CommentInfo commentInfo) {
        if (posting == null || ObjectUtils.isEmpty(posting.getStories())) {
            return;
        }
        List<String> sheriffs = new ArrayList<>();
        for (Story story : posting.getStories()) {
            FeedOperations.getFeedSheriffs(universalContext.getOptions(), story.getFeedName())
                    .ifPresent(sheriffs::addAll);
        }
        if (ObjectUtils.isEmpty(sheriffs)) {
            return;
        }
        if (commentInfo.getOwnerName().equals(universalContext.nodeName())) {
            return;
        }
        for (String sheriff : sheriffs) {
            boolean mark;
            if (sheriff.equals(universalContext.nodeName())) {
                mark = userListItemRepository.findByListAndNodeName(
                                universalContext.nodeId(), UserList.SHERIFF_HIDE, commentInfo.getOwnerName())
                        .isPresent();
            } else {
                mark = remoteUserListItemRepository.findByListAndNodeName(
                                universalContext.nodeId(), sheriff, UserList.SHERIFF_HIDE, commentInfo.getOwnerName())
                        .map(i -> !i.isAbsent())
                        .orElse(false);
            }
            if (mark) {
                SheriffUtil.addSheriffMark(commentInfo, sheriff);
            }
        }
    }

    public void addToList(String listNodeName, String listName, String nodeName) {
        RemoteUserListItem item = remoteUserListItemRepository
                .findByListAndNodeName(universalContext.nodeId(), listNodeName, listName, nodeName)
                .orElse(null);
        if (item != null) {
            if (!item.isAbsent()) {
                return;
            } else {
                item.setAbsent(false);
                item.setDeadline(Timestamp.from(Instant.now().plus(PRESENT_TTL)));
            }
        } else {
            item = new RemoteUserListItem();
            item.setId(UUID.randomUUID());
            item.setNodeId(universalContext.nodeId());
            item.setListNodeName(listNodeName);
            item.setListName(listName);
            item.setNodeName(nodeName);
            item.setAbsent(false);
            item.setDeadline(Timestamp.from(Instant.now().plus(PRESENT_TTL)));
            remoteUserListItemRepository.save(item);
        }

        if (UserList.SHERIFF_HIDE.equals(listName)) {
            addToSheriffList(listNodeName, nodeName);
        }
    }

    private void addToSheriffList(String sheriffName, String nodeName) {
        // TODO not optimized for large lists of postings/comments
        List<String> feeds = feedOperations.getSheriffFeeds(sheriffName);
        for (String feedName : feeds) {
            List<Posting> postings = postingRepository.findByOwnerNameAndFeed(
                    universalContext.nodeId(), nodeName, feedName);
            for (Posting posting : postings) {
                if (posting.isSheriffUserListReferred()) {
                    continue;
                }
                posting.setSheriffUserListReferred(true);
                posting.setEditedAt(Util.now());
                universalContext.send(
                        new PostingUpdatedLiberin(posting, posting.getCurrentRevision(), posting.getViewE()));
            }
            commentRepository.updateSheriffReferredByOwnerNameAndFeed(
                    universalContext.nodeId(), nodeName, feedName, true);
        }
    }

    public void deleteFromList(String listNodeName, String listName, List<String> sheriffFeeds, String nodeName) {
        RemoteUserListItem item = remoteUserListItemRepository
                .findByListAndNodeName(universalContext.nodeId(), listNodeName, listName, nodeName)
                .orElse(null);
        if (item == null || item.isAbsent()) {
            return;
        }
        item.setAbsent(true);
        item.setDeadline(Timestamp.from(Instant.now().plus(ABSENT_TTL)));

        if (UserList.SHERIFF_HIDE.equals(listName)) {
            deleteFromSheriffList(listNodeName, sheriffFeeds, nodeName);
        }
    }

    private void deleteFromSheriffList(String sheriffName, List<String> sheriffFeeds, String nodeName) {
        // TODO not optimized for large lists of postings/comments
        Set<String> otherFeeds = new HashSet<>();
        // feeds, where nodeName is still hidden by other sheriffs
        remoteUserListItemRepository.findByNodeNameNotAbsent(universalContext.nodeId(), UserList.SHERIFF_HIDE, nodeName)
                .stream()
                .map(RemoteUserListItem::getListNodeName)
                .filter(sh -> !sh.equals(sheriffName))
                .map(feedOperations::getSheriffFeeds)
                .forEach(otherFeeds::addAll);
        if (otherFeeds.equals(new HashSet<>(sheriffFeeds))) {
            // nodeName is still hidden on the same feeds by other sheriffs, nothing changed
            return;
        }
        for (String feedName : sheriffFeeds) {
            List<Posting> postings = postingRepository.findByOwnerNameAndFeed(
                    universalContext.nodeId(), nodeName, feedName);
            for (Posting posting : postings) {
                if (!posting.isSheriffUserListReferred()) {
                    continue;
                }
                boolean referred = false;
                if (!otherFeeds.isEmpty()) {
                    referred = posting.getStories().stream().map(Story::getFeedName).anyMatch(otherFeeds::contains);
                }
                if (referred) {
                    continue;
                }
                posting.setSheriffUserListReferred(false);
                posting.setEditedAt(Util.now());
                universalContext.send(
                        new PostingUpdatedLiberin(posting, posting.getCurrentRevision(), posting.getViewE()));
            }

            if (otherFeeds.isEmpty()) {
                commentRepository.updateSheriffReferredByOwnerNameAndFeed(
                        universalContext.nodeId(), nodeName, feedName, true);
                continue;
            }
            List<Comment> comments = commentRepository.findByOwnerNameAndFeed(
                    universalContext.nodeId(), nodeName, feedName);
            for (Comment comment : comments) {
                if (!comment.isSheriffUserListReferred()) {
                    continue;
                }
                boolean referred = comment.getPosting().getStories().stream()
                        .map(Story::getFeedName)
                        .anyMatch(otherFeeds::contains);
                comment.setSheriffUserListReferred(referred);
            }
        }
    }

    @Scheduled(fixedDelayString = "P1D")
    @Transactional
    public void purgeExpired() {
        try (var ignored = requestCounter.allot()) {
            log.info("Purging expired user list entries");

            // TODO this is for SHERIFF_HIDE user lists only
            remoteUserListItemRepository.deleteExpiredAbsent(Util.now());

            Collection<RemoteUserListItem> expired = remoteUserListItemRepository.findExpiredNotAbsent(Util.now());
            for (RemoteUserListItem item : expired) {
                universalContext.associate(item.getNodeId());
                List<String> feeds = feedOperations.getSheriffFeeds(item.getListNodeName());
                boolean used = false;
                for (String feedName : feeds) {
                    int count = postingRepository.countByOwnerNameAndFeed(item.getNodeId(), item.getNodeName(), feedName);
                    if (count > 0) {
                        used = true;
                        break;
                    }
                    count = commentRepository.countByOwnerNameAndFeed(item.getNodeId(), item.getNodeName(), feedName);
                    if (count > 0) {
                        used = true;
                        break;
                    }
                }
                if (!used) {
                    remoteUserListItemRepository.delete(item);
                }
            }
        }
    }

    @OptionHook("sheriffs.timeline")
    public void timelineSheriffChanged(OptionValueChange change) {
        universalContext.associate(change.getNodeId());
        Function<String, String> prevOptions =
                name -> name.equals(change.getName())
                    ? change.getPreviousValue().toString()
                    : universalContext.getOptions().getString(change.getName());
        List<String> sheriffs = feedOperations.getFeedSheriffs(Feed.TIMELINE)
                .orElse(Collections.emptyList());
        List<String> prevSheriffs = FeedOperations.getFeedSheriffs(prevOptions, Feed.TIMELINE)
                .orElse(Collections.emptyList());
        for (String sheriffName : sheriffs) {
            if (!prevSheriffs.contains(sheriffName)) {
                subscriptionOperations.subscribe(subscription -> {
                    subscription.setSubscriptionType(SubscriptionType.USER_LIST);
                    subscription.setRemoteNodeName(sheriffName);
                    subscription.setRemoteFeedName(UserList.SHERIFF_HIDE);
                    subscription.setReason(SubscriptionReason.USER);
                });
                jobs.run(
                        UserListUpdateJob.class,
                        new UserListUpdateJob.Parameters(
                                sheriffName,
                                UserList.SHERIFF_HIDE,
                                feedOperations.getSheriffFeeds(sheriffName),
                                null,
                                false),
                        change.getNodeId());
            }
        }
        for (String sheriffName : prevSheriffs) {
            if (!sheriffs.contains(sheriffName)) {
                userSubscriptionRepository.findAllByTypeAndNodeAndFeedName(
                            universalContext.nodeId(), SubscriptionType.USER_LIST, sheriffName, UserList.SHERIFF_HIDE)
                        .forEach(us -> userSubscriptionRepository.delete(us));

                jobs.run(
                        UserListUpdateJob.class,
                        new UserListUpdateJob.Parameters(
                                sheriffName,
                                UserList.SHERIFF_HIDE,
                                FeedOperations.getSheriffFeeds(prevOptions, sheriffName),
                                null,
                                true),
                        change.getNodeId());
            }
        }
    }
}
