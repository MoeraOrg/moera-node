package org.moera.node.rest.task;

import java.security.interfaces.ECPrivateKey;

import org.moera.commons.crypto.CryptoUtil;
import org.moera.node.api.NodeApiUnknownNameException;
import org.moera.node.fingerprint.CommentFingerprint;
import org.moera.node.fingerprint.PostingFingerprint;
import org.moera.node.fingerprint.ReactionFingerprint;
import org.moera.node.model.CommentInfo;
import org.moera.node.model.PostingInfo;
import org.moera.node.model.ReactionAttributes;
import org.moera.node.model.ReactionCreated;
import org.moera.node.model.ReactionDescription;
import org.moera.node.task.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RemoteCommentReactionPostTask extends Task {

    private static Logger log = LoggerFactory.getLogger(RemoteCommentReactionPostTask.class);

    private String targetNodeName;
    private String postingId;
    private String commentId;
    private ReactionAttributes attributes;

    public RemoteCommentReactionPostTask(String targetNodeName, String postingId, String commentId,
                                         ReactionAttributes attributes) {
        this.targetNodeName = targetNodeName;
        this.postingId = postingId;
        this.commentId = commentId;
        this.attributes = attributes;
    }

    @Override
    public void run() {
        try {
            nodeApi.setNodeId(nodeId);
            PostingInfo postingInfo = nodeApi.getPosting(targetNodeName, postingId);
            CommentInfo commentInfo = nodeApi.getComment(targetNodeName, postingId, commentId);
            ReactionCreated created = nodeApi.postCommentReaction(targetNodeName, postingId, commentId,
                    buildReaction(postingInfo, commentInfo));
            success(created);
        } catch (Exception e) {
            error(e);
        }
    }

    private ReactionDescription buildReaction(PostingInfo postingInfo, CommentInfo commentInfo) {
        PostingFingerprint postingFingerprint = new PostingFingerprint(postingInfo);
        CommentFingerprint commentFingerprint = new CommentFingerprint(commentInfo, postingFingerprint);
        ReactionFingerprint fingerprint = new ReactionFingerprint(nodeName, attributes, commentFingerprint);

        ReactionDescription description = new ReactionDescription(nodeName, attributes);
        description.setSignature(CryptoUtil.sign(fingerprint, (ECPrivateKey) signingKey));
        description.setSignatureVersion(ReactionFingerprint.VERSION);

        return description;
    }

    private void success(ReactionCreated info) {
        initLoggingDomain();
        log.info("Succeeded to post reaction to comment {} under posting {} at node {}",
                info.getReaction().getCommentId(), info.getReaction().getPostingId(), targetNodeName);
    }

    private void error(Throwable e) {
        initLoggingDomain();
        if (e instanceof NodeApiUnknownNameException) {
            log.error("Cannot find a node {}", targetNodeName);
        } else {
            log.error("Error adding reaction to comment {} under posting {} at node {}: {}",
                    commentId, postingId, targetNodeName, e.getMessage());
        }
    }

}
