package org.moera.node.rest.task;

import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Consumer;
import javax.inject.Inject;

import org.moera.node.api.NodeApiException;
import org.moera.node.api.NodeApiNotFoundException;
import org.moera.node.api.NodeApiUnknownNameException;
import org.moera.node.data.SheriffComplainGroup;
import org.moera.node.data.SheriffComplainGroupRepository;
import org.moera.node.data.SheriffComplainStatus;
import org.moera.node.data.SheriffDecision;
import org.moera.node.data.SheriffDecisionRepository;
import org.moera.node.data.SheriffOrder;
import org.moera.node.data.SheriffOrderRepository;
import org.moera.node.model.CommentInfo;
import org.moera.node.model.FeedInfo;
import org.moera.node.model.PostingInfo;
import org.moera.node.model.WhoAmI;
import org.moera.node.task.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

public class SheriffComplainGroupPrepareTask extends Task {

    private static final Logger log = LoggerFactory.getLogger(SheriffComplainGroupPrepareTask.class);

    private static final int MAX_RETRIES = 6;
    private static final Duration RETRY_DELAY = Duration.ofMinutes(10);

    private final UUID groupId;
    private final String nodeName;
    private final String feedName;
    private final String postingId;
    private final String commentId;

    @Inject
    private SheriffComplainGroupRepository sheriffComplainGroupRepository;

    @Inject
    private SheriffDecisionRepository sheriffDecisionRepository;

    @Inject
    private SheriffOrderRepository sheriffOrderRepository;

    public SheriffComplainGroupPrepareTask(UUID groupId, String nodeName, String feedName,
                                           String postingId, String commentId) {
        this.groupId = groupId;
        this.nodeName = nodeName;
        this.feedName = feedName;
        this.postingId = postingId;
        this.commentId = commentId;
    }

    @Override
    protected void execute() {
        for (int i = 0; i < MAX_RETRIES; i++) {
            try {
                prepare();
                autoDecide();
                return;
            } catch (NodeApiUnknownNameException e) {
                updateComplainGroupStatus(SheriffComplainStatus.NOT_FOUND);
                return;
            } catch (NodeApiNotFoundException e) {
                if (e.getResult() != null && Objects.equals(e.getResult().getErrorCode(), "comment.wrong-posting")) {
                    updateComplainGroupStatus(SheriffComplainStatus.INVALID_TARGET);
                } else {
                    updateComplainGroupStatus(SheriffComplainStatus.NOT_FOUND);
                }
                return;
            } catch (Exception e) {
                log.error("Error preparing complain group {}: {}", groupId, e.getMessage());
            }
            try {
                Thread.sleep(RETRY_DELAY.toMillis());
            } catch (InterruptedException e) {
                // ignore
            }
        }
        log.error("Reached max number of retries to prepare complain group {}, giving up", groupId);

        updateComplainGroupStatus(SheriffComplainStatus.PREPARE_FAILED);
    }

    private void prepare() throws NodeApiException {
        WhoAmI whoAmI = nodeApi.whoAmI(nodeName);
        FeedInfo feedInfo = nodeApi.getFeed(nodeName, feedName);
        if (feedInfo.getSheriffs() == null || !feedInfo.getSheriffs().contains(nodeName())) {
            updateComplainGroupStatus(SheriffComplainStatus.NOT_SHERIFF);
            return;
        }
        PostingInfo postingInfo = postingId != null
                ? nodeApi.getPosting(nodeName, generateCarte(nodeName), postingId)
                : null;
        if (postingInfo != null) {
            boolean inFeed = postingInfo.getFeedReferences().stream().anyMatch(fr -> fr.getFeedName().equals(feedName));
            if (!inFeed) {
                updateComplainGroupStatus(SheriffComplainStatus.INVALID_TARGET);
                return;
            }
            if (!postingInfo.isOriginal()) {
                updateComplainGroupStatus(SheriffComplainStatus.NOT_ORIGINAL);
                return;
            }
        }
        CommentInfo commentInfo = commentId != null
                ? nodeApi.getComment(nodeName, generateCarte(nodeName), postingId, commentId)
                : null;

        updateComplainGroup(complainGroup -> {
            if (complainGroup.getRemoteNodeFullName() == null) {
                complainGroup.setRemoteNodeFullName(whoAmI.getFullName());
            }
            if (postingInfo != null) {
                complainGroup.setRemotePostingOwnerName(postingInfo.getOwnerName());
                complainGroup.setRemotePostingOwnerFullName(postingInfo.getOwnerFullName());
                complainGroup.setRemotePostingOwnerGender(postingInfo.getOwnerGender());
                complainGroup.setRemotePostingHeading(postingInfo.getHeading());
                complainGroup.setRemotePostingRevisionId(postingInfo.getRevisionId());
            }
            if (commentInfo != null) {
                complainGroup.setRemoteCommentOwnerName(commentInfo.getOwnerName());
                complainGroup.setRemoteCommentOwnerFullName(commentInfo.getOwnerFullName());
                complainGroup.setRemoteCommentOwnerGender(commentInfo.getOwnerGender());
                complainGroup.setRemoteCommentHeading(commentInfo.getHeading());
                complainGroup.setRemoteCommentRevisionId(commentInfo.getRevisionId());
            }
            complainGroup.setStatus(SheriffComplainStatus.PREPARED);
        });
    }

    private void autoDecide() {
        List<SheriffOrder> orders;
        PageRequest page = PageRequest.of(0, 1, Sort.Direction.DESC, "createdAt");
        if (postingId == null) {
            orders = sheriffOrderRepository.findByFeed(nodeId, nodeName, feedName, page);
        } else if (commentId == null) {
            orders = sheriffOrderRepository.findByPosting(nodeId, nodeName, feedName, postingId, page);
        } else {
            orders = sheriffOrderRepository.findByComment(nodeId, nodeName, feedName, postingId, commentId, page);
        }
        if (!orders.isEmpty()) {
            SheriffOrder order = orders.get(0);
            if (!order.isDelete()) {
                updateComplainGroup(complainGroup -> {
                    SheriffDecision decision = new SheriffDecision();
                    decision.setId(UUID.randomUUID());
                    decision.setAccepted(true);
                    decision.setReasonCode(order.getReasonCode());
                    decision.setReasonDetails(order.getReasonDetails());
                    decision = sheriffDecisionRepository.save(decision);
                    complainGroup.setSheriffDecision(decision);
                    complainGroup.setStatus(SheriffComplainStatus.DECIDED);
                });
            }
        }
    }

    private void updateComplainGroup(Consumer<SheriffComplainGroup> updater) {
        try {
            inTransaction(() -> {
                SheriffComplainGroup complainGroup = sheriffComplainGroupRepository.findByNodeIdAndId(nodeId, groupId)
                        .orElse(null);
                if (complainGroup == null) {
                    return null;
                }
                updater.accept(complainGroup);
                sheriffComplainGroupRepository.save(complainGroup);
                return null;
            });
        } catch (Throwable e) {
            log.error("Could not store complain group", e);
            if (e instanceof RuntimeException) {
                throw (RuntimeException) e;
            }
        }
    }

    private void updateComplainGroupStatus(SheriffComplainStatus status) {
        updateComplainGroup(c -> c.setStatus(status));
    }

}
