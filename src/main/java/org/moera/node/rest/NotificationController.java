package org.moera.node.rest;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import javax.inject.Inject;
import javax.validation.Valid;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.moera.commons.util.LogUtil;
import org.moera.node.global.ApiController;
import org.moera.node.model.Result;
import org.moera.node.model.ValidationFailure;
import org.moera.node.notification.NotificationPacket;
import org.moera.node.notification.receive.NotificationRouter;
import org.moera.node.notification.NotificationType;
import org.moera.node.notification.model.Notification;
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
    private NotificationRouter notificationRouter;

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

}
