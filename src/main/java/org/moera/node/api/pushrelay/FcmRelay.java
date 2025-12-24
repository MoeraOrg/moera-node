package org.moera.node.api.pushrelay;

import java.security.interfaces.ECPrivateKey;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;

import org.moera.lib.crypto.CryptoUtil;
import org.moera.lib.node.Fingerprints;
import org.moera.lib.node.types.PushContent;
import org.moera.lib.node.types.PushContentType;
import org.moera.lib.pushrelay.PushRelay;
import org.moera.lib.pushrelay.PushRelayApiException;
import org.moera.lib.pushrelay.PushRelayError;
import org.moera.lib.util.LogUtil;
import org.moera.node.config.Config;
import org.moera.node.data.Feed;
import org.moera.node.domain.Domains;
import org.moera.node.util.Util;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Service;

@Service
public class FcmRelay {

    private static final Logger log = LoggerFactory.getLogger(FcmRelay.class);

    private PushRelay service;
    private final BlockingQueue<Pair<UUID, PushContent>> queue = new LinkedBlockingQueue<>();

    @Inject
    private Config config;

    @Inject
    private Domains domains;

    @PostConstruct
    public void init() {
        service = new PushRelay(config.getFcmRelay());

        Thread thread = new Thread(this::deliver);
        thread.setDaemon(true);
        thread.setName("fcmDelivery");
        thread.start();
    }

    private void deliver() {
        try {
            while (true) {
                Pair<UUID, PushContent> packet = queue.take();
                UUID nodeId = packet.getFirst();
                String nodeName = domains.getDomainOptions(nodeId).nodeName();
                PushContent content = packet.getSecond();
                log.info(
                    "Sending {} to the FCM relay to the clients of node {} ({})",
                    LogUtil.format(content.getType().getValue()), LogUtil.format(nodeId), LogUtil.format(nodeName)
                );
                int retry = 0;
                do {
                    long now = Instant.now().getEpochSecond();
                    byte[] signature = getSignature(nodeId, now);

                    try {
                        switch (content.getType()) {
                            case FEED_UPDATED ->
                                service.feedStatus(
                                    content.getFeedStatus().getFeedName(),
                                    content.getFeedStatus().getNotViewed(),
                                    content.getFeedStatus().getNotViewedMoment(),
                                    nodeName,
                                    now,
                                    signature
                                );
                            case STORY_ADDED ->
                                service.storyAdded(content.getStory(), nodeName, now, signature);
                            case STORY_DELETED ->
                                service.storyDeleted(content.getId(), nodeName, now, signature);
                        }
                    } catch (PushRelayApiException e) {
                        log.error("RPC error {} returned from FCM relay call", e.getRpcCode());
                        switch (e.getRpcCode()) {
                            case PushRelayError.NODE_NAME_UNKNOWN -> {
                                // Maybe a temporary error
                                if (retry == 0) {
                                    retry = 3;
                                }
                            }
                            case PushRelayError.NO_CLIENTS ->
                                domains.getDomainOptions(nodeId).set("push-relay.fcm.active", false);
                        }
                    } catch (Exception e) {
                        log.error("Error sending to the FCM relay: {}", e.getMessage());
                        if (retry == 0) {
                            retry = 20;
                        }
                    }

                    retry--;
                    if (retry > 0) {
                        Thread.sleep(90000);
                    } else if (retry == 0) {
                        log.error("Permanent error from the FCM relay, giving up");
                    }
                } while (retry > 0);
            }
        } catch (InterruptedException e) {
            // just exit
        }
    }

    private byte[] getSignature(UUID nodeId, long signedAt) {
        ECPrivateKey signingKey = (ECPrivateKey) domains.getDomainOptions(nodeId).getPrivateKey("profile.signing-key");
        byte[] fingerprint = Fingerprints.pushRelayMessage(Util.toTimestamp(signedAt));
        return CryptoUtil.sign(fingerprint, signingKey);
    }

    public void register(String clientId, String nodeName, String lang, long signedAt, byte[] signature) {
        service.register(clientId, nodeName, lang, signedAt, signature);
    }

    public void send(UUID nodeId, PushContent pushContent) {
        if (
            pushContent.getType() == PushContentType.FEED_UPDATED
            && !Objects.equals(pushContent.getFeedStatus().getFeedName(), Feed.NEWS)
            && !Objects.equals(pushContent.getFeedStatus().getFeedName(), Feed.EXPLORE)
        ) {
            return;
        }

        boolean active = domains.getDomainOptions(nodeId).getBool("push-relay.fcm.active");
        if (active) {
            queue.offer(Pair.of(nodeId, pushContent));
        }
    }

}
