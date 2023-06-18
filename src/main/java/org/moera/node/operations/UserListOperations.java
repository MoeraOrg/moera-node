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
import java.util.stream.Collectors;
import javax.inject.Inject;
import javax.transaction.Transactional;

import org.moera.node.data.Comment;
import org.moera.node.data.CommentRepository;
import org.moera.node.data.Entry;
import org.moera.node.data.Posting;
import org.moera.node.data.PostingRepository;
import org.moera.node.data.RemoteUserListItem;
import org.moera.node.data.RemoteUserListItemRepository;
import org.moera.node.data.Story;
import org.moera.node.data.UserList;
import org.moera.node.data.UserListItem;
import org.moera.node.data.UserListItemRepository;
import org.moera.node.global.UniversalContext;
import org.moera.node.model.CommentInfo;
import org.moera.node.model.PostingInfo;
import org.moera.node.model.StoryInfo;
import org.moera.node.rest.task.RemoteUserListItemFetchTask;
import org.moera.node.task.TaskAutowire;
import org.moera.node.util.SheriffUtil;
import org.moera.node.util.Util;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.util.ObjectUtils;

@Component
public class UserListOperations {

    public static final Duration ABSENT_TTL = Duration.ofDays(3);
    public static final Duration PRESENT_TTL = Duration.ofDays(30);

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
    private FeedOperations feedOperations;

    @Inject
    @Qualifier("remoteTaskExecutor")
    private TaskExecutor taskExecutor;

    @Inject
    private TaskAutowire taskAutowire;

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
                var fetchTask = new RemoteUserListItemFetchTask(sheriffName, entry);
                taskAutowire.autowire(fetchTask);
                taskExecutor.execute(fetchTask);
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

    public void fillSheriffListMarks(Posting posting, List<CommentInfo> comments) {
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

    public void fillSheriffListMarks(Posting posting, CommentInfo commentInfo) {
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

    @Scheduled(fixedDelayString = "P1D")
    @Transactional
    public void purgeExpired() {
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
