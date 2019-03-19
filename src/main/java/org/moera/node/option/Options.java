package org.moera.node.option;

import java.io.IOException;
import java.security.PrivateKey;
import java.sql.Timestamp;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.transaction.Transactional;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.moera.node.data.Option;
import org.moera.node.data.OptionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class Options {

    private static Logger log = LoggerFactory.getLogger(Options.class);

    private Map<String, OptionTypeBase> types;
    private Map<String, OptionDescriptor> descriptors;
    private Map<String, Object> values = new HashMap<>();
    private ReadWriteLock valuesLock = new ReentrantReadWriteLock();
    private ThreadLocal<Map<String, Object>> transaction = new ThreadLocal<>();
    private ThreadLocal<Integer> transactionDepth = new ThreadLocal<>();

    @Value("${node.id}")
    private UUID nodeId;

    @Inject
    private ApplicationContext applicationContext;

    @Inject
    private ApplicationEventPublisher applicationEventPublisher;

    @Inject
    private OptionRepository optionRepository;

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

    @PostConstruct
    public void init() throws NodeIdNotSetException, IOException {
        if (StringUtils.isEmpty(nodeId)) {
            throw new NodeIdNotSetException();
        }

        types = applicationContext.getBeansWithAnnotation(OptionType.class).values().stream()
            .filter(bean -> bean instanceof OptionTypeBase)
            .map(bean -> (OptionTypeBase) bean)
            .collect(Collectors.toMap(OptionTypeBase::getTypeName, Function.identity()));

        ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
        List<OptionDescriptor> data = mapper.readValue(
                applicationContext.getResource("classpath:options.yaml").getInputStream(),
                new TypeReference<List<OptionDescriptor>>() {
                });
        descriptors = data.stream().collect(Collectors.toMap(OptionDescriptor::getName, Function.identity()));
        data.stream()
            .filter(desc -> desc.getDefaultValue() != null)
            .forEach(desc -> putValue(desc.getName(), desc.getDefaultValue()));
    }

    @EventListener(ApplicationReadyEvent.class)
    public void load() {
        optionRepository.findAllByNodeId(nodeId).forEach(option -> putValue(option.getName(), option.getValue()));
        applicationEventPublisher.publishEvent(new OptionsLoadedEvent(this));
    }

    private OptionTypeBase getType(String type) {
        OptionTypeBase optionType = types.get(type);
        if (optionType == null) {
            throw new UnknownOptionTypeException(type);
        }
        return optionType;
    }

    private OptionTypeBase getOptionType(String name) {
        OptionDescriptor desc = descriptors.get(name);
        if (desc == null) {
            log.warn("Unknown option: {}", name);
            return null;
        }
        return getType(desc.getType());
    }

    private String serializeValue(String type, Object value) {
        if (value == null) {
            return null;
        }
        return getType(type).serializeValue(value);
    }

    private Object deserializeValue(String type, String value) {
        if (value == null) {
            return null;
        }
        return getType(type).deserializeValue(value);
    }

    private void putValue(String name, String value) {
        OptionDescriptor desc = descriptors.get(name);
        if (desc == null) {
            log.warn("Unknown option: {}", name);
            return;
        }
        try {
            transactionalPut(name, deserializeValue(desc.getType(), value));
        } catch (DeserializeOptionValueException e) {
            log.error("{}: {}", e.getMessage(), name);
        }
    }

    public String getString(String name) {
        OptionTypeBase optionType = getOptionType(name);
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
        OptionTypeBase optionType = getOptionType(name);
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
        OptionTypeBase optionType = getOptionType(name);
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
        OptionTypeBase optionType = getOptionType(name);
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
        OptionTypeBase optionType = getOptionType(name);
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
        OptionTypeBase optionType = getOptionType(name);
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
        OptionTypeBase optionType = getOptionType(name);
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
        OptionTypeBase optionType = getOptionType(name);
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
        OptionDescriptor desc = descriptors.get(name);
        if (desc == null) {
            log.warn("Unknown option: {}", name);
            return;
        }

        lockWrite();
        try {
            optionRepository.deleteByNodeIdAndName(nodeId, name);
            transactionalPut(name, desc.getDefaultValue());
        } finally {
            unlockWrite();
        }
    }

    public UUID nodeId() {
        return nodeId;
    }

}
