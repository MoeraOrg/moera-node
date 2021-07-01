package org.moera.node.instant;

import java.util.UUID;
import javax.inject.Inject;

import org.moera.node.data.Feed;
import org.moera.node.data.Story;
import org.moera.node.global.UniversalContext;
import org.moera.node.model.event.Event;
import org.moera.node.model.event.FeedStatusUpdatedEvent;
import org.moera.node.naming.NodeName;
import org.moera.node.operations.StoryOperations;
import org.moera.node.push.PushContent;
import org.moera.node.push.PushService;
import org.moera.node.util.Util;

public class InstantsCreator {

    @Inject
    protected UniversalContext universalContext;

    @Inject
    private StoryOperations storyOperations;

    @Inject
    private PushService pushService;

    protected UUID nodeId() {
        return universalContext.nodeId();
    }

    protected String nodeName() {
        return universalContext.nodeName();
    }

    protected void send(Event event) {
        universalContext.send(event);
    }

    protected void updateMoment(Story story) {
        storyOperations.updateMoment(story, nodeId());
    }

    protected void feedStatusUpdated() {
        send(new FeedStatusUpdatedEvent(Feed.INSTANT, storyOperations.getFeedStatus(Feed.INSTANT, nodeId())));
    }

    protected void sendPush(Story story) {
        PushContent content = PushContent.storyAdded(story);
        pushService.send(nodeId(), content);
    }

    protected void deletePush(UUID id) {
        PushContent content = PushContent.storyDeleted(nodeId(), id);
        pushService.send(nodeId(), content);
    }

    protected static String formatNodeName(String name, String fullName) {
        return spanNodeName(name, fullName != null ? fullName : NodeName.shorten(name));
    }

    private static String spanNodeName(String nodeName, String text) {
        return String.format("<span class=\"node-name\" data-nodename=\"%s\">%s</span>", Util.he(nodeName), text);
    }

}
