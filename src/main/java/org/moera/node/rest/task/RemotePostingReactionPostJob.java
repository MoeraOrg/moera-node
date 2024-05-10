package org.moera.node.rest.task;

import java.security.interfaces.ECPrivateKey;
import java.util.UUID;
import javax.inject.Inject;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.moera.commons.crypto.CryptoUtil;
import org.moera.node.data.MediaFile;
import org.moera.node.data.OwnReaction;
import org.moera.node.data.OwnReactionRepository;
import org.moera.node.fingerprint.Fingerprints;
import org.moera.node.fingerprint.ReactionFingerprint;
import org.moera.node.liberin.model.RemotePostingReactionAddedLiberin;
import org.moera.node.liberin.model.RemotePostingReactionAddingFailedLiberin;
import org.moera.node.media.MediaManager;
import org.moera.node.model.PostingInfo;
import org.moera.node.model.ReactionAttributes;
import org.moera.node.model.ReactionCreated;
import org.moera.node.model.ReactionDescription;
import org.moera.node.model.ReactionInfo;
import org.moera.node.model.WhoAmI;
import org.moera.node.operations.ContactOperations;
import org.moera.node.task.Job;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RemotePostingReactionPostJob
        extends Job<RemotePostingReactionPostJob.Parameters, RemotePostingReactionPostJob.State> {

    public static class Parameters {

        private String targetNodeName;
        private String postingId;
        private ReactionAttributes attributes;

        public Parameters() {
        }

        public Parameters(String targetNodeName, String postingId, ReactionAttributes attributes) {
            this.targetNodeName = targetNodeName;
            this.postingId = postingId;
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

        public ReactionAttributes getAttributes() {
            return attributes;
        }

        public void setAttributes(ReactionAttributes attributes) {
            this.attributes = attributes;
        }

    }

    public static class State {

        private WhoAmI target;
        private MediaFile targetAvatarMediaFile;
        private boolean targetAvatarMediaFileLoaded;
        private boolean ownerAvatarUploaded;
        private PostingInfo postingInfo;
        private ReactionDescription reactionDescription;
        private ReactionInfo reactionInfo;

        public State() {
        }

        public WhoAmI getTarget() {
            return target;
        }

        public void setTarget(WhoAmI target) {
            this.target = target;
        }

        public MediaFile getTargetAvatarMediaFile() {
            return targetAvatarMediaFile;
        }

        public void setTargetAvatarMediaFile(MediaFile targetAvatarMediaFile) {
            this.targetAvatarMediaFile = targetAvatarMediaFile;
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

        public PostingInfo getPostingInfo() {
            return postingInfo;
        }

        public void setPostingInfo(PostingInfo postingInfo) {
            this.postingInfo = postingInfo;
        }

        public ReactionDescription getReactionDescription() {
            return reactionDescription;
        }

        public void setReactionDescription(ReactionDescription reactionDescription) {
            this.reactionDescription = reactionDescription;
        }

        public ReactionInfo getReactionInfo() {
            return reactionInfo;
        }

        public void setReactionInfo(ReactionInfo reactionInfo) {
            this.reactionInfo = reactionInfo;
        }

    }

    private static final Logger log = LoggerFactory.getLogger(RemotePostingReactionPostJob.class);

    @Inject
    private OwnReactionRepository ownReactionRepository;

    @Inject
    private ContactOperations contactOperations;

    @Inject
    private MediaManager mediaManager;

    public RemotePostingReactionPostJob() {
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
        log.info("Adding reaction to posting {} at node {}", parameters.postingId, parameters.targetNodeName);
    }

    @Override
    protected void execute() throws Exception {
        if (state.target == null) {
            state.target = nodeApi.whoAmI(parameters.targetNodeName);
            checkpoint();
        }

        if (!state.targetAvatarMediaFileLoaded) {
            state.targetAvatarMediaFile = mediaManager.downloadPublicMedia(
                    parameters.targetNodeName,
                    state.target.getAvatar());
            state.targetAvatarMediaFileLoaded = true;
            checkpoint();
        }

        if (!state.ownerAvatarUploaded) {
            mediaManager.uploadPublicMedia(
                    parameters.targetNodeName,
                    generateCarte(parameters.targetNodeName),
                    getAvatar());
            state.ownerAvatarUploaded = true;
            checkpoint();
        }

        if (state.postingInfo == null) {
            state.postingInfo = nodeApi.getPosting(
                    parameters.targetNodeName,
                    generateCarte(parameters.targetNodeName),
                    parameters.postingId);
            checkpoint();
        }

        if (state.postingInfo.getOwnerAvatar() != null && state.postingInfo.getOwnerAvatar().getMediaFile() == null) {
            MediaFile mediaFile = mediaManager.downloadPublicMedia(
                    parameters.targetNodeName,
                    state.postingInfo.getOwnerAvatar());
            state.postingInfo.getOwnerAvatar().setMediaFile(mediaFile);
            checkpoint();
        }

        if (state.reactionDescription == null) {
            state.reactionDescription = buildReaction();
            checkpoint();
        }

        if (state.reactionInfo == null) {
            ReactionCreated created = nodeApi.postPostingReaction(
                    parameters.targetNodeName,
                    parameters.postingId,
                    state.reactionDescription);
            state.reactionInfo = created.getReaction();
            checkpoint();
        }

        saveReaction();
    }

    private ReactionDescription buildReaction() {
        byte[] parentMediaDigest = state.postingInfo.getParentMediaId() != null
                ? mediaManager.getPrivateMediaDigest(
                        parameters.targetNodeName,
                        generateCarte(parameters.targetNodeName),
                        state.postingInfo.getParentMediaId(),
                        null)
                : null;
        ReactionFingerprint fingerprint = new ReactionFingerprint(
                nodeName(),
                parameters.attributes,
                Fingerprints.posting(state.postingInfo.getSignatureVersion()).create(
                        state.postingInfo,
                        parentMediaDigest,
                        pmf -> mediaManager.getPrivateMediaDigest(
                                parameters.targetNodeName,
                                generateCarte(parameters.targetNodeName),
                                pmf)));
        ReactionDescription description = new ReactionDescription(
                nodeName(), fullName(), gender(), getAvatar(), parameters.attributes);
        description.setSignature(CryptoUtil.sign(fingerprint, (ECPrivateKey) signingKey()));
        description.setSignatureVersion(ReactionFingerprint.VERSION);
        return description;
    }

    private void saveReaction() throws Exception {
        tx.executeWrite(
            () -> {
                OwnReaction ownReaction = ownReactionRepository
                        .findByRemotePostingId(nodeId, parameters.targetNodeName, parameters.postingId)
                        .orElse(null);
                if (ownReaction == null) {
                    ownReaction = new OwnReaction();
                    ownReaction.setId(UUID.randomUUID());
                    ownReaction.setNodeId(nodeId);
                    ownReaction.setRemoteNodeName(parameters.targetNodeName);
                    ownReaction.setRemoteFullName(state.target.getFullName());
                    if (state.targetAvatarMediaFile != null) {
                        ownReaction.setRemoteAvatarMediaFile(state.targetAvatarMediaFile);
                        ownReaction.setRemoteAvatarShape(state.target.getAvatar().getShape());
                    }
                    ownReaction = ownReactionRepository.save(ownReaction);
                    contactOperations.updateCloseness(nodeId, parameters.targetNodeName, 0.25f);
                }
                state.reactionInfo.toOwnReaction(ownReaction);
                ownReaction.setPostingHeading(state.postingInfo.getHeading());
            }
        );
        send(new RemotePostingReactionAddedLiberin(parameters.targetNodeName, parameters.postingId, state.reactionInfo));
    }

    @Override
    protected void succeeded() {
        super.succeeded();
        log.info("Succeeded to post reaction to posting {} at node {}",
                state.reactionInfo.getPostingId(), parameters.targetNodeName);
    }

    @Override
    protected void failed() {
        super.failed();
        if (state.postingInfo.getParentMediaId() == null) {
            send(new RemotePostingReactionAddingFailedLiberin(
                    parameters.targetNodeName, parameters.postingId, state.postingInfo));
        } else {
            jobs.run(
                    RemoteMediaReactionFailedJob.class,
                    new RemoteMediaReactionFailedJob.Parameters(
                            parameters.targetNodeName,
                            state.postingInfo.getParentMediaId(),
                            parameters.postingId),
                    nodeId);
        }
    }

}
