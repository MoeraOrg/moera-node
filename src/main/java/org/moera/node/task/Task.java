package org.moera.node.task;

import java.net.InetAddress;
import java.security.PrivateKey;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.Callable;
import javax.inject.Inject;

import org.moera.naming.rpc.RegisteredNameInfo;
import org.moera.node.api.NodeApi;
import org.moera.node.data.Avatar;
import org.moera.node.event.EventManager;
import org.moera.node.global.UniversalContext;
import org.moera.node.model.event.Event;
import org.moera.node.naming.NamingClient;
import org.moera.node.naming.NodeName;
import org.moera.node.naming.RegisteredName;
import org.moera.node.option.Options;
import org.moera.node.util.Carte;
import org.moera.node.util.Transaction;
import org.springframework.transaction.PlatformTransactionManager;

public abstract class Task implements Runnable {

    protected UUID nodeId;
    protected InetAddress localAddr;

    @Inject
    protected UniversalContext universalContext;

    @Inject
    protected NodeApi nodeApi;

    @Inject
    protected EventManager eventManager;

    @Inject
    private NamingClient namingClient;

    @Inject
    private PlatformTransactionManager txManager;

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
        RegisteredName registeredName = (RegisteredName) NodeName.parse(remoteNodeName);
        RegisteredNameInfo nameInfo =
                namingClient.getPast(registeredName.getName(), registeredName.getGeneration(), at, namingLocation);
        return nameInfo != null ? nameInfo.getSigningKey() : null;
    }

    protected String generateCarte(String targetNodeName) {
        return Carte.generate(nodeName(), localAddr, Instant.now(), signingKey(), targetNodeName);
    }

    protected void send(Event event) {
        universalContext.send(event);
    }

    protected <T> T inTransaction(Callable<T> inside) throws Throwable {
        return Transaction.execute(txManager, inside);
    }

    protected <T> T inTransactionQuietly(Callable<T> inside) {
        return Transaction.executeQuietly(txManager, inside);
    }

    @Override
    public final void run() {
        universalContext.associate(this);
        execute();
    }

    protected abstract void execute();

}
