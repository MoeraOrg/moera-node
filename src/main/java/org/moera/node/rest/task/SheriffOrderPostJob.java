package org.moera.node.rest.task;

import java.security.interfaces.ECPrivateKey;
import java.time.Instant;
import java.util.UUID;
import jakarta.inject.Inject;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.moera.lib.crypto.CryptoUtil;
import org.moera.lib.node.exception.MoeraNodeException;
import org.moera.lib.node.types.CommentInfo;
import org.moera.lib.node.types.PostingInfo;
import org.moera.lib.node.types.Scope;
import org.moera.lib.node.types.SheriffOrderAttributes;
import org.moera.lib.node.types.SheriffOrderDetails;
import org.moera.lib.node.types.WhoAmI;
import org.moera.node.data.SheriffComplaintGroup;
import org.moera.node.data.SheriffComplaintGroupRepository;
import org.moera.node.data.SheriffOrder;
import org.moera.node.data.SheriffOrderRepository;
import org.moera.node.fingerprint.SheriffOrderFingerprintBuilder;
import org.moera.node.liberin.model.SheriffOrderSentLiberin;
import org.moera.node.media.MediaManager;
import org.moera.node.model.AvatarDescriptionUtil;
import org.moera.node.model.SheriffOrderDetailsUtil;
import org.moera.node.task.Job;
import org.moera.node.util.MomentFinder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SheriffOrderPostJob extends Job<SheriffOrderPostJob.Parameters, SheriffOrderPostJob.State> {

    public static class Parameters {

        private String remoteNodeName;
        private SheriffOrderAttributes attributes;
        private UUID complaintGroupId;

        public Parameters() {
        }

        public Parameters(String remoteNodeName, SheriffOrderAttributes attributes, UUID complaintGroupId) {
            this.remoteNodeName = remoteNodeName;
            this.attributes = attributes;
            this.complaintGroupId = complaintGroupId;
        }

        public String getRemoteNodeName() {
            return remoteNodeName;
        }

        public void setRemoteNodeName(String remoteNodeName) {
            this.remoteNodeName = remoteNodeName;
        }

        public SheriffOrderAttributes getAttributes() {
            return attributes;
        }

        public void setAttributes(SheriffOrderAttributes attributes) {
            this.attributes = attributes;
        }

        public UUID getComplaintGroupId() {
            return complaintGroupId;
        }

        public void setComplaintGroupId(UUID complaintGroupId) {
            this.complaintGroupId = complaintGroupId;
        }

    }

    public static class State {

        private UUID sheriffOrderId;
        private WhoAmI whoAmI;
        private PostingInfo postingInfo;
        private CommentInfo commentInfo;
        private boolean avatarUploaded;
        private SheriffOrderDetails sheriffOrderDetails;

        public State() {
        }

        public UUID getSheriffOrderId() {
            return sheriffOrderId;
        }

        public void setSheriffOrderId(UUID sheriffOrderId) {
            this.sheriffOrderId = sheriffOrderId;
        }

        public WhoAmI getWhoAmI() {
            return whoAmI;
        }

        public void setWhoAmI(WhoAmI whoAmI) {
            this.whoAmI = whoAmI;
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

        public boolean isAvatarUploaded() {
            return avatarUploaded;
        }

        public void setAvatarUploaded(boolean avatarUploaded) {
            this.avatarUploaded = avatarUploaded;
        }

        public SheriffOrderDetails getSheriffOrderDetails() {
            return sheriffOrderDetails;
        }

        public void setSheriffOrderDetails(SheriffOrderDetails sheriffOrderDetails) {
            this.sheriffOrderDetails = sheriffOrderDetails;
        }

    }

    private static final Logger log = LoggerFactory.getLogger(SheriffOrderPostJob.class);

    @Inject
    private SheriffOrderRepository sheriffOrderRepository;

    @Inject
    private SheriffComplaintGroupRepository sheriffComplaintGroupRepository;

    @Inject
    private MediaManager mediaManager;

    private final MomentFinder momentFinder = new MomentFinder();

    public SheriffOrderPostJob() {
        state = new State();
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
        log.info("Posting sheriff order to node {}", parameters.remoteNodeName);
    }

    @Override
    protected void execute() throws MoeraNodeException {
        if (state.sheriffOrderId == null) {
            state.sheriffOrderId = UUID.randomUUID();
            checkpoint();
        }

        if (state.whoAmI == null) {
            state.whoAmI = nodeApi.at(parameters.remoteNodeName).whoAmI();
            checkpoint();
        }

        String postingId = parameters.attributes.getPostingId();
        String commentId = parameters.attributes.getCommentId();

        if (postingId != null && state.postingInfo == null) {
            state.postingInfo = nodeApi
                .at(parameters.remoteNodeName, generateCarte(parameters.remoteNodeName, Scope.SHERIFF))
                .getPosting(postingId, false);
            checkpoint();
        }

        if (postingId != null && commentId != null && state.commentInfo == null) {
            state.commentInfo = nodeApi
                .at(parameters.remoteNodeName, generateCarte(parameters.remoteNodeName, Scope.SHERIFF))
                .getComment(postingId, commentId, false);
            checkpoint();
        }

        if (!state.avatarUploaded) {
            mediaManager.uploadPublicMedia(
                parameters.remoteNodeName,
                generateCarte(parameters.remoteNodeName, Scope.UPLOAD_PUBLIC_MEDIA),
                getAvatar()
            );
            state.avatarUploaded = true;
            checkpoint();
        }

        if (state.sheriffOrderDetails == null) {
            state.sheriffOrderDetails = SheriffOrderDetailsUtil.build(
                state.sheriffOrderId.toString(),
                nodeName(),
                AvatarDescriptionUtil.build(getAvatar()),
                parameters.attributes
            );
            state.sheriffOrderDetails.setCreatedAt(Instant.now().getEpochSecond());

            byte[] digest = null;
            if (state.postingInfo != null) {
                digest = state.commentInfo == null ? state.postingInfo.getDigest() : state.commentInfo.getDigest();
            }
            byte[] fingerprint = SheriffOrderFingerprintBuilder.build(
                parameters.remoteNodeName, state.sheriffOrderDetails, digest
            );
            state.sheriffOrderDetails.setSignature(CryptoUtil.sign(fingerprint, (ECPrivateKey) signingKey()));
            state.sheriffOrderDetails.setSignatureVersion(SheriffOrderFingerprintBuilder.LATEST_VERSION);
            nodeApi.at(parameters.remoteNodeName).createSheriffOrder(state.sheriffOrderDetails);
            checkpoint();
        }

        try {
            SheriffOrder sheriffOrder = tx.executeWrite(
                () -> {
                    SheriffComplaintGroup complaintGroup = parameters.complaintGroupId != null
                        ? sheriffComplaintGroupRepository.findByNodeIdAndId(nodeId, parameters.complaintGroupId)
                            .orElse(null)
                        : null;

                    SheriffOrder order = buildSheriffOrder(complaintGroup);
                    SheriffOrderDetailsUtil.toSheriffOrder(state.sheriffOrderDetails, order);
                    order.setMoment(
                        momentFinder.find(
                            moment -> sheriffOrderRepository.countMoments(getNodeId(), moment) == 0,
                            order.getCreatedAt()
                        )
                    );
                    return sheriffOrderRepository.save(order);
                }
            );

            send(new SheriffOrderSentLiberin(nodeName(), sheriffOrder));
        } catch (Exception e) {
            log.error("Could not store sheriff order", e);
        }
    }

    private SheriffOrder buildSheriffOrder(SheriffComplaintGroup complaintGroup) {
        SheriffOrder order = new SheriffOrder(state.sheriffOrderId, nodeId, parameters.remoteNodeName);
        order.setComplaintGroup(complaintGroup);
        order.setRemoteNodeFullName(state.whoAmI.getFullName());
        if (state.postingInfo != null) {
            order.setRemotePosting(state.postingInfo);
        }
        if (state.commentInfo != null) {
            order.setRemoteComment(state.commentInfo);
        }
        return order;
    }

}
