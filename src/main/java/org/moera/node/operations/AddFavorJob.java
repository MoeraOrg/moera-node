package org.moera.node.operations;

import jakarta.inject.Inject;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.moera.lib.util.LogUtil;
import org.moera.node.task.Job;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AddFavorJob extends Job<AddFavorJob.Parameters, Object> {

    private static final Logger log = LoggerFactory.getLogger(AddFavorJob.class);

    public static class Parameters {

        private String remoteNodeName;
        private FavorType favorType;

        public Parameters() {
        }

        public Parameters(String remoteNodeName, FavorType favorType) {
            this.remoteNodeName = remoteNodeName;
            this.favorType = favorType;
        }

        public String getRemoteNodeName() {
            return remoteNodeName;
        }

        public void setRemoteNodeName(String remoteNodeName) {
            this.remoteNodeName = remoteNodeName;
        }

        public FavorType getFavorType() {
            return favorType;
        }

        public void setFavorType(FavorType favorType) {
            this.favorType = favorType;
        }

    }

    @Inject
    private FavorOperations favorOperations;

    public AddFavorJob() {
    }

    @Override
    protected void setParameters(String parameters, ObjectMapper objectMapper) throws JsonProcessingException {
        this.parameters = objectMapper.readValue(parameters, Parameters.class);
    }

    @Override
    protected void setState(String state, ObjectMapper objectMapper) throws JsonProcessingException {
        this.state = null;
    }

    @Override
    protected void started() {
        super.started();
        log.debug(
            "Adding favor {} to node {}",
            LogUtil.format(parameters.favorType.name()), LogUtil.format(parameters.remoteNodeName)
        );
    }

    @Override
    protected void execute() {
        favorOperations.addFavor(nodeId, parameters.remoteNodeName, parameters.favorType);
    }

}
