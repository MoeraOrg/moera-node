package org.moera.node.liberin;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import javax.annotation.PostConstruct;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.stereotype.Service;
import org.springframework.web.method.HandlerMethod;

@Service
public class LiberinManager {

    private static final Logger log = LoggerFactory.getLogger(LiberinManager.class);

    private final Map<Class<? extends Liberin>, HandlerMethod> handlers = new HashMap<>();

    @Inject
    private ApplicationContext applicationContext;

    @PostConstruct
    public void init() {
        for (Object processor : applicationContext.getBeansWithAnnotation(LiberinReceptor.class).values()) {
            for (Method method : processor.getClass().getMethods()) {
                LiberinMapping mapping = AnnotationUtils.findAnnotation(method, LiberinMapping.class);
                if (mapping == null) {
                    continue;
                }
                Class<?>[] params = method.getParameterTypes();
                if (params.length != 1 || !Liberin.class.isAssignableFrom(params[0])) {
                    throw new InvalidLiberinHandlerMethod(method);
                }
                Class<? extends Liberin> type = (Class<? extends Liberin>) params[0];
                if (handlers.containsKey(type)) {
                    throw new DuplicationLiberinMapping(type, handlers.get(type).getMethod());
                }
                handlers.put(type, new HandlerMethod(processor, method));
                log.debug("Mapping {} to method {}", type.getSimpleName(), method);
            }
        }
    }

    public void send(Liberin liberin) {
        send(Collections.singletonList(liberin));
    }

    public void send(Liberin[] liberins) {
        send(Arrays.asList(liberins));
    }

    public void send(Collection<Liberin> liberins) {
        new Thread(() -> { // TODO should be short-living, but TaskExecutor is possibly needed
            for (Liberin liberin : liberins) {
                HandlerMethod handler = handlers.get(liberin.getClass());
                if (handler == null) {
                    log.warn("Mapping for liberin {} not found, skipping", liberin.getClass().getSimpleName());
                    continue;
                }
                try {
                    handler.getMethod().invoke(handler.getBean(), liberin);
                } catch (InvocationTargetException | IllegalAccessException e) {
                    log.error(String.format("Error handling liberin %s:", liberin.getClass().getSimpleName()), e);
                }
            }
        }).start();
    }

}
