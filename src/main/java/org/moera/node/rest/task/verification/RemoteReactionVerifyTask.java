package org.moera.node.rest.task.verification;

import java.util.function.Consumer;
import java.util.function.Function;
import jakarta.inject.Inject;

import org.moera.lib.crypto.CryptoUtil;
import org.moera.lib.node.exception.MoeraNodeApiNotFoundException;
import org.moera.lib.node.types.CommentInfo;
import org.moera.lib.node.types.CommentRevisionInfo;
import org.moera.lib.node.types.PostingInfo;
import org.moera.lib.node.types.PostingRevisionInfo;
import org.moera.lib.node.types.PrivateMediaFileInfo;
import org.moera.lib.node.types.ReactionInfo;
import org.moera.lib.node.types.Scope;
import org.moera.lib.node.types.VerificationStatus;
import org.moera.node.data.RemoteReactionVerification;
import org.moera.node.data.RemoteReactionVerificationRepository;
import org.moera.node.fingerprint.CommentFingerprintBuilder;
import org.moera.node.fingerprint.PostingFingerprintBuilder;
import org.moera.node.fingerprint.ReactionFingerprintBuilder;
import org.moera.node.liberin.model.RemoteReactionVerificationFailedLiberin;
import org.moera.node.liberin.model.RemoteReactionVerifiedLiberin;
import org.moera.node.media.MediaManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RemoteReactionVerifyTask extends RemoteVerificationTask {

    private static final Logger log = LoggerFactory.getLogger(RemoteReactionVerifyTask.class);

    private final RemoteReactionVerification data;

    @Inject
    private RemoteReactionVerificationRepository remoteReactionVerificationRepository;

    @Inject
    private MediaManager mediaManager;

    public RemoteReactionVerifyTask(RemoteReactionVerification data) {
        this.data = data;
    }

    @Override
    protected void execute() {
        try {
            String remoteNodeName = data.getNodeName();
            String remotePostingId = data.getPostingId();
            PostingInfo postingInfo = nodeApi
                .at(remoteNodeName, generateCarte(remoteNodeName, Scope.VIEW_CONTENT))
                .getPosting(remotePostingId, false);
            if (postingInfo.getReceiverName() != null) {
                remoteNodeName = postingInfo.getReceiverName();
                remotePostingId = postingInfo.getReceiverPostingId();
                postingInfo = nodeApi
                    .at(remoteNodeName, generateCarte(remoteNodeName, Scope.VIEW_CONTENT))
                    .getPosting(remotePostingId, false);
            }
            if (data.getCommentId() == null) {
                ReactionInfo reactionInfo = nodeApi
                    .at(remoteNodeName, generateCarte(remoteNodeName, Scope.VIEW_CONTENT))
                    .getPostingReaction(remotePostingId, data.getOwnerName());
                try {
                    PostingRevisionInfo postingRevisionInfo = nodeApi
                        .at(remoteNodeName, generateCarte(remoteNodeName, Scope.VIEW_CONTENT))
                        .getPostingRevision(remotePostingId, reactionInfo.getPostingRevisionId());
                    verify(postingInfo, postingRevisionInfo, reactionInfo);
                } catch (MoeraNodeApiNotFoundException e) {
                    succeeded(false);
                }
            } else {
                ReactionInfo reactionInfo = nodeApi
                    .at(remoteNodeName, generateCarte(remoteNodeName, Scope.VIEW_CONTENT))
                    .getCommentReaction(remotePostingId, data.getCommentId(), data.getOwnerName());
                CommentInfo commentInfo = nodeApi
                    .at(remoteNodeName, generateCarte(remoteNodeName, Scope.VIEW_CONTENT))
                    .getComment(remotePostingId, data.getCommentId(), false);
                try {
                    CommentRevisionInfo commentRevisionInfo = nodeApi
                        .at(remoteNodeName, generateCarte(remoteNodeName, Scope.VIEW_CONTENT))
                        .getCommentRevision(remotePostingId, data.getCommentId(), reactionInfo.getCommentRevisionId());
                    PostingRevisionInfo postingRevisionInfo = nodeApi
                        .at(remoteNodeName, generateCarte(remoteNodeName, Scope.VIEW_CONTENT))
                        .getPostingRevision(remotePostingId, commentInfo.getPostingRevisionId());
                    verify(postingInfo, postingRevisionInfo, commentInfo, commentRevisionInfo, reactionInfo);
                } catch (MoeraNodeApiNotFoundException e) {
                    succeeded(false);
                }
            }
        } catch (Exception e) {
            error(e);
        }
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

        byte[] postingParentMediaDigest = postingInfo.getParentMediaId() != null
            ? mediaManager.getPrivateMediaDigest(
                data.getNodeName(), generateCarte(data.getNodeName(), Scope.VIEW_MEDIA), postingInfo.getParentMediaId(),
                null
            )
            : null;
        Function<PrivateMediaFileInfo, byte[]> postingMediaDigest =
            pmf -> mediaManager.getPrivateMediaDigest(
                data.getNodeName(), generateCarte(data.getNodeName(), Scope.VIEW_MEDIA), pmf
            );

        byte[] fingerprint = ReactionFingerprintBuilder.build(
            reactionInfo.getSignatureVersion(),
            reactionInfo,
            PostingFingerprintBuilder.build(
                postingRevisionInfo.getSignatureVersion(),
                postingInfo,
                postingRevisionInfo,
                postingParentMediaDigest,
                postingMediaDigest
            )
        );
        succeeded(CryptoUtil.verifySignature(fingerprint, reactionInfo.getSignature(), signingKey));
    }

    private void verify(
        PostingInfo postingInfo,
        PostingRevisionInfo postingRevisionInfo,
        CommentInfo commentInfo,
        CommentRevisionInfo commentRevisionInfo,
        ReactionInfo reactionInfo
    ) {
        if (postingRevisionInfo.getSignature() == null || commentRevisionInfo.getSignature() == null) {
            succeeded(false);
            return;
        }

        byte[] signingKey = fetchSigningKey(reactionInfo.getOwnerName(), reactionInfo.getCreatedAt());
        if (signingKey == null) {
            succeeded(false);
            return;
        }

        byte[] parentMediaDigest = postingInfo.getParentMediaId() != null
            ? mediaManager.getPrivateMediaDigest(
                data.getNodeName(), generateCarte(data.getNodeName(), Scope.VIEW_MEDIA), postingInfo.getParentMediaId(),
                null
            )
            : null;
        Function<PrivateMediaFileInfo, byte[]> mediaDigest =
            pmf -> mediaManager.getPrivateMediaDigest(
                data.getNodeName(), generateCarte(data.getNodeName(), Scope.VIEW_MEDIA), pmf
            );

        byte[] fingerprint = ReactionFingerprintBuilder.build(
            reactionInfo.getSignatureVersion(),
            reactionInfo,
            CommentFingerprintBuilder.build(
                commentRevisionInfo.getSignatureVersion(),
                commentInfo,
                commentRevisionInfo,
                mediaDigest,
                PostingFingerprintBuilder.build(
                    postingRevisionInfo.getSignatureVersion(),
                    postingInfo,
                    postingRevisionInfo,
                    parentMediaDigest,
                    mediaDigest
                )
            )
        );
        succeeded(CryptoUtil.verifySignature(fingerprint, reactionInfo.getSignature(), signingKey));
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
            log.info(
                "Verified reaction of {} to posting {} at node {}: {}",
                data.getOwnerName(), data.getPostingId(), data.getNodeName(), status
            );
        } else {
            log.info(
                "Verified reaction of {} to comment {} to posting {} at node {}: {}",
                data.getOwnerName(), data.getCommentId(), data.getPostingId(), data.getNodeName(), status
            );
        }
        updateData(data -> data.setStatus(correct ? VerificationStatus.CORRECT : VerificationStatus.INCORRECT));
        send(new RemoteReactionVerifiedLiberin(data));
    }

    @Override
    protected void reportFailure(String errorCode, String errorMessage) {
        if (data.getCommentId() == null) {
            log.info(
                "Verification of reaction of {} to posting {} at node {} failed: {} ({})",
                data.getOwnerName(), data.getPostingId(), data.getNodeName(), errorMessage, errorCode
            );
        } else {
            log.info(
                "Verification of reaction of {} to comment {} to posting {} at node {} failed: {} ({})",
                data.getOwnerName(), data.getCommentId(), data.getPostingId(), data.getNodeName(),
                errorMessage, errorCode
            );
        }
        updateData(data -> {
            data.setStatus(VerificationStatus.ERROR);
            data.setErrorCode(errorCode);
            data.setErrorMessage(errorMessage);
        });
        send(new RemoteReactionVerificationFailedLiberin(data));
    }

}
