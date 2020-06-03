package org.moera.node.task;

import java.security.PrivateKey;
import java.util.UUID;
import javax.inject.Inject;

import org.moera.naming.rpc.RegisteredNameInfo;
import org.moera.node.domain.Domains;
import org.moera.node.event.EventManager;
import org.moera.node.model.event.Event;
import org.moera.node.naming.NamingCache;
import org.moera.node.naming.NamingClient;
import org.moera.node.naming.NodeName;
import org.moera.node.naming.RegisteredName;
import org.moera.node.naming.RegisteredNameDetails;
import org.moera.node.util.UriUtil;
import org.slf4j.MDC;

public abstract class Task implements Runnable {

    protected UUID nodeId;
    protected String nodeName;
    protected PrivateKey signingKey;

    @Inject
    private Domains domains;

    @Inject
    private NamingCache namingCache;

    @Inject
    private NamingClient namingClient;

    @Inject
    private EventManager eventManager;

    public void setNodeId(UUID nodeId) {
        this.nodeId = nodeId;
    }

    public void setNodeName(String nodeName) {
        this.nodeName = nodeName;
    }

    public void setSigningKey(PrivateKey signingKey) {
        this.signingKey = signingKey;
    }

    protected void initLoggingDomain() {
        MDC.put("domain", domains.getDomainName(nodeId));
    }

    protected String fetchNodeUri(String ownerName) {
        namingCache.setNodeId(nodeId);
        RegisteredNameDetails details = namingCache.get(ownerName);
        return details != null ? UriUtil.normalize(details.getNodeUri()) : null;
    }

    protected byte[] fetchSigningKey(String ownerName, long at) {
        String namingLocation = domains.getDomainOptions(nodeId).getString("naming.location");
        RegisteredName registeredName = (RegisteredName) NodeName.parse(ownerName);
        RegisteredNameInfo nameInfo =
                namingClient.getPast(registeredName.getName(), registeredName.getGeneration(), at, namingLocation);
        return nameInfo != null ? nameInfo.getSigningKey() : null;
    }

    protected void send(Event event) {
        eventManager.send(nodeId, event);
    }

}
