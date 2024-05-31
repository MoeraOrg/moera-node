package org.moera.node.api.naming;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import javax.inject.Inject;

import org.moera.naming.rpc.RegisteredName;
import org.moera.naming.rpc.RegisteredNameInfo;
import org.moera.node.global.RequestCounter;
import org.moera.node.global.UniversalContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
public class NamingCache {

    private static class Key {

        public String namingLocation;
        public String name;

        Key(String namingLocation, String name) {
            this.namingLocation = namingLocation;
            this.name = name;
        }

        @Override
        public boolean equals(Object peer) {
            if (this == peer) {
                return true;
            }
            if (peer == null || getClass() != peer.getClass()) {
                return false;
            }
            Key key = (Key) peer;
            return namingLocation.equals(key.namingLocation) && name.equals(key.name);
        }

        @Override
        public int hashCode() {
            return Objects.hash(namingLocation, name);
        }

    }

    private static final class Record {

        public Instant accessed = Instant.now();
        public Instant deadline;
        public RegisteredNameDetails details;
        private Throwable error;

    }

    private static final Logger log = LoggerFactory.getLogger(NamingCache.class);

    private static final Duration NORMAL_TTL = Duration.of(6, ChronoUnit.HOURS);
    private static final Duration ERROR_TTL = Duration.of(1, ChronoUnit.MINUTES);

    private final ReadWriteLock cacheLock = new ReentrantReadWriteLock();
    private final Map<Key, Record> cache = new HashMap<>();
    private final Object queryDone = new Object();

    @Inject
    private RequestCounter requestCounter;

    @Inject
    @Qualifier("namingTaskExecutor")
    private TaskExecutor taskExecutor;

    @Inject
    @Lazy
    private NamingClient namingClient;

    @Inject
    private UniversalContext universalContext;

    private Key getKey(String name) {
        return new Key(universalContext.getOptions().getString("naming.location"), name);
    }

    public RegisteredNameDetails getFast(String name) {
        RegisteredNameDetails details = getOrRun(getKey(name));
        return details != null
                ? details.clone()
                : new RegisteredNameDetails(name, null, null);
    }

    public RegisteredNameDetails get(String name) {
        Key key = getKey(name);
        RegisteredNameDetails details = getOrRun(key);
        if (details != null) {
            return details.clone();
        }
        synchronized (queryDone) {
            while (true) {
                Record record = readRecord(key);
                if (record != null) {
                    if (record.error != null) {
                        throw new NamingNotAvailableException(record.error);
                    }
                    if (record.details != null) {
                        return record.details.clone();
                    }
                }
                try {
                    queryDone.wait();
                } catch (InterruptedException e) {
                    // ignore
                }
            }
        }
    }

    private Record readRecord(Key key) {
        cacheLock.readLock().lock();
        try {
            return cache.get(key);
        } finally {
            cacheLock.readLock().unlock();
        }
    }

    private RegisteredNameDetails getOrRun(Key key) {
        Record record = readRecord(key);
        if (record == null) {
            cacheLock.writeLock().lock();
            try {
                record = cache.get(key);
                if (record == null) {
                    cache.put(key, new Record());
                    run(key);
                    return null;
                }
            } finally {
                cacheLock.writeLock().unlock();
            }
        }
        record.accessed = Instant.now();
        if (record.error != null) {
            throw new NamingNotAvailableException(record.error);
        } else if (record.details != null) {
            return record.details.clone();
        } else {
            return null;
        }
    }

    private void run(Key key) {
        taskExecutor.execute(() -> queryName(key));
    }

    private void queryName(Key key) {
        RegisteredName registeredName = RegisteredName.parse(key.name);
        RegisteredNameInfo info = null;
        Throwable error = null;
        try {
            info = namingClient.getCurrent(registeredName.getName(), registeredName.getGeneration(),
                                           key.namingLocation);
        } catch (Exception e) {
            error = e;
        }
        Record record = readRecord(key);
        if (record == null) {
            record = new Record();
            cacheLock.writeLock().lock();
            try {
                cache.put(key, record);
                if (registeredName.getGeneration() == 0) {
                    if (key.name.equals(registeredName.getName())) {
                        cache.put(new Key(key.namingLocation, registeredName.toString()), record);
                    } else {
                        cache.put(new Key(key.namingLocation, registeredName.getName()), record);
                    }
                }
            } finally {
                cacheLock.writeLock().unlock();
            }
        }
        record.details = info == null ? new RegisteredNameDetails() : new RegisteredNameDetails(info);
        record.error = error;
        record.deadline = Instant.now().plus(error == null ? NORMAL_TTL : ERROR_TTL);
        synchronized (queryDone) {
            queryDone.notifyAll();
        }
    }

    @Scheduled(fixedDelayString = "PT1M")
    public void purge() {
        try (var ignored = requestCounter.allot()) {
            log.debug("Purging naming cache");

            List<Key> remove;
            cacheLock.readLock().lock();
            try {
                remove = cache.entrySet().stream()
                        .filter(e -> e.getValue().deadline != null && e.getValue().deadline.isBefore(Instant.now()))
                        .map(Map.Entry::getKey)
                        .toList();
            } finally {
                cacheLock.readLock().unlock();
            }
            if (!remove.isEmpty()) {
                cacheLock.writeLock().lock();
                try {
                    remove.forEach(key -> {
                        if (cache.get(key).accessed.plus(NORMAL_TTL).isAfter(Instant.now())) {
                            run(key);
                        } else {
                            cache.remove(key);
                        }
                    });
                } finally {
                    cacheLock.writeLock().unlock();
                }
            }
        }
    }

}
