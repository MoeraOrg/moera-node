package org.moera.node.option;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.annotation.PostConstruct;
import javax.inject.Inject;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class Options {

    private static Logger log = LoggerFactory.getLogger(Options.class);

    private Map<String, OptionDescriptor> descriptors;
    private Map<String, Object> values = new HashMap<>();

    @Value("${node-id}")
    private UUID nodeId;

    @Inject
    private ApplicationContext applicationContext;

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

    private void putValue(String name, String value) {
        OptionDescriptor desc = descriptors.get(name);
        if (desc == null) {
            log.warn("Unknown option: {}", name);
            return;
        }
        if (desc.getType().equalsIgnoreCase("string")) {
            values.put(name, value);
        } else if (desc.getType().equalsIgnoreCase("int")) {
            try {
                Integer v = Integer.parseInt(value);
                values.put(name, v);
            } catch (NumberFormatException e) {
                log.error("Invalid value of type 'int' for option: {}", name);
            }
        } else if (desc.getType().equalsIgnoreCase("long")) {
            try {
                Long v = Long.parseLong(value);
                values.put(name, v);
            } catch (NumberFormatException e) {
                log.error("Invalid value of type 'long' for option: {}", name);
            }
        } else {
            log.error("Unknown type '{}' of option: {}", desc.getType(), name);
        }
    }

    public String getString(String name) {
        if (!requireType(name, "string")) {
            return null;
        }
        return (String) values.get(name);
    }

    public Integer getInt(String name) {
        if (!requireType(name, "int")) {
            return null;
        }
        return (Integer) values.get(name);
    }

    public Long getLong(String name) {
        if (!requireType(name, "long")) {
            return null;
        }
        return (Long) values.get(name);
    }

    private boolean requireType(String name, String type) {
        OptionDescriptor desc = descriptors.get(name);
        if (desc == null) {
            log.warn("Unknown option: {}", name);
            return false;
        }
        if (!desc.getType().equals(type)) {
            throw new InvalidOptionTypeException(desc.getName(), desc.getType(), type);
        }
        return true;
    }

    public UUID nodeId() {
        return nodeId;
    }

}
