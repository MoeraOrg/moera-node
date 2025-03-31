package org.moera.node.rest.notification;

import jakarta.inject.Inject;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.moera.lib.node.types.WhoAmI;
import org.moera.node.data.MediaFile;
import org.moera.node.liberin.model.RemoteNodeAvatarChangedLiberin;
import org.moera.node.liberin.model.RemoteNodeFullNameChangedLiberin;
import org.moera.node.media.MediaManager;
import org.moera.node.model.AvatarImageUtil;
import org.moera.node.operations.ContactOperations;
import org.moera.node.task.Job;

public class ProfileUpdateJob extends Job<ProfileUpdateJob.Parameters, ProfileUpdateJob.State> {

    public static class Parameters {

        private String nodeName;

        public Parameters() {
        }

        public Parameters(String nodeName) {
            this.nodeName = nodeName;
        }

        public String getNodeName() {
            return nodeName;
        }

        public void setNodeName(String nodeName) {
            this.nodeName = nodeName;
        }

    }

    public static class State {

        private WhoAmI whoAmI;
        private boolean detailsUpdated;

        public State() {
        }

        public WhoAmI getWhoAmI() {
            return whoAmI;
        }

        public void setWhoAmI(WhoAmI whoAmI) {
            this.whoAmI = whoAmI;
        }

        public boolean isDetailsUpdated() {
            return detailsUpdated;
        }

        public void setDetailsUpdated(boolean detailsUpdated) {
            this.detailsUpdated = detailsUpdated;
        }

    }

    @Inject
    private ContactOperations contactOperations;

    @Inject
    private MediaManager mediaManager;

    public ProfileUpdateJob() {
        state = new State();
        retryCount(3, "PT5M");
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
    protected void execute() throws Exception {
        if (state.whoAmI == null) {
            state.whoAmI = nodeApi.at(parameters.nodeName).whoAmI();
            checkpoint();
        }

        if (!state.detailsUpdated) {
            contactOperations.updateDetails(
                    parameters.nodeName,
                    state.whoAmI.getFullName(),
                    state.whoAmI.getGender(),
                    () -> universalContext.send(
                        new RemoteNodeFullNameChangedLiberin(parameters.nodeName, state.whoAmI.getFullName())
                    )
            );
            state.detailsUpdated = true;
            checkpoint();
        }

        tx.executeWriteWithExceptions(() -> mediaManager.downloadAvatar(parameters.nodeName, state.whoAmI.getAvatar()));
        if (state.whoAmI.getAvatar() != null && AvatarImageUtil.getMediaFile(state.whoAmI.getAvatar()) != null) {
            MediaFile mediaFile = AvatarImageUtil.getMediaFile(state.whoAmI.getAvatar());
            String shape = state.whoAmI.getAvatar().getShape();
            contactOperations.updateAvatar(
                    parameters.nodeName,
                    mediaFile,
                    shape,
                    () -> universalContext.send(
                        new RemoteNodeAvatarChangedLiberin(parameters.nodeName, AvatarImageUtil.build(mediaFile, shape))
                    )
            );
        }
    }

}
