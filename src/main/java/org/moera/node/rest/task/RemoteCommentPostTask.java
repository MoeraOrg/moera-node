package org.moera.node.rest.task;

import java.security.interfaces.ECPrivateKey;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import javax.inject.Inject;

import org.moera.commons.crypto.CryptoUtil;
import org.moera.node.api.node.NodeApiUnknownNameException;
import org.moera.node.data.MediaFile;
import org.moera.node.data.OwnComment;
import org.moera.node.data.OwnCommentRepository;
import org.moera.node.fingerprint.CommentFingerprint;
import org.moera.node.fingerprint.Fingerprints;
import org.moera.node.liberin.model.RemoteCommentAddingFailedLiberin;
import org.moera.node.liberin.model.RemoteCommentUpdateFailedLiberin;
import org.moera.node.liberin.model.RemoteCommentAddedLiberin;
import org.moera.node.liberin.model.RemoteCommentUpdatedLiberin;
import org.moera.node.media.MediaManager;
import org.moera.node.model.CommentCreated;
import org.moera.node.model.CommentInfo;
import org.moera.node.model.CommentSourceText;
import org.moera.node.model.CommentText;
import org.moera.node.model.MediaWithDigest;
import org.moera.node.model.PostingInfo;
import org.moera.node.model.WhoAmI;
import org.moera.node.operations.ContactOperations;
import org.moera.node.task.Task;
import org.moera.node.text.TextConverter;
import org.moera.node.util.Util;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RemoteCommentPostTask extends Task {

    private static final Logger log = LoggerFactory.getLogger(RemoteCommentPostTask.class);

    private final String targetNodeName;
    private WhoAmI target;
    private MediaFile targetAvatarMediaFile;
    private final String postingId;
    private String commentId;
    private final CommentSourceText sourceText;
    private PostingInfo postingInfo;
    private CommentInfo prevCommentInfo;

    @Inject
    private TextConverter textConverter;

    @Inject
    private RepliedToDigestVerifier repliedToDigestVerifier;

    @Inject
    private OwnCommentRepository ownCommentRepository;

    @Inject
    private ContactOperations contactOperations;

    @Inject
    private MediaManager mediaManager;

    public RemoteCommentPostTask(String targetNodeName, String postingId, String commentId,
                                 CommentSourceText sourceText) {
        this.targetNodeName = targetNodeName;
        this.postingId = postingId;
        this.commentId = commentId;
        this.sourceText = sourceText;
    }

    @Override
    protected void execute() {
        try {
            target = nodeApi.whoAmI(targetNodeName);
            targetAvatarMediaFile = mediaManager.downloadPublicMedia(targetNodeName, target.getAvatar());

            postingInfo = nodeApi.getPosting(targetNodeName, generateCarte(targetNodeName), postingId);
            if (postingInfo.getOwnerAvatar() != null) {
                MediaFile mediaFile = mediaManager.downloadPublicMedia(targetNodeName, postingInfo.getOwnerAvatar());
                postingInfo.getOwnerAvatar().setMediaFile(mediaFile);
            }

            mediaManager.uploadPublicMedia(targetNodeName, generateCarte(targetNodeName),
                    sourceText.getOwnerAvatarMediaFile());

            prevCommentInfo = commentId != null
                    ? nodeApi.getComment(targetNodeName, generateCarte(targetNodeName), postingId, commentId)
                    : null;
            String repliedToId = null;
            String repliedToRevisionId = null;
            if (prevCommentInfo != null) {
                repliedToId = prevCommentInfo.getRepliedToId();
                repliedToRevisionId = prevCommentInfo.getRepliedToRevisionId();
            } else if (sourceText.getRepliedToId() != null) {
                CommentInfo repliedToCommentInfo = nodeApi.getComment(targetNodeName, generateCarte(targetNodeName),
                        postingId, sourceText.getRepliedToId().toString());
                if (repliedToCommentInfo != null) {
                    repliedToId = repliedToCommentInfo.getId();
                    repliedToRevisionId = repliedToCommentInfo.getRevisionId();
                }
            }
            byte[] repliedToDigest = repliedToDigestVerifier.getRepliedToDigest(targetNodeName, this::generateCarte,
                    postingInfo, new HashMap<>(), repliedToId, repliedToRevisionId);
            CommentText commentText = buildComment(postingInfo, repliedToDigest);
            CommentInfo commentInfo;
            if (commentId == null) {
                CommentCreated created = nodeApi.postComment(targetNodeName, postingId, commentText);
                commentInfo = created.getComment();
                commentId = commentInfo.getId();
                send(new RemoteCommentAddedLiberin(targetNodeName, postingId, commentId));
            } else {
                commentInfo = nodeApi.putComment(targetNodeName, postingId, commentId, commentText);
                send(new RemoteCommentUpdatedLiberin(targetNodeName, postingId, commentId));
            }

            MediaFile repliedToAvatarMediaFile = null;
            if (commentInfo.getRepliedToAvatar() != null) {
                repliedToAvatarMediaFile =
                        mediaManager.downloadPublicMedia(targetNodeName, commentInfo.getRepliedToAvatar());
            }

            saveComment(commentInfo, repliedToAvatarMediaFile);
            success();
        } catch (Exception e) {
            error(e);
        }
    }

    private CommentText buildComment(PostingInfo postingInfo, byte[] repliedToDigest) {
        CommentText commentText = new CommentText(nodeName(), fullName(), gender(), sourceText, textConverter);
        Map<UUID, byte[]> mediaDigests = buildMediaDigestsMap();
        cacheMediaDigests(mediaDigests);
        byte[] parentMediaDigest = postingInfo.getParentMediaId() != null
                ? mediaManager.getPrivateMediaDigest(targetNodeName, generateCarte(targetNodeName),
                                                     postingInfo.getParentMediaId(), null)
                : null;
        CommentFingerprint fingerprint = new CommentFingerprint(
                commentText,
                Fingerprints.posting(postingInfo.getSignatureVersion()).create(
                        postingInfo,
                        parentMediaDigest,
                        pmf -> mediaManager.getPrivateMediaDigest(targetNodeName, generateCarte(targetNodeName), pmf)),
                repliedToDigest,
                id -> commentMediaDigest(id, mediaDigests));
        commentText.setSignature(CryptoUtil.sign(fingerprint, (ECPrivateKey) signingKey()));
        commentText.setSignatureVersion(CommentFingerprint.VERSION);
        return commentText;
    }

    private Map<UUID, byte[]> buildMediaDigestsMap() {
        if (sourceText.getMedia() == null) {
            return Collections.emptyMap();
        }

        return Arrays.stream(sourceText.getMedia())
                .filter(md -> md.getDigest() != null)
                .collect(Collectors.toMap(MediaWithDigest::getId, md -> Util.base64decode(md.getDigest())));
    }

    private void cacheMediaDigests(Map<UUID, byte[]> mediaDigests) {
        mediaDigests.forEach((id, digest) ->
                mediaManager.cacheUploadedRemoteMedia(targetNodeName, id.toString(), digest));
    }

    private byte[] commentMediaDigest(UUID id, Map<UUID, byte[]> mediaDigests) {
        if (mediaDigests.containsKey(id)) {
            return mediaDigests.get(id);
        }
        return mediaManager.getPrivateMediaDigest(targetNodeName, generateCarte(targetNodeName), id.toString(),
                null);
    }

    private void saveComment(CommentInfo info, MediaFile repliedToAvatarMediaFile) {
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
                    ownComment.setRemoteFullName(target.getFullName());
                    if (targetAvatarMediaFile != null) {
                        ownComment.setRemoteAvatarMediaFile(targetAvatarMediaFile);
                        ownComment.setRemoteAvatarShape(target.getAvatar().getShape());
                    }
                    if (repliedToAvatarMediaFile != null) {
                        ownComment.setRemoteRepliedToAvatarMediaFile(repliedToAvatarMediaFile);
                        ownComment.setRemoteRepliedToAvatarShape(info.getRepliedToAvatar().getShape());
                    }
                    ownComment = ownCommentRepository.save(ownComment);
                    contactOperations.updateCloseness(nodeId, targetNodeName, 1);
                    contactOperations.updateCloseness(nodeId, info.getRepliedToName(), 1);
                }
                info.toOwnComment(ownComment);
                ownComment.setPostingHeading(postingInfo.getHeading());
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

        if (prevCommentInfo == null) {
            send(new RemoteCommentAddingFailedLiberin(targetNodeName, postingId, postingInfo));
        } else {
            send(new RemoteCommentUpdateFailedLiberin(targetNodeName, postingId, postingInfo, commentId,
                    prevCommentInfo));
        }
    }

}
