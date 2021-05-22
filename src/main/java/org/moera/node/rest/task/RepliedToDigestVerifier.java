package org.moera.node.rest.task;

import java.lang.reflect.Constructor;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import javax.inject.Inject;

import org.moera.commons.crypto.CryptoUtil;
import org.moera.commons.crypto.Fingerprint;
import org.moera.node.api.NodeApi;
import org.moera.node.api.NodeApiException;
import org.moera.node.api.NodeApiNotFoundException;
import org.moera.node.fingerprint.FingerprintManager;
import org.moera.node.fingerprint.FingerprintObjectType;
import org.moera.node.model.CommentInfo;
import org.moera.node.model.CommentRevisionInfo;
import org.moera.node.model.ObjectNotFoundFailure;
import org.moera.node.model.OperationFailure;
import org.moera.node.model.PostingInfo;
import org.moera.node.model.PostingRevisionInfo;
import org.springframework.stereotype.Component;

@Component
public class RepliedToDigestVerifier {

    private static final int MAX_REPLY_DEPTH = 5;

    @Inject
    protected NodeApi nodeApi;

    @Inject
    private FingerprintManager fingerprintManager;

    public byte[] getRepliedToDigest(String targetNodeName, PostingInfo postingInfo,
                                     Map<String, PostingRevisionInfo> revisions, String repliedToId,
                                     String repliedToRevisionId) throws NodeApiException {
        if (repliedToId == null) {
            return null;
        }

        return getRepliedToDigest(targetNodeName, postingInfo, revisions, 0, new HashSet<>(), repliedToId,
                new HashMap<>(), repliedToRevisionId);
    }

    private byte[] getRepliedToDigest(String targetNodeName, PostingInfo postingInfo,
                                      Map<String, PostingRevisionInfo> postingRevisions, int depth, Set<String> visited,
                                      String id, Map<String, CommentRevisionInfo> commentRevisions,
                                      String revisionId) throws NodeApiException {
        if (id == null) {
            return null;
        }
        if (visited.contains(id)) {
            throw new OperationFailure("comment.reply-loop");
        }

        CommentInfo commentInfo;
        try {
            commentInfo = nodeApi.getComment(targetNodeName, postingInfo.getId(), id);
        } catch (NodeApiNotFoundException e) {
            throw new ObjectNotFoundFailure("comment.reply-not-found");
        }
        CommentRevisionInfo commentRevisionInfo = commentRevisions.get(revisionId);
        if (commentRevisionInfo == null) {
            try {
                commentRevisionInfo = nodeApi.getCommentRevision(targetNodeName, postingInfo.getId(), id, revisionId);
            } catch (NodeApiNotFoundException e) {
                throw new ObjectNotFoundFailure("comment.reply-not-found");
            }
            commentRevisions.put(revisionId, commentRevisionInfo);
        }

        if (depth >= MAX_REPLY_DEPTH) {
            return commentRevisionInfo.getDigest();
        }

        visited.add(id);
        String repliedToId = null;
        String repliedToRevisionId = null;
        if (commentInfo.getRepliedTo() != null) {
            repliedToId = commentInfo.getRepliedTo().getId();
            repliedToRevisionId = commentInfo.getRepliedTo().getRevisionId();
        }
        byte[] repliedToDigest = getRepliedToDigest(targetNodeName, postingInfo, postingRevisions, depth + 1,
                visited, repliedToId, commentRevisions, repliedToRevisionId);

        PostingRevisionInfo postingRevisionInfo = postingRevisions.get(commentRevisionInfo.getPostingRevisionId());
        if (postingRevisionInfo == null) {
            try {
                postingRevisionInfo = nodeApi.getPostingRevision(targetNodeName, postingInfo.getId(),
                        commentRevisionInfo.getPostingRevisionId());
            } catch (NodeApiNotFoundException e) {
                throw new ObjectNotFoundFailure("comment.reply-not-found");
            }
            postingRevisions.put(commentRevisionInfo.getPostingRevisionId(), postingRevisionInfo);
        }
        Constructor<? extends Fingerprint> constructor = getFingerprintConstructor(
                commentInfo.getSignatureVersion(), CommentInfo.class, CommentRevisionInfo.class,
                PostingInfo.class, PostingRevisionInfo.class, byte[].class);
        return CryptoUtil.digest(constructor, commentInfo, commentRevisionInfo, postingInfo, postingRevisionInfo,
                repliedToDigest);
    }

    private Constructor<? extends Fingerprint> getFingerprintConstructor(short version, Class<?>... parameterTypes) {
        return fingerprintManager.getConstructor(FingerprintObjectType.COMMENT, version, parameterTypes);
    }

}
