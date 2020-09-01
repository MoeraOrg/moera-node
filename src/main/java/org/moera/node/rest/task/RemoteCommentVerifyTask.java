package org.moera.node.rest.task;

import java.lang.reflect.Constructor;
import java.util.Arrays;
import java.util.function.Consumer;
import javax.inject.Inject;

import org.moera.commons.crypto.CryptoUtil;
import org.moera.commons.crypto.Fingerprint;
import org.moera.node.data.RemoteCommentVerification;
import org.moera.node.data.RemoteCommentVerificationRepository;
import org.moera.node.data.VerificationStatus;
import org.moera.node.fingerprint.FingerprintManager;
import org.moera.node.fingerprint.FingerprintObjectType;
import org.moera.node.model.CommentInfo;
import org.moera.node.model.CommentRevisionInfo;
import org.moera.node.model.PostingInfo;
import org.moera.node.model.PostingRevisionInfo;
import org.moera.node.model.event.RemoteCommentVerificationFailedEvent;
import org.moera.node.model.event.RemoteCommentVerifiedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RemoteCommentVerifyTask extends RemoteVerificationTask {

    private static Logger log = LoggerFactory.getLogger(RemoteCommentVerifyTask.class);

    private RemoteCommentVerification data;

    @Inject
    private RemoteCommentVerificationRepository remoteCommentVerificationRepository;

    @Inject
    private FingerprintManager fingerprintManager;

    public RemoteCommentVerifyTask(RemoteCommentVerification data) {
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
            PostingRevisionInfo[] revisions = nodeApi.getPostingRevisions(remoteNodeName, remotePostingId);
            CommentInfo commentInfo = nodeApi.getComment(remoteNodeName, remotePostingId, data.getCommentId());
            if (data.getRevisionId() == null) {
                verify(postingInfo, revisions, commentInfo);
            } else {
                CommentRevisionInfo revisionInfo = nodeApi.getCommentRevision(data.getNodeName(), data.getPostingId(),
                        data.getCommentId(), data.getRevisionId());
                verify(postingInfo, revisions, commentInfo, revisionInfo);
            }
        } catch (Exception e) {
            error(e);
        }
    }

    private Constructor<? extends Fingerprint> getFingerprintConstructor(short version, Class<?>... parameterTypes) {
        return fingerprintManager.getConstructor(FingerprintObjectType.COMMENT, version, parameterTypes);
    }

    private PostingRevisionInfo getPostingRevisionByComment(PostingRevisionInfo[] revisions, CommentInfo commentInfo) {
        return Arrays.stream(revisions)
                .filter(r -> r.getCreatedAt() <= commentInfo.getEditedAt())
                .filter(r -> r.getDeletedAt() == null || r.getDeletedAt() > commentInfo.getEditedAt())
                .findFirst()
                .orElse(null);
    }

    private void verify(PostingInfo postingInfo, PostingRevisionInfo[] revisions, CommentInfo commentInfo) {
        PostingRevisionInfo revisionInfo = getPostingRevisionByComment(revisions, commentInfo);
        if (revisionInfo == null || revisionInfo.getSignature() == null) {
            succeeded(false);
            return;
        }

        byte[] signingKey = fetchSigningKey(commentInfo.getOwnerName(), commentInfo.getEditedAt());
        if (signingKey == null) {
            succeeded(false);
            return;
        }

        updateData(data -> {
            data.setRevisionId(commentInfo.getRevisionId());
        });

        Constructor<? extends Fingerprint> constructor = getFingerprintConstructor(
                commentInfo.getSignatureVersion(), CommentInfo.class, PostingInfo.class, PostingRevisionInfo.class);
        succeeded(CryptoUtil.verify(
                commentInfo.getSignature(), signingKey, constructor, commentInfo, postingInfo, revisionInfo));
    }

    private void verify(PostingInfo postingInfo, PostingRevisionInfo[] revisions, CommentInfo commentInfo,
                        CommentRevisionInfo commentRevisionInfo) {
        PostingRevisionInfo postingRevisionInfo = getPostingRevisionByComment(revisions, commentInfo);
        if (postingRevisionInfo == null || postingRevisionInfo.getSignature() == null) {
            succeeded(false);
            return;
        }

        byte[] signingKey = fetchSigningKey(commentInfo.getOwnerName(), commentRevisionInfo.getCreatedAt());
        if (signingKey == null) {
            succeeded(false);
            return;
        }

        Constructor<? extends Fingerprint> constructor = getFingerprintConstructor(
                commentInfo.getSignatureVersion(), CommentInfo.class, CommentRevisionInfo.class,
                PostingInfo.class, PostingRevisionInfo.class);
        succeeded(CryptoUtil.verify(
                commentInfo.getSignature(), signingKey, constructor, commentInfo, commentRevisionInfo,
                postingInfo, postingRevisionInfo));
    }

    private void updateData(Consumer<RemoteCommentVerification> updater) {
        updater.accept(data);
        RemoteCommentVerification status = remoteCommentVerificationRepository.findById(data.getId()).orElse(null);
        if (status == null) {
            return;
        }
        updater.accept(status);
        remoteCommentVerificationRepository.saveAndFlush(status);
    }

    @Override
    protected void reportSuccess(boolean correct) {
        log.info("Verified comment {} to posting {} at node {}: {}",
                data.getCommentId(), data.getPostingId(), data.getNodeName(),
                correct ? "correct" : "incorrect");
        updateData(data -> {
            data.setStatus(correct ? VerificationStatus.CORRECT : VerificationStatus.INCORRECT);
        });
        send(new RemoteCommentVerifiedEvent(data));
    }

    @Override
    protected void reportFailure(String errorCode, String errorMessage) {
        log.info("Verification of comment {} to posting {} at node {} failed: {} ({})",
                data.getCommentId(), data.getPostingId(), data.getNodeName(), errorMessage, errorCode);
        updateData(data -> {
            data.setStatus(VerificationStatus.ERROR);
            data.setErrorCode(errorCode);
            data.setErrorMessage(errorMessage);
        });
        send(new RemoteCommentVerificationFailedEvent(data));
    }

}
