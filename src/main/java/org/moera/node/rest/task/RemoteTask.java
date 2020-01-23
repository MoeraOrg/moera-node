package org.moera.node.rest.task;

import java.util.UUID;

import javax.inject.Inject;

import org.moera.node.domain.Domains;
import org.moera.node.naming.NamingCache;
import org.moera.node.naming.RegisteredNameDetails;
import org.moera.node.util.UriUtil;
import org.slf4j.MDC;

public class RemoteTask {

    protected UUID nodeId;

    @Inject
    protected Domains domains;

    @Inject
    protected NamingCache namingCache;

    public RemoteTask(UUID nodeId) {
        this.nodeId = nodeId;
    }

    protected void initLoggingDomain() {
        MDC.put("domain", domains.getDomainName(nodeId));
    }

    protected String fetchNodeUri(String nodeName) {
        namingCache.setNodeId(nodeId);
        RegisteredNameDetails details = namingCache.get(nodeName);
        return details != null ? UriUtil.normalize(details.getNodeUri()) : null;
    }

}
