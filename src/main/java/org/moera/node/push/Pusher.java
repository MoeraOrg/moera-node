package org.moera.node.push;

import java.io.IOException;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import javax.inject.Inject;

import org.moera.node.data.PushClient;
import org.moera.node.data.PushNotificationRepository;
import org.moera.node.task.Task;
import org.moera.node.util.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.task.TaskExecutor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

public class Pusher extends Task {

    private static Logger log = LoggerFactory.getLogger(Pusher.class);

    private static final int QUEUE_CAPACITY = 20;
    private static final int DB_SLICE_SIZE = 20;

    private final PushClients clients;
    private final BlockingQueue<PushPacket> queue = new LinkedBlockingQueue<>(QUEUE_CAPACITY);
    private final PushClient client;
    private final SseEmitter emitter;
    private Thread thread;
    private boolean stopped;
    private final Queue<PushPacket> offlineQueue = new LinkedList<>();
    private long lastOfflineMoment;
    private long lastSentMoment;

    @Inject
    @Qualifier("pushTaskExecutor")
    private TaskExecutor taskExecutor;

    @Inject
    private PlatformTransactionManager txManager;

    @Inject
    private PushNotificationRepository pushNotificationRepository;

    public Pusher(PushClients clients, PushClient client, SseEmitter emitter) {
        this.clients = clients;
        this.client = client;
        this.emitter = emitter;
        stopped = true;
    }

    private boolean isOffline() {
        return lastSentMoment < lastOfflineMoment;
    }

    public void offer(PushPacket packet) {
        if (!isOffline()) {
            if (!queue.offer(packet)) {
                lastOfflineMoment = Math.max(lastOfflineMoment, packet.getMoment());
                queue.clear(); // all packets are in DB, so avoid sending them twice
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

    public String getClientId() {
        return client.getClientId();
    }

    public void setLastSentMoment(long lastSentMoment) {
        this.lastSentMoment = lastSentMoment;
        try {
            Transaction.execute(txManager, () -> {
                Long moment = pushNotificationRepository.findLastMoment(client.getId());
                lastOfflineMoment = moment != null ? moment : 0;
                pushNotificationRepository.deleteTill(client.getId(), lastSentMoment);
                return null;
            });
        } catch (Throwable e) {
            log.error("Error initializing SSE pusher", e);
        }
    }

    public synchronized void activate() {
        if (stopped) {
            stopped = false;
            taskExecutor.execute(this);
        }
    }

    public PushPacket takeOfflinePacket() {
        if (offlineQueue.isEmpty()) {
            pushNotificationRepository.findSlice(client.getId(), lastSentMoment,
                        PageRequest.of(0, DB_SLICE_SIZE, Sort.Direction.ASC, "moment")).stream()
                    .map(PushPacket::new)
                    .forEach(offlineQueue::add);
        }
        return offlineQueue.poll();
    }

    @Override
    protected void execute() {
        thread = Thread.currentThread();
        try {
            while (!stopped) {
                PushPacket packet = null;

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
                    clients.unregister(client.getClientId());
                    stopped = true;
                }
            }
        } catch (Throwable e) {
            log.error(e.getMessage());
        }
        thread = null;
    }

}
