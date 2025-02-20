package org.moera.node.rest;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import jakarta.inject.Inject;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.moera.lib.crypto.CryptoUtil;
import org.moera.lib.naming.NodeName;
import org.moera.lib.naming.types.RegisteredNameInfo;
import org.moera.lib.node.types.NotificationPacket;
import org.moera.lib.node.types.Result;
import org.moera.lib.node.types.validate.ValidationUtil;
import org.moera.lib.util.LogUtil;
import org.moera.node.api.naming.NamingClient;
import org.moera.node.data.FrozenNotification;
import org.moera.node.data.FrozenNotificationRepository;
import org.moera.node.domain.Domains;
import org.moera.node.fingerprint.NotificationPacketFingerprintBuilder;
import org.moera.node.global.ApiController;
import org.moera.node.global.NoCache;
import org.moera.node.global.RequestContext;
import org.moera.node.global.RequestCounter;
import org.moera.node.model.ValidationFailure;
import org.moera.node.model.notification.Notification;
import org.moera.node.model.notification.NotificationType;
import org.moera.node.notification.receive.NotificationRouter;
import org.moera.node.util.Transaction;
import org.moera.node.util.Util;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.method.HandlerMethod;

@ApiController
@RequestMapping("/moera/api/notifications")
@NoCache
public class NotificationController {

    private static final Duration FREEZING_PERIOD = Duration.of(7, ChronoUnit.DAYS);

    private static final Logger log = LoggerFactory.getLogger(NotificationController.class);

    @Inject
    private RequestCounter requestCounter;

    @Inject
    private RequestContext requestContext;

    @Inject
    private FrozenNotificationRepository frozenNotificationRepository;

    @Inject
    private NotificationRouter notificationRouter;

    @Inject
    private Domains domains;

    @Inject
    private NamingClient namingClient;

    @Inject
    private ObjectMapper objectMapper;

    @Inject
    private Validator validator;

    @Inject
    private Transaction tx;

    @PostMapping
    public Result post(@RequestBody NotificationPacket packet, Errors errors) throws Throwable {
        log.info(
            "POST /notifications (nodeName = {}, id = {}, type = {})",
            LogUtil.format(packet.getNodeName()), LogUtil.format(packet.getId()), LogUtil.format(packet.getType())
        );

        packet.validate();
        NotificationType type = NotificationType.forValue(packet.getType());
        ValidationUtil.notNull(type, "notification.type.unknown");
        HandlerMethod handler = notificationRouter.getHandler(type);
        if (handler == null) {
            return Result.OK;
        }
        ValidationUtil.assertion(
            Instant.ofEpochSecond(packet.getCreatedAt()).plus(10, ChronoUnit.MINUTES).isAfter(Instant.now()),
            "notification.created-at.too-old"
        );
        ValidationUtil.assertion(verifySignature(packet), "notification.signature.invalid");
        requestContext.authenticatedWithSignature(packet.getNodeName());

        Notification notification;
        try {
            notification = objectMapper.readValue(packet.getNotification(), type.getStructure());
        } catch (IOException e) {
            throw new ValidationFailure("notification.notification.invalid");
        }

        validate(notification, errors);

        if (requestContext.getOptions().isFrozen()) {
            freeze(packet);
            return Result.FROZEN;
        }

        notification.setSenderNodeName(packet.getNodeName());
        notification.setSenderFullName(packet.getFullName());
        notification.setSenderGender(packet.getGender());
        notification.setSenderAvatar(packet.getAvatar());
        try {
            handler.getMethod().invoke(handler.getBean(), notification);
        } catch (InvocationTargetException e) {
            throw e.getCause();
        }

        return Result.OK;
    }

    private void validate(Notification notification, Errors errors) {
        validator.validate(notification, errors);
        if (errors.hasErrors()) {
            String[] codes = errors.getAllErrors().get(0).getCodes();
            if (codes != null && codes.length > 0) {
                throw new ValidationFailure(codes[0]);
            } else {
                throw new ValidationFailure("notification.notification.invalid");
            }
        }
    }

    private boolean verifySignature(NotificationPacket packet) {
        byte[] signingKey = fetchSigningKey(packet.getNodeName());
        if (signingKey == null) {
            return false;
        }

        byte[] fingerprint = NotificationPacketFingerprintBuilder.build(packet.getSignatureVersion(), packet);
        return CryptoUtil.verifySignature(fingerprint, packet.getSignature(), signingKey);
    }

    private byte[] fetchSigningKey(String ownerName) {
        String namingLocation = domains.getDomainOptions(requestContext.nodeId()).getString("naming.location");
        NodeName registeredName = NodeName.parse(ownerName);
        RegisteredNameInfo nameInfo =
                namingClient.getCurrent(registeredName.getName(), registeredName.getGeneration(), namingLocation);
        return nameInfo != null ? nameInfo.getSigningKey() : null;
    }

    private void freeze(NotificationPacket packet) throws JsonProcessingException {
        FrozenNotification frozenNotification = new FrozenNotification();
        frozenNotification.setId(UUID.randomUUID());
        frozenNotification.setNodeId(requestContext.nodeId());
        frozenNotification.setPacket(objectMapper.writeValueAsString(packet));
        frozenNotification.setDeadline(Timestamp.from(Instant.now().plus(FREEZING_PERIOD)));
        frozenNotificationRepository.save(frozenNotification);
    }

    @Scheduled(fixedDelayString = "P1D")
    public void deleteExpiredFrozen() {
        try (var ignored = requestCounter.allot()) {
            log.info("Purging expired frozen notifications");

            tx.executeWrite(() -> frozenNotificationRepository.deleteExpired(Util.now()));
        }
    }

}
