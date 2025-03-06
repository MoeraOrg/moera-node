package org.moera.node.rest.task.verification;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;
import jakarta.inject.Inject;

import org.moera.lib.crypto.CryptoUtil;
import org.moera.lib.node.exception.MoeraNodeApiNotFoundException;
import org.moera.lib.node.exception.MoeraNodeException;
import org.moera.lib.node.types.CommentInfo;
import org.moera.lib.node.types.CommentRevisionInfo;
import org.moera.lib.node.types.PostingInfo;
import org.moera.lib.node.types.PostingRevisionInfo;
import org.moera.lib.node.types.PrivateMediaFileInfo;
import org.moera.lib.node.types.Scope;
import org.moera.lib.node.types.VerificationStatus;
import org.moera.node.data.RemoteCommentVerification;
import org.moera.node.data.RemoteCommentVerificationRepository;
import org.moera.node.fingerprint.CommentFingerprintBuilder;
import org.moera.node.fingerprint.PostingFingerprintBuilder;
import org.moera.node.liberin.model.RemoteCommentVerificationFailedLiberin;
import org.moera.node.liberin.model.RemoteCommentVerifiedLiberin;
import org.moera.node.media.MediaManager;
import org.moera.node.rest.task.RepliedToDigestVerifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RemoteCommentVerifyTask extends RemoteVerificationTask {

    private static final Logger log = LoggerFactory.getLogger(RemoteCommentVerifyTask.class);

    private final RemoteCommentVerification data;

    private String remoteNodeName;

    @Inject
    private RemoteCommentVerificationRepository remoteCommentVerificationRepository;

    @Inject
    private MediaManager mediaManager;

    @Inject
    private RepliedToDigestVerifier repliedToDigestVerifier;

    public RemoteCommentVerifyTask(RemoteCommentVerification data) {
        this.data = data;
    }

    @Override
    protected void execute() {
        try {
            remoteNodeName = data.getNodeName();
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
            CommentInfo commentInfo = nodeApi
                .at(remoteNodeName, generateCarte(remoteNodeName, Scope.VIEW_CONTENT))
                .getComment(remotePostingId, data.getCommentId(), false);
            if (data.getRevisionId() == null) {
                verify(postingInfo, commentInfo);
            } else {
                CommentRevisionInfo revisionInfo = nodeApi
                    .at(data.getNodeName(), generateCarte(data.getNodeName(), Scope.VIEW_CONTENT))
                    .getCommentRevision(data.getPostingId(), data.getCommentId(), data.getRevisionId());
                verify(postingInfo, commentInfo, revisionInfo);
            }
        } catch (Exception e) {
            error(e);
        }
    }

    private void verify(PostingInfo postingInfo, CommentInfo commentInfo) throws MoeraNodeException {
        PostingRevisionInfo revisionInfo;
        try {
            revisionInfo = nodeApi
                .at(remoteNodeName, generateCarte(remoteNodeName, Scope.VIEW_CONTENT))
                .getPostingRevision(postingInfo.getId(), commentInfo.getPostingRevisionId());
        } catch (MoeraNodeApiNotFoundException e) {
            succeeded(false);
            return;
        }
        if (revisionInfo.getSignature() == null) {
            succeeded(false);
            return;
        }
        Map<String, PostingRevisionInfo> revisions = new HashMap<>();
        revisions.put(revisionInfo.getId(), revisionInfo);

        byte[] signingKey = fetchSigningKey(commentInfo.getOwnerName(), commentInfo.getEditedAt());
        if (signingKey == null) {
            succeeded(false);
            return;
        }

        updateData(data -> data.setRevisionId(commentInfo.getRevisionId()));

        byte[] parentMediaDigest = postingInfo.getParentMediaId() != null
                ? mediaManager.getPrivateMediaDigest(
                    remoteNodeName, generateCarte(remoteNodeName, Scope.VIEW_MEDIA), postingInfo.getParentMediaId(),
                    null
                )
                : null;
        Function<PrivateMediaFileInfo, byte[]> mediaDigest =
                pmf -> mediaManager.getPrivateMediaDigest(
                    remoteNodeName, generateCarte(remoteNodeName, Scope.VIEW_MEDIA), pmf
                );

        String repliedToId = null;
        String repliedToRevisionId = null;
        if (commentInfo.getRepliedTo() != null) {
            repliedToId = commentInfo.getRepliedTo().getId();
            repliedToRevisionId = commentInfo.getRepliedTo().getRevisionId();
        }
        byte[] repliedToDigest = repliedToDigestVerifier.getRepliedToDigest(
            remoteNodeName, this::generateCarte, postingInfo, revisions, repliedToId, repliedToRevisionId
        );
        byte[] fingerprint = CommentFingerprintBuilder.build(
            commentInfo.getSignatureVersion(),
            commentInfo,
            mediaDigest,
            PostingFingerprintBuilder.build(
                revisionInfo.getSignatureVersion(),
                postingInfo,
                revisionInfo,
                parentMediaDigest,
                mediaDigest
            ),
            repliedToDigest
        );
        succeeded(CryptoUtil.verifySignature(fingerprint, commentInfo.getSignature(), signingKey));
    }

    private void verify(
        PostingInfo postingInfo, CommentInfo commentInfo, CommentRevisionInfo commentRevisionInfo
    ) throws MoeraNodeException {
        PostingRevisionInfo postingRevisionInfo;
        try {
            postingRevisionInfo = nodeApi
                .at(remoteNodeName, generateCarte(remoteNodeName, Scope.VIEW_CONTENT))
                .getPostingRevision(postingInfo.getId(), commentInfo.getPostingRevisionId());
        } catch (MoeraNodeApiNotFoundException e) {
            succeeded(false);
            return;
        }
        if (postingRevisionInfo.getSignature() == null) {
            succeeded(false);
            return;
        }
        Map<String, PostingRevisionInfo> revisions = new HashMap<>();
        revisions.put(postingRevisionInfo.getId(), postingRevisionInfo);

        byte[] signingKey = fetchSigningKey(commentInfo.getOwnerName(), commentRevisionInfo.getCreatedAt());
        if (signingKey == null) {
            succeeded(false);
            return;
        }

        byte[] parentMediaDigest = postingInfo.getParentMediaId() != null
            ? mediaManager.getPrivateMediaDigest(
                remoteNodeName, generateCarte(remoteNodeName, Scope.VIEW_MEDIA), postingInfo.getParentMediaId(), null
            )
            : null;
        Function<PrivateMediaFileInfo, byte[]> mediaDigest =
            pmf -> mediaManager.getPrivateMediaDigest(
                remoteNodeName, generateCarte(remoteNodeName, Scope.VIEW_MEDIA), pmf
            );

        String repliedToId = null;
        String repliedToRevisionId = null;
        if (commentInfo.getRepliedTo() != null) {
            repliedToId = commentInfo.getRepliedTo().getId();
            repliedToRevisionId = commentInfo.getRepliedTo().getRevisionId();
        }
        byte[] repliedToDigest = repliedToDigestVerifier.getRepliedToDigest(
            remoteNodeName, this::generateCarte, postingInfo, revisions, repliedToId, repliedToRevisionId
        );
        byte[] fingerprint = CommentFingerprintBuilder.build(
            commentInfo.getSignatureVersion(),
            commentInfo,
            commentRevisionInfo,
            mediaDigest,
            PostingFingerprintBuilder.build(
                postingRevisionInfo.getSignatureVersion(),
                postingInfo,
                postingRevisionInfo,
                parentMediaDigest,
                mediaDigest
            ),
            repliedToDigest
        );
        succeeded(CryptoUtil.verifySignature(fingerprint, commentInfo.getSignature(), signingKey));
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
        log.info(
            "Verified comment {} to posting {} at node {}: {}",
            data.getCommentId(), data.getPostingId(), data.getNodeName(), correct ? "correct" : "incorrect"
        );
        updateData(data -> data.setStatus(correct ? VerificationStatus.CORRECT : VerificationStatus.INCORRECT));
        send(new RemoteCommentVerifiedLiberin(data));
    }

    @Override
    protected void reportFailure(String errorCode, String errorMessage) {
        log.info(
            "Verification of comment {} to posting {} at node {} failed: {} ({})",
            data.getCommentId(), data.getPostingId(), data.getNodeName(), errorMessage, errorCode
        );
        updateData(data -> {
            data.setStatus(VerificationStatus.ERROR);
            data.setErrorCode(errorCode);
            data.setErrorMessage(errorMessage);
        });
        send(new RemoteCommentVerificationFailedLiberin(data));
    }

}
