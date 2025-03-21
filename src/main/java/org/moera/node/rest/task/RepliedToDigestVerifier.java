package org.moera.node.rest.task;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import jakarta.inject.Inject;

import org.moera.lib.crypto.CryptoUtil;
import org.moera.lib.node.exception.MoeraNodeApiNotFoundException;
import org.moera.lib.node.exception.MoeraNodeException;
import org.moera.lib.node.types.CommentInfo;
import org.moera.lib.node.types.CommentRevisionInfo;
import org.moera.lib.node.types.PostingInfo;
import org.moera.lib.node.types.PostingRevisionInfo;
import org.moera.lib.node.types.PrivateMediaFileInfo;
import org.moera.lib.node.types.Scope;
import org.moera.node.api.node.NodeApi;
import org.moera.node.fingerprint.CommentFingerprintBuilder;
import org.moera.node.fingerprint.PostingFingerprintBuilder;
import org.moera.node.media.MediaManager;
import org.moera.node.model.ObjectNotFoundFailure;
import org.moera.node.model.OperationFailure;
import org.moera.node.util.CarteGenerator;
import org.springframework.stereotype.Component;

@Component
public class RepliedToDigestVerifier {

    private static final int MAX_REPLY_DEPTH = 5;

    @Inject
    protected NodeApi nodeApi;

    @Inject
    private MediaManager mediaManager;

    public byte[] getRepliedToDigest(
        String targetNodeName,
        CarteGenerator generateCarte,
        PostingInfo postingInfo,
        Map<String, PostingRevisionInfo> revisions,
        String repliedToId,
        String repliedToRevisionId
    ) throws MoeraNodeException {
        if (repliedToId == null) {
            return null;
        }

        return getRepliedToDigest(
            targetNodeName,
            generateCarte,
            postingInfo,
            revisions,
            0,
            new HashSet<>(),
            repliedToId,
            new HashMap<>(),
            repliedToRevisionId
        );
    }

    private byte[] getRepliedToDigest(
        String targetNodeName,
        CarteGenerator generateCarte,
        PostingInfo postingInfo,
        Map<String, PostingRevisionInfo> postingRevisions,
        int depth,
        Set<String> visited,
        String id,
        Map<String, CommentRevisionInfo> commentRevisions,
        String revisionId
    ) throws MoeraNodeException {
        if (id == null) {
            return null;
        }
        if (visited.contains(id)) {
            throw new OperationFailure("comment.reply-loop");
        }

        CommentInfo commentInfo;
        try {
            commentInfo = nodeApi
                .at(targetNodeName, generateCarte.generate(targetNodeName, Scope.VIEW_CONTENT))
                .getComment(postingInfo.getId(), id, false);
        } catch (MoeraNodeApiNotFoundException e) {
            throw new ObjectNotFoundFailure("comment.reply-not-found");
        }
        CommentRevisionInfo commentRevisionInfo = commentRevisions.get(revisionId);
        if (commentRevisionInfo == null) {
            try {
                commentRevisionInfo = nodeApi
                    .at(targetNodeName, generateCarte.generate(targetNodeName, Scope.VIEW_CONTENT))
                    .getCommentRevision(postingInfo.getId(), id, revisionId);
            } catch (MoeraNodeApiNotFoundException e) {
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
        byte[] repliedToDigest = getRepliedToDigest(
            targetNodeName,
            generateCarte,
            postingInfo,
            postingRevisions,
            depth + 1,
            visited,
            repliedToId,
            commentRevisions,
            repliedToRevisionId
        );

        PostingRevisionInfo postingRevisionInfo = postingRevisions.get(commentRevisionInfo.getPostingRevisionId());
        if (postingRevisionInfo == null) {
            try {
                postingRevisionInfo = nodeApi
                    .at(targetNodeName, generateCarte.generate(targetNodeName, Scope.VIEW_CONTENT))
                    .getPostingRevision(postingInfo.getId(), commentRevisionInfo.getPostingRevisionId());
            } catch (MoeraNodeApiNotFoundException e) {
                throw new ObjectNotFoundFailure("comment.reply-not-found");
            }
            postingRevisions.put(commentRevisionInfo.getPostingRevisionId(), postingRevisionInfo);
        }

        byte[] parentMediaDigest = postingInfo.getParentMediaId() != null
            ? mediaManager.getPrivateMediaDigest(
                targetNodeName, generateCarte.generate(targetNodeName, Scope.VIEW_MEDIA),
                postingInfo.getParentMediaId(), null
            )
            : null;
        Function<PrivateMediaFileInfo, byte[]> mediaDigest =
            pmf -> mediaManager.getPrivateMediaDigest(
                targetNodeName, generateCarte.generate(targetNodeName, Scope.VIEW_MEDIA), pmf
            );

        byte[] fingerprint = CommentFingerprintBuilder.build(
            commentInfo.getSignatureVersion(),
            commentInfo,
            commentRevisionInfo,
            mediaDigest,
            PostingFingerprintBuilder.build(
                postingRevisionInfo.getSignatureVersion(),
                postingInfo,
                postingRevisionInfo,
                parentMediaDigest,
                mediaDigest
            ),
            repliedToDigest
        );
        return CryptoUtil.digest(fingerprint);
    }

}
