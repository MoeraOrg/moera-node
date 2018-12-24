package org.moera.node.option;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.spec.InvalidKeySpecException;
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
import org.moera.commons.util.CryptoUtil;
import org.moera.commons.util.Util;
import org.moera.node.data.Option;
import org.moera.node.data.OptionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationContext;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class Options {

    private static Logger log = LoggerFactory.getLogger(Options.class);

    private Map<String, OptionDescriptor> descriptors;
    private Map<String, Object> values = new HashMap<>();
    private ReadWriteLock valuesLock = new ReentrantReadWriteLock();

    @Value("${node-id}")
    private UUID nodeId;

    @Inject
    private ApplicationContext applicationContext;

    @Inject
    private OptionRepository optionRepository;

    @PostConstruct
    public void init() throws NodeIdNotSetException, IOException {
        if (StringUtils.isEmpty(nodeId)) {
            throw new NodeIdNotSetException();
        }

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
    }

    private String serializeValue(Object value) {
        if (value instanceof PrivateKey) {
            return Util.base64encode(CryptoUtil.toRawPrivateKey((PrivateKey) value));
        }
        return value.toString();
    }

    private Object deserializeValue(String type, String value) throws DeserializeOptionValueException {
        if (value == null) {
            return null;
        }
        if (type.equalsIgnoreCase("string")) {
            return value;
        }
        if (type.equalsIgnoreCase("int")) {
            try {
                return Integer.parseInt(value);
            } catch (NumberFormatException e) {
                throw new DeserializeOptionValueException("Invalid value of type 'int' for option");
            }
        }
        if (type.equalsIgnoreCase("long")) {
            try {
                return Long.parseLong(value);
            } catch (NumberFormatException e) {
                throw new DeserializeOptionValueException("Invalid value of type 'long' for option");
            }
        }
        if (type.equalsIgnoreCase("PrivateKey")) {
            try {
                return CryptoUtil.toPrivateKey(Util.base64decode(value));
            } catch (NoSuchAlgorithmException e) {
                throw new DeserializeOptionValueException("ECDSA algorithm is not available");
            } catch (InvalidKeySpecException e) {
                throw new DeserializeOptionValueException("Invalid value of type 'PrivateKey' for option");
            }
        }
        throw new DeserializeOptionValueException(String.format("Unknown type '%s' of option", type));
    }

    private void putValue(String name, String value) {
        OptionDescriptor desc = descriptors.get(name);
        if (desc == null) {
            log.warn("Unknown option: {}", name);
            return;
        }
        try {
            values.put(name, deserializeValue(desc.getType(), value));
        } catch (DeserializeOptionValueException e) {
            log.error("{}: {}", e.getMessage(), name);
        }
    }

    public String getString(String name) {
        if (!requireType(name, "string")) {
            return null;
        }
        valuesLock.readLock().lock();
        try {
            return (String) values.get(name);
        } finally {
            valuesLock.readLock().unlock();
        }
    }

    public Integer getInt(String name) {
        if (!requireType(name, "int")) {
            return null;
        }
        valuesLock.readLock().lock();
        try {
            return (Integer) values.get(name);
        } finally {
            valuesLock.readLock().unlock();
        }
    }

    public Long getLong(String name) {
        if (!requireType(name, "long")) {
            return null;
        }
        try {
            return (Long) values.get(name);
        } finally {
            valuesLock.readLock().unlock();
        }
    }

    public PrivateKey getPrivateKey(String name) {
        if (!requireType(name, "PrivateKey")) {
            return null;
        }
        try {
            return (PrivateKey) values.get(name);
        } finally {
            valuesLock.readLock().unlock();
        }
    }

    private boolean requireType(String name, String type) {
        OptionDescriptor desc = descriptors.get(name);
        if (desc == null) {
            log.warn("Unknown option: {}", name);
            return false;
        }
        if (!desc.getType().equalsIgnoreCase(type)) {
            throw new InvalidOptionTypeException(desc.getName(), desc.getType(), type);
        }
        return true;
    }

    @Transactional
    public void set(String name, Object value) {
        OptionDescriptor desc = descriptors.get(name);
        if (desc == null) {
            log.warn("Unknown option: {}", name);
            return;
        }

        Object newValue;
        if (value == null) {
            newValue = null;
        } else if (desc.getType().equalsIgnoreCase("string")) {
            newValue = value.toString();
        } else if (desc.getType().equalsIgnoreCase("int")) {
            if (value instanceof Integer) {
                newValue = value;
            } else if (value instanceof Long
                    && ((Long) value) < Integer.MAX_VALUE
                    && ((Long) value) > Integer.MIN_VALUE) {
                newValue = ((Long) value).intValue();
            } else {
                log.error("Invalid value of type 'int' for option: {}", name);
                return;
            }
        } else if (desc.getType().equalsIgnoreCase("long")) {
            if (value instanceof Integer) {
                newValue = ((Integer) value).longValue();
            } else if (value instanceof Long) {
                newValue = value;
            } else {
                log.error("Invalid value of type 'long' for option: {}", name);
                return;
            }
        } else if (desc.getType().equalsIgnoreCase("PrivateKey")) {
            if (value instanceof PrivateKey) {
                newValue = value;
            } else {
                log.error("Invalid value of type 'PrivateKey' for option: {}", name);
                return;
            }
        } else {
            log.error("Unknown type '{}' of option: {}", desc.getType(), name);
            return;
        }

        valuesLock.writeLock().lock();
        try {
            Option option = optionRepository.findByNodeIdAndName(nodeId, name).orElse(new Option(nodeId, name));
            option.setValue(serializeValue(newValue));
            optionRepository.saveAndFlush(option);

            values.put(name, newValue);
        } finally {
            valuesLock.writeLock().unlock();
        }
    }

    @Transactional
    public void reset(String name) {
        OptionDescriptor desc = descriptors.get(name);
        if (desc == null) {
            log.warn("Unknown option: {}", name);
            return;
        }

        valuesLock.writeLock().lock();
        try {
            optionRepository.deleteByNodeIdAndName(nodeId, name);
            values.put(name, desc.getDefaultValue());
        } finally {
            valuesLock.writeLock().unlock();
        }
    }

    public UUID nodeId() {
        return nodeId;
    }

}
