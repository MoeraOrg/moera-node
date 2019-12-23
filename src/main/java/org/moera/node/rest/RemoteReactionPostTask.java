package org.moera.node.rest;

import java.security.PrivateKey;
import java.security.interfaces.ECPrivateKey;
import java.util.UUID;

import org.moera.commons.crypto.CryptoUtil;
import org.moera.node.fingerprint.PostingFingerprint;
import org.moera.node.fingerprint.ReactionFingerprint;
import org.moera.node.model.PostingInfo;
import org.moera.node.model.ReactionAttributes;
import org.moera.node.model.ReactionDescription;
import org.moera.node.model.ReactionInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.reactive.function.client.WebClient;

public class RemoteReactionPostTask extends RemoteTask implements Runnable {

    private static Logger log = LoggerFactory.getLogger(RemoteReactionPostTask.class);

    private String nodeName;
    private String postingId;
    private String ownerName;
    private PrivateKey ownerKey;
    private ReactionAttributes attributes;

    private String nodeUri;

    public RemoteReactionPostTask(UUID nodeId, String nodeName, String postingId, String ownerName, PrivateKey ownerKey,
                                  ReactionAttributes attributes) {
        super(nodeId);
        this.nodeName = nodeName;
        this.postingId = postingId;
        this.ownerName = ownerName;
        this.ownerKey = ownerKey;
        this.attributes = attributes;
    }

    @Override
    public void run() {
        nodeUri = fetchNodeUri(nodeName);
        if (nodeUri == null) {
            initLoggingDomain();
            log.error("Cannot find a node {}", nodeName);
            return;
        }
        WebClient.create(String.format("%s/api/postings/%s", nodeUri, postingId))
                .get()
                .retrieve()
                .bodyToMono(PostingInfo.class)
                .subscribe(this::postReaction, this::error);
    }

    private void postReaction(PostingInfo postingInfo) {
        ReactionFingerprint fingerprint
                = new ReactionFingerprint(ownerName, attributes, new PostingFingerprint(postingInfo));
        ReactionDescription description = new ReactionDescription(ownerName, attributes);
        description.setSignature(CryptoUtil.sign(fingerprint, (ECPrivateKey) ownerKey));
        description.setSignatureVersion(ReactionFingerprint.VERSION);
        WebClient.create(String.format("%s/api/postings/%s/reactions", nodeUri, postingId))
                .post()
                .syncBody(description)
                .retrieve()
                .bodyToMono(ReactionInfo.class)
                .subscribe(this::success, this::error);
    }

    private void success(ReactionInfo info) {
        initLoggingDomain();
        log.info("Succeeded to post reaction {} to posting {} at node {}", info.getId(), info.getPostingId(), nodeName);
    }

    private void error(Throwable e) {
        initLoggingDomain();
        log.error("Error fetching posting {} at node {}: {}", postingId, nodeName, e.getMessage());
    }

}
