package org.moera.node.operations;

import javax.inject.Inject;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.moera.lib.util.LogUtil;
import org.moera.node.task.Job;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UpdateClosenessJob extends Job<UpdateClosenessJob.Parameters, Object> {

    private static final Logger log = LoggerFactory.getLogger(UpdateClosenessJob.class);

    public static class Parameters {

        private String remoteNodeName;
        private float delta;

        public Parameters() {
        }

        public Parameters(String remoteNodeName, float delta) {
            this.remoteNodeName = remoteNodeName;
            this.delta = delta;
        }

        public String getRemoteNodeName() {
            return remoteNodeName;
        }

        public void setRemoteNodeName(String remoteNodeName) {
            this.remoteNodeName = remoteNodeName;
        }

        public float getDelta() {
            return delta;
        }

        public void setDelta(float delta) {
            this.delta = delta;
        }

    }

    @Inject
    private ContactOperations contactOperations;

    public UpdateClosenessJob() {
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
        log.debug("Updating closeness of {} ({})", LogUtil.format(parameters.remoteNodeName), parameters.delta);
    }

    @Override
    protected void execute() {
        contactOperations.updateCloseness(nodeId, parameters.remoteNodeName, parameters.delta);
    }

}
