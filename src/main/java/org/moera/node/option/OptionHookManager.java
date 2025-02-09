package org.moera.node.option;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;

import org.moera.node.global.UniversalContext;
import org.moera.node.option.exception.InvalidOptionHookMethod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;
import org.springframework.web.method.HandlerMethod;

@Service
public class OptionHookManager {

    private static final Logger log = LoggerFactory.getLogger(OptionHookManager.class);

    private final Map<String, List<HandlerMethod>> handlers = new HashMap<>();

    @Inject
    private ApplicationContext applicationContext;

    @Inject
    private UniversalContext universalContext;

    @PostConstruct
    public void init() {
        for (Object bean : applicationContext.getBeansOfType(Object.class, false, false).values()) {
            for (Method method : bean.getClass().getMethods()) {
                OptionHook mapping = AnnotationUtils.findAnnotation(method, OptionHook.class);
                if (mapping == null) {
                    continue;
                }
                Class<?>[] params = method.getParameterTypes();
                if (params.length > 1 || params.length == 1 && params[0] != OptionValueChange.class) {
                    throw new InvalidOptionHookMethod(method);
                }
                for (String name : mapping.value()) {
                    handlers.computeIfAbsent(name, nm -> new ArrayList<>()).add(new HandlerMethod(bean, method));
                    log.debug("Adding hook to option '{}': {}", name, method);
                }
            }
        }
    }

    public void invoke(OptionValueChange change) {
        List<HandlerMethod> list = handlers.get(change.getName());
        if (ObjectUtils.isEmpty(list)) {
            return;
        }
        for (HandlerMethod handler : list) {
            universalContext.associate(change.getNodeId());
            try {
                if (handler.getMethod().getParameterCount() == 1) {
                    handler.getMethod().invoke(handler.getBean(), change);
                } else {
                    handler.getMethod().invoke(handler.getBean());
                }
            } catch (IllegalAccessException | InvocationTargetException e) {
                log.error(String.format("Error executing hook for setting '%s' (%s):",
                        change.getName(), handler.getMethod()), e);
            }
        }
    }

}
