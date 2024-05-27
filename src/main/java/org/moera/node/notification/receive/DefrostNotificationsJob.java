package org.moera.node.notification.receive;

import java.util.List;
import javax.inject.Inject;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.moera.commons.util.LogUtil;
import org.moera.node.data.FrozenNotification;
import org.moera.node.data.FrozenNotificationRepository;
import org.moera.node.model.notification.Notification;
import org.moera.node.model.notification.NotificationType;
import org.moera.node.notification.NotificationPacket;
import org.moera.node.task.Job;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.web.method.HandlerMethod;

public class DefrostNotificationsJob extends Job<DefrostNotificationsJob.Parameters, Object> {

    private static final Logger log = LoggerFactory.getLogger(DefrostNotificationsJob.class);

    private static final int PAGE_SIZE = 50;

    public static class Parameters {

        public Parameters() {
        }

    }

    @Inject
    private FrozenNotificationRepository frozenNotificationRepository;

    @Inject
    private NotificationRouter notificationRouter;

    @Inject
    private ObjectMapper objectMapper;

    public DefrostNotificationsJob() {
    }

    @Override
    protected void setParameters(String parameters, ObjectMapper objectMapper) throws JsonProcessingException {
        this.parameters = objectMapper.readValue(parameters, Parameters.class);
    }

    @Override
    protected void setState(String state, ObjectMapper objectMapper) throws JsonProcessingException {
        this.state = null;
    }

    @Override
    protected void execute() {
        Pageable pageable = PageRequest.of(0, PAGE_SIZE, Sort.Direction.ASC, "receivedAt");
        while (true) {
            List<FrozenNotification> list = tx.executeRead(() ->
                    frozenNotificationRepository.findAllByNodeId(nodeId, pageable));
            if (list.isEmpty()) {
                break;
            }

            for (FrozenNotification frozen : list) {
                try {
                    defrost(frozen);
                } catch (Exception e) {
                    log.error("Error defrosting notification packet {}: {}",
                            LogUtil.format(frozen.getPacket()), e.getMessage());
                } catch (Throwable e) {
                    log.error("Error defrosting notification packet {}",
                            LogUtil.format(frozen.getPacket()), e);
                } finally {
                    tx.executeWrite(() -> frozenNotificationRepository.deleteById(frozen.getId()));
                }
            }
        }
    }

    private void defrost(FrozenNotification frozen) throws Exception {
        NotificationPacket packet = objectMapper.readValue(frozen.getPacket(), NotificationPacket.class);
        NotificationType type = NotificationType.forValue(packet.getType());
        if (type == null) {
            log.error("Unknown notification packet type: {}", LogUtil.format(packet.getType()));
            return;
        }
        Notification notification = objectMapper.readValue(packet.getNotification(), type.getStructure());

        universalContext.authenticatedWithSignature(packet.getNodeName());

        notification.setSenderNodeName(packet.getNodeName());
        notification.setSenderFullName(packet.getFullName());
        notification.setSenderGender(packet.getGender());
        notification.setSenderAvatar(packet.getAvatar());

        HandlerMethod handler = notificationRouter.getHandler(type);
        if (handler == null) {
            return;
        }
        handler.getMethod().invoke(handler.getBean(), notification);
    }

}
