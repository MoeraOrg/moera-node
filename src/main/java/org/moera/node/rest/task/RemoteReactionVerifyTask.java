package org.moera.node.rest.task;

import java.lang.reflect.Constructor;
import java.util.function.Consumer;
import javax.inject.Inject;

import org.moera.commons.crypto.CryptoUtil;
import org.moera.commons.crypto.Fingerprint;
import org.moera.node.api.NodeApiNotFoundException;
import org.moera.node.data.RemoteReactionVerification;
import org.moera.node.data.RemoteReactionVerificationRepository;
import org.moera.node.data.VerificationStatus;
import org.moera.node.fingerprint.FingerprintManager;
import org.moera.node.fingerprint.FingerprintObjectType;
import org.moera.node.model.CommentInfo;
import org.moera.node.model.CommentRevisionInfo;
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
            String remoteNodeName = data.getNodeName();
            String remotePostingId = data.getPostingId();
            PostingInfo postingInfo = nodeApi.getPosting(remoteNodeName, remotePostingId);
            if (postingInfo.getReceiverName() != null) {
                remoteNodeName = postingInfo.getReceiverName();
                remotePostingId = postingInfo.getReceiverPostingId();
                postingInfo = nodeApi.getPosting(remoteNodeName, remotePostingId);
            }
            if (data.getCommentId() == null) {
                ReactionInfo reactionInfo = nodeApi.getPostingReaction(remoteNodeName, remotePostingId,
                        data.getReactionOwnerName());
                try {
                    PostingRevisionInfo postingRevisionInfo = nodeApi.getPostingRevision(remoteNodeName,
                            remotePostingId, reactionInfo.getPostingRevisionId());
                    verify(postingInfo, postingRevisionInfo, reactionInfo);
                } catch (NodeApiNotFoundException e) {
                    succeeded(false);
                }
            } else {
                ReactionInfo reactionInfo = nodeApi.getCommentReaction(remoteNodeName, remotePostingId,
                        data.getCommentId(), data.getReactionOwnerName());
                CommentInfo commentInfo = nodeApi.getComment(remoteNodeName, remotePostingId, data.getCommentId());
                try {
                    CommentRevisionInfo commentRevisionInfo = nodeApi.getCommentRevision(remoteNodeName,
                            remotePostingId, data.getCommentId(), reactionInfo.getCommentRevisionId());
                    PostingRevisionInfo postingRevisionInfo = nodeApi.getPostingRevision(remoteNodeName,
                            remotePostingId, commentInfo.getPostingRevisionId());
                    verify(postingInfo, postingRevisionInfo, commentInfo, commentRevisionInfo, reactionInfo);
                } catch (NodeApiNotFoundException e) {
                    succeeded(false);
                }
            }
        } catch (Exception e) {
            error(e);
        }
    }

    private Constructor<? extends Fingerprint> getFingerprintConstructor(short version, Class<?>... parameterTypes) {
        return fingerprintManager.getConstructor(FingerprintObjectType.REACTION, version, parameterTypes);
    }

    private void verify(PostingInfo postingInfo, PostingRevisionInfo postingRevisionInfo, ReactionInfo reactionInfo) {
        if (postingRevisionInfo.getSignature() == null) {
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
                reactionInfo.getSignature(), signingKey, constructor, reactionInfo, postingInfo, postingRevisionInfo));
    }

    private void verify(PostingInfo postingInfo, PostingRevisionInfo postingRevisionInfo,
                        CommentInfo commentInfo, CommentRevisionInfo commentRevisionInfo, ReactionInfo reactionInfo) {
        if (postingRevisionInfo.getSignature() == null || commentRevisionInfo.getSignature() == null) {
            succeeded(false);
            return;
        }

        byte[] signingKey = fetchSigningKey(reactionInfo.getOwnerName(), reactionInfo.getCreatedAt());
        if (signingKey == null) {
            succeeded(false);
            return;
        }

        Constructor<? extends Fingerprint> constructor = getFingerprintConstructor(
                reactionInfo.getSignatureVersion(), ReactionInfo.class, CommentInfo.class, CommentRevisionInfo.class,
                PostingInfo.class, PostingRevisionInfo.class);
        succeeded(CryptoUtil.verify(
                reactionInfo.getSignature(), signingKey, constructor, reactionInfo, commentInfo, commentRevisionInfo,
                postingInfo, postingRevisionInfo));
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
        String status = correct ? "correct" : "incorrect";
        if (data.getCommentId() == null) {
            log.info("Verified reaction of {} to posting {} at node {}: {}",
                    data.getReactionOwnerName(), data.getPostingId(), data.getNodeName(), status);
        } else {
            log.info("Verified reaction of {} to comment {} to posting {} at node {}: {}",
                    data.getReactionOwnerName(), data.getCommentId(), data.getPostingId(), data.getNodeName(), status);
        }
        updateData(data -> {
            data.setStatus(correct ? VerificationStatus.CORRECT : VerificationStatus.INCORRECT);
        });
        send(new RemoteReactionVerifiedEvent(data));
    }

    @Override
    protected void reportFailure(String errorCode, String errorMessage) {
        if (data.getCommentId() == null) {
            log.info("Verification of reaction of {} to posting {} at node {} failed: {} ({})",
                    data.getReactionOwnerName(), data.getPostingId(), data.getNodeName(), errorMessage, errorCode);
        } else {
            log.info("Verification of reaction of {} to comment {} to posting {} at node {} failed: {} ({})",
                    data.getReactionOwnerName(), data.getCommentId(), data.getPostingId(), data.getNodeName(),
                    errorMessage, errorCode);
        }
        updateData(data -> {
            data.setStatus(VerificationStatus.ERROR);
            data.setErrorCode(errorCode);
            data.setErrorMessage(errorMessage);
        });
        send(new RemoteReactionVerificationFailedEvent(data));
    }

}
