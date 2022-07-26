package org.moera.node.sse;

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

public class SseTask extends Task {

    private static final Logger log = LoggerFactory.getLogger(SseTask.class);

    private static final int QUEUE_CAPACITY = 20;

    protected long lastOfflineMoment;
    protected long lastSentMoment;

    private final BlockingQueue<SsePacket> queue = new LinkedBlockingQueue<>(QUEUE_CAPACITY);
    private final SseEmitter emitter;
    private Thread thread;
    private boolean stopped;

    @Inject
    @Qualifier("pushTaskExecutor")
    private TaskExecutor taskExecutor;

    public SseTask(SseEmitter emitter) {
        this.emitter = emitter;
        stopped = true;
    }

    private boolean isOffline() {
        return lastSentMoment < lastOfflineMoment;
    }

    public void offer(SsePacket packet) {
        if (!isOffline()) {
            if (!queue.offer(packet)) {
                lastOfflineMoment = Math.max(lastOfflineMoment, packet.getMoment());
                queue.clear(); // if all packets are in DB, avoid sending them twice
            }
        }
        activate();
    }

    public void complete() {
        stopped = true;
        emitter.complete();
        if (thread != null) {
            thread.interrupt();
        }
    }

    public synchronized void activate() {
        if (stopped) {
            stopped = false;
            taskExecutor.execute(this);
        }
    }

    protected void emitterClosed() {
    }

    protected void setLastSentMoment(long lastSentMoment) {
        this.lastSentMoment = lastSentMoment;
    }

    protected SsePacket takeOfflinePacket() {
        return null;
    }

    @Override
    protected void execute() {
        thread = Thread.currentThread();
        try {
            while (!stopped) {
                SsePacket packet = null;

                if (isOffline()) {
                    packet = takeOfflinePacket();
                    if (packet == null) {
                        lastSentMoment = lastOfflineMoment;
                    }
                }

                if (packet == null) {
                    try {
                        packet = queue.poll(1, TimeUnit.MINUTES);
                    } catch (InterruptedException e) {
                        continue;
                    }
                }

                if (packet == null) {
                    stopped = true;
                    if (!queue.isEmpty() || isOffline()) {
                        // queue may receive content before the previous statement
                        stopped = false;
                    }
                    continue;
                }

                try {
                    emitter.send(SseEmitter.event()
                            .id(Long.toString(packet.getMoment()))
                            .data(packet.getContent()));
                    lastSentMoment = packet.getMoment();
                } catch (IOException e) {
                    emitterClosed();
                    stopped = true;
                }
            }
        } catch (Throwable e) {
            log.error(e.getMessage());
        }
        thread = null;
    }

}
