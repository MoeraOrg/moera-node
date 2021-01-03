package org.moera.node.webpush;

import java.io.IOException;
import java.math.BigInteger;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.LinkedBlockingQueue;
import javax.annotation.PostConstruct;
import javax.inject.Inject;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.novacrypto.bip39.Words;
import nl.martijndwars.webpush.Notification;
import nl.martijndwars.webpush.PushService;
import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.bouncycastle.jce.ECNamedCurveTable;
import org.bouncycastle.jce.spec.ECParameterSpec;
import org.bouncycastle.math.ec.ECPoint;
import org.jose4j.lang.JoseException;
import org.moera.commons.crypto.CryptoException;
import org.moera.node.data.WebPushSubscription;
import org.moera.node.data.WebPushSubscriptionRepository;
import org.moera.node.domain.Domains;
import org.moera.node.option.Options;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class WebPushService {

    private static Logger log = LoggerFactory.getLogger(WebPushService.class);

    @Inject
    private Domains domains;

    @Inject
    private WebPushSubscriptionRepository webPushSubscriptionRepository;

    @Inject
    private ObjectMapper objectMapper;

    private final BlockingQueue<WebPushPacket> queue = new LinkedBlockingQueue<>();

    @PostConstruct
    public void init() {
        Thread thread = new Thread(this::run);
        thread.setDaemon(true);
        thread.start();
    }

    public PublicKey generateKeys(Options options) {
        try {
            ECParameterSpec ecSpec = ECNamedCurveTable.getParameterSpec(WebPush.EC_CURVE);

            SecureRandom random = new SecureRandom();
            byte[] seed = new byte[Words.TWENTY_FOUR.byteLength()];
            random.nextBytes(seed);

            BigInteger d = new BigInteger(seed);
            ECPoint q = ecSpec.getG().multiply(d);

            KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("EC", "BC");
            keyPairGenerator.initialize(ecSpec, random);
            KeyPair keyPair = keyPairGenerator.generateKeyPair();
            options.set("web-push.private-key", keyPair.getPrivate());
            options.set("web-push.public-key", keyPair.getPublic());

            return keyPair.getPublic();
        } catch (GeneralSecurityException e) {
            throw new CryptoException(e);
        }
    }

    private void run() {
        while (true) {
            WebPushPacket packet;
            try {
                packet = queue.take();
            } catch (InterruptedException e) {
                continue;
            }
            deliver(packet);
        }
    }

    public void send(WebPushPacket packet) {
        try {
            queue.put(packet);
        } catch (InterruptedException e) {
            // ignore
        }
    }

    private void deliver(WebPushPacket packet) {
        var subscriptions = webPushSubscriptionRepository.findAllByNodeId(packet.getNodeId());
        if (subscriptions.isEmpty()) {
            return;
        }

        String domainName = domains.getDomainName(packet.getNodeId());
        packet.setOriginUrl(String.format("https://%s/moera", domainName));

        MDC.put("domain", domainName);
        String id = packet.getId() != null ? packet.getId() : packet.getStory().getId();
        log.debug("Delivering story '{}' to Web Push subscribers", id);

        Options options = domains.getDomainOptions(packet.getNodeId());
        PushService pushService = new PushService(new KeyPair(
                options.getPublicKey("web-push.public-key"),
                options.getPrivateKey("web-push.private-key")));

        String payload;
        try {
            payload = objectMapper.writeValueAsString(packet);
        } catch (JsonProcessingException e) {
            log.error("Error encoding a story for Web Push notification", e);
            return;
        }

        subscriptions.forEach(sub -> deliver(pushService, sub, payload));
    }

    private void deliver(PushService pushService, WebPushSubscription subscription, String payload) {
        log.debug("Delivering to Web Push subscriber '{}'", subscription.getId());

        try {
            Notification notification = new Notification(subscription.getEndpoint(), subscription.getPublicKey(),
                    subscription.getAuthKey(), payload);
            while (true) {
                HttpResponse response = pushService.send(notification);
                HttpStatus status = HttpStatus.valueOf(response.getStatusLine().getStatusCode());
                switch (status) {
                    case CREATED:
                        log.debug("Web Push notification delivered successfully");
                        return;
                    case TOO_MANY_REQUESTS:
                        try {
                            Thread.sleep(getRetryDelay(response).toMillis());
                        } catch (InterruptedException e) {
                            // ignore
                        }
                        break;

                    case BAD_REQUEST:
                        log.error("Web Push service returned BAD_REQUEST status");
                        return;

                    case NOT_FOUND:
                    case GONE:
                        log.info("Web Push subscription '{}' reported as gone, deleting", subscription.getId());
                        webPushSubscriptionRepository.delete(subscription);
                        return;

                    case PAYLOAD_TOO_LARGE:
                        log.error("Web Push service returned PAYLOAD_TOO_LARGE status");
                        return;

                    default:
                        log.warn("Web Push service returned unexpected status: {}", status);
                        return;
                }
            }
        } catch (GeneralSecurityException | IOException | JoseException | ExecutionException
                | InterruptedException e) {
            log.error("Error sending Web Push notification", e);
        }
    }

    private Duration getRetryDelay(HttpResponse response) {
        Header header = response.getFirstHeader("Retry-After");
        if (header == null) {
            return Duration.ofMinutes(1);
        }
        try {
            int seconds = Integer.parseInt(header.getValue());
            return Duration.ofSeconds(seconds);
        } catch (NumberFormatException e) {
            // ignore
        }
        try {
            LocalDateTime dateTime = LocalDateTime.from(DateTimeFormatter.RFC_1123_DATE_TIME.parse(header.getValue()));
            return Duration.between(Instant.now(), dateTime);
        } catch (Exception e) {
            return Duration.ofMinutes(1);
        }
    }

}
