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
import org.moera.node.data.SheriffComplain;
import org.moera.node.data.SheriffComplainRepository;
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

public class SheriffComplainPrepareTask extends Task {

    private static final Logger log = LoggerFactory.getLogger(SheriffComplainPrepareTask.class);

    private static final int MAX_RETRIES = 6;
    private static final Duration RETRY_DELAY = Duration.ofMinutes(10);

    private final UUID complainId;
    private final String nodeName;
    private final String feedName;
    private final String postingId;
    private final String commentId;

    @Inject
    private SheriffComplainRepository sheriffComplainRepository;

    @Inject
    private SheriffDecisionRepository sheriffDecisionRepository;

    @Inject
    private SheriffOrderRepository sheriffOrderRepository;

    public SheriffComplainPrepareTask(UUID complainId, String nodeName, String feedName,
                                      String postingId, String commentId) {
        this.complainId = complainId;
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
                updateComplainStatus(SheriffComplainStatus.NOT_FOUND);
                return;
            } catch (NodeApiNotFoundException e) {
                if (e.getResult() != null && Objects.equals(e.getResult().getErrorCode(), "comment.wrong-posting")) {
                    updateComplainStatus(SheriffComplainStatus.INVALID_TARGET);
                } else {
                    updateComplainStatus(SheriffComplainStatus.NOT_FOUND);
                }
                return;
            } catch (Exception e) {
                log.error("Error preparing complain {}: {}", complainId, e.getMessage());
            }
            try {
                Thread.sleep(RETRY_DELAY.toMillis());
            } catch (InterruptedException e) {
                // ignore
            }
        }
        log.error("Reached max number of retries to prepare complain {}, giving up", complainId);

        updateComplainStatus(SheriffComplainStatus.PREPARE_FAILED);
    }

    private void prepare() throws NodeApiException {
        WhoAmI whoAmI = nodeApi.whoAmI(nodeName);
        FeedInfo feedInfo = nodeApi.getFeed(nodeName, feedName);
        if (feedInfo.getSheriffs() == null || !feedInfo.getSheriffs().contains(nodeName())) {
            updateComplainStatus(SheriffComplainStatus.NOT_SHERIFF);
            return;
        }
        PostingInfo postingInfo = postingId != null
                ? nodeApi.getPosting(nodeName, generateCarte(nodeName), postingId)
                : null;
        if (postingInfo != null) {
            boolean inFeed = postingInfo.getFeedReferences().stream().anyMatch(fr -> fr.getFeedName().equals(feedName));
            if (!inFeed) {
                updateComplainStatus(SheriffComplainStatus.INVALID_TARGET);
                return;
            }
            if (!postingInfo.isOriginal()) {
                updateComplainStatus(SheriffComplainStatus.NOT_ORIGINAL);
                return;
            }
        }
        CommentInfo commentInfo = commentId != null
                ? nodeApi.getComment(nodeName, generateCarte(nodeName), postingId, commentId)
                : null;

        updateComplain(complain -> {
            if (complain.getRemoteNodeFullName() == null) {
                complain.setRemoteNodeFullName(whoAmI.getFullName());
            }
            if (postingInfo != null) {
                complain.setRemotePostingOwnerName(postingInfo.getOwnerName());
                complain.setRemotePostingOwnerFullName(postingInfo.getOwnerFullName());
                complain.setRemotePostingOwnerGender(postingInfo.getOwnerGender());
                complain.setRemotePostingHeading(postingInfo.getHeading());
                complain.setRemotePostingRevisionId(postingInfo.getRevisionId());
            }
            if (commentInfo != null) {
                complain.setRemoteCommentOwnerName(commentInfo.getOwnerName());
                complain.setRemoteCommentOwnerFullName(commentInfo.getOwnerFullName());
                complain.setRemoteCommentOwnerGender(commentInfo.getOwnerGender());
                complain.setRemoteCommentHeading(commentInfo.getHeading());
                complain.setRemoteCommentRevisionId(commentInfo.getRevisionId());
            }
            complain.setStatus(SheriffComplainStatus.PREPARED);
        });
    }

    private void autoDecide() {
        PageRequest page = PageRequest.of(0, 1, Sort.Direction.DESC, "createdAt");

        List<SheriffComplain> decided;
        if (postingId == null) {
            decided = sheriffComplainRepository.findDecidedByFeed(nodeId, nodeName, feedName, page);
        } else if (commentId == null) {
            decided = sheriffComplainRepository.findDecidedByPosting(nodeId, nodeName, feedName, postingId, page);
        } else {
            decided = sheriffComplainRepository.findDecidedByComment(nodeId, nodeName, feedName, postingId, commentId,
                    page);
        }
        if (!decided.isEmpty()) {
            SheriffDecision decision = decided.get(0).getSheriffDecision();
            updateComplain(complain -> {
                complain.setSheriffDecision(decision);
                complain.setStatus(SheriffComplainStatus.DECIDED);
            });
            return;
        }

        List<SheriffOrder> orders;
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
                updateComplain(complain -> {
                    SheriffDecision decision = new SheriffDecision();
                    decision.setId(UUID.randomUUID());
                    decision.setAccepted(true);
                    decision.setReasonCode(order.getReasonCode());
                    decision.setReasonDetails(order.getReasonDetails());
                    decision = sheriffDecisionRepository.save(decision);
                    complain.setSheriffDecision(decision);
                    complain.setStatus(SheriffComplainStatus.DECIDED);
                });
            }
        }
    }

    private void updateComplain(Consumer<SheriffComplain> updater) {
        try {
            inTransaction(() -> {
                SheriffComplain complain = sheriffComplainRepository.findByNodeIdAndId(nodeId, complainId);
                updater.accept(complain);
                sheriffComplainRepository.save(complain);
                return null;
            });
        } catch (Throwable e) {
            log.error("Could not store complain", e);
            if (e instanceof RuntimeException) {
                throw (RuntimeException) e;
            }
        }
    }

    private void updateComplainStatus(SheriffComplainStatus status) {
        updateComplain(c -> c.setStatus(status));
    }

}
