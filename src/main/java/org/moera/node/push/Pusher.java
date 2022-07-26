package org.moera.node.push;

import java.util.LinkedList;
import java.util.Queue;
import javax.inject.Inject;

import org.moera.node.data.PushClient;
import org.moera.node.data.PushNotificationRepository;
import org.moera.node.sse.SsePacket;
import org.moera.node.sse.SseTask;
import org.moera.node.util.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

public class Pusher extends SseTask {

    private static final Logger log = LoggerFactory.getLogger(Pusher.class);

    private static final int DB_SLICE_SIZE = 20;

    private final Queue<SsePacket> offlineQueue = new LinkedList<>();
    private final PushClients clients;
    private final PushClient client;

    @Inject
    private PlatformTransactionManager txManager;

    @Inject
    private PushNotificationRepository pushNotificationRepository;

    public Pusher(PushClients clients, PushClient client, SseEmitter emitter) {
        super(emitter);
        this.clients = clients;
        this.client = client;
    }

    public String getClientId() {
        return client.getClientId();
    }

    @Override
    public void setLastSentMoment(long lastSentMoment) {
        super.setLastSentMoment(lastSentMoment);
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

    @Override
    protected SsePacket takeOfflinePacket() {
        if (offlineQueue.isEmpty()) {
            pushNotificationRepository.findSlice(client.getId(), lastSentMoment,
                        PageRequest.of(0, DB_SLICE_SIZE, Sort.Direction.ASC, "moment")).stream()
                    .map(PushPacket::new)
                    .forEach(offlineQueue::add);
        }
        return offlineQueue.poll();
    }

    @Override
    protected void emitterClosed() {
        clients.unregister(client.getClientId());
    }

}
