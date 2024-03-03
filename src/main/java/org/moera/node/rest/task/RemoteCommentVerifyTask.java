package org.moera.node.rest.task;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;
import javax.inject.Inject;

import org.moera.commons.crypto.CryptoUtil;
import org.moera.commons.crypto.Fingerprint;
import org.moera.node.api.node.NodeApiException;
import org.moera.node.api.node.NodeApiNotFoundException;
import org.moera.node.data.RemoteCommentVerification;
import org.moera.node.data.RemoteCommentVerificationRepository;
import org.moera.node.data.VerificationStatus;
import org.moera.node.fingerprint.Fingerprints;
import org.moera.node.liberin.model.RemoteCommentVerificationFailedLiberin;
import org.moera.node.liberin.model.RemoteCommentVerifiedLiberin;
import org.moera.node.media.MediaManager;
import org.moera.node.model.CommentInfo;
import org.moera.node.model.CommentRevisionInfo;
import org.moera.node.model.PostingInfo;
import org.moera.node.model.PostingRevisionInfo;
import org.moera.node.model.PrivateMediaFileInfo;
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
            PostingInfo postingInfo = nodeApi.getPosting(remoteNodeName, generateCarte(remoteNodeName),
                    remotePostingId);
            if (postingInfo.getReceiverName() != null) {
                remoteNodeName = postingInfo.getReceiverName();
                remotePostingId = postingInfo.getReceiverPostingId();
                postingInfo = nodeApi.getPosting(remoteNodeName, generateCarte(remoteNodeName), remotePostingId);
            }
            CommentInfo commentInfo = nodeApi.getComment(remoteNodeName, generateCarte(remoteNodeName), remotePostingId,
                    data.getCommentId());
            if (data.getRevisionId() == null) {
                verify(postingInfo, commentInfo);
            } else {
                CommentRevisionInfo revisionInfo = nodeApi.getCommentRevision(data.getNodeName(),
                        generateCarte(data.getNodeName()), data.getPostingId(), data.getCommentId(),
                        data.getRevisionId());
                verify(postingInfo, commentInfo, revisionInfo);
            }
        } catch (Exception e) {
            error(e);
        }
    }

    private void verify(PostingInfo postingInfo, CommentInfo commentInfo) throws NodeApiException {
        PostingRevisionInfo revisionInfo;
        try {
            revisionInfo = nodeApi.getPostingRevision(remoteNodeName, generateCarte(remoteNodeName),
                    postingInfo.getId(), commentInfo.getPostingRevisionId());
        } catch (NodeApiNotFoundException e) {
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
                ? mediaManager.getPrivateMediaDigest(remoteNodeName, generateCarte(remoteNodeName),
                                                     postingInfo.getParentMediaId(), null)
                : null;
        Function<PrivateMediaFileInfo, byte[]> mediaDigest =
                pmf -> mediaManager.getPrivateMediaDigest(remoteNodeName, generateCarte(remoteNodeName), pmf);

        String repliedToId = null;
        String repliedToRevisionId = null;
        if (commentInfo.getRepliedTo() != null) {
            repliedToId = commentInfo.getRepliedTo().getId();
            repliedToRevisionId = commentInfo.getRepliedTo().getRevisionId();
        }
        byte[] repliedToDigest = repliedToDigestVerifier.getRepliedToDigest(remoteNodeName, this::generateCarte,
                postingInfo, revisions, repliedToId, repliedToRevisionId);
        Fingerprint fingerprint = Fingerprints.comment(commentInfo.getSignatureVersion())
                .create(commentInfo, mediaDigest, postingInfo, revisionInfo, parentMediaDigest, mediaDigest,
                        repliedToDigest);
        succeeded(CryptoUtil.verify(fingerprint, commentInfo.getSignature(), signingKey));
    }

    private void verify(PostingInfo postingInfo, CommentInfo commentInfo, CommentRevisionInfo commentRevisionInfo)
            throws NodeApiException {

        PostingRevisionInfo postingRevisionInfo;
        try {
            postingRevisionInfo = nodeApi.getPostingRevision(remoteNodeName, generateCarte(remoteNodeName),
                    postingInfo.getId(), commentInfo.getPostingRevisionId());
        } catch (NodeApiNotFoundException e) {
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
                ? mediaManager.getPrivateMediaDigest(remoteNodeName, generateCarte(remoteNodeName),
                                                     postingInfo.getParentMediaId(), null)
                : null;
        Function<PrivateMediaFileInfo, byte[]> mediaDigest =
                pmf -> mediaManager.getPrivateMediaDigest(remoteNodeName, generateCarte(remoteNodeName), pmf);

        String repliedToId = null;
        String repliedToRevisionId = null;
        if (commentInfo.getRepliedTo() != null) {
            repliedToId = commentInfo.getRepliedTo().getId();
            repliedToRevisionId = commentInfo.getRepliedTo().getRevisionId();
        }
        byte[] repliedToDigest = repliedToDigestVerifier.getRepliedToDigest(remoteNodeName, this::generateCarte,
                postingInfo, revisions, repliedToId, repliedToRevisionId);
        Fingerprint fingerprint = Fingerprints.comment(commentInfo.getSignatureVersion())
                .create(commentInfo, commentRevisionInfo, mediaDigest, postingInfo, postingRevisionInfo,
                        parentMediaDigest, mediaDigest, repliedToDigest);
        succeeded(CryptoUtil.verify(fingerprint, commentInfo.getSignature(), signingKey));
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
        updateData(data -> data.setStatus(correct ? VerificationStatus.CORRECT : VerificationStatus.INCORRECT));
        send(new RemoteCommentVerifiedLiberin(data));
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
        send(new RemoteCommentVerificationFailedLiberin(data));
    }

}
