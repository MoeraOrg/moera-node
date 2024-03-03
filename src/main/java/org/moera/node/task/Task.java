package org.moera.node.task;

import java.net.InetAddress;
import java.security.PrivateKey;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.Callable;
import javax.inject.Inject;

import org.moera.naming.rpc.NodeName;
import org.moera.naming.rpc.RegisteredName;
import org.moera.naming.rpc.RegisteredNameInfo;
import org.moera.node.api.node.NodeApi;
import org.moera.node.auth.AuthCategory;
import org.moera.node.data.Avatar;
import org.moera.node.global.UniversalContext;
import org.moera.node.liberin.Liberin;
import org.moera.node.api.naming.NamingClient;
import org.moera.node.option.Options;
import org.moera.node.util.Carte;
import org.moera.node.util.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.PlatformTransactionManager;

public abstract class Task implements Runnable {

    private static final Logger log = LoggerFactory.getLogger(Task.class);

    protected UUID nodeId;
    protected InetAddress localAddr;

    @Inject
    protected UniversalContext universalContext;

    @Inject
    protected NodeApi nodeApi;

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
        RegisteredName registeredName = (RegisteredName) NodeName.parse(remoteNodeName);
        RegisteredNameInfo nameInfo =
                namingClient.getPast(registeredName.getName(), registeredName.getGeneration(), at, namingLocation);
        return nameInfo != null ? nameInfo.getSigningKey() : null;
    }

    protected String generateCarte(String targetNodeName) {
        try {
            return Carte.generate(nodeName(), localAddr, Instant.now(), signingKey(), targetNodeName, AuthCategory.ALL);
        } catch (Exception e) {
            log.info("Error generating carte by {} {}", nodeId, universalContext.nodeId());
            throw e;
        }
    }

    protected void send(Liberin liberin) {
        universalContext.send(liberin);
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
