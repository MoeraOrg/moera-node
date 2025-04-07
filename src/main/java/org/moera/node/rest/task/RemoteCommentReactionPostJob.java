package org.moera.node.rest.task;

import java.security.interfaces.ECPrivateKey;
import java.util.function.Function;
import jakarta.inject.Inject;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.moera.lib.crypto.CryptoUtil;
import org.moera.lib.node.exception.MoeraNodeException;
import org.moera.lib.node.types.CommentInfo;
import org.moera.lib.node.types.PostingInfo;
import org.moera.lib.node.types.PostingRevisionInfo;
import org.moera.lib.node.types.PrivateMediaFileInfo;
import org.moera.lib.node.types.ReactionAttributes;
import org.moera.lib.node.types.ReactionCreated;
import org.moera.lib.node.types.ReactionDescription;
import org.moera.lib.node.types.Scope;
import org.moera.node.data.MediaFile;
import org.moera.node.fingerprint.CommentFingerprintBuilder;
import org.moera.node.fingerprint.PostingFingerprintBuilder;
import org.moera.node.fingerprint.ReactionFingerprintBuilder;
import org.moera.node.liberin.model.RemoteCommentReactionAddingFailedLiberin;
import org.moera.node.media.MediaManager;
import org.moera.node.model.AvatarImageUtil;
import org.moera.node.model.ReactionDescriptionUtil;
import org.moera.node.operations.FavorOperations;
import org.moera.node.operations.FavorType;
import org.moera.node.task.Job;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RemoteCommentReactionPostJob
        extends Job<RemoteCommentReactionPostJob.Parameters, RemoteCommentReactionPostJob.State> {

    public static class Parameters {

        private String targetNodeName;
        private String postingId;
        private String commentId;
        private ReactionAttributes attributes;

        public Parameters() {
        }

        public Parameters(String targetNodeName, String postingId, String commentId, ReactionAttributes attributes) {
            this.targetNodeName = targetNodeName;
            this.postingId = postingId;
            this.commentId = commentId;
            this.attributes = attributes;
        }

        public String getTargetNodeName() {
            return targetNodeName;
        }

        public void setTargetNodeName(String targetNodeName) {
            this.targetNodeName = targetNodeName;
        }

        public String getPostingId() {
            return postingId;
        }

        public void setPostingId(String postingId) {
            this.postingId = postingId;
        }

        public String getCommentId() {
            return commentId;
        }

        public void setCommentId(String commentId) {
            this.commentId = commentId;
        }

        public ReactionAttributes getAttributes() {
            return attributes;
        }

        public void setAttributes(ReactionAttributes attributes) {
            this.attributes = attributes;
        }

    }

    public static class State {

        private PostingInfo postingInfo;
        private PostingRevisionInfo postingRevisionInfo;
        private CommentInfo commentInfo;
        private ReactionCreated reactionCreated;

        public State() {
        }

        public PostingInfo getPostingInfo() {
            return postingInfo;
        }

        public void setPostingInfo(PostingInfo postingInfo) {
            this.postingInfo = postingInfo;
        }

        public PostingRevisionInfo getPostingRevisionInfo() {
            return postingRevisionInfo;
        }

        public void setPostingRevisionInfo(PostingRevisionInfo postingRevisionInfo) {
            this.postingRevisionInfo = postingRevisionInfo;
        }

        public CommentInfo getCommentInfo() {
            return commentInfo;
        }

        public void setCommentInfo(CommentInfo commentInfo) {
            this.commentInfo = commentInfo;
        }

        public ReactionCreated getReactionCreated() {
            return reactionCreated;
        }

        public void setReactionCreated(ReactionCreated reactionCreated) {
            this.reactionCreated = reactionCreated;
        }

    }

    private static final Logger log = LoggerFactory.getLogger(RemoteCommentReactionPostJob.class);

    @Inject
    private FavorOperations favorOperations;

    @Inject
    private MediaManager mediaManager;

    public RemoteCommentReactionPostJob() {
        state = new State();
        exponentialRetry("PT10S", "PT30M");
    }

    @Override
    protected void setParameters(String parameters, ObjectMapper objectMapper) throws JsonProcessingException {
        this.parameters = objectMapper.readValue(parameters, Parameters.class);
    }

    @Override
    protected void setState(String state, ObjectMapper objectMapper) throws JsonProcessingException {
        this.state = objectMapper.readValue(state, State.class);
    }

    @Override
    protected void started() {
        super.started();
        log.info(
            "Adding a reaction to the comment {} under posting {} at node {}",
            parameters.commentId, parameters.postingId, parameters.targetNodeName
        );
    }

    @Override
    protected void execute() throws MoeraNodeException {
        if (state.commentInfo == null) {
            mediaManager.uploadPublicMedia(
                parameters.targetNodeName,
                generateCarte(parameters.targetNodeName, Scope.UPLOAD_PUBLIC_MEDIA),
                getAvatar()
            );
            state.commentInfo = nodeApi
                .at(parameters.targetNodeName, generateCarte(parameters.targetNodeName, Scope.VIEW_CONTENT))
                .getComment(parameters.postingId, parameters.commentId, false);
            if (state.commentInfo.getOwnerAvatar() != null) {
                MediaFile mediaFile =
                        mediaManager.downloadPublicMedia(parameters.targetNodeName, state.commentInfo.getOwnerAvatar());
                AvatarImageUtil.setMediaFile(state.commentInfo.getOwnerAvatar(), mediaFile);
            }
            checkpoint();
        }

        if (state.postingInfo == null) {
            state.postingInfo = nodeApi
                .at(parameters.targetNodeName, generateCarte(parameters.targetNodeName, Scope.VIEW_CONTENT))
                .getPosting(parameters.postingId, false);
            if (state.postingInfo.getOwnerAvatar() != null) {
                MediaFile mediaFile =
                    mediaManager.downloadPublicMedia(parameters.targetNodeName, state.postingInfo.getOwnerAvatar());
                AvatarImageUtil.setMediaFile(state.postingInfo.getOwnerAvatar(), mediaFile);
            }
            checkpoint();
        }

        if (
            state.postingRevisionInfo == null
            && !state.commentInfo.getPostingRevisionId().equals(state.postingInfo.getRevisionId())
        ) {
            state.postingRevisionInfo = nodeApi
                .at(parameters.targetNodeName, generateCarte(parameters.targetNodeName, Scope.VIEW_CONTENT))
                .getPostingRevision(parameters.postingId, state.commentInfo.getPostingRevisionId());
            checkpoint();
        }

        if (state.reactionCreated == null) {
            state.reactionCreated = nodeApi
                .at(parameters.targetNodeName)
                .createCommentReaction(parameters.postingId, parameters.commentId, buildReaction());
        }

        favorOperations.addFavor(state.commentInfo.getOwnerName(), FavorType.LIKE_COMMENT);
    }

    private ReactionDescription buildReaction() {
        byte[] parentMediaDigest = state.postingInfo.getParentMediaId() != null
            ? mediaManager.getPrivateMediaDigest(
                parameters.targetNodeName,
                generateCarte(parameters.targetNodeName, Scope.VIEW_MEDIA),
                state.postingInfo.getParentMediaId(),
                null
            )
            : null;
        Function<PrivateMediaFileInfo, byte[]> mediaDigest =
            pmf -> mediaManager.getPrivateMediaDigest(
                parameters.targetNodeName, generateCarte(parameters.targetNodeName, Scope.VIEW_MEDIA), pmf
            );
        byte[] postingFingerprint = state.postingRevisionInfo == null
            ? PostingFingerprintBuilder.build(
                  state.postingInfo.getSignatureVersion(), state.postingInfo, parentMediaDigest, mediaDigest
            )
            : PostingFingerprintBuilder.build(
                  state.postingRevisionInfo.getSignatureVersion(), state.postingInfo, state.postingRevisionInfo,
                  parentMediaDigest, mediaDigest
            );
        byte[] commentFingerprint = CommentFingerprintBuilder.build(
            state.commentInfo.getSignatureVersion(), state.commentInfo, mediaDigest, postingFingerprint
        );
        byte[] fingerprint = ReactionFingerprintBuilder.build(nodeName(), parameters.attributes, commentFingerprint);

        ReactionDescription description = ReactionDescriptionUtil.build(
            nodeName(), fullName(), gender(), getAvatar(), parameters.attributes
        );
        description.setSignature(CryptoUtil.sign(fingerprint, (ECPrivateKey) signingKey()));
        description.setSignatureVersion(ReactionFingerprintBuilder.LATEST_VERSION);

        return description;
    }

    @Override
    protected void succeeded() {
        super.succeeded();
        log.info(
            "Succeeded to post a reaction to the comment {} under posting {} at node {}",
            state.reactionCreated.getReaction().getCommentId(),
            state.reactionCreated.getReaction().getPostingId(),
            parameters.targetNodeName
        );
    }

    @Override
    protected void failed() {
        super.failed();
        send(
            new RemoteCommentReactionAddingFailedLiberin(
                parameters.targetNodeName, parameters.postingId, state.postingInfo, parameters.commentId,
                state.commentInfo
            )
        );
    }

}
