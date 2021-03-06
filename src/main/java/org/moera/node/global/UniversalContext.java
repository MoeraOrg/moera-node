package org.moera.node.global;

import java.util.UUID;
import javax.inject.Inject;

import org.moera.node.data.Avatar;
import org.moera.node.data.AvatarRepository;
import org.moera.node.domain.Domains;
import org.moera.node.event.EventManager;
import org.moera.node.model.event.Event;
import org.moera.node.option.Options;
import org.moera.node.task.Task;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;

@Component
public class UniversalContext {

    private final ThreadLocal<UUID> nodeId = new ThreadLocal<>();
    private final ThreadLocal<Avatar> avatar = new ThreadLocal<>();

    @Inject
    private RequestContext requestContext;

    @Inject
    private Domains domains;

    @Inject
    private EventManager eventManager;

    @Inject
    private AvatarRepository avatarRepository;

    private boolean isBackground() {
        return nodeId.get() != null;
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

    public Options getOptions() {
        return isBackground() ? domains.getDomainOptions(nodeId()) : requestContext.getOptions();
    }

    public String fullName() {
        return isBackground() ? getOptions().getString("profile.full-name") : requestContext.fullName();
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

    public void associate(Task task) {
        setNodeId(task.getNodeId());
        MDC.put("domain", domains.getDomainName(nodeId()));
    }

    public void send(Event event) {
        if (nodeId.get() != null) {
            eventManager.send(nodeId.get(), event);
        } else {
            requestContext.send(event);
        }
    }

}
