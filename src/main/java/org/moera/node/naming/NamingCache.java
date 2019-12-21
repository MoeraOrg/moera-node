package org.moera.node.naming;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Collectors;
import javax.inject.Inject;

import org.moera.naming.rpc.RegisteredNameInfo;
import org.moera.node.global.RequestContext;
import org.moera.node.option.Options;
import org.moera.node.util.Util;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
public class NamingCache {

    private static class Record {

        public Instant deadline;
        public RegisteredNameDetails details;
        private Throwable error;

    }

    private static final Duration NORMAL_TTL = Duration.of(6, ChronoUnit.HOURS);
    private static final Duration ERROR_TTL = Duration.of(1, ChronoUnit.MINUTES);

    private ReadWriteLock cacheLock = new ReentrantReadWriteLock();
    private Map<String, Record> cache = new HashMap<>();
    private final Object queryDone = new Object();

    @Inject
    @Qualifier("namingTaskExecutor")
    private TaskExecutor taskExecutor;

    @Inject
    @Lazy
    private NamingClient namingClient;

    @Inject
    private RequestContext requestContext;

    public RegisteredNameDetails getFast(String name) {
        RegisteredNameDetails details = getOrRun(name);
        return details != null ? details.clone() : new RegisteredNameDetails(false, getRedirector(name), null);
    }

    public RegisteredNameDetails get(String name) {
        RegisteredNameDetails details = getOrRun(name);
        if (details != null) {
            return details.clone();
        }
        synchronized (queryDone) {
            while (true) {
                Record record = readRecord(name);
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
                }
            }
        }
    }

    private Record readRecord(String name) {
        cacheLock.readLock().lock();
        try {
            return cache.get(name);
        } finally {
            cacheLock.readLock().unlock();
        }
    }

    private RegisteredNameDetails getOrRun(String name) {
        Record record = readRecord(name);
        if (record == null) {
            cacheLock.writeLock().lock();
            try {
                record = cache.get(name);
                if (record == null) {
                    cache.put(name, new Record());
                    Options options = requestContext.getOptions();
                    taskExecutor.execute(() -> queryName(name, options));
                    return null;
                }
            } finally {
                cacheLock.writeLock().unlock();
            }
        }
        if (record.error != null) {
            throw new NamingNotAvailableException(record.error);
        } else if (record.details != null) {
            return record.details.clone();
        } else {
            return null;
        }
    }

    private void queryName(String name, Options options) {
        RegisteredName registeredName = RegisteredName.parse(name);
        RegisteredNameInfo info = null;
        Throwable error = null;
        try {
            info = namingClient.getCurrent(registeredName.getName(), registeredName.getGeneration(), options);
        } catch (Exception e) {
            error = e;
        }
        Record record = readRecord(name);
        if (record == null) {
            record = new Record();
            cacheLock.writeLock().lock();
            try {
                cache.put(name, record);
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

    private String getRedirector(String name) {
        return "/moera/gotoname?name=" + Util.ue(name);
    }

    @Scheduled(fixedDelayString = "PT1M")
    public void purge() {
        List<String> remove;
        cacheLock.readLock().lock();
        try {
            remove = cache.entrySet().stream()
                    .filter(e -> e.getValue().deadline.isBefore(Instant.now()))
                    .map(Map.Entry::getKey)
                    .collect(Collectors.toList());
        } finally {
            cacheLock.readLock().unlock();
        }
        if (remove.size() > 0) {
            cacheLock.writeLock().lock();
            try {
                remove.forEach(cache::remove);
            } finally {
                cacheLock.writeLock().unlock();
            }
        }
    }

}
