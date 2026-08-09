package org.moera.node.operations;

import jakarta.inject.Inject;

import org.moera.lib.util.LogUtil;
import org.moera.node.data.FavorType;
import org.moera.node.task.Job;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tools.jackson.databind.ObjectMapper;

public class AddFavorJob extends Job<AddFavorJob.Parameters, Object> {

    public record Parameters(
        String remoteNodeName,
        FavorType favorType
    ) {
    }

    private static final Logger log = LoggerFactory.getLogger(AddFavorJob.class);

    @Inject
    private FavorOperations favorOperations;

    public AddFavorJob() {
    }

    @Override
    protected void setParameters(String parameters, ObjectMapper objectMapper) {
        this.parameters = objectMapper.readValue(parameters, Parameters.class);
    }

    @Override
    protected void setState(String state, ObjectMapper objectMapper) {
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
