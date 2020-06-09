package org.moera.node.rest.task;

import java.security.interfaces.ECPrivateKey;

import org.moera.commons.crypto.CryptoUtil;
import org.moera.node.api.NodeApiUnknownNameException;
import org.moera.node.fingerprint.PostingFingerprint;
import org.moera.node.fingerprint.ReactionFingerprint;
import org.moera.node.model.PostingInfo;
import org.moera.node.model.ReactionAttributes;
import org.moera.node.model.ReactionCreated;
import org.moera.node.model.ReactionDescription;
import org.moera.node.task.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RemoteReactionPostTask extends Task {

    private static Logger log = LoggerFactory.getLogger(RemoteReactionPostTask.class);

    private String targetNodeName;
    private String postingId;
    private ReactionAttributes attributes;

    public RemoteReactionPostTask(String targetNodeName, String postingId, ReactionAttributes attributes) {
        this.targetNodeName = targetNodeName;
        this.postingId = postingId;
        this.attributes = attributes;
    }

    @Override
    public void run() {
        try {
            nodeApi.setNodeId(nodeId);
            PostingInfo postingInfo = nodeApi.getPosting(targetNodeName, postingId);
            success(nodeApi.postPostingReaction(targetNodeName, postingId, buildReaction(postingInfo)));
        } catch (Exception e) {
            error(e);
        }
    }

    private ReactionDescription buildReaction(PostingInfo postingInfo) {
        ReactionFingerprint fingerprint
                = new ReactionFingerprint(nodeName, attributes, new PostingFingerprint(postingInfo));
        ReactionDescription description = new ReactionDescription(nodeName, attributes);
        description.setSignature(CryptoUtil.sign(fingerprint, (ECPrivateKey) signingKey));
        description.setSignatureVersion(ReactionFingerprint.VERSION);
        return description;
    }

    private void success(ReactionCreated info) {
        initLoggingDomain();
        log.info("Succeeded to post reaction to posting {} at node {}",
                info.getReaction().getPostingId(), targetNodeName);
    }

    private void error(Throwable e) {
        initLoggingDomain();
        if (e instanceof NodeApiUnknownNameException) {
            log.error("Cannot find a node {}", targetNodeName);
        } else {
            log.error("Error adding reaction to posting {} at node {}: {}", postingId, targetNodeName, e.getMessage());
        }
    }

}
