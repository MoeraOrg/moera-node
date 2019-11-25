package org.moera.node.rest;

import java.util.Locale;
import java.util.UUID;
import javax.inject.Inject;

import org.moera.commons.crypto.CryptoUtil;
import org.moera.naming.rpc.RegisteredNameInfo;
import org.moera.node.domain.Domains;
import org.moera.node.event.EventManager;
import org.moera.node.event.model.RemotePostingVerifiedEvent;
import org.moera.node.event.model.RemotePostingVerifyFailedEvent;
import org.moera.node.fingerprint.PostingFingerprint;
import org.moera.node.model.PostingInfo;
import org.moera.node.naming.DelegatedName;
import org.moera.node.naming.NamingClient;
import org.moera.node.naming.RegisteredName;
import org.moera.node.util.UriUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.context.MessageSource;
import org.springframework.http.HttpStatus;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

public class RemotePostingVerifyTask implements Runnable {

    private static Logger log = LoggerFactory.getLogger(RemotePostingVerifyTask.class);

    private UUID nodeId;
    private String nodeName;
    private String id;
    private String revisionId;

    @Inject
    private NamingClient namingClient;

    @Inject
    private MessageSource messageSource;

    @Inject
    private Domains domains;

    @Inject
    private EventManager eventManager;

    public RemotePostingVerifyTask(UUID nodeId, String nodeName, String id) {
        this.nodeId = nodeId;
        this.nodeName = nodeName;
        this.id = id;
    }

    private void initLoggingDomain() {
        MDC.put("domain", domains.getDomainName(nodeId));
    }

    @Override
    public void run() {
        String nodeUri = getNodeUri();
        if (nodeUri == null) {
            failed("remote-node.not-found", null);
            return;
        }
        WebClient.create(nodeUri + "/api/postings/" + id)
                .get()
                .retrieve()
                .bodyToMono(PostingInfo.class)
                .subscribe(this::verify, this::error);
    }

    private String getNodeUri() {
        DelegatedName delegatedName = (DelegatedName) RegisteredName.parse(nodeName);
        RegisteredNameInfo nameInfo = delegatedName.getGeneration() != null
                ? namingClient.getCurrent(delegatedName.getName(), delegatedName.getGeneration())
                : namingClient.getCurrentForLatest(delegatedName.getName());
        return UriUtil.normalize(nameInfo != null ? nameInfo.getNodeUri() : null);
    }

    private byte[] getSigningKey(String ownerName) {
        DelegatedName delegatedName = (DelegatedName) RegisteredName.parse(ownerName);
        RegisteredNameInfo nameInfo = delegatedName.getGeneration() != null
                ? namingClient.getCurrent(delegatedName.getName(), delegatedName.getGeneration())
                : namingClient.getCurrentForLatest(delegatedName.getName());  // FIXME previous keys?
        return nameInfo != null ? nameInfo.getSigningKey() : null;
    }

    private void verify(PostingInfo postingInfo) {
        revisionId = postingInfo.getRevisionId().toString();
        byte[] signingKey = getSigningKey(postingInfo.getOwnerName());
        if (signingKey == null) {
            succeeded(false);
        } else {
            succeeded(CryptoUtil.verify(new PostingFingerprint(postingInfo), postingInfo.getSignature(), signingKey));
        }
    }

    private void error(Throwable e) {
        if (e instanceof WebClientResponseException) {
            if (((WebClientResponseException) e).getStatusCode() == HttpStatus.NOT_FOUND) {
                failed("remote-node.object-not-found", null);
            } else {
                failed("remote-node.internal-error", null);
            }
        } else {
            failed("remote-node.invalid-answer", e.getMessage());
        }
    }

    private void succeeded(boolean correct) {
        initLoggingDomain();
        log.info("Verified posting {}/{} at node {}: {}", id, revisionId, nodeName, correct ? "correct" : "incorrect");
        eventManager.send(nodeId, new RemotePostingVerifiedEvent(nodeName, id, revisionId, correct));
    }

    private void failed(String errorCode, String message) {
        initLoggingDomain();

        String errorMessage = messageSource.getMessage(errorCode, null, Locale.getDefault());
        if (message != null) {
            errorMessage += ": " + message;
        }
        log.info("Verification of posting {}/{} at node {} failed: {} ({})",
                id, revisionId, nodeName, errorMessage, errorCode);
        eventManager.send(nodeId,
                new RemotePostingVerifyFailedEvent(nodeName, id, revisionId, errorCode, errorMessage));
    }

}
