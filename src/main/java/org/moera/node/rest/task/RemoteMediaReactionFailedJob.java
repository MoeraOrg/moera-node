package org.moera.node.rest.task;

import jakarta.inject.Inject;

import org.moera.lib.node.exception.MoeraNodeException;
import org.moera.lib.node.types.CommentInfo;
import org.moera.lib.node.types.ParentMediaInfo;
import org.moera.lib.node.types.PostingInfo;
import org.moera.lib.node.types.Scope;
import org.moera.node.liberin.model.RemoteCommentMediaReactionAddingFailedLiberin;
import org.moera.node.liberin.model.RemotePostingMediaReactionAddingFailedLiberin;
import org.moera.node.media.MediaManager;
import org.moera.node.model.AvatarImageUtil;
import org.moera.node.task.Job;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tools.jackson.databind.ObjectMapper;

public class RemoteMediaReactionFailedJob
        extends Job<RemoteMediaReactionFailedJob.Parameters, RemoteMediaReactionFailedJob.State> {

    public static class Parameters {

        private String targetNodeName;
        private ParentMediaInfo parentMedia;
        private String postingId; // The posting linked to the media

        public Parameters() {
        }

        public Parameters(String targetNodeName, ParentMediaInfo parentMedia, String postingId) {
            this.targetNodeName = targetNodeName;
            this.parentMedia = parentMedia;
            this.postingId = postingId;
        }

        public String getTargetNodeName() {
            return targetNodeName;
        }

        public void setTargetNodeName(String targetNodeName) {
            this.targetNodeName = targetNodeName;
        }

        public ParentMediaInfo getMediaParent() {
            return parentMedia;
        }

        public void setMediaParent(ParentMediaInfo mediaParent) {
            this.parentMedia = mediaParent;
        }

        public String getPostingId() {
            return postingId;
        }

        public void setPostingId(String postingId) {
            this.postingId = postingId;
        }

    }

    public static class State {

        private PostingInfo parentPosting;
        private CommentInfo parentComment;

        public State() {
        }

        public PostingInfo getParentPosting() {
            return parentPosting;
        }

        public void setParentPosting(PostingInfo parentPosting) {
            this.parentPosting = parentPosting;
        }

        public CommentInfo getParentComment() {
            return parentComment;
        }

        public void setParentComment(CommentInfo parentComment) {
            this.parentComment = parentComment;
        }

    }

    private static final Logger log = LoggerFactory.getLogger(RemoteMediaReactionFailedJob.class);

    @Inject
    private MediaManager mediaManager;

    public RemoteMediaReactionFailedJob() {
        state = new State();
    }

    @Override
    protected void setParameters(String parameters, ObjectMapper objectMapper) {
        this.parameters = objectMapper.readValue(parameters, Parameters.class);
    }

    @Override
    protected void setState(String state, ObjectMapper objectMapper) {
        this.state = objectMapper.readValue(state, State.class);
    }

    @Override
    protected void execute() throws MoeraNodeException {
        if (state.parentComment.getPostingId() == null) {
            success();
        }

        if (state.parentPosting == null) {
            state.parentPosting = nodeApi
                .at(parameters.targetNodeName, generateCarte(parameters.targetNodeName, Scope.VIEW_CONTENT))
                .getPosting(state.parentComment.getPostingId(), false);
            checkpoint();
        }

        if (parameters.parentMedia.getCommentId() != null && state.parentComment == null) {
            state.parentComment = nodeApi
                .at(parameters.targetNodeName, generateCarte(parameters.targetNodeName, Scope.VIEW_CONTENT))
                .getComment(state.parentComment.getPostingId(), parameters.parentMedia.getCommentId(), false);
            checkpoint();
        }

        if (state.parentPosting.getOwnerAvatar() != null) {
            AvatarImageUtil.setMediaFile(
                state.parentPosting.getOwnerAvatar(),
                mediaManager.downloadPublicMedia(parameters.targetNodeName, state.parentPosting.getOwnerAvatar())
            );
        }
        if (state.parentComment != null && state.parentComment.getOwnerAvatar() != null) {
            AvatarImageUtil.setMediaFile(
                state.parentComment.getOwnerAvatar(),
                mediaManager.downloadPublicMedia(parameters.targetNodeName, state.parentComment.getOwnerAvatar())
            );
        }

        if (state.parentComment == null) {
            send(
                new RemotePostingMediaReactionAddingFailedLiberin(
                    parameters.targetNodeName,
                    parameters.postingId,
                    state.parentPosting.getId(),
                    parameters.parentMedia.getMediaId(),
                    state.parentPosting
                )
            );
        } else {
            send(
                new RemoteCommentMediaReactionAddingFailedLiberin(
                    parameters.targetNodeName,
                    parameters.postingId,
                    parameters.parentMedia.getMediaId(),
                    state.parentPosting,
                    state.parentComment
                )
            );
        }
    }

    @Override
    protected void failed() {
        super.failed();
        if (parameters.parentMedia.getNodeName() == null) {
            log.error(
                "Failed to send error message to the owner of the parent posting/comment for media {} at node {}",
                parameters.parentMedia.getMediaId(), parameters.targetNodeName
            );
        } else {
            log.error(
                "Failed to send error message to the owner of the parent posting/comment for media {} leased from"
                    + " the node {} at node {}",
                parameters.parentMedia.getMediaId(), parameters.parentMedia.getNodeName(), parameters.targetNodeName
            );
        }
    }

}
