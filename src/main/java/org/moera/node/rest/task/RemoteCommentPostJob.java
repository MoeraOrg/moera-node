package org.moera.node.rest.task;

import java.security.interfaces.ECPrivateKey;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import jakarta.inject.Inject;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.moera.lib.crypto.CryptoUtil;
import org.moera.lib.node.exception.MoeraNodeException;
import org.moera.lib.node.types.AvatarImage;
import org.moera.lib.node.types.CommentCreated;
import org.moera.lib.node.types.CommentInfo;
import org.moera.lib.node.types.CommentSourceText;
import org.moera.lib.node.types.CommentText;
import org.moera.lib.node.types.PostingInfo;
import org.moera.lib.node.types.Scope;
import org.moera.lib.node.types.WhoAmI;
import org.moera.node.data.MediaFile;
import org.moera.node.data.MediaFileRepository;
import org.moera.node.data.OwnComment;
import org.moera.node.data.OwnCommentRepository;
import org.moera.node.fingerprint.CommentFingerprintBuilder;
import org.moera.node.fingerprint.PostingFingerprintBuilder;
import org.moera.node.liberin.model.RemoteCommentAddedLiberin;
import org.moera.node.liberin.model.RemoteCommentAddingFailedLiberin;
import org.moera.node.liberin.model.RemoteCommentUpdateFailedLiberin;
import org.moera.node.liberin.model.RemoteCommentUpdatedLiberin;
import org.moera.node.media.MediaManager;
import org.moera.node.model.AvatarDescriptionUtil;
import org.moera.node.model.AvatarImageUtil;
import org.moera.node.model.CommentInfoUtil;
import org.moera.node.model.CommentTextUtil;
import org.moera.node.operations.ContactOperations;
import org.moera.node.task.Job;
import org.moera.node.text.TextConverter;
import org.moera.node.util.Util;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.util.Pair;

public class RemoteCommentPostJob extends Job<RemoteCommentPostJob.Parameters, RemoteCommentPostJob.State> {

    public static class Parameters {

        private String targetNodeName;
        private String postingId;
        private String commentId;
        private CommentSourceText sourceText;

        public Parameters() {
        }

        public Parameters(String targetNodeName, String postingId, String commentId, CommentSourceText sourceText) {
            this.targetNodeName = targetNodeName;
            this.postingId = postingId;
            this.commentId = commentId;
            this.sourceText = sourceText;
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

        public CommentSourceText getSourceText() {
            return sourceText;
        }

        public void setSourceText(CommentSourceText sourceText) {
            this.sourceText = sourceText;
        }

    }

    public static class State {

        private WhoAmI target;
        private String targetAvatarMediaFileId;
        private boolean targetAvatarMediaFileLoaded;
        private PostingInfo postingInfo;
        private boolean ownerAvatarUploaded;
        private CommentInfo prevCommentInfo;
        private byte[] repliedToDigest;
        private boolean repliedToLoaded;
        private CommentText commentText;
        private CommentInfo commentInfo;

        public State() {
        }

        public WhoAmI getTarget() {
            return target;
        }

        public void setTarget(WhoAmI target) {
            this.target = target;
        }

        public String getTargetAvatarMediaFileId() {
            return targetAvatarMediaFileId;
        }

        public void setTargetAvatarMediaFileId(String targetAvatarMediaFileId) {
            this.targetAvatarMediaFileId = targetAvatarMediaFileId;
        }

        public boolean isTargetAvatarMediaFileLoaded() {
            return targetAvatarMediaFileLoaded;
        }

        public void setTargetAvatarMediaFileLoaded(boolean targetAvatarMediaFileLoaded) {
            this.targetAvatarMediaFileLoaded = targetAvatarMediaFileLoaded;
        }

        public PostingInfo getPostingInfo() {
            return postingInfo;
        }

        public void setPostingInfo(PostingInfo postingInfo) {
            this.postingInfo = postingInfo;
        }

        public boolean isOwnerAvatarUploaded() {
            return ownerAvatarUploaded;
        }

        public void setOwnerAvatarUploaded(boolean ownerAvatarUploaded) {
            this.ownerAvatarUploaded = ownerAvatarUploaded;
        }

        public CommentInfo getPrevCommentInfo() {
            return prevCommentInfo;
        }

        public void setPrevCommentInfo(CommentInfo prevCommentInfo) {
            this.prevCommentInfo = prevCommentInfo;
        }

        public byte[] getRepliedToDigest() {
            return repliedToDigest;
        }

        public void setRepliedToDigest(byte[] repliedToDigest) {
            this.repliedToDigest = repliedToDigest;
        }

        public boolean isRepliedToLoaded() {
            return repliedToLoaded;
        }

        public void setRepliedToLoaded(boolean repliedToLoaded) {
            this.repliedToLoaded = repliedToLoaded;
        }

        public CommentText getCommentText() {
            return commentText;
        }

        public void setCommentText(CommentText commentText) {
            this.commentText = commentText;
        }

        public CommentInfo getCommentInfo() {
            return commentInfo;
        }

        public void setCommentInfo(CommentInfo commentInfo) {
            this.commentInfo = commentInfo;
        }

    }

    private static final Logger log = LoggerFactory.getLogger(RemoteCommentPostJob.class);

    @Inject
    private TextConverter textConverter;

    @Inject
    private RepliedToDigestVerifier repliedToDigestVerifier;

    @Inject
    private OwnCommentRepository ownCommentRepository;

    @Inject
    private MediaFileRepository mediaFileRepository;

    @Inject
    private ContactOperations contactOperations;

    @Inject
    private MediaManager mediaManager;

    public RemoteCommentPostJob() {
        state = new State();
    }

    @Override
    protected void setParameters(String parameters, ObjectMapper objectMapper) throws JsonProcessingException {
        this.parameters = objectMapper.readValue(parameters, RemoteCommentPostJob.Parameters.class);
    }

    @Override
    protected void setState(String state, ObjectMapper objectMapper) throws JsonProcessingException {
        this.state = objectMapper.readValue(state, RemoteCommentPostJob.State.class);
    }

    @Override
    protected void started() {
        super.started();
        log.info("Posting comment to posting {} at node {}", parameters.postingId, parameters.targetNodeName);
    }

    @Override
    protected void execute() throws MoeraNodeException {
        if (state.target == null) {
            state.target = nodeApi.at(parameters.targetNodeName).whoAmI();
            checkpoint();
        }

        if (!state.targetAvatarMediaFileLoaded) {
            MediaFile mediaFile = mediaManager.downloadPublicMedia(parameters.targetNodeName, state.target.getAvatar());
            state.targetAvatarMediaFileId = mediaFile != null ? mediaFile.getId() : null;
            state.targetAvatarMediaFileLoaded = true;
            checkpoint();
        }

        if (state.postingInfo == null) {
            state.postingInfo = nodeApi
                .at(parameters.targetNodeName, generateCarte(parameters.targetNodeName, Scope.VIEW_CONTENT))
                .getPosting(parameters.postingId, false);
            checkpoint();
        }

        if (
            state.postingInfo.getOwnerAvatar() != null
            && AvatarImageUtil.getMediaFile(state.postingInfo.getOwnerAvatar()) == null
        ) {
            MediaFile mediaFile = mediaManager.downloadPublicMedia(
                parameters.targetNodeName,
                state.postingInfo.getOwnerAvatar()
            );
            AvatarImageUtil.setMediaFile(state.postingInfo.getOwnerAvatar(), mediaFile);
            checkpoint();
        }

        if (!state.ownerAvatarUploaded) {
            mediaManager.uploadPublicMedia(
                parameters.targetNodeName,
                generateCarte(parameters.targetNodeName, Scope.UPLOAD_PUBLIC_MEDIA),
                AvatarDescriptionUtil.getMediaFile(parameters.sourceText.getOwnerAvatar())
            );
            state.ownerAvatarUploaded = true;
            checkpoint();
        }

        if (parameters.commentId != null && state.prevCommentInfo == null) {
            state.prevCommentInfo = nodeApi
                .at(parameters.targetNodeName, generateCarte(parameters.targetNodeName, Scope.VIEW_CONTENT))
                .getComment(parameters.postingId, parameters.commentId, false);
            checkpoint();

        }

        if (!state.repliedToLoaded) {
            String repliedToId = null;
            String repliedToRevisionId = null;
            if (state.prevCommentInfo != null) {
                repliedToId = CommentInfoUtil.getRepliedToId(state.prevCommentInfo);
                repliedToRevisionId = CommentInfoUtil.getRepliedToRevisionId(state.prevCommentInfo);
            } else if (parameters.sourceText.getRepliedToId() != null) {
                CommentInfo repliedToCommentInfo = nodeApi
                    .at(parameters.targetNodeName, generateCarte(parameters.targetNodeName, Scope.VIEW_CONTENT))
                    .getComment(parameters.postingId, parameters.sourceText.getRepliedToId(), false);
                if (repliedToCommentInfo != null) {
                    repliedToId = repliedToCommentInfo.getId();
                    repliedToRevisionId = repliedToCommentInfo.getRevisionId();
                }
            }
            state.repliedToDigest = repliedToDigestVerifier.getRepliedToDigest(
                parameters.targetNodeName,
                this::generateCarte,
                state.postingInfo,
                new HashMap<>(),
                repliedToId,
                repliedToRevisionId
            );
            state.repliedToLoaded = true;
            checkpoint();
        }

        if (state.commentText == null) {
            state.commentText = buildComment();
            checkpoint();
        }

        if (state.commentInfo == null) {
            if (parameters.commentId == null) {
                CommentCreated created = nodeApi
                    .at(parameters.targetNodeName)
                    .createComment(parameters.postingId, state.commentText);
                state.commentInfo = created.getComment();
                String commentId = state.commentInfo.getId();
                send(new RemoteCommentAddedLiberin(parameters.targetNodeName, parameters.postingId, commentId));
            } else {
                state.commentInfo = nodeApi
                    .at(parameters.targetNodeName)
                    .updateComment(parameters.postingId, parameters.commentId, state.commentText);
                send(
                    new RemoteCommentUpdatedLiberin(
                        parameters.targetNodeName, parameters.postingId, parameters.commentId
                    )
                );
            }
            checkpoint();
        }

        MediaFile repliedToAvatarMediaFile = null;
        if (CommentInfoUtil.getRepliedToAvatar(state.commentInfo) != null) {
            repliedToAvatarMediaFile = mediaManager.downloadPublicMedia(
                parameters.targetNodeName,
                CommentInfoUtil.getRepliedToAvatar(state.commentInfo)
            );
        }

        saveComment(state.commentInfo, repliedToAvatarMediaFile);
    }

    private CommentText buildComment() {
        CommentText commentText = CommentTextUtil.build(
            nodeName(), fullName(), gender(), parameters.sourceText, textConverter
        );
        Map<UUID, byte[]> mediaDigests = buildMediaDigestsMap();
        cacheMediaDigests(mediaDigests);
        byte[] parentMediaDigest = state.postingInfo.getParentMediaId() != null
                ? mediaManager.getPrivateMediaDigest(
                    parameters.targetNodeName,
                    generateCarte(parameters.targetNodeName, Scope.VIEW_MEDIA),
                    state.postingInfo.getParentMediaId(),
                    null
                )
                : null;
        byte[] fingerprint = CommentFingerprintBuilder.build(
            commentText,
            id -> commentMediaDigest(id, mediaDigests),
            CryptoUtil.digest(PostingFingerprintBuilder.build(
                state.postingInfo.getSignatureVersion(),
                state.postingInfo,
                parentMediaDigest,
                pmf -> mediaManager.getPrivateMediaDigest(
                    parameters.targetNodeName,
                    generateCarte(parameters.targetNodeName, Scope.VIEW_MEDIA),
                    pmf
                )
            )),
            state.repliedToDigest
        );
        commentText.setSignature(CryptoUtil.sign(fingerprint, (ECPrivateKey) signingKey()));
        commentText.setSignatureVersion(CommentFingerprintBuilder.LATEST_VERSION);
        return commentText;
    }

    private Map<UUID, byte[]> buildMediaDigestsMap() {
        if (parameters.sourceText.getMedia() == null) {
            return Collections.emptyMap();
        }

        return parameters.sourceText.getMedia()
            .stream()
            .filter(md -> md.getDigest() != null)
            .map(md -> Pair.of(Util.uuid(md.getId()), md.getDigest()))
            .filter(p -> p.getFirst().isPresent())
            .collect(Collectors.toMap(p -> p.getFirst().get(), p -> Util.base64decode(p.getSecond())));
    }

    private void cacheMediaDigests(Map<UUID, byte[]> mediaDigests) {
        mediaDigests.forEach((id, digest) ->
            mediaManager.cacheUploadedRemoteMedia(parameters.targetNodeName, id.toString(), digest)
        );
    }

    private byte[] commentMediaDigest(UUID id, Map<UUID, byte[]> mediaDigests) {
        if (mediaDigests.containsKey(id)) {
            return mediaDigests.get(id);
        }
        return mediaManager.getPrivateMediaDigest(
            parameters.targetNodeName,
            generateCarte(parameters.targetNodeName, Scope.VIEW_MEDIA),
            id.toString(),
            null
        );
    }

    private void saveComment(CommentInfo info, MediaFile repliedToAvatarMediaFile) {
        tx.executeWrite(
            () -> {
                OwnComment ownComment = ownCommentRepository
                    .findByRemoteCommentId(nodeId, parameters.targetNodeName, parameters.postingId, info.getId())
                    .orElse(null);
                if (ownComment == null) {
                    ownComment = new OwnComment();
                    ownComment.setId(UUID.randomUUID());
                    ownComment.setNodeId(nodeId);
                    ownComment.setRemoteNodeName(parameters.targetNodeName);
                    ownComment.setRemoteFullName(state.target.getFullName());
                    if (state.targetAvatarMediaFileId != null) {
                        MediaFile mediaFile = mediaFileRepository.findById(state.targetAvatarMediaFileId).orElse(null);
                        ownComment.setRemoteAvatarMediaFile(mediaFile);
                        ownComment.setRemoteAvatarShape(state.target.getAvatar().getShape());
                    }
                    if (repliedToAvatarMediaFile != null) {
                        ownComment.setRemoteRepliedToAvatarMediaFile(repliedToAvatarMediaFile);
                        AvatarImage repliedToAvatar = CommentInfoUtil.getRepliedToAvatar(info);
                        String remoteRepliedToAvatarShape = repliedToAvatar != null ? repliedToAvatar.getShape() : null;
                        ownComment.setRemoteRepliedToAvatarShape(remoteRepliedToAvatarShape);
                    }
                    ownComment = ownCommentRepository.save(ownComment);
                    contactOperations.updateCloseness(nodeId, parameters.targetNodeName, 1);
                    contactOperations.updateCloseness(nodeId, CommentInfoUtil.getRepliedToName(info), 1);
                }
                CommentInfoUtil.toOwnComment(info, ownComment);
                ownComment.setPostingHeading(state.postingInfo.getHeading());
            }
        );
    }

    @Override
    protected void succeeded() {
        super.succeeded();
        log.info("Succeeded to post comment to posting {} at node {}", parameters.postingId, parameters.targetNodeName);
    }

    @Override
    protected void failed() {
        super.failed();
        if (state.prevCommentInfo == null) {
            send(
                new RemoteCommentAddingFailedLiberin(
                    parameters.targetNodeName,
                    parameters.postingId,
                    state.postingInfo
                )
            );
        } else {
            send(
                new RemoteCommentUpdateFailedLiberin(
                    parameters.targetNodeName,
                    parameters.postingId,
                    state.postingInfo,
                    parameters.commentId,
                    state.prevCommentInfo
                )
            );
        }
    }

}
