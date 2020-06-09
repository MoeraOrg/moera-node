package org.moera.node.rest.task;

import java.lang.reflect.Constructor;
import java.util.Arrays;
import java.util.function.Consumer;
import javax.inject.Inject;

import org.moera.commons.crypto.CryptoUtil;
import org.moera.commons.crypto.Fingerprint;
import org.moera.node.data.RemoteReactionVerification;
import org.moera.node.data.RemoteReactionVerificationRepository;
import org.moera.node.data.VerificationStatus;
import org.moera.node.fingerprint.FingerprintManager;
import org.moera.node.fingerprint.FingerprintObjectType;
import org.moera.node.model.PostingInfo;
import org.moera.node.model.PostingRevisionInfo;
import org.moera.node.model.ReactionInfo;
import org.moera.node.model.event.RemoteReactionVerificationFailedEvent;
import org.moera.node.model.event.RemoteReactionVerifiedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RemoteReactionVerifyTask extends RemoteVerificationTask {

    private static Logger log = LoggerFactory.getLogger(RemoteReactionVerifyTask.class);

    private RemoteReactionVerification data;

    @Inject
    private RemoteReactionVerificationRepository remoteReactionVerificationRepository;

    @Inject
    private FingerprintManager fingerprintManager;

    public RemoteReactionVerifyTask(RemoteReactionVerification data) {
        this.data = data;
    }

    @Override
    public void run() {
        try {
            nodeApi.setNodeId(nodeId);
            PostingInfo postingInfo = nodeApi.getPosting(data.getNodeName(), data.getPostingId());
            PostingRevisionInfo[] revisions = nodeApi.getPostingRevisions(data.getNodeName(), data.getPostingId());
            ReactionInfo reactionInfo = nodeApi.getPostingReaction(data.getNodeName(), data.getPostingId(),
                    data.getReactionOwnerName());
            verify(postingInfo, revisions, reactionInfo);
        } catch (Exception e) {
            error(e);
        }
    }

    private Constructor<? extends Fingerprint> getFingerprintConstructor(short version, Class<?>... parameterTypes) {
        return fingerprintManager.getConstructor(FingerprintObjectType.REACTION, version, parameterTypes);
    }

    private void verify(PostingInfo postingInfo, PostingRevisionInfo[] revisions, ReactionInfo reactionInfo) {
        PostingRevisionInfo revisionInfo = Arrays.stream(revisions)
                .filter(r -> r.getCreatedAt() <= reactionInfo.getCreatedAt())
                .filter(r -> r.getDeletedAt() == null || r.getDeletedAt() > reactionInfo.getCreatedAt())
                .findFirst()
                .orElse(null);
        if (revisionInfo == null || revisionInfo.getSignature() == null) {
            succeeded(false);
            return;
        }

        byte[] signingKey = fetchSigningKey(reactionInfo.getOwnerName(), reactionInfo.getCreatedAt());
        if (signingKey == null) {
            succeeded(false);
            return;
        }

        Constructor<? extends Fingerprint> constructor = getFingerprintConstructor(
                reactionInfo.getSignatureVersion(), ReactionInfo.class, PostingInfo.class, PostingRevisionInfo.class);
        succeeded(CryptoUtil.verify(
                reactionInfo.getSignature(), signingKey, constructor, reactionInfo, postingInfo, revisionInfo));
    }

    private void updateData(Consumer<RemoteReactionVerification> updater) {
        updater.accept(data);
        RemoteReactionVerification status = remoteReactionVerificationRepository.findById(data.getId()).orElse(null);
        if (status == null) {
            return;
        }
        updater.accept(status);
        remoteReactionVerificationRepository.saveAndFlush(status);
    }

    @Override
    protected void reportSuccess(boolean correct) {
        log.info("Verified reaction of {} to posting {} at node {}: {}",
                data.getReactionOwnerName(), data.getPostingId(), data.getNodeName(),
                correct ? "correct" : "incorrect");
        updateData(data -> {
            data.setStatus(correct ? VerificationStatus.CORRECT : VerificationStatus.INCORRECT);
        });
        send(new RemoteReactionVerifiedEvent(data));
    }

    @Override
    protected void reportFailure(String errorCode, String errorMessage) {
        log.info("Verification of reaction of {} to posting {} at node {} failed: {} ({})",
                data.getReactionOwnerName(), data.getPostingId(), data.getNodeName(), errorMessage, errorCode);
        updateData(data -> {
            data.setStatus(VerificationStatus.ERROR);
            data.setErrorCode(errorCode);
            data.setErrorMessage(errorMessage);
        });
        send(new RemoteReactionVerificationFailedEvent(data));
    }

}
