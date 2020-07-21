package org.moera.node.rest.task;

import java.security.interfaces.ECPrivateKey;
import javax.inject.Inject;

import org.moera.commons.crypto.CryptoUtil;
import org.moera.node.api.NodeApiUnknownNameException;
import org.moera.node.fingerprint.CommentFingerprint;
import org.moera.node.fingerprint.PostingFingerprint;
import org.moera.node.model.CommentCreated;
import org.moera.node.model.CommentSourceText;
import org.moera.node.model.CommentText;
import org.moera.node.model.PostingInfo;
import org.moera.node.model.event.RemoteCommentAddedEvent;
import org.moera.node.task.Task;
import org.moera.node.text.TextConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RemoteCommentPostTask extends Task {

    private static Logger log = LoggerFactory.getLogger(RemoteCommentPostTask.class);

    private String targetNodeName;
    private String postingId;
    private CommentSourceText sourceText;

    @Inject
    private TextConverter textConverter;

    public RemoteCommentPostTask(String targetNodeName, String postingId, CommentSourceText sourceText) {
        this.targetNodeName = targetNodeName;
        this.postingId = postingId;
        this.sourceText = sourceText;
    }

    @Override
    public void run() {
        try {
            nodeApi.setNodeId(nodeId);
            PostingInfo postingInfo = nodeApi.getPosting(targetNodeName, postingId);
            CommentCreated created = nodeApi.postComment(targetNodeName, postingId, buildComment(postingInfo));
            send(new RemoteCommentAddedEvent(targetNodeName, postingId, created.getComment().getId()));
            success(created);
        } catch (Exception e) {
            error(e);
        }
    }

    private CommentText buildComment(PostingInfo postingInfo) {
        CommentText commentText = new CommentText(nodeName, sourceText, textConverter);
        CommentFingerprint fingerprint
                = new CommentFingerprint(targetNodeName, commentText, new PostingFingerprint(postingInfo));
        commentText.setSignature(CryptoUtil.sign(fingerprint, (ECPrivateKey) signingKey));
        commentText.setSignatureVersion(CommentFingerprint.VERSION);
        return commentText;
    }

    private void success(CommentCreated info) {
        initLoggingDomain();
        log.info("Succeeded to post reaction to posting {} at node {}",
                info.getComment().getPostingId(), targetNodeName);
    }

    private void error(Throwable e) {
        initLoggingDomain();
        if (e instanceof NodeApiUnknownNameException) {
            log.error("Cannot find a node {}", targetNodeName);
        } else {
            log.error("Error adding reaction to posting {} at node {}: {}", postingId, targetNodeName, e.getMessage());
        }
    }

}
