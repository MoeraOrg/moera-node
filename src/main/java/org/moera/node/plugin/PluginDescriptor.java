package org.moera.node.plugin;

import java.util.Collections;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.persistence.EntityManager;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.EvictingQueue;
import org.moera.node.liberin.Liberin;
import org.moera.node.sse.SsePacket;
import org.moera.node.task.TaskAutowire;
import org.moera.node.util.Util;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@SuppressWarnings("UnstableApiUsage")
public class PluginDescriptor {

    private static final Logger log = LoggerFactory.getLogger(PluginDescriptor.class);

    private static final int BUFFER_SIZE = 128;

    private UUID nodeId;
    private String name;
    private String location;
    private Set<String> acceptedEvents = Collections.emptySet();
    private PluginEventsSender eventsSender;
    private final Object eventsSenderLock = new Object();
    private final Queue<SsePacket> eventsBuffer = EvictingQueue.create(BUFFER_SIZE);
    private final Object eventsBufferLock = new Object();
    private long lastEventMoment;
    private long lastMoment;
    private final Object lastMomentLock = new Object();

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

    public Set<String> getAcceptedEvents() {
        return acceptedEvents;
    }

    public void setAcceptedEvents(Set<String> acceptedEvents) {
        this.acceptedEvents = acceptedEvents;
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

    public void dropEventsSender() {
        synchronized (eventsSenderLock) {
            eventsSender = null;
        }
    }

    public void cancelEventsSender() {
        synchronized (eventsSenderLock) {
            if (eventsSender != null) {
                eventsSender.complete();
            }
            eventsSender = null;
        }
    }

    private void sendEvent(SsePacket packet) {
        synchronized (eventsBufferLock) {
            eventsBuffer.add(packet);
            lastEventMoment = packet.getMoment();
        }
        if (eventsSender != null) {
            eventsSender.offer(packet);
        }
    }

    public void sendEvent(Liberin liberin, EntityManager entityManager) {
        if (!acceptedEvents.contains(liberin.getTypeName())) {
            return;
        }

        try {
            sendEvent(buildEventPacket(new ObjectMapper().writeValueAsString(liberin.getModel(entityManager))));
        } catch (Throwable e) { // any exception in getModel() should end here
            log.error("Error serializing {}", liberin.getClass().getSimpleName(), e);
        }
    }

    private SsePacket buildEventPacket(String content) {
        long moment = Util.currentMoment();
        synchronized (lastMomentLock) {
            if (lastEventMoment < moment) {
                lastMoment = moment;
            } else {
                lastMoment++;
                moment = lastMoment;
            }
        }

        return new SsePacket(moment, content);
    }

    List<SsePacket> getEventsTill(long lastMoment) {
        List<SsePacket> events;
        synchronized (eventsBufferLock) {
            events = eventsBuffer.stream().filter(p -> p.getMoment() <= lastMoment).collect(Collectors.toList());
        }
        return events;
    }

    void removeEventsTill(long lastMoment) {
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

    long getLastEventMoment() {
        return lastEventMoment;
    }

}
