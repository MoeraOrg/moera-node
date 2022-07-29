package org.moera.node.liberin;

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import javax.annotation.PostConstruct;
import javax.inject.Inject;

import org.moera.node.global.UniversalContext;
import org.moera.node.plugin.Plugins;
import org.moera.node.util.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.web.method.HandlerMethod;

@Service
public class LiberinManager implements Runnable {

    private static final Logger log = LoggerFactory.getLogger(LiberinManager.class);

    private final Map<Class<? extends Liberin>, HandlerMethod> handlers = new HashMap<>();
    private final BlockingQueue<Liberin> queue = new LinkedBlockingQueue<>();

    @Inject
    private ApplicationContext applicationContext;

    @Inject
    private UniversalContext universalContext;

    @Inject
    private Plugins plugins;

    @Inject
    private PlatformTransactionManager txManager;

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

        new Thread(this).start();
    }

    public void send(Liberin ...liberins) {
        try {
            for (Liberin liberin : liberins) {
                queue.put(liberin);
            }
        } catch (InterruptedException e) {
            // ignore
        }
    }

    public void send(Collection<Liberin> liberins) {
        send(liberins.toArray(Liberin[]::new));
    }

    @Override
    public void run() {
        boolean stopped = false;
        while (!stopped || queue.peek() != null) {
            Liberin liberin = null;
            try {
                liberin = queue.take();
            } catch (InterruptedException e) {
                stopped = true;
            }
            if (liberin == null) {
                continue;
            }

            universalContext.associate(liberin.getNodeId());
            if (liberin.getPluginContext() == null) {
                liberin.setPluginContext(universalContext);
            }

            try {
                Liberin lb = liberin;
                Transaction.execute(txManager, () -> {
                    plugins.send(lb);
                    return null;
                });
            } catch (Throwable e) {
                log.error(String.format("Error sending liberin %s to plugins:", liberin.getClass().getSimpleName()), e);
            }

            log.debug("Delivering liberin {}", liberin.getClass().getSimpleName());
            HandlerMethod handler = handlers.get(liberin.getClass());
            if (handler == null) {
                log.debug("Mapping for liberin {} not found, skipping", liberin.getClass().getSimpleName());
                continue;
            }
            try {
                Liberin lb = liberin;
                Transaction.execute(txManager, () -> {
                    handler.getMethod().invoke(handler.getBean(), lb);
                    return null;
                });
            } catch (Throwable e) {
                log.error(String.format("Error handling liberin %s:", liberin.getClass().getSimpleName()), e);
            }
        }
    }

}
