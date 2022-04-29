package org.moera.node.rest.task;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import javax.inject.Inject;

import org.moera.commons.crypto.CryptoUtil;
import org.moera.commons.crypto.Fingerprint;
import org.moera.node.api.NodeApi;
import org.moera.node.api.NodeApiException;
import org.moera.node.api.NodeApiNotFoundException;
import org.moera.node.fingerprint.Fingerprints;
import org.moera.node.media.MediaManager;
import org.moera.node.model.CommentInfo;
import org.moera.node.model.CommentRevisionInfo;
import org.moera.node.model.ObjectNotFoundFailure;
import org.moera.node.model.OperationFailure;
import org.moera.node.model.PostingInfo;
import org.moera.node.model.PostingRevisionInfo;
import org.moera.node.model.PrivateMediaFileInfo;
import org.springframework.stereotype.Component;

@Component
public class RepliedToDigestVerifier {

    private static final int MAX_REPLY_DEPTH = 5;

    @Inject
    protected NodeApi nodeApi;

    @Inject
    private MediaManager mediaManager;

    public byte[] getRepliedToDigest(String targetNodeName, Function<String, String> generateCarte,
                                     PostingInfo postingInfo, Map<String, PostingRevisionInfo> revisions,
                                     String repliedToId, String repliedToRevisionId) throws NodeApiException {
        if (repliedToId == null) {
            return null;
        }

        return getRepliedToDigest(targetNodeName, generateCarte, postingInfo, revisions, 0, new HashSet<>(),
                repliedToId, new HashMap<>(), repliedToRevisionId);
    }

    private byte[] getRepliedToDigest(String targetNodeName, Function<String, String> generateCarte,
                                      PostingInfo postingInfo, Map<String, PostingRevisionInfo> postingRevisions,
                                      int depth, Set<String> visited, String id,
                                      Map<String, CommentRevisionInfo> commentRevisions,
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
        byte[] repliedToDigest = getRepliedToDigest(targetNodeName, generateCarte, postingInfo, postingRevisions,
                depth + 1, visited, repliedToId, commentRevisions, repliedToRevisionId);

        PostingRevisionInfo postingRevisionInfo = postingRevisions.get(commentRevisionInfo.getPostingRevisionId());
        if (postingRevisionInfo == null) {
            try {
                postingRevisionInfo = nodeApi.getPostingRevision(targetNodeName, generateCarte.apply(targetNodeName),
                        postingInfo.getId(), commentRevisionInfo.getPostingRevisionId());
            } catch (NodeApiNotFoundException e) {
                throw new ObjectNotFoundFailure("comment.reply-not-found");
            }
            postingRevisions.put(commentRevisionInfo.getPostingRevisionId(), postingRevisionInfo);
        }

        byte[] parentMediaDigest = postingInfo.getParentMediaId() != null
                ? mediaManager.getPrivateMediaDigest(targetNodeName, generateCarte.apply(targetNodeName),
                                                     postingInfo.getParentMediaId(), null)
                : null;
        Function<PrivateMediaFileInfo, byte[]> mediaDigest =
                pmf -> mediaManager.getPrivateMediaDigest(targetNodeName, generateCarte.apply(targetNodeName), pmf);

        Fingerprint fingerprint = Fingerprints.comment(commentInfo.getSignatureVersion())
                .create(commentInfo, commentRevisionInfo, mediaDigest, postingInfo, postingRevisionInfo,
                        parentMediaDigest, mediaDigest, repliedToDigest);
        return CryptoUtil.digest(fingerprint);
    }

}
