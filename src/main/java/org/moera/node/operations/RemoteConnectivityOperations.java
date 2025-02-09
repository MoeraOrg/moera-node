package org.moera.node.operations;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import jakarta.inject.Inject;

import org.moera.node.data.ConnectivityStatus;
import org.moera.node.data.RemoteConnectivity;
import org.moera.node.data.RemoteConnectivityRepository;
import org.moera.node.global.RequestCounter;
import org.moera.node.util.Transaction;
import org.moera.node.util.Util;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class RemoteConnectivityOperations {

    private static class Record {

        public ConnectivityStatus status;
        public Instant lastUsedAt;

        Record(ConnectivityStatus status, Instant lastUsedAt) {
            this.status = status;
            this.lastUsedAt = lastUsedAt;
        }

    }

    private static final Logger log = LoggerFactory.getLogger(RemoteConnectivityOperations.class);

    private static final Duration TTL = Duration.of(1, ChronoUnit.HOURS);

    private final ReadWriteLock cacheLock = new ReentrantReadWriteLock();
    private final Map<String, Record> cache = new HashMap<>();

    @Inject
    private RequestCounter requestCounter;

    @Inject
    private RemoteConnectivityRepository remoteConnectivityRepository;

    @Inject
    private Transaction tx;

    public ConnectivityStatus getStatus(String remoteNodeName) {
        cacheLock.readLock().lock();
        try {
            var record = cache.get(remoteNodeName);
            if (record != null) {
                record.lastUsedAt = Instant.now();
                return record.status;
            }
        } finally {
            cacheLock.readLock().unlock();
        }

        RemoteConnectivity remoteConnectivity =
                tx.executeRead(() -> remoteConnectivityRepository.findByRemoteNodeName(remoteNodeName)).orElse(null);
        ConnectivityStatus status = remoteConnectivity != null
                ? remoteConnectivity.getStatus()
                : ConnectivityStatus.NORMAL;
        cacheLock.writeLock().lock();
        try {
            cache.put(remoteNodeName, new Record(status, Instant.now()));
        } finally {
            cacheLock.writeLock().unlock();
        }

        return status;
    }

    public void setStatus(String remoteNodeName, ConnectivityStatus status) {
        cacheLock.readLock().lock();
        try {
            var record = cache.get(remoteNodeName);
            if (record != null && record.status == status) {
                return;
            }
        } finally {
            cacheLock.readLock().unlock();
        }

        cacheLock.writeLock().lock();
        try {
            cache.put(remoteNodeName, new Record(status, Instant.now()));
        } finally {
            cacheLock.writeLock().unlock();
        }

        tx.executeWrite(() -> {
            if (status == ConnectivityStatus.NORMAL) {
                remoteConnectivityRepository.deleteByRemoteNodeName(remoteNodeName);
            } else {
                RemoteConnectivity remoteConnectivity =
                        remoteConnectivityRepository.findByRemoteNodeName(remoteNodeName).orElse(null);
                if (remoteConnectivity == null) {
                    remoteConnectivity = new RemoteConnectivity();
                    remoteConnectivity.setId(UUID.randomUUID());
                    remoteConnectivity.setRemoteNodeName(remoteNodeName);
                }
                remoteConnectivity.setStatus(status);
                remoteConnectivity.setUpdatedAt(Util.now());
                remoteConnectivityRepository.save(remoteConnectivity);
            }
        });

    }

    @Scheduled(fixedDelayString = "PT1H")
    public void clearCache() {
        try (var ignored = requestCounter.allot()) {
            log.info("Purging remote nodes connectivity cache");

            List<String> remove;
            cacheLock.readLock().lock();
            try {
                remove = cache.entrySet().stream()
                        .filter(e -> e.getValue().lastUsedAt.plus(TTL).isBefore(Instant.now()))
                        .map(Map.Entry::getKey)
                        .toList();
            } finally {
                cacheLock.readLock().unlock();
            }

            if (!remove.isEmpty()) {
                cacheLock.writeLock().lock();
                try {
                    remove.forEach(cache::remove);
                } finally {
                    cacheLock.writeLock().unlock();
                }
            }
        }
    }

}
