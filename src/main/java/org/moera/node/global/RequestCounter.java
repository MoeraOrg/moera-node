package org.moera.node.global;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.scheduling.annotation.Scheduled;
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
    private final Map<Integer, Instant> running = new ConcurrentHashMap<>();

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
        running.put(rid, Instant.now());
    }

    private void undefineRequestId() {
        running.remove(Integer.parseInt(MDC.get("rid"), 16));
        MDC.put("rid", null);
    }

    private void logCount(int count) {
        log.debug("Concurrent requests: {}", count);
    }

    @Scheduled(fixedDelayString = "PT1M")
    public void cleanupOldRequests() {
        Instant now = Instant.now();
        for (var entry : running.entrySet()) {
            long mins = Duration.between(entry.getValue(), now).toMinutes();
            if (mins > 1) {
                log.warn("Request %06x is running %d minutes already".formatted(entry.getKey(), mins));
            }
        }
    }

}
