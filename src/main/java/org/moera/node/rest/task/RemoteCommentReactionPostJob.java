package org.moera.node.rest.task;

import java.security.interfaces.ECPrivateKey;
import java.util.function.Function;
import javax.inject.Inject;

import org.moera.commons.crypto.CryptoUtil;
import org.moera.commons.crypto.Fingerprint;
import org.moera.node.api.node.NodeApiException;
import org.moera.node.api.node.NodeApiUnknownNameException;
import org.moera.node.data.MediaFile;
import org.moera.node.fingerprint.Fingerprints;
import org.moera.node.fingerprint.ReactionFingerprint;
import org.moera.node.liberin.model.RemoteCommentReactionAddingFailedLiberin;
import org.moera.node.media.MediaManager;
import org.moera.node.model.CommentInfo;
import org.moera.node.model.PostingInfo;
import org.moera.node.model.PostingRevisionInfo;
import org.moera.node.model.PrivateMediaFileInfo;
import org.moera.node.model.ReactionCreated;
import org.moera.node.model.ReactionDescription;
import org.moera.node.task.Job;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RemoteCommentReactionPostJob extends Job<RemoteCommentReactionPostJobParameters, Object> {

    private static final Logger log = LoggerFactory.getLogger(RemoteCommentReactionPostJob.class);

    private PostingInfo postingInfo;
    private PostingRevisionInfo postingRevisionInfo;
    private CommentInfo commentInfo;

    @Inject
    private MediaManager mediaManager;

    @Override
    protected void execute() throws NodeApiException {
        mediaManager.uploadPublicMedia(
                parameters.targetNodeName,
                generateCarte(parameters.targetNodeName),
                getAvatar());
        commentInfo = nodeApi.getComment(
                parameters.targetNodeName,
                generateCarte(parameters.targetNodeName),
                parameters.postingId,
                parameters.commentId);
        if (commentInfo.getOwnerAvatar() != null) {
            MediaFile mediaFile =
                    mediaManager.downloadPublicMedia(parameters.targetNodeName, commentInfo.getOwnerAvatar());
            commentInfo.getOwnerAvatar().setMediaFile(mediaFile);
        }

        postingInfo = nodeApi.getPosting(
                parameters.targetNodeName,
                generateCarte(parameters.targetNodeName),
                parameters.postingId);
        if (postingInfo.getOwnerAvatar() != null) {
            MediaFile mediaFile =
                    mediaManager.downloadPublicMedia(parameters.targetNodeName, postingInfo.getOwnerAvatar());
            postingInfo.getOwnerAvatar().setMediaFile(mediaFile);
        }

        if (!commentInfo.getPostingRevisionId().equals(postingInfo.getRevisionId())) {
            postingRevisionInfo = nodeApi.getPostingRevision(
                    parameters.targetNodeName,
                    generateCarte(parameters.targetNodeName),
                    parameters.postingId,
                    commentInfo.getPostingRevisionId());
        }

        ReactionCreated created = nodeApi.postCommentReaction(
                parameters.targetNodeName,
                parameters.postingId,
                parameters.commentId,
                buildReaction());

        log.info("Succeeded to post a reaction to the comment {} under posting {} at node {}",
                created.getReaction().getCommentId(), created.getReaction().getPostingId(), parameters.targetNodeName);
    }

    private ReactionDescription buildReaction() {
        byte[] parentMediaDigest = postingInfo.getParentMediaId() != null
                ? mediaManager.getPrivateMediaDigest(
                        parameters.targetNodeName,
                        generateCarte(parameters.targetNodeName),
                        postingInfo.getParentMediaId(),
                        null)
                : null;
        Function<PrivateMediaFileInfo, byte[]> mediaDigest =
                pmf -> mediaManager.getPrivateMediaDigest(
                        parameters.targetNodeName, generateCarte(parameters.targetNodeName), pmf);
        Fingerprint postingFingerprint = postingRevisionInfo == null
                ? Fingerprints.posting(postingInfo.getSignatureVersion())
                        .create(postingInfo, parentMediaDigest, mediaDigest)
                : Fingerprints.posting(postingRevisionInfo.getSignatureVersion())
                        .create(postingInfo, postingRevisionInfo, parentMediaDigest, mediaDigest);
        Fingerprint commentFingerprint = Fingerprints.comment(commentInfo.getSignatureVersion())
                .create(commentInfo, postingFingerprint, mediaDigest);
        ReactionFingerprint fingerprint = new ReactionFingerprint(nodeName(), parameters.attributes, commentFingerprint);

        ReactionDescription description = new ReactionDescription(
                nodeName(), fullName(), gender(), getAvatar(), parameters.attributes);
        description.setSignature(CryptoUtil.sign(fingerprint, (ECPrivateKey) signingKey()));
        description.setSignatureVersion(ReactionFingerprint.VERSION);

        return description;
    }

    @Override
    protected void unhandledException(Throwable e) {
        if (e instanceof NodeApiUnknownNameException) {
            log.error("Cannot find a node {}", parameters.targetNodeName);
        } else {
            log.error("Error adding a reaction to the comment {} under posting {} at node {}: {}",
                    parameters.commentId, parameters.postingId, parameters.targetNodeName, e.getMessage());
        }

        send(new RemoteCommentReactionAddingFailedLiberin(parameters.targetNodeName, parameters.postingId, postingInfo,
                parameters.commentId, commentInfo));
    }

}
