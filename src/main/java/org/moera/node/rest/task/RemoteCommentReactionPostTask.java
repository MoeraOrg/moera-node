package org.moera.node.rest.task;

import java.security.interfaces.ECPrivateKey;
import javax.inject.Inject;

import org.moera.commons.crypto.CryptoUtil;
import org.moera.node.api.NodeApiUnknownNameException;
import org.moera.node.fingerprint.CommentFingerprint;
import org.moera.node.fingerprint.PostingFingerprint;
import org.moera.node.fingerprint.ReactionFingerprint;
import org.moera.node.instant.CommentReactionInstants;
import org.moera.node.media.MediaManager;
import org.moera.node.model.CommentInfo;
import org.moera.node.model.PostingInfo;
import org.moera.node.model.PostingRevisionInfo;
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
    private PostingInfo postingInfo;
    private PostingRevisionInfo postingRevisionInfo;
    private CommentInfo commentInfo;

    @Inject
    private CommentReactionInstants commentReactionInstants;

    @Inject
    private MediaManager mediaManager;

    public RemoteCommentReactionPostTask(String targetNodeName, String postingId, String commentId,
                                         ReactionAttributes attributes) {
        this.targetNodeName = targetNodeName;
        this.postingId = postingId;
        this.commentId = commentId;
        this.attributes = attributes;
    }

    @Override
    protected void execute() {
        try {
            mediaManager.uploadPublicMedia(targetNodeName, generateCarte(targetNodeName), getAvatar());
            commentInfo = nodeApi.getComment(targetNodeName, postingId, commentId);
            postingInfo = nodeApi.getPosting(targetNodeName, postingId);
            if (!commentInfo.getPostingRevisionId().equals(postingInfo.getRevisionId())) {
                postingRevisionInfo = nodeApi.getPostingRevision(targetNodeName, postingId,
                        commentInfo.getPostingRevisionId());
            }
            ReactionCreated created = nodeApi.postCommentReaction(targetNodeName, postingId, commentId,
                    buildReaction());
            success(created);
        } catch (Exception e) {
            error(e);
        }
    }

    private ReactionDescription buildReaction() {
        PostingFingerprint postingFingerprint = postingRevisionInfo == null
                ? new PostingFingerprint(postingInfo) : new PostingFingerprint(postingInfo, postingRevisionInfo);
        CommentFingerprint commentFingerprint = new CommentFingerprint(commentInfo, postingFingerprint);
        ReactionFingerprint fingerprint = new ReactionFingerprint(nodeName(), attributes, commentFingerprint);

        ReactionDescription description = new ReactionDescription(nodeName(), fullName(), getAvatar(), attributes);
        description.setSignature(CryptoUtil.sign(fingerprint, (ECPrivateKey) signingKey()));
        description.setSignatureVersion(ReactionFingerprint.VERSION);

        return description;
    }

    private void success(ReactionCreated info) {
        log.info("Succeeded to post reaction to comment {} under posting {} at node {}",
                info.getReaction().getCommentId(), info.getReaction().getPostingId(), targetNodeName);
    }

    private void error(Throwable e) {
        if (e instanceof NodeApiUnknownNameException) {
            log.error("Cannot find a node {}", targetNodeName);
        } else {
            log.error("Error adding reaction to comment {} under posting {} at node {}: {}",
                    commentId, postingId, targetNodeName, e.getMessage());
        }

        commentReactionInstants.addingFailed(postingId, postingInfo, commentId, commentInfo);
    }

}
