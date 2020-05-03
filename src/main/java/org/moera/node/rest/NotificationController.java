package org.moera.node.rest;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import javax.inject.Inject;
import javax.validation.Valid;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.moera.commons.crypto.CryptoUtil;
import org.moera.commons.crypto.Fingerprint;
import org.moera.commons.util.LogUtil;
import org.moera.naming.rpc.RegisteredNameInfo;
import org.moera.node.domain.Domains;
import org.moera.node.fingerprint.FingerprintManager;
import org.moera.node.fingerprint.FingerprintObjectType;
import org.moera.node.global.ApiController;
import org.moera.node.global.RequestContext;
import org.moera.node.model.Result;
import org.moera.node.model.ValidationFailure;
import org.moera.node.naming.NamingClient;
import org.moera.node.naming.NodeName;
import org.moera.node.naming.RegisteredName;
import org.moera.node.notification.NotificationPacket;
import org.moera.node.notification.NotificationType;
import org.moera.node.notification.model.Notification;
import org.moera.node.notification.receive.NotificationRouter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.method.HandlerMethod;

@ApiController
@RequestMapping("/moera/api/notifications")
public class NotificationController {

    private static Logger log = LoggerFactory.getLogger(NotificationController.class);

    @Inject
    private RequestContext requestContext;

    @Inject
    private NotificationRouter notificationRouter;

    @Inject
    private Domains domains;

    @Inject
    private NamingClient namingClient;

    @Inject
    private FingerprintManager fingerprintManager;

    @PostMapping
    public Result post(@Valid @RequestBody NotificationPacket packet)
            throws InvocationTargetException, IllegalAccessException {

        log.info("POST /notifications (nodeName = {}, id = {}, type = {})",
                LogUtil.format(packet.getNodeName()), LogUtil.format(packet.getId()), LogUtil.format(packet.getType()));

        NotificationType type = NotificationType.forValue(packet.getType());
        if (type == null) {
            throw new ValidationFailure("notificationPacket.type.unknown");
        }
        HandlerMethod handler = notificationRouter.getHandler(type);
        if (handler == null) {
            return Result.OK;
        }
        if (Instant.ofEpochSecond(packet.getCreatedAt()).plus(10, ChronoUnit.MINUTES).isBefore(Instant.now())) {
            throw new ValidationFailure("notificationPacket.createdAt.too-old");
        }
        if (!verifySignature(packet)) {
            throw new ValidationFailure("notificationPacket.signature.invalid");
        }

        ObjectMapper mapper = new ObjectMapper();
        Notification notification;
        try {
            notification = mapper.readValue(packet.getNotification(), type.getStructure());
        } catch (IOException e) {
            throw new ValidationFailure("notificationPacket.notification.invalid");
        }

        notification.setSenderNodeName(packet.getNodeName());
        handler.getMethod().invoke(handler.getBean(), notification);

        return Result.OK;
    }

    private boolean verifySignature(NotificationPacket packet) {
        byte[] signingKey = fetchSigningKey(packet.getNodeName());
        if (signingKey == null) {
            return false;
        }

        Constructor<? extends Fingerprint> constructor = fingerprintManager.getConstructor(
                FingerprintObjectType.NOTIFICATION_PACKET, packet.getSignatureVersion(), NotificationPacket.class);
        return CryptoUtil.verify(packet.getSignature(), signingKey, constructor, packet);
    }

    private byte[] fetchSigningKey(String ownerName) {
        String namingLocation = domains.getDomainOptions(requestContext.nodeId()).getString("naming.location");
        RegisteredName registeredName = (RegisteredName) NodeName.parse(ownerName);
        RegisteredNameInfo nameInfo =
                namingClient.getCurrent(registeredName.getName(), registeredName.getGeneration(), namingLocation);
        return nameInfo != null ? nameInfo.getSigningKey() : null;
    }

}
