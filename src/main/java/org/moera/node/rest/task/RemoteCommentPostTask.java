package org.moera.node.rest.task;

import java.security.interfaces.ECPrivateKey;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import javax.inject.Inject;

import org.moera.commons.crypto.CryptoUtil;
import org.moera.node.api.NodeApiUnknownNameException;
import org.moera.node.data.OwnComment;
import org.moera.node.data.OwnCommentRepository;
import org.moera.node.fingerprint.CommentFingerprint;
import org.moera.node.fingerprint.PostingFingerprint;
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
    private String postingId;
    private String commentId;
    private CommentSourceText sourceText;

    @Inject
    private TextConverter textConverter;

    @Inject
    private RepliedToDigestVerifier repliedToDigestVerifier;

    @Inject
    private OwnCommentRepository ownCommentRepository;

    public RemoteCommentPostTask(String targetNodeName, String postingId, String commentId,
                                 CommentSourceText sourceText) {
        this.targetNodeName = targetNodeName;
        this.postingId = postingId;
        this.commentId = commentId;
        this.sourceText = sourceText;
    }

    @Override
    public void run() {
        try {
            nodeApi.setNodeId(nodeId);
            PostingInfo postingInfo = nodeApi.getPosting(targetNodeName, postingId);
            CommentInfo prevCommentInfo = commentId != null
                    ? nodeApi.getComment(targetNodeName, postingId, commentId) : null;
            long repliedAt = prevCommentInfo != null
                    ? prevCommentInfo.getCreatedAt() : Instant.now().getEpochSecond();
            String repliedToId = prevCommentInfo != null
                    ? prevCommentInfo.getRepliedToId() : Objects.toString(sourceText.getRepliedToId(), null);
            byte[] repliedToDigest = repliedToDigestVerifier.getRepliedToDigest(nodeId, targetNodeName, postingInfo,
                    repliedToId, repliedAt);
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
        CommentText commentText = new CommentText(nodeName, sourceText, textConverter);
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
        initLoggingDomain();
        log.info("Succeeded to post comment to posting {} at node {}", postingId, targetNodeName);
    }

    private void error(Throwable e) {
        initLoggingDomain();
        if (e instanceof NodeApiUnknownNameException) {
            log.error("Cannot find a node {}", targetNodeName);
        } else {
            log.error("Error adding comment to posting {} at node {}: {}", postingId, targetNodeName, e.getMessage());
        }
    }

}
