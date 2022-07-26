package org.moera.node.plugin;

import java.util.List;
import java.util.Queue;
import java.util.UUID;
import java.util.stream.Collectors;

import com.google.common.collect.EvictingQueue;
import org.moera.node.sse.SsePacket;
import org.moera.node.task.TaskAutowire;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@SuppressWarnings("UnstableApiUsage")
public class PluginDescriptor {

    private static final int BUFFER_SIZE = 128;

    private UUID nodeId;
    private String name;
    private String location;
    private PluginEventsSender eventsSender;
    private final Object eventsSenderLock = new Object();
    private final Queue<SsePacket> eventsBuffer = EvictingQueue.create(BUFFER_SIZE);
    private final Object eventsBufferLock = new Object();
    private long lastEventMoment;

    public PluginDescriptor(UUID nodeId) {
        this.nodeId = nodeId;
    }

    public UUID getNodeId() {
        return nodeId;
    }

    public void setNodeId(UUID nodeId) {
        this.nodeId = nodeId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public void replaceEventsSender(SseEmitter emitter, TaskAutowire taskAutowire, long lastSeenMoment) {
        synchronized (eventsSenderLock) {
            if (eventsSender != null) {
                eventsSender.complete();
            }
            eventsSender = new PluginEventsSender(emitter, this);
            taskAutowire.autowire(eventsSender);
            eventsSender.setLastSentMoment(lastSeenMoment);
            eventsSender.activate();
        }
    }

    public void removeEventsSender() {
        synchronized (eventsSenderLock) {
            eventsSender = null;
        }
    }

    public void sendEvent(SsePacket packet) {
        synchronized (eventsBufferLock) {
            eventsBuffer.add(packet);
            lastEventMoment = packet.getMoment();
        }
        if (eventsSender != null) {
            eventsSender.offer(packet);
        }
    }

    public List<SsePacket> getEventsTill(long lastMoment) {
        List<SsePacket> events;
        synchronized (eventsBufferLock) {
            events = eventsBuffer.stream().filter(p -> p.getMoment() <= lastMoment).collect(Collectors.toList());
        }
        return events;
    }

    public void removeEventsTill(long lastMoment) {
        synchronized (eventsBufferLock) {
            while (!eventsBuffer.isEmpty()) {
                SsePacket packet = eventsBuffer.element();
                if (packet.getMoment() > lastMoment) {
                    break;
                }
                eventsBuffer.remove();
            }
        }
    }

    public long getLastEventMoment() {
        return lastEventMoment;
    }

}
