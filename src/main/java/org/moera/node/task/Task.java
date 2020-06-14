package org.moera.node.task;

import java.net.InetAddress;
import java.security.PrivateKey;
import java.time.Instant;
import java.util.UUID;
import javax.inject.Inject;

import org.moera.naming.rpc.RegisteredNameInfo;
import org.moera.node.api.NodeApi;
import org.moera.node.domain.Domains;
import org.moera.node.event.EventManager;
import org.moera.node.model.event.Event;
import org.moera.node.naming.NamingClient;
import org.moera.node.naming.NodeName;
import org.moera.node.naming.RegisteredName;
import org.moera.node.util.Carte;
import org.slf4j.MDC;

public abstract class Task implements Runnable {

    protected UUID nodeId;
    protected String nodeName;
    protected PrivateKey signingKey;
    protected InetAddress localAddr;

    @Inject
    protected NodeApi nodeApi;

    @Inject
    protected EventManager eventManager;

    @Inject
    private Domains domains;

    @Inject
    private NamingClient namingClient;

    public void setNodeId(UUID nodeId) {
        this.nodeId = nodeId;
    }

    public void setNodeName(String nodeName) {
        this.nodeName = nodeName;
    }

    public void setSigningKey(PrivateKey signingKey) {
        this.signingKey = signingKey;
    }

    public void setLocalAddr(InetAddress localAddr) {
        this.localAddr = localAddr;
    }

    protected void initLoggingDomain() {
        MDC.put("domain", domains.getDomainName(nodeId));
    }

    protected byte[] fetchSigningKey(String remoteNodeName, long at) {
        String namingLocation = domains.getDomainOptions(nodeId).getString("naming.location");
        RegisteredName registeredName = (RegisteredName) NodeName.parse(remoteNodeName);
        RegisteredNameInfo nameInfo =
                namingClient.getPast(registeredName.getName(), registeredName.getGeneration(), at, namingLocation);
        return nameInfo != null ? nameInfo.getSigningKey() : null;
    }

    protected String generateCarte() {
        return Carte.generate(nodeName, localAddr, Instant.now(), signingKey);
    }

    protected void send(Event event) {
        eventManager.send(nodeId, event);
    }

}
