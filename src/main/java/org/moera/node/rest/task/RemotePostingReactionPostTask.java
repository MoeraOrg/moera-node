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
import org.moera.node.instant.PostingReactionInstants;
import org.moera.node.media.MediaManager;
import org.moera.node.model.PostingInfo;
import org.moera.node.model.ReactionAttributes;
import org.moera.node.model.ReactionCreated;
import org.moera.node.model.ReactionDescription;
import org.moera.node.model.ReactionInfo;
import org.moera.node.model.event.RemoteReactionAddedEvent;
import org.moera.node.operations.ContactOperations;
import org.moera.node.task.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RemotePostingReactionPostTask extends Task {

    private static Logger log = LoggerFactory.getLogger(RemotePostingReactionPostTask.class);

    private String targetNodeName;
    private String targetFullName;
    private String postingId;
    private ReactionAttributes attributes;
    private PostingInfo postingInfo;

    @Inject
    private OwnReactionRepository ownReactionRepository;

    @Inject
    private ContactOperations contactOperations;

    @Inject
    private PostingReactionInstants postingReactionInstants;

    @Inject
    private MediaManager mediaManager;

    public RemotePostingReactionPostTask(String targetNodeName, String postingId, ReactionAttributes attributes) {
        this.targetNodeName = targetNodeName;
        this.postingId = postingId;
        this.attributes = attributes;
    }

    @Override
    protected void execute() {
        try {
            targetFullName = nodeApi.whoAmI(targetNodeName).getFullName();
            mediaManager.uploadPublicMedia(targetNodeName, generateCarte(targetNodeName), getAvatar());
            postingInfo = nodeApi.getPosting(targetNodeName, postingId);
            ReactionCreated created = nodeApi.postPostingReaction(targetNodeName, postingId, buildReaction(postingInfo));
            saveReaction(created.getReaction());
            success(created);
        } catch (Exception e) {
            error(e);
        }
    }

    private ReactionDescription buildReaction(PostingInfo postingInfo) {
        ReactionFingerprint fingerprint
                = new ReactionFingerprint(nodeName(), attributes, new PostingFingerprint(postingInfo));
        ReactionDescription description = new ReactionDescription(nodeName(), fullName(), getAvatar(), attributes);
        description.setSignature(CryptoUtil.sign(fingerprint, (ECPrivateKey) signingKey()));
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
                    ownReaction.setRemoteFullName(targetFullName);
                    ownReaction = ownReactionRepository.save(ownReaction);
                    contactOperations.updateCloseness(nodeId, targetNodeName, 0.25f);
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
        log.info("Succeeded to post reaction to posting {} at node {}",
                info.getReaction().getPostingId(), targetNodeName);
    }

    private void error(Throwable e) {
        if (e instanceof NodeApiUnknownNameException) {
            log.error("Cannot find a node {}", targetNodeName);
        } else {
            log.error("Error adding reaction to posting {} at node {}: {}", postingId, targetNodeName, e.getMessage());
        }

        postingReactionInstants.addingFailed(postingId, postingInfo);
    }

}
