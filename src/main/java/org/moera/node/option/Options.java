package org.moera.node.option;

import java.security.PrivateKey;
import java.sql.Timestamp;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import javax.transaction.Transactional;

import org.moera.node.data.Option;
import org.moera.node.data.OptionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Options {

    private static Logger log = LoggerFactory.getLogger(Options.class);

    private Map<String, Object> values = new HashMap<>();
    private ReadWriteLock valuesLock = new ReentrantReadWriteLock();
    private ThreadLocal<Map<String, Object>> transaction = new ThreadLocal<>();
    private ThreadLocal<Integer> transactionDepth = new ThreadLocal<>();

    private UUID nodeId;
    private OptionsMetadata optionsMetadata;
    private OptionRepository optionRepository;

    public Options(UUID nodeId, OptionsMetadata optionsMetadata, OptionRepository optionRepository) {
        this.nodeId = nodeId;
        this.optionsMetadata = optionsMetadata;
        this.optionRepository = optionRepository;

        optionsMetadata.getDescriptors().values().stream()
                .filter(desc -> desc.getDefaultValue() != null)
                .forEach(desc -> putValue(desc.getName(), desc.getDefaultValue()));
        optionRepository.findAllByNodeId(nodeId).forEach(option -> putValue(option.getName(), option.getValue()));
    }

    private void lockRead() {
        if (!inTransaction()) {
            valuesLock.readLock().lock();
        }
    }

    private void unlockRead() {
        if (!inTransaction()) {
            valuesLock.readLock().unlock();
        }
    }

    private void lockWrite() {
        if (!inTransaction()) {
            valuesLock.writeLock().lock();
        }
    }

    private void unlockWrite() {
        if (!inTransaction()) {
            valuesLock.writeLock().unlock();
        }
    }

    public boolean inTransaction() {
        return transaction.get() != null;
    }

    public void beginTransaction() {
        if (inTransaction()) {
            transactionDepth.set(transactionDepth.get() + 1);
            return;
        }
        lockWrite();
        transaction.set(new HashMap<>());
        transactionDepth.set(1);
    }

    public void commit() {
        if (!inTransaction()) {
            throw new TransactionAbsentException();
        }
        if (transactionDepth.get() > 1) {
            transactionDepth.set(transactionDepth.get() - 1);
            return;
        }
        values.putAll(transaction.get());
        transaction.remove();
        transactionDepth.remove();
        unlockWrite();
    }

    public void rollback() {
        if (!inTransaction()) {
            throw new TransactionAbsentException();
        }
        if (transactionDepth.get() > 1) {
            transactionDepth.set(transactionDepth.get() - 1);
            return;
        }
        transaction.remove();
        transactionDepth.remove();
        unlockWrite();
    }

    public void runInTransaction(Runnable runnable) {
        beginTransaction();
        try {
            runnable.run();
        } catch (Throwable t) {
            rollback();
            throw t;
        }
        commit();
    }

    private Object transactionalGet(String name) {
        if (inTransaction()) {
            if (transaction.get().containsKey(name)) {
                return transaction.get().get(name);
            } else {
                return values.get(name);
            }
        } else {
            return values.get(name);
        }
    }

    private void transactionalPut(String name, Object value) {
        if (inTransaction()) {
            transaction.get().put(name, value);
        } else {
            values.put(name, value);
        }
    }

    private String serializeValue(String type, Object value) {
        if (value == null) {
            return null;
        }
        return optionsMetadata.getType(type).serializeValue(value);
    }

    private Object deserializeValue(String type, String value) {
        if (value == null) {
            return null;
        }
        return optionsMetadata.getType(type).deserializeValue(value);
    }

    private void putValue(String name, String value) {
        try {
            transactionalPut(name, deserializeValue(optionsMetadata.getDescriptor(name).getType(), value));
        } catch (DeserializeOptionValueException e) {
            log.error("{}: {}", e.getMessage(), name);
        }
    }

    public String getString(String name) {
        OptionTypeBase optionType = optionsMetadata.getOptionType(name);
        if (optionType == null) {
            return null;
        }
        lockRead();
        try {
            return optionType.getString(transactionalGet(name));
        } finally {
            unlockRead();
        }
    }

    public Integer getInt(String name) {
        OptionTypeBase optionType = optionsMetadata.getOptionType(name);
        if (optionType == null) {
            return null;
        }
        lockRead();
        try {
            return optionType.getInt(transactionalGet(name));
        } finally {
            unlockRead();
        }
    }

    public Long getLong(String name) {
        OptionTypeBase optionType = optionsMetadata.getOptionType(name);
        if (optionType == null) {
            return null;
        }
        lockRead();
        try {
            return optionType.getLong(transactionalGet(name));
        } finally {
            unlockRead();
        }
    }

    public PrivateKey getPrivateKey(String name) {
        OptionTypeBase optionType = optionsMetadata.getOptionType(name);
        if (optionType == null) {
            return null;
        }
        lockRead();
        try {
            return optionType.getPrivateKey(transactionalGet(name));
        } finally {
            unlockRead();
        }
    }

    public Duration getDuration(String name) {
        OptionTypeBase optionType = optionsMetadata.getOptionType(name);
        if (optionType == null) {
            return null;
        }
        lockRead();
        try {
            return optionType.getDuration(transactionalGet(name));
        } finally {
            unlockRead();
        }
    }

    public UUID getUuid(String name) {
        OptionTypeBase optionType = optionsMetadata.getOptionType(name);
        if (optionType == null) {
            return null;
        }
        lockRead();
        try {
            return optionType.getUuid(transactionalGet(name));
        } finally {
            unlockRead();
        }
    }

    public Timestamp getTimestamp(String name) {
        OptionTypeBase optionType = optionsMetadata.getOptionType(name);
        if (optionType == null) {
            return null;
        }
        lockRead();
        try {
            return optionType.getTimestamp(transactionalGet(name));
        } finally {
            unlockRead();
        }
    }

    @Transactional
    public void set(String name, Object value) {
        OptionTypeBase optionType = optionsMetadata.getOptionType(name);
        if (optionType == null) {
            return;
        }

        Object newValue = optionType.accept(value);

        lockWrite();
        try {
            Option option = optionRepository.findByNodeIdAndName(nodeId, name).orElse(new Option(nodeId, name));
            option.setValue(serializeValue(optionType.getTypeName(), newValue));
            optionRepository.saveAndFlush(option);

            transactionalPut(name, newValue);
        } finally {
            unlockWrite();
        }
    }

    @Transactional
    public void reset(String name) {
        lockWrite();
        try {
            optionRepository.deleteByNodeIdAndName(nodeId, name);
            transactionalPut(name, optionsMetadata.getDescriptor(name).getDefaultValue());
        } finally {
            unlockWrite();
        }
    }

    public UUID nodeId() {
        return nodeId;
    }

}
