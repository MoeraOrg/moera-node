package org.moera.node.task;

import java.net.InetAddress;
import java.security.PrivateKey;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.Callable;
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
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.DefaultTransactionDefinition;

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

    @Inject
    private PlatformTransactionManager txManager;

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

    protected <T> T inTransaction(Callable<T> inside) throws Throwable {
        TransactionStatus status = beginTransaction();
        T result;
        try {
            result = inside.call();
            commitTransaction(status);
        } catch (Throwable e) {
            rollbackTransaction(status);
            throw e;
        }
        return result;
    }

    private TransactionStatus beginTransaction() {
        return txManager != null ? txManager.getTransaction(new DefaultTransactionDefinition()) : null;
    }

    private void commitTransaction(TransactionStatus status) {
        if (status != null) {
            txManager.commit(status);
        }
    }

    private void rollbackTransaction(TransactionStatus status) {
        if (status != null) {
            txManager.rollback(status);
        }
    }

    protected abstract void error(Throwable e);

}
