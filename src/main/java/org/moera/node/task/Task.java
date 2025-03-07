package org.moera.node.task;

import java.net.InetAddress;
import java.security.PrivateKey;
import java.time.Instant;
import java.util.UUID;
import jakarta.inject.Inject;

import org.moera.lib.naming.NodeName;
import org.moera.lib.naming.types.RegisteredNameInfo;
import org.moera.lib.node.carte.Carte;
import org.moera.lib.node.types.Scope;
import org.moera.node.api.naming.NamingClient;
import org.moera.node.api.node.NodeApi;
import org.moera.node.api.node.MoeraNodeUnknownNameException;
import org.moera.node.data.Avatar;
import org.moera.node.global.RequestCounter;
import org.moera.node.global.UniversalContext;
import org.moera.node.liberin.Liberin;
import org.moera.node.option.Options;
import org.moera.node.util.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class Task implements Runnable {

    private static final Logger log = LoggerFactory.getLogger(Task.class);

    protected UUID nodeId;
    protected InetAddress localAddr;

    @Inject
    protected UniversalContext universalContext;

    @Inject
    protected NodeApi nodeApi;

    @Inject
    protected Transaction tx;

    @Inject
    private NamingClient namingClient;

    @Inject
    private RequestCounter requestCounter;

    public UUID getNodeId() {
        return nodeId;
    }

    public void setNodeId(UUID nodeId) {
        this.nodeId = nodeId;
    }

    protected Options getOptions() {
        return universalContext.getOptions();
    }

    protected String nodeName() {
        return universalContext.nodeName();
    }

    protected String fullName() {
        return universalContext.fullName();
    }

    protected String gender() {
        return universalContext.gender();
    }

    protected Avatar getAvatar() {
        return universalContext.getAvatar();
    }

    protected PrivateKey signingKey() {
        return universalContext.getOptions().getPrivateKey("profile.signing-key");
    }

    public void setLocalAddr(InetAddress localAddr) {
        this.localAddr = localAddr;
    }

    protected byte[] fetchSigningKey(String remoteNodeName, long at) {
        String namingLocation = getOptions().getString("naming.location");
        NodeName registeredName = NodeName.parse(remoteNodeName);
        RegisteredNameInfo nameInfo =
            namingClient.getPast(registeredName.getName(), registeredName.getGeneration(), at, namingLocation);
        return nameInfo != null ? nameInfo.getSigningKey() : null;
    }

    protected String generateCarte(String targetNodeName, Scope clientScope) {
        return generateCarte(targetNodeName, clientScope, Scope.NONE);
    }

    protected String generateCarte(String targetNodeName, Scope clientScope, Scope adminScope) {
        try {
            return Carte.generate(
                nodeName(), localAddr, Instant.now(), signingKey(), targetNodeName, clientScope.getMask(),
                adminScope.getMask()
            );
        } catch (Exception e) {
            log.info("Error generating carte by {} {}", nodeId, universalContext.nodeId());
            throw e;
        }
    }

    protected void send(Liberin liberin) {
        universalContext.send(liberin);
    }

    @Override
    public final void run() {
        universalContext.associate(this);
        requestCounter.allot();
        started();

        boolean exceptionThrown = false;
        try {
            execute();
        } catch (Throwable e) {
            handleException(e);
            exceptionThrown = true;
        } finally {
            if (!exceptionThrown) {
                succeeded();
            }
            requestCounter.free();
        }
    }

    protected void started() {
        log.info("Executing task {}", this.getClass().getSimpleName());
    }

    protected abstract void execute() throws Exception;

    protected void handleException(Throwable e) {
        unhandledException(e);
    }

    protected void unhandledException(Throwable e) {
        if (e instanceof MoeraNodeUnknownNameException ex) {
            log.error("Cannot find a node {}", ex.getNodeName());
        } else {
            log.error("Error executing task {}: {}", this.getClass().getSimpleName(), e.getMessage());
        }
        failed();
    }

    protected void succeeded() {
    }

    protected void failed() {
    }

}
