package org.moera.node.rest.task;

import java.security.interfaces.ECPrivateKey;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import javax.inject.Inject;

import org.moera.commons.crypto.CryptoUtil;
import org.moera.node.api.NodeApiUnknownNameException;
import org.moera.node.data.MediaFile;
import org.moera.node.data.OwnCommentRepository;
import org.moera.node.fingerprint.CommentFingerprint;
import org.moera.node.fingerprint.PostingFingerprint;
import org.moera.node.instant.CommentInstants;
import org.moera.node.media.MediaManager;
import org.moera.node.model.MediaWithDigest;
import org.moera.node.model.PostingInfo;
import org.moera.node.model.PostingSourceText;
import org.moera.node.model.PostingText;
import org.moera.node.model.WhoAmI;
import org.moera.node.operations.ContactOperations;
import org.moera.node.task.Task;
import org.moera.node.text.TextConverter;
import org.moera.node.util.Util;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RemotePostingPostTask extends Task {

    private static final Logger log = LoggerFactory.getLogger(RemotePostingPostTask.class);

    private final String targetNodeName;
    private WhoAmI target;
    private MediaFile targetAvatarMediaFile;
    private String postingId;
    private final PostingSourceText sourceText;
    private PostingInfo prevPostingInfo;

    @Inject
    private TextConverter textConverter;

    @Inject
    private OwnCommentRepository ownCommentRepository;

    @Inject
    private ContactOperations contactOperations;

    @Inject
    private CommentInstants commentInstants;

    @Inject
    private MediaManager mediaManager;

    public RemotePostingPostTask(String targetNodeName, String postingId, PostingSourceText sourceText) {
        this.targetNodeName = targetNodeName;
        this.postingId = postingId;
        this.sourceText = sourceText;
    }

    @Override
    protected void execute() {
        try {
            target = nodeApi.whoAmI(targetNodeName);
            targetAvatarMediaFile = mediaManager.downloadPublicMedia(targetNodeName, target.getAvatar());

            mediaManager.uploadPublicMedia(targetNodeName, generateCarte(targetNodeName),
                    sourceText.getOwnerAvatarMediaFile());

            prevPostingInfo = postingId != null ? nodeApi.getPosting(targetNodeName, postingId) : null;
            PostingText postingText = buildPosting();
            PostingInfo postingInfo;
            if (postingId == null) {
                postingInfo = nodeApi.postPosting(targetNodeName, postingText);
                postingId = postingInfo.getId();
//                send(new RemoteCommentAddedEvent(targetNodeName, postingId, commentId));
            } else {
                postingInfo = nodeApi.putPosting(targetNodeName, postingId, postingText);
//                send(new RemoteCommentUpdatedEvent(targetNodeName, postingId, commentId));
            }

            savePosting(postingInfo);
            success();
        } catch (Exception e) {
            error(e);
        }
    }

    private PostingText buildPosting() {
        PostingText postingText = new PostingText(nodeName(), fullName(), sourceText, textConverter);
        Map<UUID, byte[]> mediaDigests = buildMediaDigestsMap();
        cacheMediaDigests(mediaDigests);
        PostingFingerprint fingerprint = new PostingFingerprint(
                postingText,
                id -> postingMediaDigest(id, mediaDigests));
        postingText.setSignature(CryptoUtil.sign(fingerprint, (ECPrivateKey) signingKey()));
        postingText.setSignatureVersion(CommentFingerprint.VERSION);
        return postingText;
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

    private byte[] postingMediaDigest(UUID id, Map<UUID, byte[]> mediaDigests) {
        if (mediaDigests.containsKey(id)) {
            return mediaDigests.get(id);
        }
        return mediaManager.getPrivateMediaDigest(targetNodeName, generateCarte(targetNodeName), id.toString(),
                null);
    }

    private void savePosting(PostingInfo info) {
        /*try {
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
                return null;
            });
        } catch (Throwable e) {
            error(e);
        }*/
    }

    private void success() {
        log.info("Succeeded to post posting to node {}", targetNodeName);
    }

    private void error(Throwable e) {
        if (e instanceof NodeApiUnknownNameException) {
            log.error("Cannot find a node {}", targetNodeName);
        } else {
            log.error("Error adding posting to node {}: {}", targetNodeName, e.getMessage());
        }

        /*if (prevPostingInfo == null) {
            commentInstants.addingFailed(postingId, postingInfo);
        } else {
            commentInstants.updateFailed(postingId, postingInfo, commentId, prevPostingInfo);
        }*/
    }

}
