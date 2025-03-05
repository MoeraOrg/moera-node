package org.moera.node.global;

import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;

@Component
public class RequestCounter {

    public class AutoFree implements AutoCloseable {

        @Override
        public void close() {
            free();
        }

    }

    private static final Logger log = LoggerFactory.getLogger(RequestCounter.class);

    private final AutoFree autoFree = new AutoFree();
    private final AtomicInteger total = new AtomicInteger(0);

    public AutoFree allot() {
        defineRequestId();
        int count = total.incrementAndGet();
        logCount(count);
        return autoFree;
    }

    public void free() {
        int count = total.decrementAndGet();
        logCount(count);
        undefineRequestId();
    }

    private void defineRequestId() {
        int rid = new Random().nextInt(0x1000000);
        MDC.put("rid", "%06x".formatted(rid));
    }

    private void undefineRequestId() {
        MDC.put("rid", null);
    }

    private void logCount(int count) {
        log.debug("Concurrent requests: {}", count);
    }

}
