package org.moera.node.rest.task;

import java.security.interfaces.ECPrivateKey;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import jakarta.inject.Inject;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.moera.lib.crypto.CryptoUtil;
import org.moera.lib.node.types.Scope;
import org.moera.node.api.node.NodeApiException;
import org.moera.node.data.MediaFile;
import org.moera.node.data.MediaFileRepository;
import org.moera.node.data.OwnPosting;
import org.moera.node.data.OwnPostingRepository;
import org.moera.node.fingerprint.PostingFingerprintBuilder;
import org.moera.node.liberin.model.RemotePostingAddedLiberin;
import org.moera.node.liberin.model.RemotePostingAddingFailedLiberin;
import org.moera.node.liberin.model.RemotePostingUpdateFailedLiberin;
import org.moera.node.liberin.model.RemotePostingUpdatedLiberin;
import org.moera.node.media.MediaManager;
import org.moera.node.model.MediaWithDigest;
import org.moera.node.model.PostingInfo;
import org.moera.node.model.PostingSourceText;
import org.moera.node.model.PostingText;
import org.moera.node.model.WhoAmI;
import org.moera.node.operations.ContactOperations;
import org.moera.node.task.Job;
import org.moera.node.text.TextConverter;
import org.moera.node.util.Util;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RemotePostingPostJob extends Job<RemotePostingPostJob.Parameters, RemotePostingPostJob.State> {

    public static class Parameters {

        private String targetNodeName;
        private String postingId;
        private PostingSourceText sourceText;

        public Parameters() {
        }

        public Parameters(String targetNodeName, String postingId, PostingSourceText sourceText) {
            this.targetNodeName = targetNodeName;
            this.postingId = postingId;
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

        public PostingSourceText getSourceText() {
            return sourceText;
        }

        public void setSourceText(PostingSourceText sourceText) {
            this.sourceText = sourceText;
        }

    }

    public static class State {

        private WhoAmI target;
        private String targetAvatarMediaFileId;
        private boolean targetAvatarMediaFileLoaded;
        private boolean ownerAvatarUploaded;
        private PostingInfo prevPostingInfo;
        private PostingText postingText;
        private PostingInfo postingInfo;

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

        public boolean isOwnerAvatarUploaded() {
            return ownerAvatarUploaded;
        }

        public void setOwnerAvatarUploaded(boolean ownerAvatarUploaded) {
            this.ownerAvatarUploaded = ownerAvatarUploaded;
        }

        public PostingInfo getPrevPostingInfo() {
            return prevPostingInfo;
        }

        public void setPrevPostingInfo(PostingInfo prevPostingInfo) {
            this.prevPostingInfo = prevPostingInfo;
        }

        public PostingText getPostingText() {
            return postingText;
        }

        public void setPostingText(PostingText postingText) {
            this.postingText = postingText;
        }

        public PostingInfo getPostingInfo() {
            return postingInfo;
        }

        public void setPostingInfo(PostingInfo postingInfo) {
            this.postingInfo = postingInfo;
        }

    }

    private static final Logger log = LoggerFactory.getLogger(RemotePostingPostJob.class);

    @Inject
    private TextConverter textConverter;

    @Inject
    private OwnPostingRepository ownPostingRepository;

    @Inject
    private MediaFileRepository mediaFileRepository;

    @Inject
    private ContactOperations contactOperations;

    @Inject
    private MediaManager mediaManager;

    public RemotePostingPostJob() {
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
    protected void started() {
        super.started();
        log.info("Adding posting to node {}", parameters.targetNodeName);
    }

    @Override
    protected void execute() throws NodeApiException {
        if (state.target == null) {
            state.target = nodeApi.whoAmI(parameters.targetNodeName);
            checkpoint();
        }

        if (!state.targetAvatarMediaFileLoaded) {
            MediaFile mediaFile = mediaManager.downloadPublicMedia(
                    parameters.targetNodeName,
                    state.target.getAvatar());
            state.targetAvatarMediaFileId = mediaFile != null ? mediaFile.getId() : null;
            state.targetAvatarMediaFileLoaded = true;
            checkpoint();
        }

        if (!state.ownerAvatarUploaded) {
            mediaManager.uploadPublicMedia(
                    parameters.targetNodeName,
                    generateCarte(parameters.targetNodeName, Scope.UPLOAD_PUBLIC_MEDIA),
                    parameters.sourceText.getOwnerAvatarMediaFile());
            state.ownerAvatarUploaded = true;
            checkpoint();
        }

        if (state.prevPostingInfo == null && parameters.postingId != null) {
            state.prevPostingInfo = nodeApi.getPosting(
                    parameters.targetNodeName,
                    generateCarte(parameters.targetNodeName, Scope.VIEW_CONTENT),
                    parameters.postingId);
            checkpoint();
        }

        if (state.postingText == null) {
            state.postingText = buildPosting();
            checkpoint();
        }

        if (state.postingInfo == null) {
            if (parameters.postingId == null) {
                state.postingInfo = nodeApi.postPosting(
                        parameters.targetNodeName,
                        state.postingText);
                String postingId = state.postingInfo.getId();
                send(new RemotePostingAddedLiberin(parameters.targetNodeName, postingId));
            } else {
                state.postingInfo = nodeApi.putPosting(
                        parameters.targetNodeName,
                        parameters.postingId,
                        state.postingText);
                send(new RemotePostingUpdatedLiberin(parameters.targetNodeName, parameters.postingId));
            }
            checkpoint();
        }

        savePosting();
    }

    private PostingText buildPosting() {
        PostingText postingText =
                new PostingText(nodeName(), fullName(), gender(), parameters.sourceText, textConverter);
        Map<UUID, byte[]> mediaDigests = buildMediaDigestsMap();
        cacheMediaDigests(mediaDigests);
        byte[] parentMediaDigest = state.prevPostingInfo != null && state.prevPostingInfo.getParentMediaId() != null
                ? mediaManager.getPrivateMediaDigest(
                        parameters.targetNodeName,
                        generateCarte(parameters.targetNodeName, Scope.VIEW_MEDIA),
                        state.prevPostingInfo.getParentMediaId(),
                        null)
                : null;
        byte[] fingerprint = PostingFingerprintBuilder.build(
            postingText,
            parentMediaDigest,
            id -> postingMediaDigest(id, mediaDigests)
        );
        postingText.setSignature(CryptoUtil.sign(fingerprint, (ECPrivateKey) signingKey()));
        postingText.setSignatureVersion(PostingFingerprintBuilder.LATEST_VERSION);
        return postingText;
    }

    private Map<UUID, byte[]> buildMediaDigestsMap() {
        if (parameters.sourceText.getMedia() == null) {
            return Collections.emptyMap();
        }

        return Arrays.stream(parameters.sourceText.getMedia())
                .filter(md -> md.getDigest() != null)
                .collect(Collectors.toMap(MediaWithDigest::getId, md -> Util.base64decode(md.getDigest())));
    }

    private void cacheMediaDigests(Map<UUID, byte[]> mediaDigests) {
        mediaDigests.forEach((id, digest) ->
                mediaManager.cacheUploadedRemoteMedia(parameters.targetNodeName, id.toString(), digest));
    }

    private byte[] postingMediaDigest(UUID id, Map<UUID, byte[]> mediaDigests) {
        if (mediaDigests.containsKey(id)) {
            return mediaDigests.get(id);
        }
        return mediaManager.getPrivateMediaDigest(
                parameters.targetNodeName,
                generateCarte(parameters.targetNodeName, Scope.VIEW_MEDIA),
                id.toString(),
                null);
    }

    private void savePosting() {
        if (state.postingInfo.getParentMediaId() != null) {
            return;
        }

        tx.executeWrite(
            () -> {
                OwnPosting ownPosting = ownPostingRepository
                        .findByRemotePostingId(nodeId, parameters.targetNodeName, state.postingInfo.getId())
                        .orElse(null);
                if (ownPosting == null) {
                    ownPosting = new OwnPosting();
                    ownPosting.setId(UUID.randomUUID());
                    ownPosting.setNodeId(nodeId);
                    ownPosting.setRemoteNodeName(parameters.targetNodeName);
                    ownPosting.setRemoteFullName(state.target.getFullName());
                    if (state.targetAvatarMediaFileId != null) {
                        MediaFile mediaFile = mediaFileRepository.findById(state.targetAvatarMediaFileId).orElse(null);
                        ownPosting.setRemoteAvatarMediaFile(mediaFile);
                        ownPosting.setRemoteAvatarShape(state.target.getAvatar().getShape());
                    }
                    ownPosting = ownPostingRepository.save(ownPosting);
                    contactOperations.updateCloseness(nodeId, parameters.targetNodeName, 1);
                }
                state.postingInfo.toOwnPosting(ownPosting);
            }
        );
    }

    @Override
    protected void succeeded() {
        super.succeeded();
        log.info("Succeeded to post posting to node {}", parameters.targetNodeName);
    }

    @Override
    protected void failed() {
        super.failed();

        if (state.prevPostingInfo == null) {
            send(new RemotePostingAddingFailedLiberin(state.target));
        } else {
            send(new RemotePostingUpdateFailedLiberin(state.target, parameters.postingId, state.prevPostingInfo));
        }
    }

}
