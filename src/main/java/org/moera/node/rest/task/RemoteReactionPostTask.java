package org.moera.node.rest.task;

import java.security.interfaces.ECPrivateKey;

import org.moera.commons.crypto.CryptoUtil;
import org.moera.node.fingerprint.PostingFingerprint;
import org.moera.node.fingerprint.ReactionFingerprint;
import org.moera.node.model.PostingInfo;
import org.moera.node.model.ReactionAttributes;
import org.moera.node.model.ReactionCreated;
import org.moera.node.model.ReactionDescription;
import org.moera.node.task.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.reactive.function.client.WebClient;

public class RemoteReactionPostTask extends Task {

    private static Logger log = LoggerFactory.getLogger(RemoteReactionPostTask.class);

    private String targetNodeName;
    private String postingId;
    private ReactionAttributes attributes;

    private String nodeUri;

    public RemoteReactionPostTask(String targetNodeName, String postingId, ReactionAttributes attributes) {
        this.targetNodeName = targetNodeName;
        this.postingId = postingId;
        this.attributes = attributes;
    }

    @Override
    public void run() {
        nodeUri = fetchNodeUri(targetNodeName);
        if (nodeUri == null) {
            initLoggingDomain();
            log.error("Cannot find a node {}", targetNodeName);
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
                = new ReactionFingerprint(nodeName, attributes, new PostingFingerprint(postingInfo));
        ReactionDescription description = new ReactionDescription(nodeName, attributes);
        description.setSignature(CryptoUtil.sign(fingerprint, (ECPrivateKey) signingKey));
        description.setSignatureVersion(ReactionFingerprint.VERSION);
        WebClient.create(String.format("%s/api/postings/%s/reactions", nodeUri, postingId))
                .post()
                .syncBody(description)
                .retrieve()
                .bodyToMono(ReactionCreated.class)
                .subscribe(this::success, this::error);
    }

    private void success(ReactionCreated info) {
        initLoggingDomain();
        log.info("Succeeded to post reaction to posting {} at node {}",
                info.getReaction().getPostingId(), targetNodeName);
    }

    private void error(Throwable e) {
        initLoggingDomain();
        log.error("Error adding reaction to posting {} at node {}: {}", postingId, targetNodeName, e.getMessage());
    }

}
