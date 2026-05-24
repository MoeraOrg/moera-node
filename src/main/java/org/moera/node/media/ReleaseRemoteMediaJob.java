package org.moera.node.media;

import jakarta.inject.Inject;

import org.moera.lib.node.exception.MoeraNodeApiNotFoundException;
import org.moera.lib.node.types.Scope;
import org.moera.lib.util.LogUtil;
import org.moera.node.data.RemoteMediaFileRepository;
import org.moera.node.task.Job;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tools.jackson.databind.ObjectMapper;

public class ReleaseRemoteMediaJob extends Job<ReleaseRemoteMediaJob.Parameters, Object> {

    public static class Parameters {

        private String remoteNodeName;
        private String leaseId;

        public Parameters() {
        }

        public Parameters(String remoteNodeName, String leaseId) {
            this.remoteNodeName = remoteNodeName;
            this.leaseId = leaseId;
        }

        public String getRemoteNodeName() {
            return remoteNodeName;
        }

        public void setRemoteNodeName(String remoteNodeName) {
            this.remoteNodeName = remoteNodeName;
        }

        public String getLeaseId() {
            return leaseId;
        }

        public void setLeaseId(String leaseId) {
            this.leaseId = leaseId;
        }

    }

    private static final Logger log = LoggerFactory.getLogger(ReleaseRemoteMediaJob.class);

    private boolean released;

    @Inject
    private RemoteMediaFileRepository remoteMediaFileRepository;

    public ReleaseRemoteMediaJob() {
        exponentialRetry("PT10M", "P7D");
    }

    @Override
    protected void setParameters(String parameters, ObjectMapper objectMapper) {
        this.parameters = objectMapper.readValue(parameters, ReleaseRemoteMediaJob.Parameters.class);
    }

    @Override
    protected void setState(String state, ObjectMapper objectMapper) {
        this.state = null;
    }

    @Override
    protected void started() {
        super.started();
        log.info(
            "Releasing remote media lease {} on node {}",
            LogUtil.format(parameters.leaseId),
            LogUtil.format(parameters.remoteNodeName)
        );
    }

    @Override
    protected void execute() throws Exception {
        int used = tx.executeRead(() ->
            remoteMediaFileRepository.countUsedByNodeIdAndLeaseId(nodeId, parameters.remoteNodeName, parameters.leaseId)
        );
        if (used > 0) {
            log.info(
                "Remote media lease {} is still used on node {}, skipping release",
                LogUtil.format(parameters.leaseId),
                LogUtil.format(nodeName())
            );
            success();
        }

        try {
            nodeApi.at(
                parameters.remoteNodeName,
                generateCarte(parameters.remoteNodeName, Scope.LEASE_MEDIA)
            ).deleteMediaLease(parameters.leaseId);
            released = true;
        } catch (MoeraNodeApiNotFoundException e) {
            log.info(
                "Remote media lease {} on node {} is already released",
                LogUtil.format(parameters.leaseId),
                LogUtil.format(parameters.remoteNodeName)
            );
            released = true;
            success();
        }
    }

    @Override
    protected void succeeded() {
        super.succeeded();
        if (released) {
            log.info(
                "Released remote media lease {} on node {}",
                LogUtil.format(parameters.leaseId),
                LogUtil.format(parameters.remoteNodeName)
            );
        }
    }

}
