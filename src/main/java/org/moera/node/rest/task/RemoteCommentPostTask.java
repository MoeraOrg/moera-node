package org.moera.node.rest.task;

import java.security.interfaces.ECPrivateKey;
import java.util.HashMap;
import java.util.UUID;
import javax.inject.Inject;

import org.moera.commons.crypto.CryptoUtil;
import org.moera.node.api.NodeApiUnknownNameException;
import org.moera.node.data.OwnComment;
import org.moera.node.data.OwnCommentRepository;
import org.moera.node.fingerprint.CommentFingerprint;
import org.moera.node.fingerprint.PostingFingerprint;
import org.moera.node.instant.CommentInstants;
import org.moera.node.model.CommentCreated;
import org.moera.node.model.CommentInfo;
import org.moera.node.model.CommentSourceText;
import org.moera.node.model.CommentText;
import org.moera.node.model.PostingInfo;
import org.moera.node.model.event.RemoteCommentAddedEvent;
import org.moera.node.model.event.RemoteCommentUpdatedEvent;
import org.moera.node.task.Task;
import org.moera.node.text.TextConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RemoteCommentPostTask extends Task {

    private static Logger log = LoggerFactory.getLogger(RemoteCommentPostTask.class);

    private String targetNodeName;
    private String targetFullName;
    private String postingId;
    private String commentId;
    private CommentSourceText sourceText;
    private PostingInfo postingInfo;
    private CommentInfo prevCommentInfo;

    @Inject
    private TextConverter textConverter;

    @Inject
    private RepliedToDigestVerifier repliedToDigestVerifier;

    @Inject
    private OwnCommentRepository ownCommentRepository;

    @Inject
    private CommentInstants commentInstants;

    public RemoteCommentPostTask(String targetNodeName, String postingId, String commentId,
                                 CommentSourceText sourceText) {
        this.targetNodeName = targetNodeName;
        this.postingId = postingId;
        this.commentId = commentId;
        this.sourceText = sourceText;
    }

    @Override
    public void run() {
        initLoggingDomain();
        try {
            nodeApi.setNodeId(nodeId);
            targetFullName = nodeApi.whoAmI(targetNodeName).getFullName();
            postingInfo = nodeApi.getPosting(targetNodeName, postingId);
            prevCommentInfo = commentId != null ? nodeApi.getComment(targetNodeName, postingId, commentId) : null;
            String repliedToId = null;
            String repliedToRevisionId = null;
            if (prevCommentInfo != null) {
                repliedToId = prevCommentInfo.getRepliedToId();
                repliedToRevisionId = prevCommentInfo.getRepliedToRevisionId();
            } else if (sourceText.getRepliedToId() != null) {
                CommentInfo repliedToCommentInfo =
                        nodeApi.getComment(targetNodeName, postingId, sourceText.getRepliedToId().toString());
                if (repliedToCommentInfo != null) {
                    repliedToId = repliedToCommentInfo.getId();
                    repliedToRevisionId = repliedToCommentInfo.getRevisionId();
                }
            }
            byte[] repliedToDigest = repliedToDigestVerifier.getRepliedToDigest(nodeId, targetNodeName, postingInfo,
                    new HashMap<>(), repliedToId, repliedToRevisionId);
            CommentText commentText = buildComment(postingInfo, repliedToDigest);
            CommentInfo commentInfo;
            if (commentId == null) {
                CommentCreated created = nodeApi.postComment(targetNodeName, postingId, commentText);
                commentInfo = created.getComment();
                commentId = commentInfo.getId();
                send(new RemoteCommentAddedEvent(targetNodeName, postingId, commentId));
            } else {
                commentInfo = nodeApi.putComment(targetNodeName, postingId, commentId, commentText);
                send(new RemoteCommentUpdatedEvent(targetNodeName, postingId, commentId));
            }
            saveComment(commentInfo);
            success();
        } catch (Exception e) {
            error(e);
        }
    }

    private CommentText buildComment(PostingInfo postingInfo, byte[] repliedToDigest) {
        CommentText commentText = new CommentText(nodeName, fullName, sourceText, textConverter);
        CommentFingerprint fingerprint =
                new CommentFingerprint(commentText, new PostingFingerprint(postingInfo), repliedToDigest);
        commentText.setSignature(CryptoUtil.sign(fingerprint, (ECPrivateKey) signingKey));
        commentText.setSignatureVersion(CommentFingerprint.VERSION);
        return commentText;
    }

    private void saveComment(CommentInfo info) {
        try {
            inTransaction(() -> {
                OwnComment ownComment = ownCommentRepository
                        .findByRemoteCommentId(nodeId, targetNodeName, postingId, commentId)
                        .orElse(null);
                if (ownComment == null) {
                    ownComment = new OwnComment();
                    ownComment.setId(UUID.randomUUID());
                    ownComment.setNodeId(nodeId);
                    ownComment.setRemoteNodeName(targetNodeName);
                    ownComment.setRemoteFullName(targetFullName);
                    ownComment = ownCommentRepository.save(ownComment);
                }
                info.toOwnComment(ownComment);
                return null;
            });
        } catch (Throwable e) {
            error(e);
        }
    }

    private void success() {
        log.info("Succeeded to post comment to posting {} at node {}", postingId, targetNodeName);
    }

    private void error(Throwable e) {
        if (e instanceof NodeApiUnknownNameException) {
            log.error("Cannot find a node {}", targetNodeName);
        } else {
            log.error("Error adding comment to posting {} at node {}: {}", postingId, targetNodeName, e.getMessage());
        }

        commentInstants.associate(this);
        if (prevCommentInfo == null) {
            commentInstants.addingFailed(postingId, postingInfo);
        } else {
            commentInstants.updateFailed(postingId, postingInfo, commentId, prevCommentInfo);
        }
    }

}
