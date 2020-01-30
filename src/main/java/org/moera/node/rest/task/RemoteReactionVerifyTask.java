package org.moera.node.rest.task;

import java.lang.reflect.Constructor;
import java.util.List;
import java.util.function.Consumer;
import javax.inject.Inject;

import org.moera.commons.crypto.CryptoUtil;
import org.moera.commons.crypto.Fingerprint;
import org.moera.node.data.RemoteReactionVerification;
import org.moera.node.data.RemoteReactionVerificationRepository;
import org.moera.node.data.VerificationStatus;
import org.moera.node.event.EventManager;
import org.moera.node.event.model.RemoteReactionVerificationFailedEvent;
import org.moera.node.event.model.RemoteReactionVerifiedEvent;
import org.moera.node.fingerprint.FingerprintManager;
import org.moera.node.fingerprint.FingerprintObjectType;
import org.moera.node.model.PostingInfo;
import org.moera.node.model.PostingRevisionInfo;
import org.moera.node.model.ReactionInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.web.reactive.function.client.WebClient;

public class RemoteReactionVerifyTask extends RemoteVerificationTask implements Runnable {

    private static Logger log = LoggerFactory.getLogger(RemoteReactionVerifyTask.class);

    private RemoteReactionVerification data;

    private String nodeUri;

    @Inject
    private EventManager eventManager;

    @Inject
    private RemoteReactionVerificationRepository remoteReactionVerificationRepository;

    @Inject
    private FingerprintManager fingerprintManager;

    public RemoteReactionVerifyTask(RemoteReactionVerification data) {
        super(data.getNodeId());
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
                .subscribe(this::fetchRevisions, this::error);
    }

    private void fetchRevisions(PostingInfo postingInfo) {
        WebClient.create(String.format("%s/api/postings/%s/revisions", nodeUri, data.getPostingId()))
                .get()
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<List<PostingRevisionInfo>>() {
                })
                .subscribe(r -> fetchReaction(postingInfo, r), this::error);
    }

    private void fetchReaction(PostingInfo postingInfo, List<PostingRevisionInfo> revisions) {
        WebClient.create(String.format("%s/api/postings/%s/reactions/%s",
                                       nodeUri, data.getPostingId(), data.getReactionOwnerName()))
                .get()
                .retrieve()
                .bodyToMono(ReactionInfo.class)
                .subscribe(r -> verify(postingInfo, revisions, r), this::error);
    }

    private Constructor<? extends Fingerprint> getFingerprintConstructor(short version, Class<?>... parameterTypes) {
        return fingerprintManager.getConstructor(FingerprintObjectType.REACTION, version, parameterTypes);
    }

    private void verify(PostingInfo postingInfo, List<PostingRevisionInfo> revisions, ReactionInfo reactionInfo) {
        PostingRevisionInfo revisionInfo = revisions.stream()
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
        eventManager.send(data.getNodeId(), new RemoteReactionVerifiedEvent(data));
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
        eventManager.send(data.getNodeId(), new RemoteReactionVerificationFailedEvent(data));
    }

}
