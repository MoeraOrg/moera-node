package org.moera.node.push;

import java.io.IOException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import javax.inject.Inject;

import org.moera.node.task.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.task.TaskExecutor;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

public class Pusher extends Task {

    private static Logger log = LoggerFactory.getLogger(Pusher.class);

    private static final int QUEUE_CAPACITY = 20;

    private final PushClients clients;
    private final BlockingQueue<PushPacket> queue = new LinkedBlockingQueue<>(QUEUE_CAPACITY);
    private final String clientId;
    private SseEmitter emitter;
    private boolean stopped;

    @Inject
    @Qualifier("pushTaskExecutor")
    private TaskExecutor taskExecutor;

    public Pusher(PushClients clients, String clientId, SseEmitter emitter) {
        this.clients = clients;
        this.clientId = clientId;
        this.emitter = emitter;
        stopped = true;
    }

    public BlockingQueue<PushPacket> getQueue() {
        return queue;
    }

    public String getClientId() {
        return clientId;
    }

    public void replaceEmitter(SseEmitter newEmitter) {
        emitter.complete();
        emitter = newEmitter;
    }

    public synchronized void activate() {
        if (stopped) {
            stopped = false;
            taskExecutor.execute(this);
        }
    }

    @Override
    protected void execute() {
        try {
            while (!stopped) {
                PushPacket packet;
                try {
                    packet = queue.poll(1, TimeUnit.MINUTES);
                } catch (InterruptedException e) {
                    continue;
                }
                if (packet == null) {
                    stopped = true;
                    if (!queue.isEmpty()) { // queue may receive content before the previous statement
                        stopped = false;
                    }
                } else {
                    try {
                        emitter.send(SseEmitter.event()
                                .id(Long.toString(packet.getMoment()))
                                .data(packet.getContent()));
                    } catch (IOException e) {
                        clients.unregister(clientId);
                        stopped = true;
                    }
                }
            }
        } catch (Throwable e) {
            log.error(e.getMessage());
        }
    }

}
