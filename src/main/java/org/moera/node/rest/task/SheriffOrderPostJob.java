package org.moera.node.rest.task;

import java.security.interfaces.ECPrivateKey;
import java.time.Instant;
import java.util.UUID;
import javax.inject.Inject;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.moera.commons.crypto.CryptoUtil;
import org.moera.commons.crypto.Fingerprint;
import org.moera.node.api.node.NodeApiException;
import org.moera.node.data.SheriffComplainGroup;
import org.moera.node.data.SheriffComplainGroupRepository;
import org.moera.node.data.SheriffOrder;
import org.moera.node.data.SheriffOrderRepository;
import org.moera.node.fingerprint.Fingerprints;
import org.moera.node.fingerprint.SheriffOrderFingerprint;
import org.moera.node.liberin.model.SheriffOrderSentLiberin;
import org.moera.node.media.MediaManager;
import org.moera.node.model.AvatarDescription;
import org.moera.node.model.CommentInfo;
import org.moera.node.model.PostingInfo;
import org.moera.node.model.SheriffOrderAttributes;
import org.moera.node.model.SheriffOrderDetailsQ;
import org.moera.node.model.WhoAmI;
import org.moera.node.task.Job;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SheriffOrderPostJob extends Job<SheriffOrderPostJob.Parameters, SheriffOrderPostJob.State> {

    public static class Parameters {

        private String remoteNodeName;
        private SheriffOrderAttributes attributes;
        private UUID complainGroupId;

        public Parameters() {
        }

        public Parameters(String remoteNodeName, SheriffOrderAttributes attributes, UUID complainGroupId) {
            this.remoteNodeName = remoteNodeName;
            this.attributes = attributes;
            this.complainGroupId = complainGroupId;
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

        public UUID getComplainGroupId() {
            return complainGroupId;
        }

        public void setComplainGroupId(UUID complainGroupId) {
            this.complainGroupId = complainGroupId;
        }

    }

    public static class State {

        private UUID sheriffOrderId;
        private WhoAmI whoAmI;
        private PostingInfo postingInfo;
        private CommentInfo commentInfo;
        private boolean avatarUploaded;
        private SheriffOrderDetailsQ sheriffOrderDetails;

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

        public SheriffOrderDetailsQ getSheriffOrderDetails() {
            return sheriffOrderDetails;
        }

        public void setSheriffOrderDetails(SheriffOrderDetailsQ sheriffOrderDetails) {
            this.sheriffOrderDetails = sheriffOrderDetails;
        }

    }

    private static final Logger log = LoggerFactory.getLogger(SheriffOrderPostJob.class);

    @Inject
    private SheriffOrderRepository sheriffOrderRepository;

    @Inject
    private SheriffComplainGroupRepository sheriffComplainGroupRepository;

    @Inject
    private MediaManager mediaManager;

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
    protected void execute() throws NodeApiException {
        if (state.sheriffOrderId == null) {
            state.sheriffOrderId = UUID.randomUUID();
            checkpoint();
        }

        if (state.whoAmI == null) {
            state.whoAmI = nodeApi.whoAmI(parameters.remoteNodeName);
            checkpoint();
        }

        String postingId = parameters.attributes.getPostingId();
        String commentId = parameters.attributes.getCommentId();

        if (postingId != null && state.postingInfo == null) {
            state.postingInfo = nodeApi.getPosting(
                    parameters.remoteNodeName,
                    generateCarte(parameters.remoteNodeName),
                    postingId);
            checkpoint();
        }

        if (postingId != null && commentId != null && state.commentInfo == null) {
            state.commentInfo = nodeApi.getComment(
                    parameters.remoteNodeName,
                    generateCarte(parameters.remoteNodeName),
                    postingId,
                    commentId);
            checkpoint();
        }

        if (!state.avatarUploaded) {
            mediaManager.uploadPublicMedia(
                    parameters.remoteNodeName,
                    generateCarte(parameters.remoteNodeName),
                    getAvatar());
            state.avatarUploaded = true;
            checkpoint();
        }

        if (state.sheriffOrderDetails == null) {
            state.sheriffOrderDetails = new SheriffOrderDetailsQ(
                    state.sheriffOrderId.toString(),
                    nodeName(),
                    new AvatarDescription(getAvatar()),
                    parameters.attributes);
            state.sheriffOrderDetails.setCreatedAt(Instant.now().getEpochSecond());

            byte[] digest = null;
            if (state.postingInfo != null) {
                digest = state.commentInfo == null ? state.postingInfo.getDigest() : state.commentInfo.getDigest();
            }
            Fingerprint fingerprint = Fingerprints.sheriffOrder(SheriffOrderFingerprint.VERSION)
                    .create(parameters.remoteNodeName, state.sheriffOrderDetails, digest);

            state.sheriffOrderDetails.setSignature(CryptoUtil.sign(fingerprint, (ECPrivateKey) signingKey()));
            state.sheriffOrderDetails.setSignatureVersion(SheriffOrderFingerprint.VERSION);
            nodeApi.postSheriffOrder(parameters.remoteNodeName, state.sheriffOrderDetails);
            checkpoint();
        }

        try {
            SheriffOrder sheriffOrder = tx.executeWrite(
                () -> {
                    SheriffComplainGroup complainGroup = parameters.complainGroupId != null
                            ? sheriffComplainGroupRepository.findByNodeIdAndId(nodeId, parameters.complainGroupId)
                                    .orElse(null)
                            : null;

                    SheriffOrder order = buildSheriffOrder(complainGroup);
                    state.sheriffOrderDetails.toSheriffOrder(order);
                    return sheriffOrderRepository.save(order);
                }
            );

            send(new SheriffOrderSentLiberin(nodeName(), sheriffOrder));
        } catch (Exception e) {
            log.error("Could not store sheriff order", e);
        }
    }

    private SheriffOrder buildSheriffOrder(SheriffComplainGroup complainGroup) {
        SheriffOrder order = new SheriffOrder(state.sheriffOrderId, nodeId, parameters.remoteNodeName);
        order.setComplainGroup(complainGroup);
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
