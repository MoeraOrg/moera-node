package org.moera.node.notification;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import javax.annotation.PostConstruct;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;

@Component
public class NotificationRouter {

    private static Logger log = LoggerFactory.getLogger(NotificationRouter.class);

    private Map<NotificationType, HandlerMethod> handlers = new HashMap<>();

    @Inject
    private ApplicationContext applicationContext;

    @PostConstruct
    public void init() {
        for (Object processor : applicationContext.getBeansWithAnnotation(NotificationProcessor.class).values()) {
            for (Method method : processor.getClass().getMethods()) {
                NotificationMapping mapping = AnnotationUtils.findAnnotation(method, NotificationMapping.class);
                if (mapping == null) {
                    continue;
                }
                NotificationType type = mapping.value();
                if (handlers.containsKey(type)) {
                    throw new DuplicationNotificationMapping(type, handlers.get(type).getMethod());
                }
                Class<?>[] params = method.getParameterTypes();
                if (params.length > 1 || (params.length == 1 && params[0] != type.getStructure())) {
                    throw new InvalidNotificationHandlerMethod(type);
                }
                handlers.put(type, new HandlerMethod(processor, method));
                log.debug("Mapping {} to method {}", type.name(), method);
            }
        }
    }

    public HandlerMethod getHandler(NotificationType type) {
        return handlers.get(type);
    }

}
