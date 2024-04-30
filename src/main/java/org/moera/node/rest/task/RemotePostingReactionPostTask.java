package org.moera.node.rest.task;

import java.security.interfaces.ECPrivateKey;
import java.util.UUID;
import javax.inject.Inject;

import org.moera.commons.crypto.CryptoUtil;
import org.moera.node.api.node.NodeApiException;
import org.moera.node.api.node.NodeApiUnknownNameException;
import org.moera.node.data.MediaFile;
import org.moera.node.data.OwnReaction;
import org.moera.node.data.OwnReactionRepository;
import org.moera.node.fingerprint.Fingerprints;
import org.moera.node.fingerprint.ReactionFingerprint;
import org.moera.node.liberin.model.RemoteCommentMediaReactionAddingFailedLiberin;
import org.moera.node.liberin.model.RemotePostingMediaReactionAddingFailedLiberin;
import org.moera.node.liberin.model.RemotePostingReactionAddedLiberin;
import org.moera.node.liberin.model.RemotePostingReactionAddingFailedLiberin;
import org.moera.node.media.MediaManager;
import org.moera.node.model.CommentInfo;
import org.moera.node.model.EntryInfo;
import org.moera.node.model.PostingInfo;
import org.moera.node.model.ReactionAttributes;
import org.moera.node.model.ReactionCreated;
import org.moera.node.model.ReactionDescription;
import org.moera.node.model.ReactionInfo;
import org.moera.node.model.WhoAmI;
import org.moera.node.operations.ContactOperations;
import org.moera.node.task.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RemotePostingReactionPostTask extends Task {

    private static final Logger log = LoggerFactory.getLogger(RemotePostingReactionPostTask.class);

    private final String targetNodeName;
    private WhoAmI target;
    private MediaFile targetAvatarMediaFile;
    private final String postingId;
    private final ReactionAttributes attributes;
    private PostingInfo postingInfo;

    @Inject
    private OwnReactionRepository ownReactionRepository;

    @Inject
    private ContactOperations contactOperations;

    @Inject
    private MediaManager mediaManager;

    public RemotePostingReactionPostTask(String targetNodeName, String postingId, ReactionAttributes attributes) {
        this.targetNodeName = targetNodeName;
        this.postingId = postingId;
        this.attributes = attributes;
    }

    @Override
    protected void execute() {
        try {
            target = nodeApi.whoAmI(targetNodeName);
            targetAvatarMediaFile = mediaManager.downloadPublicMedia(targetNodeName, target.getAvatar());
            mediaManager.uploadPublicMedia(targetNodeName, generateCarte(targetNodeName), getAvatar());
            postingInfo = nodeApi.getPosting(targetNodeName, generateCarte(targetNodeName), postingId);
            if (postingInfo.getOwnerAvatar() != null) {
                MediaFile mediaFile = mediaManager.downloadPublicMedia(targetNodeName, postingInfo.getOwnerAvatar());
                postingInfo.getOwnerAvatar().setMediaFile(mediaFile);
            }
            ReactionCreated created = nodeApi.postPostingReaction(targetNodeName, postingId, buildReaction(postingInfo));
            saveReaction(created.getReaction());
            success(created);
        } catch (Exception e) {
            error(e);
        }
    }

    private ReactionDescription buildReaction(PostingInfo postingInfo) {
        byte[] parentMediaDigest = postingInfo.getParentMediaId() != null
                ? mediaManager.getPrivateMediaDigest(targetNodeName, generateCarte(targetNodeName),
                                                     postingInfo.getParentMediaId(), null)
                : null;
        ReactionFingerprint fingerprint = new ReactionFingerprint(
                nodeName(),
                attributes,
                Fingerprints.posting(postingInfo.getSignatureVersion()).create(
                        postingInfo,
                        parentMediaDigest,
                        pmf -> mediaManager.getPrivateMediaDigest(targetNodeName, generateCarte(targetNodeName), pmf)));
        ReactionDescription description = new ReactionDescription(
                nodeName(), fullName(), gender(), getAvatar(), attributes);
        description.setSignature(CryptoUtil.sign(fingerprint, (ECPrivateKey) signingKey()));
        description.setSignatureVersion(ReactionFingerprint.VERSION);
        return description;
    }

    private void saveReaction(ReactionInfo info) {
        tx.executeWriteQuietly(
            () -> {
                OwnReaction ownReaction = ownReactionRepository
                        .findByRemotePostingId(nodeId, targetNodeName, postingId)
                        .orElse(null);
                if (ownReaction == null) {
                    ownReaction = new OwnReaction();
                    ownReaction.setId(UUID.randomUUID());
                    ownReaction.setNodeId(nodeId);
                    ownReaction.setRemoteNodeName(targetNodeName);
                    ownReaction.setRemoteFullName(target.getFullName());
                    if (targetAvatarMediaFile != null) {
                        ownReaction.setRemoteAvatarMediaFile(targetAvatarMediaFile);
                        ownReaction.setRemoteAvatarShape(target.getAvatar().getShape());
                    }
                    ownReaction = ownReactionRepository.save(ownReaction);
                    contactOperations.updateCloseness(nodeId, targetNodeName, 0.25f);
                }
                info.toOwnReaction(ownReaction);
                ownReaction.setPostingHeading(postingInfo.getHeading());
            },
            this::error
        );
        send(new RemotePostingReactionAddedLiberin(targetNodeName, postingId, info));
    }

    private void success(ReactionCreated info) {
        log.info("Succeeded to post reaction to posting {} at node {}",
                info.getReaction().getPostingId(), targetNodeName);
    }

    private void error(Throwable e) {
        if (e instanceof NodeApiUnknownNameException) {
            log.error("Cannot find a node {}", targetNodeName);
        } else {
            log.error("Error adding reaction to posting {} at node {}: {}", postingId, targetNodeName, e.getMessage());
        }

        if (postingInfo.getParentMediaId() == null) {
            send(new RemotePostingReactionAddingFailedLiberin(targetNodeName, postingId, postingInfo));
        } else {
            PostingInfo parentPosting = null;
            CommentInfo parentComment = null;
            try {
                EntryInfo[] parents = nodeApi.getPrivateMediaParent(targetNodeName, generateCarte(targetNodeName),
                        postingInfo.getParentMediaId());
                if (parents != null && parents.length > 0) {
                    if (parents[0].getComment() == null) {
                        parentPosting = parents[0].getPosting();
                    } else {
                        parentComment = parents[0].getComment();
                        if (parentComment != null) {
                            parentPosting = nodeApi.getPosting(targetNodeName, generateCarte(targetNodeName),
                                    parentComment.getPostingId());
                        }
                    }
                }
            } catch (NodeApiException ex) {
                log.error("Failed to get a parent posting/comment for media {} at node {}: {}",
                        postingInfo.getParentMediaId(), targetNodeName, ex.getMessage());
            }

            try {
                if (parentPosting != null && parentPosting.getOwnerAvatar() != null) {
                    parentPosting.getOwnerAvatar().setMediaFile(
                            mediaManager.downloadPublicMedia(targetNodeName, parentPosting.getOwnerAvatar()));
                }
                if (parentComment != null && parentComment.getOwnerAvatar() != null) {
                    parentComment.getOwnerAvatar().setMediaFile(
                            mediaManager.downloadPublicMedia(targetNodeName, parentComment.getOwnerAvatar()));
                }
            } catch (NodeApiException ex) {
                // ignore
            }

            if (parentComment == null) {
                String parentPostingId = parentPosting != null ? parentPosting.getId() : null;
                send(new RemotePostingMediaReactionAddingFailedLiberin(targetNodeName, postingId, parentPostingId,
                        postingInfo.getParentMediaId(), parentPosting));
            } else {
                send(new RemoteCommentMediaReactionAddingFailedLiberin(targetNodeName, postingId,
                        postingInfo.getParentMediaId(), parentPosting, parentComment));
            }
        }
    }

}
