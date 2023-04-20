package org.moera.node.rest.task;

import java.security.interfaces.ECPrivateKey;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import javax.inject.Inject;

import org.moera.commons.crypto.CryptoUtil;
import org.moera.commons.crypto.Fingerprint;
import org.moera.node.api.NodeApiException;
import org.moera.node.data.SheriffOrder;
import org.moera.node.data.SheriffOrderRepository;
import org.moera.node.fingerprint.Fingerprints;
import org.moera.node.fingerprint.SheriffOrderFingerprint;
import org.moera.node.model.CommentInfo;
import org.moera.node.model.PostingInfo;
import org.moera.node.model.SheriffOrderAttributes;
import org.moera.node.model.SheriffOrderDetailsQ;
import org.moera.node.task.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SheriffOrderPostTask extends Task {

    private static final Logger log = LoggerFactory.getLogger(SheriffOrderPostTask.class);

    private static final int MAX_RETRIES = 10;
    private static final Duration RETRY_DELAY = Duration.ofSeconds(10);

    private final String remoteNodeName;
    private final SheriffOrderAttributes attributes;
    private SheriffOrderDetailsQ sheriffOrderDetails;

    @Inject
    private SheriffOrderRepository sheriffOrderRepository;

    public SheriffOrderPostTask(String remoteNodeName, SheriffOrderAttributes attributes) {
        this.remoteNodeName = remoteNodeName;
        this.attributes = attributes;
    }

    @Override
    protected void execute() {
        for (int i = 0; i < MAX_RETRIES; i++) {
            try {
                post();
                return;
            } catch (Exception e) {
                log.error("Error posting sheriff order to node {}: {}", remoteNodeName, e.getMessage());
            }
            try {
                Thread.sleep(RETRY_DELAY.toMillis());
            } catch (InterruptedException e) {
                // ignore
            }
        }
        log.error("Reached max number of retries of posting sheriff order to node {}, giving up", remoteNodeName);
    }

    private void post() throws NodeApiException {
        if (sheriffOrderDetails == null) {
            String carte = generateCarte(remoteNodeName);
            String postingId = attributes.getPostingId();
            String commentId = attributes.getCommentId();
            byte[] digest = null;
            if (postingId != null) {
                if (commentId == null) {
                    PostingInfo postingInfo = nodeApi.getPosting(remoteNodeName, carte, postingId);
                    digest = postingInfo.getDigest();
                } else {
                    CommentInfo commentInfo = nodeApi.getComment(remoteNodeName, carte, postingId, commentId);
                    digest = commentInfo.getDigest();
                }
            }
            sheriffOrderDetails = new SheriffOrderDetailsQ(nodeName(), attributes);
            sheriffOrderDetails.setCreatedAt(Instant.now().getEpochSecond());
            Fingerprint fingerprint = Fingerprints.sheriffOrder(SheriffOrderFingerprint.VERSION)
                    .create(remoteNodeName, sheriffOrderDetails, digest);
            sheriffOrderDetails.setSignature(CryptoUtil.sign(fingerprint, (ECPrivateKey) signingKey()));
            sheriffOrderDetails.setSignatureVersion(SheriffOrderFingerprint.VERSION);
        }
        nodeApi.postSheriffOrder(remoteNodeName, sheriffOrderDetails);

        try {
            inTransaction(() -> {
                SheriffOrder sheriffOrder = new SheriffOrder(UUID.randomUUID(), nodeId, remoteNodeName);
                sheriffOrderDetails.toSheriffOrder(sheriffOrder);
                sheriffOrderRepository.save(sheriffOrder);
                return null;
            });
        } catch (Throwable e) {
            log.error("Could not store sheriff order", e);
        }
    }

}
