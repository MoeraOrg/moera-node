package org.moera.node.global;

import java.util.UUID;
import javax.inject.Inject;

import org.moera.node.data.Avatar;
import org.moera.node.data.AvatarRepository;
import org.moera.node.domain.Domains;
import org.moera.node.liberin.Liberin;
import org.moera.node.liberin.LiberinManager;
import org.moera.node.option.Options;
import org.moera.node.subscriptions.SubscriptionManager;
import org.moera.node.task.Task;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;

@Component
public class UniversalContext {

    private final ThreadLocal<UUID> nodeId = new ThreadLocal<>();
    private final ThreadLocal<Avatar> avatar = new ThreadLocal<>();

    @Inject
    private RequestContext requestContext;

    @Inject
    private Domains domains;

    @Inject
    private LiberinManager liberinManager;

    @Inject
    private SubscriptionManager subscriptionManager;

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

}
