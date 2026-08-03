package org.moera.node.rest.task;

import java.security.interfaces.ECPrivateKey;
import java.util.UUID;
import jakarta.inject.Inject;

import org.moera.lib.crypto.CryptoUtil;
import org.moera.lib.node.exception.MoeraNodeException;
import org.moera.lib.node.types.PostingInfo;
import org.moera.lib.node.types.PostingSourceText;
import org.moera.lib.node.types.PostingText;
import org.moera.lib.node.types.Scope;
import org.moera.lib.node.types.WhoAmI;
import org.moera.node.data.FavorType;
import org.moera.node.data.MediaFile;
import org.moera.node.data.MediaFileRepository;
import org.moera.node.data.OwnPosting;
import org.moera.node.data.OwnPostingRepository;
import org.moera.node.fingerprint.PostingFingerprintBuilder;
import org.moera.node.liberin.model.RemotePostingAddedLiberin;
import org.moera.node.liberin.model.RemotePostingAddingFailedLiberin;
import org.moera.node.liberin.model.RemotePostingUpdateFailedLiberin;
import org.moera.node.liberin.model.RemotePostingUpdatedLiberin;
import org.moera.node.media.MediaOperations;
import org.moera.node.model.AvatarDescriptionUtil;
import org.moera.node.model.PostingInfoUtil;
import org.moera.node.model.PostingSourceTextUtil;
import org.moera.node.model.PostingTextUtil;
import org.moera.node.operations.FavorOperations;
import org.moera.node.text.TextConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tools.jackson.databind.ObjectMapper;

public class RemotePostingPostJob
    extends RemoteEntryPostJob<RemotePostingPostJob.Parameters, RemotePostingPostJob.State> {

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

        private PostingSourceText sourceText;
        private boolean uncompressedMediaChecked;
        private boolean mediaCompressionWaited;
        private WhoAmI target;
        private String targetAvatarMediaFileId;
        private boolean targetAvatarMediaFileLoaded;
        private boolean ownerAvatarUploaded;
        private PostingInfo prevPostingInfo;
        private boolean uploadedRemoteMediaCached;
        private PostingText postingText;
        private PostingInfo postingInfo;

        public State() {
        }

        public PostingSourceText getSourceText() {
            return sourceText;
        }

        public void setSourceText(PostingSourceText sourceText) {
            this.sourceText = sourceText;
        }

        public boolean isUncompressedMediaChecked() {
            return uncompressedMediaChecked;
        }

        public void setUncompressedMediaChecked(boolean uncompressedMediaChecked) {
            this.uncompressedMediaChecked = uncompressedMediaChecked;
        }

        public boolean isMediaCompressionWaited() {
            return mediaCompressionWaited;
        }

        public void setMediaCompressionWaited(boolean mediaCompressionWaited) {
            this.mediaCompressionWaited = mediaCompressionWaited;
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

        public boolean isUploadedRemoteMediaCached() {
            return uploadedRemoteMediaCached;
        }

        public void setUploadedRemoteMediaCached(boolean uploadedRemoteMediaCached) {
            this.uploadedRemoteMediaCached = uploadedRemoteMediaCached;
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
    private FavorOperations favorOperations;

    @Inject
    private MediaOperations mediaOperations;

    @Inject
    private ObjectMapper objectMapper;

    public RemotePostingPostJob() {
        state = new State();
        exponentialRetry("PT10S", "PT12H");
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
    protected void started() {
        super.started();
        if (parameters.postingId == null) {
            log.info("Adding posting to node {}", parameters.targetNodeName);
        } else {
            log.info("Updating posting {} on node {}", parameters.postingId, parameters.targetNodeName);
        }
    }

    @Override
    protected void execute() throws MoeraNodeException {
        if (state.sourceText == null) {
            state.sourceText = objectMapper.convertValue(parameters.sourceText, PostingSourceText.class);
            checkpoint();
        }

        if (!state.uncompressedMediaChecked) {
            awaitMediaCompression();
            state.uncompressedMediaChecked = true;
            checkpoint();
        }

        if (state.target == null) {
            state.target = nodeApi.at(parameters.targetNodeName).whoAmI();
            checkpoint();
        }

        if (!state.targetAvatarMediaFileLoaded) {
            MediaFile mediaFile = mediaManager.downloadPublicMedia(
                parameters.targetNodeName,
                state.target.getAvatar()
            );
            state.targetAvatarMediaFileId = mediaFile != null ? mediaFile.getId() : null;
            state.targetAvatarMediaFileLoaded = true;
            checkpoint();
        }

        if (!state.ownerAvatarUploaded) {
            mediaManager.uploadPublicMedia(
                parameters.targetNodeName,
                generateCarte(parameters.targetNodeName, Scope.UPLOAD_PUBLIC_MEDIA),
                AvatarDescriptionUtil.getMediaFile(state.sourceText.getOwnerAvatar())
            );
            state.ownerAvatarUploaded = true;
            checkpoint();
        }

        if (state.prevPostingInfo == null && parameters.postingId != null) {
            state.prevPostingInfo = nodeApi
                .at(parameters.targetNodeName, generateCarte(parameters.targetNodeName, Scope.VIEW_CONTENT))
                .getPosting(parameters.postingId, false);
            checkpoint();
        }

        if (!state.uploadedRemoteMediaCached) {
            cacheUploadedRemoteMedia(state.sourceText.getMedia());
            state.uploadedRemoteMediaCached = true;
            checkpoint();
        }

        if (state.postingText == null) {
            state.postingText = buildPosting();
            checkpoint();
        }

        if (state.postingInfo == null) {
            if (parameters.postingId == null) {
                state.postingInfo = nodeApi.at(parameters.targetNodeName).createPosting(state.postingText);
                send(new RemotePostingAddedLiberin(
                    state.target,
                    state.postingInfo,
                    state.mediaCompressionWaited
                ));
            } else {
                state.postingInfo = nodeApi
                    .at(parameters.targetNodeName)
                    .updatePosting(parameters.postingId, state.postingText);
                send(new RemotePostingUpdatedLiberin(parameters.targetNodeName, parameters.postingId));
            }
            checkpoint();
        }

        updateCaptions();
        savePosting();
        mediaOperations.clearDraftOnlyMediaLeases(state.sourceText.getMedia());
    }

    private void awaitMediaCompression() throws MoeraNodeException {
        awaitMediaCompression(parameters.targetNodeName, state.sourceText.getBodySrc(), state.sourceText.getMedia());
    }

    @Override
    protected void mediaCompressionWaiting() {
        if (!state.mediaCompressionWaited) {
            state.mediaCompressionWaited = true;
            checkpoint();
        }
    }

    private PostingText buildPosting() throws MoeraNodeException {
        PostingText postingText = PostingTextUtil.build(
            nodeName(), fullName(), gender(), state.sourceText, textConverter
        );
        byte[] parentMediaDigest = state.prevPostingInfo != null
            ? mediaManager.getParentMediaDigest(
                state.prevPostingInfo,
                parameters.targetNodeName,
                carteGenerator(Scope.VIEW_CONTENT)
            )
            : null;
        byte[] fingerprint = PostingFingerprintBuilder.build(
            postingText,
            parentMediaDigest,
            mediaManager::getTrustedPrivateMediaDigest
        );
        postingText.setSignature(CryptoUtil.sign(fingerprint, (ECPrivateKey) signingKey()));
        postingText.setSignatureVersion(PostingFingerprintBuilder.LATEST_VERSION);
        return postingText;
    }

    private void updateCaptions() {
        updateCaptions(
            parameters.targetNodeName,
            state.postingInfo.getMedia(),
            state.sourceText.getMediaCaptions(),
            caption -> PostingSourceTextUtil.build(state.sourceText, caption)
        );
    }

    private void savePosting() {
        if (state.postingInfo.getParentMedia() != null) {
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
                    if (state.postingInfo.getParentMedia() == null) {
                        favorOperations.addFavor(nodeId, parameters.targetNodeName, FavorType.POST);
                    }
                }
                PostingInfoUtil.toOwnPosting(state.postingInfo, ownPosting);
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
