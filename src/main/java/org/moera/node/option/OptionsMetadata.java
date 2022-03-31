package org.moera.node.option;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.annotation.PostConstruct;
import javax.inject.Inject;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.moera.node.data.OptionDefault;
import org.moera.node.data.OptionDefaultRepository;
import org.moera.node.option.exception.UnknownOptionTypeException;
import org.moera.node.option.type.OptionType;
import org.moera.node.option.type.OptionTypeBase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class OptionsMetadata {

    public static final String CLIENT_PREFIX = "client.";

    private static final Logger log = LoggerFactory.getLogger(OptionsMetadata.class);

    private Map<String, OptionTypeBase> types;
    private Map<String, OptionDescriptor> descriptors;
    private Map<String, Object> typeModifiers;

    @Inject
    private ApplicationEventPublisher applicationEventPublisher;

    @Inject
    private ApplicationContext applicationContext;

    @Inject
    private OptionDefaultRepository optionDefaultRepository;

    @PostConstruct
    public void init() throws IOException {
        types = applicationContext.getBeansWithAnnotation(OptionType.class).values().stream()
                .filter(bean -> bean instanceof OptionTypeBase)
                .map(bean -> (OptionTypeBase) bean)
                .collect(Collectors.toMap(OptionTypeBase::getTypeName, Function.identity()));
        load();
    }

    private void load() throws IOException {
        ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
        List<OptionDescriptor> data = mapper.readValue(
                applicationContext.getResource("classpath:options.yaml").getInputStream(),
                new TypeReference<>() {
                });
        descriptors = data.stream().collect(Collectors.toMap(OptionDescriptor::getName, Function.identity()));
        typeModifiers = data.stream()
                .filter(desc -> desc.getModifiers() != null)
                .filter(desc -> types.get(desc.getType()) != null)
                .collect(Collectors.toMap(
                        OptionDescriptor::getName,
                        desc -> types.get(desc.getType()).parseTypeModifiers(desc.getModifiers())));
    }

    @EventListener(ApplicationReadyEvent.class)
    public void initDefaults() {
        loadDefaults();
        applicationEventPublisher.publishEvent(new OptionsMetadataConfiguredEvent(this));
    }

    private void loadDefaults() {
        Collection<OptionDefault> defaults = optionDefaultRepository.findAll();
        for (OptionDefault def : defaults) {
            OptionDescriptor desc = descriptors.get(def.getName());
            if (desc == null) {
                log.warn("Unknown option: {}", def.getName());
                continue;
            }
            desc.setDefaultValue(def.getValue());
            if (def.getPrivileged() != null) {
                desc.setPrivileged(def.getPrivileged());
            }
        }
    }

    public void reload() throws IOException {
        load();
        loadDefaults();
    }

    public OptionTypeBase getType(String type) {
        OptionTypeBase optionType = types.get(type);
        if (optionType == null) {
            throw new UnknownOptionTypeException(type);
        }
        return optionType;
    }

    private static OptionDescriptor clientDescriptor(String name) {
        OptionDescriptor descriptor = new OptionDescriptor();
        descriptor.setName(name);
        descriptor.setType("string");
        return descriptor;
    }

    public OptionDescriptor getDescriptor(String name) {
        if (name.startsWith(CLIENT_PREFIX)) {
            return clientDescriptor(name);
        }
        OptionDescriptor desc = descriptors.get(name);
        if (desc == null) {
            log.warn("Unknown option: {}", name);
            return null;
        }
        return desc;
    }

    public OptionTypeBase getOptionType(String name) {
        return getType(getDescriptor(name).getType());
    }

    public boolean isInternal(String name) {
        return getDescriptor(name).isInternal();
    }

    public boolean isPrivileged(String name) {
        return getDescriptor(name).isPrivileged();
    }

    public Map<String, OptionDescriptor> getDescriptors() {
        return descriptors;
    }

    public Object getOptionTypeModifiers(String name) {
        return typeModifiers.get(name);
    }

}
