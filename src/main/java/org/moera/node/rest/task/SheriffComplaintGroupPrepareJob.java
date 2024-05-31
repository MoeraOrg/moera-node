package org.moera.node.rest.task;

import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Consumer;
import javax.inject.Inject;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.moera.node.api.node.NodeApiException;
import org.moera.node.api.node.NodeApiNotFoundException;
import org.moera.node.api.node.NodeApiUnknownNameException;
import org.moera.node.data.SheriffComplaintGroup;
import org.moera.node.data.SheriffComplaintGroupRepository;
import org.moera.node.data.SheriffComplaintStatus;
import org.moera.node.data.SheriffOrder;
import org.moera.node.data.SheriffOrderRepository;
import org.moera.node.liberin.model.SheriffComplaintGroupUpdatedLiberin;
import org.moera.node.model.CommentInfo;
import org.moera.node.model.FeedInfo;
import org.moera.node.model.PostingInfo;
import org.moera.node.model.WhoAmI;
import org.moera.node.task.Job;
import org.moera.node.util.Util;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

public class SheriffComplaintGroupPrepareJob
        extends Job<SheriffComplaintGroupPrepareJob.Parameters, SheriffComplaintGroupPrepareJob.State> {

    public static class Parameters {

        private UUID groupId;
        private String nodeName;
        private String feedName;
        private String postingId;
        private String commentId;

        public Parameters() {
        }

        public Parameters(UUID groupId, String nodeName, String feedName, String postingId, String commentId) {
            this.groupId = groupId;
            this.nodeName = nodeName;
            this.feedName = feedName;
            this.postingId = postingId;
            this.commentId = commentId;
        }

        public UUID getGroupId() {
            return groupId;
        }

        public void setGroupId(UUID groupId) {
            this.groupId = groupId;
        }

        public String getNodeName() {
            return nodeName;
        }

        public void setNodeName(String nodeName) {
            this.nodeName = nodeName;
        }

        public String getFeedName() {
            return feedName;
        }

        public void setFeedName(String feedName) {
            this.feedName = feedName;
        }

        public String getPostingId() {
            return postingId;
        }

        public void setPostingId(String postingId) {
            this.postingId = postingId;
        }

        public String getCommentId() {
            return commentId;
        }

        public void setCommentId(String commentId) {
            this.commentId = commentId;
        }

    }

    public static class State {

        private boolean prepared;
        private WhoAmI whoAmI;
        private boolean feedSheriffChecked;
        private PostingInfo postingInfo;
        private CommentInfo commentInfo;

        public State() {
        }

        public boolean isPrepared() {
            return prepared;
        }

        public void setPrepared(boolean prepared) {
            this.prepared = prepared;
        }

        public WhoAmI getWhoAmI() {
            return whoAmI;
        }

        public void setWhoAmI(WhoAmI whoAmI) {
            this.whoAmI = whoAmI;
        }

        public boolean isFeedSheriffChecked() {
            return feedSheriffChecked;
        }

        public void setFeedSheriffChecked(boolean feedSheriffChecked) {
            this.feedSheriffChecked = feedSheriffChecked;
        }

        public PostingInfo getPostingInfo() {
            return postingInfo;
        }

        public void setPostingInfo(PostingInfo postingInfo) {
            this.postingInfo = postingInfo;
        }

        public CommentInfo getCommentInfo() {
            return commentInfo;
        }

        public void setCommentInfo(CommentInfo commentInfo) {
            this.commentInfo = commentInfo;
        }

    }

    private static final Logger log = LoggerFactory.getLogger(SheriffComplaintGroupPrepareJob.class);

    @Inject
    private SheriffComplaintGroupRepository sheriffComplaintGroupRepository;

    @Inject
    private SheriffOrderRepository sheriffOrderRepository;

    public SheriffComplaintGroupPrepareJob() {
        state = new State();
        retryCount(6, "PT10M");
    }

    @Override
    protected void setParameters(String parameters, ObjectMapper objectMapper) throws JsonProcessingException {
        this.parameters = objectMapper.readValue(parameters, Parameters.class);
    }

    @Override
    protected void setState(String state, ObjectMapper objectMapper) throws JsonProcessingException {
        this.state = objectMapper.readValue(state, State.class);
    }

    @Override
    protected void started() {
        super.started();
        log.info("Preparing complaint group {}", parameters.groupId);
    }

    @Override
    protected void execute() throws NodeApiException {
        try {
            if (!state.prepared) {
                prepare();
                state.prepared = true;
                checkpoint();
            }
            autoDecide();
        } catch (NodeApiUnknownNameException e) {
            updateComplaintGroupStatus(SheriffComplaintStatus.NOT_FOUND);
        } catch (NodeApiNotFoundException e) {
            if (e.getResult() != null && Objects.equals(e.getResult().getErrorCode(), "comment.wrong-posting")) {
                updateComplaintGroupStatus(SheriffComplaintStatus.INVALID_TARGET);
            } else {
                updateComplaintGroupStatus(SheriffComplaintStatus.NOT_FOUND);
            }
        }
    }

    private void prepare() throws NodeApiException {
        if (state.whoAmI == null) {
            state.whoAmI = nodeApi.whoAmI(parameters.nodeName);
            checkpoint();
        }

        if (!state.feedSheriffChecked) {
            FeedInfo feedInfo = nodeApi.getFeed(parameters.nodeName, parameters.feedName);
            if (feedInfo.getSheriffs() == null || !feedInfo.getSheriffs().contains(nodeName())) {
                updateComplaintGroupStatus(SheriffComplaintStatus.NOT_SHERIFF);
                return;
            }
            state.feedSheriffChecked = true;
            checkpoint();
        }

        if (state.postingInfo == null && parameters.postingId != null) {
            state.postingInfo = nodeApi.getPosting(
                    parameters.nodeName,
                    generateCarte(parameters.nodeName),
                    parameters.postingId);
            if (state.postingInfo != null) {
                boolean inFeed = state.postingInfo.getFeedReferences().stream()
                        .anyMatch(fr -> fr.getFeedName().equals(parameters.feedName));
                if (!inFeed) {
                    updateComplaintGroupStatus(SheriffComplaintStatus.INVALID_TARGET);
                    return;
                }
                if (!state.postingInfo.isOriginal()) {
                    updateComplaintGroupStatus(SheriffComplaintStatus.NOT_ORIGINAL);
                    return;
                }
            }
            checkpoint();
        }

        if (state.commentInfo == null && parameters.commentId != null) {
            state.commentInfo = nodeApi.getComment(
                    parameters.nodeName,
                    generateCarte(parameters.nodeName),
                    parameters.postingId,
                    parameters.commentId);
            checkpoint();
        }

        updateComplaintGroup(complaintGroup -> {
            if (complaintGroup.getRemoteNodeFullName() == null) {
                complaintGroup.setRemoteNodeFullName(state.whoAmI.getFullName());
            }
            if (state.postingInfo != null) {
                complaintGroup.setRemotePostingOwnerName(state.postingInfo.getOwnerName());
                complaintGroup.setRemotePostingOwnerFullName(state.postingInfo.getOwnerFullName());
                complaintGroup.setRemotePostingOwnerGender(state.postingInfo.getOwnerGender());
                complaintGroup.setRemotePostingHeading(state.postingInfo.getHeading());
                complaintGroup.setRemotePostingRevisionId(state.postingInfo.getRevisionId());
            }
            if (state.commentInfo != null) {
                complaintGroup.setRemoteCommentOwnerName(state.commentInfo.getOwnerName());
                complaintGroup.setRemoteCommentOwnerFullName(state.commentInfo.getOwnerFullName());
                complaintGroup.setRemoteCommentOwnerGender(state.commentInfo.getOwnerGender());
                complaintGroup.setRemoteCommentHeading(state.commentInfo.getHeading());
                complaintGroup.setRemoteCommentRevisionId(state.commentInfo.getRevisionId());
            }
            complaintGroup.setStatus(SheriffComplaintStatus.PREPARED);
        });
    }

    private void autoDecide() {
        List<SheriffOrder> orders;
        PageRequest page = PageRequest.of(0, 1, Sort.Direction.DESC, "createdAt");
        if (parameters.postingId == null) {
            orders = sheriffOrderRepository.findByFeed(
                    nodeId, parameters.nodeName, parameters.feedName, page);
        } else if (parameters.commentId == null) {
            orders = sheriffOrderRepository.findByPosting(
                    nodeId, parameters.nodeName, parameters.feedName, parameters.postingId, page);
        } else {
            orders = sheriffOrderRepository.findByComment(
                    nodeId, parameters.nodeName, parameters.feedName, parameters.postingId, parameters.commentId, page);
        }
        if (!orders.isEmpty()) {
            SheriffOrder order = orders.get(0);
            if (!order.isDelete()) {
                updateComplaintGroup(complaintGroup -> {
                    complaintGroup.setStatus(SheriffComplaintStatus.APPROVED);
                    complaintGroup.setDecisionCode(order.getReasonCode());
                    complaintGroup.setDecisionDetails(order.getReasonDetails());
                    complaintGroup.setDecidedAt(Util.now());
                });
            }
        }
    }

    private void updateComplaintGroup(Consumer<SheriffComplaintGroup> updater) {
        var liberin = tx.executeWrite(() -> {
            SheriffComplaintGroup complaintGroup =
                    sheriffComplaintGroupRepository.findByNodeIdAndId(nodeId, parameters.groupId).orElse(null);
            if (complaintGroup == null) {
                return null;
            }
            SheriffComplaintStatus prevStatus = complaintGroup.getStatus();
            updater.accept(complaintGroup);
            complaintGroup = sheriffComplaintGroupRepository.save(complaintGroup);
            return new SheriffComplaintGroupUpdatedLiberin(complaintGroup, prevStatus);
        });
        if (liberin != null) {
            send(liberin);
        }
    }

    private void updateComplaintGroupStatus(SheriffComplaintStatus status) {
        updateComplaintGroup(c -> c.setStatus(status));
    }

    @Override
    protected void failed() {
        super.failed();
        try {
            updateComplaintGroupStatus(SheriffComplaintStatus.PREPARE_FAILED);
        } catch (Exception e) {
            log.error("Error saving PREPARE_FAILED status", e);
        }
    }

}
