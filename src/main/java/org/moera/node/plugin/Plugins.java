package org.moera.node.plugin;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.moera.node.liberin.Liberin;
import org.springframework.stereotype.Service;

@Service
public class Plugins {

    private final ReadWriteLock lock = new ReentrantReadWriteLock();
    private final Map<PluginKey, PluginDescriptor> descriptors = new HashMap<>();

    private AutoCloseable lockRead() {
        lock.readLock().lock();
        return this::unlockRead;
    }

    private void unlockRead() {
        lock.readLock().unlock();
    }

    private AutoCloseable lockWrite() {
        lock.writeLock().lock();
        return this::unlockWrite;
    }

    private void unlockWrite() {
        lock.writeLock().unlock();
    }

    public void add(PluginDescriptor descriptor) {
        lockWrite();
        try {
            var key = new PluginKey(descriptor.getNodeId(), descriptor.getName());
            if (descriptors.containsKey(key)) {
                throw new DuplicatePluginException(descriptor.getName());
            }
            descriptors.put(key, descriptor);
        } finally {
            unlockWrite();
        }
    }

    public void remove(UUID nodeId, String name) {
        lockWrite();
        try {
            descriptors.remove(new PluginKey(nodeId, name));
        } finally {
            unlockWrite();
        }
    }

    public void remove(PluginDescriptor descriptor) {
        remove(descriptor.getNodeId(), descriptor.getName());
    }

    public PluginDescriptor get(UUID nodeId, String name) {
        lockRead();
        try {
            return descriptors.get(new PluginKey(nodeId, name));
        } finally {
            unlockRead();
        }
    }

    public void send(Liberin liberin) {
        lockRead();
        try {
            for (PluginDescriptor descriptor : descriptors.values()) {
                if (descriptor.getNodeId() == null || descriptor.getNodeId().equals(liberin.getNodeId())) {
                    descriptor.sendEvent(liberin);
                }
            }
        } finally {
            unlockRead();
        }
    }

}
