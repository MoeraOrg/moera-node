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
import org.moera.node.data.AvatarRepository;
import org.moera.node.domain.Domains;
import org.moera.node.event.EventManager;
import org.moera.node.model.event.Event;
import org.moera.node.naming.NamingClient;
import org.moera.node.naming.NodeName;
import org.moera.node.naming.RegisteredName;
import org.moera.node.option.Options;
import org.moera.node.util.Carte;
import org.moera.node.util.Transaction;
import org.slf4j.MDC;
import org.springframework.transaction.PlatformTransactionManager;

public abstract class Task implements Runnable {

    protected UUID nodeId;
    protected Options options;
    protected InetAddress localAddr;

    @Inject
    protected NodeApi nodeApi;

    @Inject
    protected EventManager eventManager;

    private Avatar avatar;

    @Inject
    private Domains domains;

    @Inject
    private NamingClient namingClient;

    @Inject
    private PlatformTransactionManager txManager;

    @Inject
    private AvatarRepository avatarRepository;

    public UUID getNodeId() {
        return nodeId;
    }

    public void setNodeId(UUID nodeId) {
        this.nodeId = nodeId;
    }

    public Options getOptions() {
        return options;
    }

    public void setOptions(Options options) {
        this.options = options;
    }

    public String nodeName() {
        return options != null ? options.nodeName() : null;
    }

    public String fullName() {
        return options != null ? options.getString("profile.full-name") : null;
    }

    public UUID avatarId() {
        return options != null ? options.getUuid("profile.avatar.id") : null;
    }

    public Avatar getAvatar() {
        if (nodeId == null || avatarId() == null) {
            return null;
        }
        if (avatar == null || !avatar.getId().equals(avatarId())) {
            avatar = avatarRepository.findByNodeIdAndId(nodeId, avatarId()).orElse(null);
        }
        return avatar;
    }

    public PrivateKey signingKey() {
        return options != null ? options.getPrivateKey("profile.signing-key") : null;
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

    protected String generateCarte(String targetNodeName) {
        return Carte.generate(nodeName(), localAddr, Instant.now(), signingKey(), targetNodeName);
    }

    protected void send(Event event) {
        eventManager.send(nodeId, event);
    }

    protected <T> T inTransaction(Callable<T> inside) throws Throwable {
        return Transaction.execute(txManager, inside);
    }

    protected <T> T inTransactionQuietly(Callable<T> inside) {
        return Transaction.executeQuietly(txManager, inside);
    }

}
