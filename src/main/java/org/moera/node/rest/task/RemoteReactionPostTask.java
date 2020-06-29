package org.moera.node.rest.task;

import java.security.interfaces.ECPrivateKey;
import java.util.UUID;

import javax.inject.Inject;

import org.moera.commons.crypto.CryptoUtil;
import org.moera.node.api.NodeApiUnknownNameException;
import org.moera.node.data.OwnReaction;
import org.moera.node.data.OwnReactionRepository;
import org.moera.node.fingerprint.PostingFingerprint;
import org.moera.node.fingerprint.ReactionFingerprint;
import org.moera.node.model.PostingInfo;
import org.moera.node.model.ReactionAttributes;
import org.moera.node.model.ReactionCreated;
import org.moera.node.model.ReactionDescription;
import org.moera.node.model.ReactionInfo;
import org.moera.node.model.event.RemoteReactionAddedEvent;
import org.moera.node.task.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RemoteReactionPostTask extends Task {

    private static Logger log = LoggerFactory.getLogger(RemoteReactionPostTask.class);

    private String targetNodeName;
    private String postingId;
    private ReactionAttributes attributes;

    @Inject
    private OwnReactionRepository ownReactionRepository;

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
            ReactionCreated created = nodeApi.postPostingReaction(targetNodeName, postingId, buildReaction(postingInfo));
            saveReaction(created.getReaction());
            success(created);
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

    private void saveReaction(ReactionInfo info) {
        try {
            inTransaction(() -> {
                OwnReaction ownReaction = ownReactionRepository
                        .findByRemotePostingId(nodeId, targetNodeName, postingId)
                        .orElse(null);
                if (ownReaction == null) {
                    ownReaction = new OwnReaction();
                    ownReaction.setId(UUID.randomUUID());
                    ownReaction.setNodeId(nodeId);
                    ownReaction.setRemoteNodeName(targetNodeName);
                    ownReaction = ownReactionRepository.save(ownReaction);
                }
                info.toOwnReaction(ownReaction);
                return null;
            });
        } catch (Throwable e) {
            error(e);
        }
        send(new RemoteReactionAddedEvent(targetNodeName, postingId, info));
    }

    private void success(ReactionCreated info) {
        initLoggingDomain();
        log.info("Succeeded to post reaction to posting {} at node {}",
                info.getReaction().getPostingId(), targetNodeName);
    }

    @Override
    protected void error(Throwable e) {
        initLoggingDomain();
        if (e instanceof NodeApiUnknownNameException) {
            log.error("Cannot find a node {}", targetNodeName);
        } else {
            log.error("Error adding reaction to posting {} at node {}: {}", postingId, targetNodeName, e.getMessage());
        }
    }

}
