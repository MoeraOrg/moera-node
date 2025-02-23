package org.moera.node.rest.task;

import jakarta.inject.Inject;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.moera.lib.node.types.CommentInfo;
import org.moera.lib.node.types.EntryInfo;
import org.moera.lib.node.types.PostingInfo;
import org.moera.lib.node.types.Scope;
import org.moera.node.api.node.NodeApiException;
import org.moera.node.liberin.model.RemoteCommentMediaReactionAddingFailedLiberin;
import org.moera.node.liberin.model.RemotePostingMediaReactionAddingFailedLiberin;
import org.moera.node.media.MediaManager;
import org.moera.node.model.AvatarImageUtil;
import org.moera.node.task.Job;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RemoteMediaReactionFailedJob
        extends Job<RemoteMediaReactionFailedJob.Parameters, RemoteMediaReactionFailedJob.State> {

    public static class Parameters {

        private String targetNodeName;
        private String mediaId;
        private String postingId; // The posting linked to the media

        public Parameters() {
        }

        public Parameters(String targetNodeName, String mediaId, String postingId) {
            this.targetNodeName = targetNodeName;
            this.mediaId = mediaId;
            this.postingId = postingId;
        }

        public String getTargetNodeName() {
            return targetNodeName;
        }

        public void setTargetNodeName(String targetNodeName) {
            this.targetNodeName = targetNodeName;
        }

        public String getMediaId() {
            return mediaId;
        }

        public void setMediaId(String mediaId) {
            this.mediaId = mediaId;
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
    protected void setParameters(String parameters, ObjectMapper objectMapper) throws JsonProcessingException {
        this.parameters = objectMapper.readValue(parameters, Parameters.class);
    }

    @Override
    protected void setState(String state, ObjectMapper objectMapper) throws JsonProcessingException {
        this.state = objectMapper.readValue(state, State.class);
    }

    @Override
    protected void execute() throws NodeApiException {
        if (state.parentPosting == null) {
            EntryInfo[] parents = nodeApi.getPrivateMediaParent(
                parameters.targetNodeName,
                generateCarte(parameters.targetNodeName, Scope.VIEW_MEDIA),
                parameters.mediaId
            );
            if (parents != null && parents.length > 0) {
                if (parents[0].getComment() == null) {
                    state.parentPosting = parents[0].getPosting();
                } else {
                    state.parentComment = parents[0].getComment();
                    if (state.parentComment != null) {
                        state.parentPosting = nodeApi.getPosting(
                            parameters.targetNodeName,
                            generateCarte(parameters.targetNodeName, Scope.VIEW_CONTENT),
                            state.parentComment.getPostingId()
                        );
                    }
                }
            }
            checkpoint();
        }

        if (state.parentPosting == null) {
            success();
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
                    parameters.mediaId,
                    state.parentPosting
                )
            );
        } else {
            send(
                new RemoteCommentMediaReactionAddingFailedLiberin(
                    parameters.targetNodeName,
                    parameters.postingId,
                    parameters.mediaId,
                    state.parentPosting,
                    state.parentComment
                )
            );
        }
    }

    @Override
    protected void failed() {
        super.failed();
        log.error("Failed to send error message to the owner of the parent posting/comment for media {} at node {}",
                parameters.mediaId, parameters.targetNodeName);
    }

}
