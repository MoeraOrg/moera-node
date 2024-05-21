package org.moera.node.global;

import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;

@Component
public class RequestCounter {

    private static final Logger log = LoggerFactory.getLogger(RequestCounter.class);

    private final AtomicInteger total = new AtomicInteger(0);

    public void allot() {
        defineRequestId();
        int count = total.incrementAndGet();
        logCount(count);
    }

    public void free() {
        int count = total.decrementAndGet();
        logCount(count);
    }

    private void defineRequestId() {
        int rid = new Random().nextInt(0x1000000);
        MDC.put("rid", String.format("%06x", rid));
    }

    private void logCount(int count) {
        log.debug("Concurrent requests: {}", count);
    }

}
