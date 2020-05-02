package org.moera.node.task;

import java.util.UUID;
import javax.inject.Inject;

import org.moera.node.domain.Domains;
import org.slf4j.MDC;

public abstract class Task implements Runnable {

    protected UUID nodeId;
    protected String nodeName;

    @Inject
    private Domains domains;

    public void setNodeId(UUID nodeId) {
        this.nodeId = nodeId;
    }

    public void setNodeName(String nodeName) {
        this.nodeName = nodeName;
    }

    protected void initLoggingDomain() {
        MDC.put("domain", domains.getDomainName(nodeId));
    }

}
