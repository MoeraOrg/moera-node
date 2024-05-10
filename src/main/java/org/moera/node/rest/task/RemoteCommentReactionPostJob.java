package org.moera.node.rest.task;

import java.security.interfaces.ECPrivateKey;
import java.util.function.Function;
import javax.inject.Inject;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.moera.commons.crypto.CryptoUtil;
import org.moera.commons.crypto.Fingerprint;
import org.moera.node.api.node.NodeApiException;
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

    }

    private static final Logger log = LoggerFactory.getLogger(RemoteCommentReactionPostJob.class);

    private ReactionCreated created;

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
        log.info("Adding a reaction to the comment {} under posting {} at node {}",
                parameters.commentId, parameters.postingId, parameters.targetNodeName);
    }

    @Override
    protected void execute() throws NodeApiException {
        if (state.commentInfo == null) {
            mediaManager.uploadPublicMedia(
                    parameters.targetNodeName,
                    generateCarte(parameters.targetNodeName),
                    getAvatar());
            state.commentInfo = nodeApi.getComment(
                    parameters.targetNodeName,
                    generateCarte(parameters.targetNodeName),
                    parameters.postingId,
                    parameters.commentId);
            if (state.commentInfo.getOwnerAvatar() != null) {
                MediaFile mediaFile =
                        mediaManager.downloadPublicMedia(parameters.targetNodeName, state.commentInfo.getOwnerAvatar());
                state.commentInfo.getOwnerAvatar().setMediaFile(mediaFile);
            }
            checkpoint();
        }

        if (state.postingInfo == null) {
            state.postingInfo = nodeApi.getPosting(
                    parameters.targetNodeName,
                    generateCarte(parameters.targetNodeName),
                    parameters.postingId);
            if (state.postingInfo.getOwnerAvatar() != null) {
                MediaFile mediaFile =
                        mediaManager.downloadPublicMedia(parameters.targetNodeName, state.postingInfo.getOwnerAvatar());
                state.postingInfo.getOwnerAvatar().setMediaFile(mediaFile);
            }
            checkpoint();
        }

        if (state.postingRevisionInfo == null
                && !state.commentInfo.getPostingRevisionId().equals(state.postingInfo.getRevisionId())) {
            state.postingRevisionInfo = nodeApi.getPostingRevision(
                    parameters.targetNodeName,
                    generateCarte(parameters.targetNodeName),
                    parameters.postingId,
                    state.commentInfo.getPostingRevisionId());
            checkpoint();
        }

        created = nodeApi.postCommentReaction(
                parameters.targetNodeName,
                parameters.postingId,
                parameters.commentId,
                buildReaction());
    }

    private ReactionDescription buildReaction() {
        byte[] parentMediaDigest = state.postingInfo.getParentMediaId() != null
                ? mediaManager.getPrivateMediaDigest(
                        parameters.targetNodeName,
                        generateCarte(parameters.targetNodeName),
                state.postingInfo.getParentMediaId(),
                        null)
                : null;
        Function<PrivateMediaFileInfo, byte[]> mediaDigest =
                pmf -> mediaManager.getPrivateMediaDigest(
                        parameters.targetNodeName, generateCarte(parameters.targetNodeName), pmf);
        Fingerprint postingFingerprint = state.postingRevisionInfo == null
                ? Fingerprints.posting(state.postingInfo.getSignatureVersion())
                        .create(state.postingInfo, parentMediaDigest, mediaDigest)
                : Fingerprints.posting(state.postingRevisionInfo.getSignatureVersion())
                        .create(state.postingInfo, state.postingRevisionInfo, parentMediaDigest, mediaDigest);
        Fingerprint commentFingerprint = Fingerprints.comment(state.commentInfo.getSignatureVersion())
                .create(state.commentInfo, postingFingerprint, mediaDigest);
        ReactionFingerprint fingerprint = new ReactionFingerprint(nodeName(), parameters.attributes, commentFingerprint);

        ReactionDescription description = new ReactionDescription(
                nodeName(), fullName(), gender(), getAvatar(), parameters.attributes);
        description.setSignature(CryptoUtil.sign(fingerprint, (ECPrivateKey) signingKey()));
        description.setSignatureVersion(ReactionFingerprint.VERSION);

        return description;
    }

    @Override
    protected void succeeded() {
        super.succeeded();
        log.info("Succeeded to post a reaction to the comment {} under posting {} at node {}",
                created.getReaction().getCommentId(), created.getReaction().getPostingId(), parameters.targetNodeName);
    }

    @Override
    protected void failed() {
        super.failed();
        send(new RemoteCommentReactionAddingFailedLiberin(parameters.targetNodeName, parameters.postingId,
                state.postingInfo, parameters.commentId, state.commentInfo));
    }

}
