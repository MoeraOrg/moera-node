package org.moera.node.naming;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import javax.inject.Inject;

import org.moera.naming.rpc.RegisteredNameInfo;
import org.moera.node.util.Util;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.task.TaskExecutor;
import org.springframework.stereotype.Service;

@Service
public class NamingCache {

    private static class Record {

        public Instant accessed = Instant.now();
        public RegisteredNameDetails details;

    }

    private static final int MAX_SIZE = 1024;

    private ReadWriteLock cacheLock = new ReentrantReadWriteLock();
    private Map<String, Record> cache = new HashMap<>();
    private final Object queryDone = new Object();

    @Inject
    @Qualifier("namingTaskExecutor")
    private TaskExecutor taskExecutor;

    @Inject
    private NamingClient namingClient;

    public RegisteredNameDetails getFast(String name) {
        RegisteredNameDetails details = getOrRun(name);
        return details != null ? details.clone() : new RegisteredNameDetails(false, getRedirector(name));
    }

    public RegisteredNameDetails get(String name) {
        RegisteredNameDetails details = getOrRun(name);
        if (details != null) {
            return details.clone();
        }
        synchronized (queryDone) {
            while (true) {
                Record record = readRecord(name);
                if (record != null && record.details != null) {
                    return record.details.clone();
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
                    taskExecutor.execute(() -> queryName(name));
                    return null;
                }
            } finally {
                cacheLock.writeLock().unlock();
            }
        }
        record.accessed = Instant.now();
        if (record.details != null) {
            return record.details;
        } else {
            return null;
        }
    }

    private void queryName(String name) {
        DelegatedName delegatedName = DelegatedName.parse(name);
        RegisteredNameInfo info = namingClient.getCurrent(delegatedName.getName(), delegatedName.getGeneration());
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
            record.details = info == null ? new RegisteredNameDetails(false, null) : new RegisteredNameDetails(info);
    }

    private String getRedirector(String name) {
        return "/moera/gotoname?name=" + Util.ue(name);
    }

}
