package org.moera.node.plugin;

import java.util.LinkedList;
import java.util.Queue;

import org.moera.node.sse.SsePacket;
import org.moera.node.sse.SseTask;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

public class PluginEventsSender extends SseTask {

    private final Queue<SsePacket> offlineQueue = new LinkedList<>();
    private final PluginDescriptor pluginDescriptor;

    public PluginEventsSender(SseEmitter emitter, PluginDescriptor pluginDescriptor) {
        super(emitter);
        this.pluginDescriptor = pluginDescriptor;
    }

    @Override
    public void setLastSentMoment(long lastSentMoment) {
        super.setLastSentMoment(lastSentMoment);
        lastOfflineMoment = pluginDescriptor.getLastEventMoment();
        pluginDescriptor.removeEventsTill(lastSentMoment);
    }

    @Override
    protected SsePacket takeOfflinePacket() {
        if (offlineQueue.isEmpty()) {
            offlineQueue.addAll(pluginDescriptor.getEventsTill(lastSentMoment));
        }
        return offlineQueue.poll();
    }

    @Override
    protected void emitterClosed() {
        pluginDescriptor.dropEventsSender();
    }

}
