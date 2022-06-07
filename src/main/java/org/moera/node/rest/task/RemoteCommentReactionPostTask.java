package org.moera.node.rest.task;

import java.security.interfaces.ECPrivateKey;
import java.util.function.Function;
import javax.inject.Inject;

import org.moera.commons.crypto.CryptoUtil;
import org.moera.commons.crypto.Fingerprint;
import org.moera.node.api.NodeApiUnknownNameException;
import org.moera.node.data.MediaFile;
import org.moera.node.fingerprint.Fingerprints;
import org.moera.node.fingerprint.ReactionFingerprint;
import org.moera.node.liberin.model.RemoteCommentReactionAddingFailedLiberin;
import org.moera.node.media.MediaManager;
import org.moera.node.model.CommentInfo;
import org.moera.node.model.PostingInfo;
import org.moera.node.model.PostingRevisionInfo;
import org.moera.node.model.PrivateMediaFileInfo;
import org.moera.node.model.ReactionAttributes;
import org.moera.node.model.ReactionCreated;
import org.moera.node.model.ReactionDescription;
import org.moera.node.task.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RemoteCommentReactionPostTask extends Task {

    private static final Logger log = LoggerFactory.getLogger(RemoteCommentReactionPostTask.class);

    private final String targetNodeName;
    private final String postingId;
    private final String commentId;
    private final ReactionAttributes attributes;
    private PostingInfo postingInfo;
    private PostingRevisionInfo postingRevisionInfo;
    private CommentInfo commentInfo;

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
            commentInfo = nodeApi.getComment(targetNodeName, generateCarte(targetNodeName), postingId, commentId);
            if (commentInfo.getOwnerAvatar() != null) {
                MediaFile mediaFile = mediaManager.downloadPublicMedia(targetNodeName, commentInfo.getOwnerAvatar());
                commentInfo.getOwnerAvatar().setMediaFile(mediaFile);
            }

            postingInfo = nodeApi.getPosting(targetNodeName, generateCarte(targetNodeName), postingId);
            if (postingInfo.getOwnerAvatar() != null) {
                MediaFile mediaFile = mediaManager.downloadPublicMedia(targetNodeName, postingInfo.getOwnerAvatar());
                postingInfo.getOwnerAvatar().setMediaFile(mediaFile);
            }

            if (!commentInfo.getPostingRevisionId().equals(postingInfo.getRevisionId())) {
                postingRevisionInfo = nodeApi.getPostingRevision(targetNodeName, generateCarte(targetNodeName),
                        postingId, commentInfo.getPostingRevisionId());
            }
            ReactionCreated created = nodeApi.postCommentReaction(targetNodeName, postingId, commentId,
                    buildReaction());
            success(created);
        } catch (Exception e) {
            error(e);
        }
    }

    private ReactionDescription buildReaction() {
        byte[] parentMediaDigest = postingInfo.getParentMediaId() != null
                ? mediaManager.getPrivateMediaDigest(targetNodeName, generateCarte(targetNodeName),
                                                     postingInfo.getParentMediaId(), null)
                : null;
        Function<PrivateMediaFileInfo, byte[]> mediaDigest =
                pmf -> mediaManager.getPrivateMediaDigest(targetNodeName, generateCarte(targetNodeName), pmf);
        Fingerprint postingFingerprint = postingRevisionInfo == null
                ? Fingerprints.posting(postingInfo.getSignatureVersion())
                        .create(postingInfo, parentMediaDigest, mediaDigest)
                : Fingerprints.posting(postingRevisionInfo.getSignatureVersion())
                        .create(postingInfo, postingRevisionInfo, parentMediaDigest, mediaDigest);
        Fingerprint commentFingerprint = Fingerprints.comment(commentInfo.getSignatureVersion())
                .create(commentInfo, postingFingerprint, mediaDigest);
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

        send(new RemoteCommentReactionAddingFailedLiberin(postingId, postingInfo, commentId, commentInfo));
    }

}
