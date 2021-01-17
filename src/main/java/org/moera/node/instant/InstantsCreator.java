package org.moera.node.instant;

import java.util.UUID;
import javax.inject.Inject;

import org.moera.node.data.Feed;
import org.moera.node.data.Story;
import org.moera.node.event.EventManager;
import org.moera.node.global.RequestContext;
import org.moera.node.model.event.Event;
import org.moera.node.model.event.FeedStatusUpdatedEvent;
import org.moera.node.naming.NodeName;
import org.moera.node.naming.RegisteredName;
import org.moera.node.operations.StoryOperations;
import org.moera.node.task.Task;
import org.moera.node.webpush.WebPushPacket;
import org.moera.node.webpush.WebPushService;

public class InstantsCreator {

    private final ThreadLocal<UUID> nodeId = new ThreadLocal<>();
    private final ThreadLocal<String> nodeName = new ThreadLocal<>();

    @Inject
    private RequestContext requestContext;

    @Inject
    private EventManager eventManager;

    @Inject
    private StoryOperations storyOperations;

    @Inject
    private WebPushService webPushService;

    protected UUID nodeId() {
        return nodeId.get() != null ? nodeId.get() : requestContext.nodeId();
    }

    public void setNodeId(UUID nodeId) {
        this.nodeId.set(nodeId);
    }

    protected String nodeName() {
        return nodeId.get() != null ? nodeName.get() : requestContext.nodeName();
    }

    public void setNodeName(String nodeName) {
        this.nodeName.set(nodeName);
    }

    public void associate(Task task) {
        setNodeId(task.getNodeId());
        setNodeName(task.getNodeName());
    }

    protected void updateMoment(Story story) {
        storyOperations.updateMoment(story, nodeId());
    }

    protected void feedStatusUpdated() {
        send(new FeedStatusUpdatedEvent(Feed.INSTANT, storyOperations.getFeedStatus(Feed.INSTANT, nodeId())));
    }

    protected void send(Event event) {
        if (nodeId.get() != null) {
            eventManager.send(nodeId.get(), event);
        } else {
            requestContext.send(event);
        }
    }

    protected void webPush(Story story) {
        webPushService.send(WebPushPacket.storyAdded(story));
    }

    protected void webPushDeleted(UUID id) {
        webPushService.send(WebPushPacket.storyDeleted(nodeId(), id));
    }

    protected static String formatNodeName(String name) {
        NodeName nodeName = NodeName.parse(name);
        if (nodeName instanceof RegisteredName) {
            RegisteredName registeredName = (RegisteredName) nodeName;
            if (registeredName.getGeneration() != 0) {
                return String.format("<span class=\"node-name\">%s<span class=\"generation\">%d</span></span>",
                        registeredName.getName(), registeredName.getGeneration());
            }
        }
        return String.format("<span class=\"node-name\">%s</span>", name);
    }

}
