package org.moera.node.rest.task;

import java.lang.reflect.Constructor;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import javax.inject.Inject;

import org.moera.commons.crypto.CryptoUtil;
import org.moera.commons.crypto.Fingerprint;
import org.moera.node.api.NodeApi;
import org.moera.node.api.NodeApiException;
import org.moera.node.fingerprint.FingerprintManager;
import org.moera.node.fingerprint.FingerprintObjectType;
import org.moera.node.model.CommentInfo;
import org.moera.node.model.CommentRevisionInfo;
import org.moera.node.model.ObjectNotFoundFailure;
import org.moera.node.model.OperationFailure;
import org.moera.node.model.PostingInfo;
import org.moera.node.model.PostingRevisionInfo;
import org.moera.node.model.RevisionInfo;
import org.springframework.stereotype.Component;

@Component
public class RepliedToDigestVerifier {

    private static final int MAX_REPLY_DEPTH = 5;

    @Inject
    protected NodeApi nodeApi;

    @Inject
    private FingerprintManager fingerprintManager;

    public byte[] getRepliedToDigest(UUID nodeId, String targetNodeName, PostingInfo postingInfo, String repliedToId,
                                     long repliedAt) throws NodeApiException {

        if (repliedToId == null) {
            return null;
        }

        nodeApi.setNodeId(nodeId);
        PostingRevisionInfo[] revisions = nodeApi.getPostingRevisions(targetNodeName, postingInfo.getId());
        return getRepliedToDigest(targetNodeName, postingInfo, revisions, 0, new HashSet<>(), repliedToId,
                repliedAt);
    }

    public byte[] getRepliedToDigest(UUID nodeId, String targetNodeName, PostingInfo postingInfo,
                                     PostingRevisionInfo[] revisions, String repliedToId, long repliedAt)
            throws NodeApiException {

        if (repliedToId == null) {
            return null;
        }

        nodeApi.setNodeId(nodeId);
        return getRepliedToDigest(targetNodeName, postingInfo, revisions, 0, new HashSet<>(), repliedToId,
                repliedAt);
    }

    private byte[] getRepliedToDigest(String targetNodeName, PostingInfo postingInfo,
                                      PostingRevisionInfo[] postingRevisions, int depth, Set<String> visited,
                                      String id, long repliedAt) throws NodeApiException {
        if (id == null) {
            return null;
        }
        if (visited.contains(id)) {
            throw new OperationFailure("comment.reply-loop");
        }

        CommentInfo commentInfo = nodeApi.getComment(targetNodeName, postingInfo.getId(), id);
        CommentRevisionInfo[] commentRevisions = nodeApi.getCommentRevisions(targetNodeName, postingInfo.getId(), id);
        if (commentInfo == null || commentRevisions == null) {
            throw new ObjectNotFoundFailure("comment.reply-not-found");
        }
        CommentRevisionInfo commentRevisionInfo = getRevisionByTimestamp(commentRevisions, repliedAt);
        if (commentRevisionInfo == null) {
            throw new ObjectNotFoundFailure("comment.reply-not-found");
        }

        if (depth >= MAX_REPLY_DEPTH) {
            return commentRevisionInfo.getDigest();
        }

        visited.add(id);
        byte[] repliedToDigest = getRepliedToDigest(targetNodeName, postingInfo, postingRevisions, depth + 1,
                visited, Objects.toString(commentInfo.getRepliedTo(), null), commentInfo.getCreatedAt());

        PostingRevisionInfo postingRevisionInfo = getRevisionByTimestamp(postingRevisions, repliedAt);
        Constructor<? extends Fingerprint> constructor = getFingerprintConstructor(
                commentInfo.getSignatureVersion(), CommentInfo.class, CommentRevisionInfo.class,
                PostingInfo.class, PostingRevisionInfo.class, byte[].class);
        return CryptoUtil.digest(constructor, commentInfo, commentRevisionInfo, postingInfo, postingRevisionInfo,
                repliedToDigest);
    }

    private Constructor<? extends Fingerprint> getFingerprintConstructor(short version, Class<?>... parameterTypes) {
        return fingerprintManager.getConstructor(FingerprintObjectType.COMMENT, version, parameterTypes);
    }

    private <R extends RevisionInfo> R getRevisionByTimestamp(R[] postingRevisions, Long timestamp) {
        return Arrays.stream(postingRevisions)
                .filter(r -> r.getCreatedAt() <= timestamp)
                .filter(r -> r.getDeletedAt() == null || r.getDeletedAt() > timestamp)
                .findFirst()
                .orElse(null);
    }

}
