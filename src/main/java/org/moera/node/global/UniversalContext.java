package org.moera.node.global;

import java.util.Objects;
import java.util.UUID;
import javax.inject.Inject;

import org.moera.node.auth.Scope;
import org.moera.node.auth.principal.PrincipalFilter;
import org.moera.node.data.Avatar;
import org.moera.node.data.AvatarRepository;
import org.moera.node.domain.Domains;
import org.moera.node.friends.FriendCache;
import org.moera.node.friends.SubscribedCache;
import org.moera.node.liberin.Liberin;
import org.moera.node.liberin.LiberinManager;
import org.moera.node.option.Options;
import org.moera.node.subscriptions.SubscriptionManager;
import org.moera.node.task.Task;
import org.slf4j.MDC;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;

@Component
public class UniversalContext {

    private final ThreadLocal<UUID> nodeId = new ThreadLocal<>();
    private final ThreadLocal<Avatar> avatar = new ThreadLocal<>();

    private final ThreadLocal<Boolean> admin = ThreadLocal.withInitial(() -> false);
    private final ThreadLocal<Boolean> subscribedToClient = ThreadLocal.withInitial(() -> false);
    private final ThreadLocal<String[]> friendGroups = new ThreadLocal<>();
    private final ThreadLocal<String> clientName = new ThreadLocal<>();

    @Inject
    private RequestContext requestContext;

    @Inject
    private Domains domains;

    @Inject
    @Lazy
    private LiberinManager liberinManager;

    @Inject
    @Lazy
    private SubscriptionManager subscriptionManager;

    @Inject
    @Lazy
    private FriendCache friendCache;

    @Inject
    @Lazy
    private SubscribedCache subscribedCache;

    @Inject
    private AvatarRepository avatarRepository;

    public boolean isBackground() {
        return RequestContextHolder.getRequestAttributes() == null;
    }

    public UUID nodeId() {
        return isBackground() ? nodeId.get() : requestContext.nodeId();
    }

    private void setNodeId(UUID nodeId) {
        this.nodeId.set(nodeId);
    }

    public String nodeName() {
        return isBackground() ? getOptions().nodeName() : requestContext.nodeName();
    }

    public String getDomainName() {
        return isBackground() ? domains.getDomainEffectiveName(nodeId()) : requestContext.getDomainName();
    }

    public Options getOptions() {
        return isBackground() ? domains.getDomainOptions(nodeId()) : requestContext.getOptions();
    }

    public String fullName() {
        return isBackground() ? getOptions().getString("profile.full-name") : requestContext.fullName();
    }

    public String gender() {
        return isBackground() ? getOptions().getString("profile.gender") : requestContext.gender();
    }

    public UUID avatarId() {
        return isBackground() ? getOptions().getUuid("profile.avatar.id") : requestContext.avatarId();
    }

    public Avatar getAvatar() {
        if (!isBackground()) {
            return requestContext.getAvatar();
        }

        if (avatarId() == null) {
            return null;
        }
        if (avatar.get() == null || !avatar.get().getId().equals(avatarId())) {
            avatar.set(avatarRepository.findByNodeIdAndId(nodeId(), avatarId()).orElse(null));
        }
        return avatar.get();
    }

    public void associate(UUID nodeId) {
        setNodeId(nodeId);
        MDC.put("domain", domains.getDomainName(nodeId()));
    }

    public void associate(Task task) {
        associate(task.getNodeId());
    }

    public boolean isAdmin(Scope scope) {
        return isBackground() ? admin.get() : requestContext.isAdmin(scope);
    }

    public boolean isSubscribedToClient(Scope scope) {
        return isBackground() ? subscribedToClient.get() : requestContext.isSubscribedToClient(scope);
    }

    public String[] getFriendGroups(Scope scope) {
        return isBackground() ? friendGroups.get() : requestContext.getFriendGroups(scope);
    }

    public String getClientName(Scope scope) {
        return isBackground() ? clientName.get() : requestContext.getClientName(scope);
    }

    public boolean isMemberOf(UUID friendGroupId, Scope scope) {
        String targetId = friendGroupId.toString();
        for (String id : getFriendGroups(scope)) {
            if (id.equals(targetId)) {
                return true;
            }
        }
        return false;
    }

    public long getAuthScope() {
        return isBackground() ? Scope.ALL.getMask() : requestContext.getAuthScope();
    }

    public boolean hasAuthScope(Scope scope) {
        return isBackground() || requestContext.hasAuthScope(scope);
    }

    public boolean isPrincipal(PrincipalFilter principal, Scope scope) {
        return principal.includes(
                isAdmin(scope), getClientName(scope), isSubscribedToClient(scope), getFriendGroups(scope));
    }

    public void authenticatedWithSignature(String nodeName) {
        if (!isBackground()) {
            requestContext.authenticatedWithSignature(nodeName);
        } else {
            admin.set(Objects.equals(nodeName, nodeName()));
            clientName.set(nodeName);
            friendGroups.set(friendCache.getClientGroupIds(nodeName));
            subscribedToClient.set(subscribedCache.isSubscribed(nodeName));
        }
    }

    public void send(Liberin liberin) {
        if (isBackground()) {
            liberin.setNodeId(nodeId());
            liberin.setPluginContext(this);
            liberinManager.send(liberin);
        } else {
            liberin.setPluginContext(requestContext);
            requestContext.send(liberin);
        }
    }

    public void subscriptionsUpdated() {
        if (isBackground()) {
            subscriptionManager.rescan();
        } else {
            requestContext.subscriptionsUpdated();
        }
    }

    public void invalidateSubscribedCache(String clientName) {
        if (isBackground()) {
            subscribedCache.invalidate(nodeId(), clientName);
        } else {
            requestContext.invalidateSubscribedCache(clientName);
        }
    }

}
