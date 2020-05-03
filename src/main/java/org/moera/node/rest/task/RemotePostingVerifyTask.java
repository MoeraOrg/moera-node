package org.moera.node.rest.task;

import java.lang.reflect.Constructor;
import java.util.function.Consumer;
import javax.inject.Inject;

import org.moera.commons.crypto.CryptoUtil;
import org.moera.commons.crypto.Fingerprint;
import org.moera.node.data.RemotePostingVerification;
import org.moera.node.data.RemotePostingVerificationRepository;
import org.moera.node.data.VerificationStatus;
import org.moera.node.event.model.RemotePostingVerificationFailedEvent;
import org.moera.node.event.model.RemotePostingVerifiedEvent;
import org.moera.node.fingerprint.FingerprintManager;
import org.moera.node.fingerprint.FingerprintObjectType;
import org.moera.node.model.PostingInfo;
import org.moera.node.model.PostingRevisionInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.reactive.function.client.WebClient;

public class RemotePostingVerifyTask extends RemoteVerificationTask {

    private static Logger log = LoggerFactory.getLogger(RemotePostingVerifyTask.class);

    private RemotePostingVerification data;

    private String nodeUri;

    @Inject
    private RemotePostingVerificationRepository remotePostingVerificationRepository;

    @Inject
    private FingerprintManager fingerprintManager;

    public RemotePostingVerifyTask(RemotePostingVerification data) {
        this.data = data;
    }

    @Override
    public void run() {
        nodeUri = fetchNodeUri(data.getNodeName());
        if (nodeUri == null) {
            failed("remote-node.not-found", null);
            return;
        }
        WebClient.create(String.format("%s/api/postings/%s", nodeUri, data.getPostingId()))
                .get()
                .retrieve()
                .bodyToMono(PostingInfo.class)
                .subscribe(this::verify, this::error);
    }

    private void verify(PostingInfo postingInfo) {
        try {
            updateData(data -> {
                data.setReceiverName(postingInfo.getReceiverName());
            });

            if (data.getRevisionId() == null) {
                verifySignature(postingInfo);
            } else {
                WebClient.create(String.format("%s/api/postings/%s/revisions/%s",
                                                nodeUri, data.getPostingId(), data.getRevisionId()))
                        .get()
                        .retrieve()
                        .bodyToMono(PostingRevisionInfo.class)
                        .subscribe(r -> verifySignature(postingInfo, r), this::error);
            }
        } catch (Exception e) {
            failed("remote-node.invalid-answer", null);
        }
    }

    private Constructor<? extends Fingerprint> getFingerprintConstructor(short version, Class<?>... parameterTypes) {
        return fingerprintManager.getConstructor(FingerprintObjectType.POSTING, version, parameterTypes);
    }

    private void verifySignature(PostingInfo postingInfo) {
        byte[] signingKey = fetchSigningKey(postingInfo.getOwnerName(), postingInfo.getEditedAt());
        if (signingKey == null) {
            succeeded(false);
            return;
        }
        data.setRevisionId(postingInfo.getRevisionId());
        Constructor<? extends Fingerprint> constructor = getFingerprintConstructor(
                postingInfo.getSignatureVersion(), PostingInfo.class);
        succeeded(CryptoUtil.verify(postingInfo.getSignature(), signingKey, constructor, postingInfo));
    }

    private void verifySignature(PostingInfo postingInfo, PostingRevisionInfo postingRevisionInfo) {
        byte [] signingKey = fetchSigningKey(postingInfo.getOwnerName(), postingRevisionInfo.getCreatedAt());
        if (signingKey == null) {
            succeeded(false);
            return;
        }
        Constructor<? extends Fingerprint> constructor = getFingerprintConstructor(
                postingInfo.getSignatureVersion(), PostingInfo.class, PostingRevisionInfo.class);
        succeeded(CryptoUtil.verify(
                postingRevisionInfo.getSignature(), signingKey, constructor, postingInfo, postingRevisionInfo));
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
        log.info("Verified posting {}/{} at node {}: {}",
                data.getPostingId(), data.getRevisionId(), data.getNodeName(), correct ? "correct" : "incorrect");
        updateData(data -> {
            data.setStatus(correct ? VerificationStatus.CORRECT : VerificationStatus.INCORRECT);
        });
        send(new RemotePostingVerifiedEvent(data));
    }

    @Override
    protected void reportFailure(String errorCode, String errorMessage) {
        log.info("Verification of posting {}/{} at node {} failed: {} ({})",
                data.getPostingId(), data.getRevisionId(), data.getNodeName(), errorMessage, errorCode);
        updateData(data -> {
            data.setStatus(VerificationStatus.ERROR);
            data.setErrorCode(errorCode);
            data.setErrorMessage(errorMessage);
        });
        send(new RemotePostingVerificationFailedEvent(data));
    }

}
