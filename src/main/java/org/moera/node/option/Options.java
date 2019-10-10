package org.moera.node.option;

import java.security.PrivateKey;
import java.sql.Timestamp;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.moera.node.data.Option;
import org.moera.node.data.OptionRepository;
import org.moera.node.option.exception.DeserializeOptionValueException;
import org.moera.node.option.exception.TransactionAbsentException;
import org.moera.node.option.type.OptionTypeBase;
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

    private boolean inTransaction() {
        return transaction.get() != null;
    }

    private void beginTransaction() {
        if (inTransaction()) {
            transactionDepth.set(transactionDepth.get() + 1);
            return;
        }
        lockWrite();
        transaction.set(new HashMap<>());
        transactionDepth.set(1);
    }

    private void commit() {
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

    private void rollback() {
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

    public void runInTransaction(OptionsOperation operation) {
        beginTransaction();
        try {
            operation.run(this);
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

    private <T> T forName(String name, OptionMapper<T> mapper) {
        OptionTypeBase optionType = optionsMetadata.getOptionType(name);
        if (optionType == null) {
            return null;
        }
        lockRead();
        try {
            return mapper.map(transactionalGet(name), optionType);
        } finally {
            unlockRead();
        }
    }

    public String getString(String name) {
        return forName(name, (value, optionType) -> optionType.getString(value));
    }

    public Boolean getBool(String name) {
        return forName(name, (value, optionType) -> optionType.getBool(value));
    }

    public Integer getInt(String name) {
        return forName(name, (value, optionType) -> optionType.getInt(value));
    }

    public Long getLong(String name) {
        return forName(name, (value, optionType) -> optionType.getLong(value));
    }

    public PrivateKey getPrivateKey(String name) {
        return forName(name, (value, optionType) -> optionType.getPrivateKey(value));
    }

    public Duration getDuration(String name) {
        return forName(name, (value, optionType) -> optionType.getDuration(value));
    }

    public UUID getUuid(String name) {
        return forName(name, (value, optionType) -> optionType.getUuid(value));
    }

    public Timestamp getTimestamp(String name) {
        return forName(name, (value, optionType) -> optionType.getTimestamp(value));
    }

    // Returns only committed values of non-internal options
    public void forEach(OptionConsumer consumer) {
        lockRead();
        try {
            for (Map.Entry<String, Object> entry : values.entrySet()) {
                if (optionsMetadata.isInternal(entry.getKey())) {
                    continue;
                }
                OptionTypeBase optionType = optionsMetadata.getOptionType(entry.getKey());
                if (optionType == null) {
                    continue;
                }
                try {
                    consumer.consume(entry.getKey(), entry.getValue(), optionType);
                } catch (Exception e) {
                }
            }
        } finally {
            unlockRead();
        }
    }

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
