package org.moera.node.notification.send;

import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.jetbrains.annotations.NotNull;
import org.moera.node.model.Result;
import org.moera.node.notification.NotificationPacket;
import org.moera.node.notification.model.Notification;
import org.moera.node.task.Task;
import org.moera.node.util.Util;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;

public class NotificationSender extends Task {

    private static Logger log = LoggerFactory.getLogger(NotificationSender.class);

    private String receiverNodeName;
    private BlockingQueue<Notification> queue = new LinkedBlockingQueue<>();
    private boolean stopped = false;
    private NotificationSenderPool pool;

    public NotificationSender(NotificationSenderPool pool, String receiverNodeName) {
        this.pool = pool;
        this.receiverNodeName = receiverNodeName;
    }

    public boolean isStopped() {
        return stopped;
    }

    public void put(@NotNull Notification notification) throws InterruptedException {
        queue.put(notification);
    }

    @Override
    public void run() {
        while (!stopped) {
            Notification notification;
            try {
                notification = queue.poll(1, TimeUnit.MINUTES);
            } catch (InterruptedException e) {
                continue;
            }
            if (notification == null) {
                stopped = true;
                if (!queue.isEmpty()) { // queue may receive content before the previous statement
                    stopped = false;
                }
            } else {
                deliver(notification);
            }
        }
        pool.deleteSender(nodeId, receiverNodeName);
    }

    private void deliver(Notification notification) {
        log.info("Delivering notification {} to node '{}'", notification.getType().name(), receiverNodeName);

        String nodeUri = fetchNodeUri(receiverNodeName);
        if (nodeUri == null) {
            failed("Receiving node not found");
            return;
        }
        WebClient.create(String.format("%s/api/notifications", nodeUri))
                .post()
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(createPacket(notification))
                .retrieve()
                .bodyToMono(Result.class)
                .subscribe(this::succeeded, this::error);
    }

    private NotificationPacket createPacket(Notification notification) {
        NotificationPacket packet = new NotificationPacket();
        packet.setId(UUID.randomUUID().toString());
        packet.setNodeName(nodeName);
        packet.setCreatedAt(Util.toEpochSecond(Util.now()));
        packet.setType(notification.getType().getValue());

        ObjectMapper mapper = new ObjectMapper();
        try {
            packet.setNotification(mapper.writeValueAsString(notification));
        } catch (JsonProcessingException e) {
            failed("Cannot serialize the notification object");
        }

        return packet;
    }

    private void succeeded(Result result) {
        initLoggingDomain();
        if (result.isOk()) {
            log.info("Notification delivered successfully");
        } else {
            log.info("Receiving node returned error: {}", result.getMessage());
        }
    }

    protected void error(Throwable e) {
        failed(e.getMessage());
    }

    protected final void failed(String message) {
        initLoggingDomain();
        log.error(message);
    }

}
