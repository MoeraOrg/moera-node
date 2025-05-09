package org.moera.node.rest.task.verification;

import java.util.function.Consumer;
import java.util.function.Function;
import jakarta.inject.Inject;

import org.moera.lib.crypto.CryptoUtil;
import org.moera.lib.node.types.PostingInfo;
import org.moera.lib.node.types.PostingRevisionInfo;
import org.moera.lib.node.types.PrivateMediaFileInfo;
import org.moera.lib.node.types.Scope;
import org.moera.lib.node.types.VerificationStatus;
import org.moera.node.data.RemotePostingVerification;
import org.moera.node.data.RemotePostingVerificationRepository;
import org.moera.node.fingerprint.PostingFingerprintBuilder;
import org.moera.node.liberin.model.RemotePostingVerificationFailedLiberin;
import org.moera.node.liberin.model.RemotePostingVerifiedLiberin;
import org.moera.node.media.MediaManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RemotePostingVerifyTask extends RemoteVerificationTask {

    private static final Logger log = LoggerFactory.getLogger(RemotePostingVerifyTask.class);

    private final RemotePostingVerification data;

    @Inject
    private RemotePostingVerificationRepository remotePostingVerificationRepository;

    @Inject
    private MediaManager mediaManager;

    public RemotePostingVerifyTask(RemotePostingVerification data) {
        this.data = data;
    }

    @Override
    protected void execute() {
        try {
            PostingInfo postingInfo = nodeApi
                .at(data.getNodeName(), generateCarte(data.getNodeName(), Scope.VIEW_CONTENT))
                .getPosting(data.getPostingId(), false);
            updateData(data -> data.setOwnerName(postingInfo.getReceiverName()));
            byte[] parentMediaDigest = postingInfo.getParentMediaId() != null
                ? mediaManager.getPrivateMediaDigest(
                    data.getNodeName(), generateCarte(data.getNodeName(), Scope.VIEW_MEDIA),
                    postingInfo.getParentMediaId(), null
                )
                : null;
            Function<PrivateMediaFileInfo, byte[]> mediaDigest =
                pmf -> mediaManager.getPrivateMediaDigest(
                    data.getNodeName(), generateCarte(data.getNodeName(), Scope.VIEW_MEDIA), pmf
                );

            if (data.getRevisionId() == null) {
                verifySignature(postingInfo, parentMediaDigest, mediaDigest);
            } else {
                PostingRevisionInfo revisionInfo = nodeApi
                    .at(data.getNodeName(), generateCarte(data.getNodeName(), Scope.VIEW_CONTENT))
                    .getPostingRevision(data.getPostingId(), data.getRevisionId());
                verifySignature(postingInfo, revisionInfo, parentMediaDigest, mediaDigest);
            }
        } catch (Exception e) {
            error(e);
        }
    }

    private void verifySignature(
        PostingInfo postingInfo, byte[] parentMediaDigest, Function<PrivateMediaFileInfo, byte[]> mediaDigest
    ) {
        byte[] signingKey = fetchSigningKey(postingInfo.getOwnerName(), postingInfo.getEditedAt());
        if (signingKey == null) {
            succeeded(false);
            return;
        }
        data.setRevisionId(postingInfo.getRevisionId());
        byte[] fingerprint = PostingFingerprintBuilder.build(
            postingInfo.getSignatureVersion(), postingInfo, parentMediaDigest, mediaDigest
        );
        succeeded(CryptoUtil.verifySignature(fingerprint, postingInfo.getSignature(), signingKey));
    }

    private void verifySignature(
        PostingInfo postingInfo, PostingRevisionInfo postingRevisionInfo, byte[] parentMediaDigest,
        Function<PrivateMediaFileInfo, byte[]> mediaDigest
    ) {
        byte [] signingKey = fetchSigningKey(postingInfo.getOwnerName(), postingRevisionInfo.getCreatedAt());
        if (signingKey == null) {
            succeeded(false);
            return;
        }
        byte[] fingerprint = PostingFingerprintBuilder.build(
            postingInfo.getSignatureVersion(), postingInfo, postingRevisionInfo, parentMediaDigest, mediaDigest
        );
        succeeded(CryptoUtil.verifySignature(fingerprint, postingRevisionInfo.getSignature(), signingKey));
    }

    private void updateData(Consumer<RemotePostingVerification> updater) {
        updater.accept(data);
        RemotePostingVerification status = remotePostingVerificationRepository.findById(data.getId()).orElse(null);
        if (status == null) {
            return;
        }
        updater.accept(status);
        remotePostingVerificationRepository.saveAndFlush(status);
    }

    @Override
    protected void reportSuccess(boolean correct) {
        log.info(
            "Verified posting {}/{} at node {}: {}",
            data.getPostingId(), data.getRevisionId(), data.getNodeName(), correct ? "correct" : "incorrect"
        );
        updateData(data -> data.setStatus(correct ? VerificationStatus.CORRECT : VerificationStatus.INCORRECT));
        send(new RemotePostingVerifiedLiberin(data));
    }

    @Override
    protected void reportFailure(String errorCode, String errorMessage) {
        log.info(
            "Verification of posting {}/{} at node {} failed: {} ({})",
            data.getPostingId(), data.getRevisionId(), data.getNodeName(), errorMessage, errorCode
        );
        updateData(data -> {
            data.setStatus(VerificationStatus.ERROR);
            data.setErrorCode(errorCode);
            data.setErrorMessage(errorMessage);
        });
        send(new RemotePostingVerificationFailedLiberin(data));
    }

}
