package org.moera.node.option;

import java.io.IOException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import jakarta.annotation.PostConstruct;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import jakarta.inject.Inject;

import org.moera.lib.node.types.SettingDescriptor;
import org.moera.lib.node.types.principal.Principal;
import org.moera.lib.util.LogUtil;
import org.moera.node.config.Config;
import org.moera.node.data.OptionDefault;
import org.moera.node.data.OptionDefaultRepository;
import org.moera.node.option.exception.UnknownOptionTypeException;
import org.moera.node.option.type.OptionType;
import org.moera.node.option.type.OptionTypeBase;
import org.moera.node.util.ExtendedDuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.dataformat.yaml.YAMLFactory;

@Component
public class OptionsMetadata {

    public static final String CLIENT_PREFIX = "client.";

    private static final Logger log = LoggerFactory.getLogger(OptionsMetadata.class);

    private Map<String, OptionTypeBase> types;
    private Map<String, SettingDescriptor> descriptors;
    private Map<String, Object> typeModifiers;

    @Inject
    private ApplicationEventPublisher applicationEventPublisher;

    @Inject
    private ApplicationContext applicationContext;

    @Inject
    private Config config;

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
        List<SettingDescriptor> data = mapper.readValue(
            applicationContext.getResource("classpath:options.yaml").getInputStream(),
            new TypeReference<>() {
            }
        );
        descriptors = data.stream().collect(Collectors.toMap(SettingDescriptor::getName, Function.identity()));
        for (var option : config.getOptions()) {
            SettingDescriptor descriptor = descriptors.get(option.getName());
            if (descriptor == null) {
                log.warn("Unknown option referenced in the config file: {}", LogUtil.format(option.getName()));
                continue;
            }
            descriptor.setDefaultValue(option.getDefaultValue());
        }
        typeModifiers = data.stream()
            .filter(desc -> desc.getModifiers() != null)
            .filter(desc -> types.get(desc.getType()) != null)
            .collect(Collectors.toMap(
                SettingDescriptor::getName,
                desc -> types.get(desc.getType()).parseTypeModifiers(desc.getModifiers())
            ));
    }

    @EventListener(ApplicationReadyEvent.class)
    public void initDefaults() {
        loadDefaults();
        applicationEventPublisher.publishEvent(new OptionsMetadataConfiguredEvent(this));
    }

    private void loadDefaults() {
        Collection<OptionDefault> defaults = optionDefaultRepository.findAll();
        for (OptionDefault def : defaults) {
            SettingDescriptor desc = getDescriptor(def.getName());
            if (desc == null) {
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

    private static SettingDescriptor clientDescriptor(String name) {
        SettingDescriptor descriptor = new SettingDescriptor();
        descriptor.setName(name);
        descriptor.setType("string");
        return descriptor;
    }

    public SettingDescriptor getDescriptor(String name) {
        if (name.startsWith(CLIENT_PREFIX)) {
            return clientDescriptor(name);
        }
        SettingDescriptor desc = descriptors.get(name);
        if (desc == null) {
            log.warn("Unknown option: {}", name);
            return null;
        }
        return desc;
    }

    public OptionTypeBase getOptionType(String name) {
        SettingDescriptor descriptor = getDescriptor(name);
        return descriptor != null ? getType(descriptor.getType()) : null;
    }

    private <T> T forName(String name, OptionMapper<T> mapper) {
        SettingDescriptor descriptor = getDescriptor(name);
        if (descriptor == null) {
            return null;
        }
        OptionTypeBase optionType = getType(descriptor.getType());
        return mapper.map(optionType.deserializeValue(descriptor.getDefaultValue()), optionType);
    }

    private Object getDefault(String name) {
        return forName(name, (value, optionType) -> value);
    }

    public String getDefaultString(String name) {
        return forName(name, (value, optionType) -> optionType.getString(value));
    }

    public Boolean getDefaultBool(String name) {
        return forName(name, (value, optionType) -> optionType.getBool(value));
    }

    public Integer getDefaultInt(String name) {
        return forName(
            name,
            (value, optionType) -> optionType.getInt(value, getOptionTypeModifiers(name))
        );
    }

    public Long getDefaultLong(String name) {
        return forName(name, (value, optionType) -> optionType.getLong(value));
    }

    public PrivateKey getDefaultPrivateKey(String name) {
        return forName(name, (value, optionType) -> optionType.getPrivateKey(value));
    }

    public PublicKey getDefaultPublicKey(String name) {
        return forName(name, (value, optionType) -> optionType.getPublicKey(value));
    }

    public ExtendedDuration getDefaultDuration(String name) {
        return forName(name, (value, optionType) -> optionType.getDuration(value));
    }

    public UUID getDefaultUuid(String name) {
        return forName(name, (value, optionType) -> optionType.getUuid(value));
    }

    public Timestamp getDefaultTimestamp(String name) {
        return forName(name, (value, optionType) -> optionType.getTimestamp(value));
    }

    public Principal getDefaultPrincipal(String name) {
        return forName(name, (value, optionType) -> optionType.getPrincipal(value));
    }

    public boolean isInternal(String name) {
        SettingDescriptor descriptor = getDescriptor(name);
        return descriptor != null && Boolean.TRUE.equals(descriptor.getInternal());
    }

    public boolean isPrivileged(String name) {
        SettingDescriptor descriptor = getDescriptor(name);
        return descriptor != null && Boolean.TRUE.equals(descriptor.getPrivileged());
    }

    public boolean isEncrypted(String name) {
        SettingDescriptor descriptor = getDescriptor(name);
        return descriptor != null && Boolean.TRUE.equals(descriptor.getEncrypted());
    }

    public List<SettingDescriptor> getDescriptors() {
        return new ArrayList<>(descriptors.values());
    }

    public Object getOptionTypeModifiers(String name) {
        return typeModifiers.get(name);
    }

    public SecretKey getEncryptionKey() {
        String key = config.getEncryptionKey();
        if (key == null) {
            return null;
        }
        return new SecretKeySpec(Base64.getDecoder().decode(key), "AES");
    }

}
