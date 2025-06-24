package org.moera.node.option;

import java.io.IOException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.UUID;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import jakarta.annotation.PostConstruct;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import jakarta.inject.Inject;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.moera.lib.node.types.PluginDescription;
import org.moera.lib.node.types.SettingDescriptor;
import org.moera.lib.node.types.principal.Principal;
import org.moera.lib.util.LogUtil;
import org.moera.node.config.Config;
import org.moera.node.data.OptionDefault;
import org.moera.node.data.OptionDefaultRepository;
import org.moera.node.option.exception.UnknownOptionTypeException;
import org.moera.node.option.type.OptionType;
import org.moera.node.option.type.OptionTypeBase;
import org.moera.node.plugin.Plugins;
import org.moera.node.util.ExtendedDuration;
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
    public static final String PLUGIN_PREFIX = "plugin.";

    private static final Logger log = LoggerFactory.getLogger(OptionsMetadata.class);

    private Map<String, OptionTypeBase> types;
    private Map<String, SettingDescriptor> descriptors;
    private final Map<String, SortedMap<String, SettingDescriptor>> pluginDescriptors = new HashMap<>();
    private Map<String, Object> typeModifiers;

    @Inject
    private ApplicationEventPublisher applicationEventPublisher;

    @Inject
    private ApplicationContext applicationContext;

    @Inject
    private Config config;

    @Inject
    private Plugins plugins;

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

    public void loadPlugin(PluginDescription pluginDescription) {
        String pluginPrefix = PLUGIN_PREFIX + pluginDescription.getName() + ".";

        SortedMap<String, SettingDescriptor> descs = new TreeMap<>();
        for (SettingDescriptor desc : pluginDescription.getOptions()) {
            desc.setName(pluginPrefix + desc.getName());
            descs.put(desc.getName(), desc);

            OptionTypeBase type = getType(desc.getType());
            if (desc.getModifiers() != null) {
                typeModifiers.put(desc.getName(), type.parseTypeModifiers(desc.getModifiers()));
            }
        }
        pluginDescriptors.put(pluginDescription.getName(), descs);

        loadDefaults(name -> name.startsWith(pluginPrefix));
    }

    @EventListener(ApplicationReadyEvent.class)
    public void initDefaults() {
        loadDefaults(name -> !name.startsWith(PLUGIN_PREFIX));
        applicationEventPublisher.publishEvent(new OptionsMetadataConfiguredEvent(this));
    }

    private void loadDefaults(Predicate<String> filter) {
        Collection<OptionDefault> defaults = optionDefaultRepository.findAll();
        for (OptionDefault def : defaults) {
            if (filter != null && !filter.test(def.getName())) {
                continue;
            }
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
        loadDefaults(name -> !name.startsWith(PLUGIN_PREFIX));
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
        SettingDescriptor desc;
        String pluginName = getPluginName(name);
        if (pluginName != null) {
            desc = pluginDescriptors.getOrDefault(pluginName, Collections.emptySortedMap()).get(name);
        } else {
            desc = descriptors.get(name);
        }
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
        return descriptor != null ? mapper.map(descriptor.getDefaultValue(), getType(descriptor.getType())) : null;
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

    private Map<String, SettingDescriptor> getDescriptors() {
        return descriptors;
    }

    public Map<String, SettingDescriptor> getPluginDescriptors(String pluginName) {
        return pluginDescriptors.get(pluginName);
    }

    public List<SettingDescriptor> getDescriptorsForNode(UUID nodeId) {
        List<SettingDescriptor> list = new ArrayList<>(getDescriptors().values());
        plugins.getNames(nodeId).forEach(pluginName -> list.addAll(getPluginDescriptors(pluginName).values()));
        return list;
    }

    public Object getOptionTypeModifiers(String name) {
        return typeModifiers.get(name);
    }

    private static String getPluginName(String optionName) {
        if (!optionName.startsWith(PLUGIN_PREFIX)) {
            return null;
        }
        int pos = optionName.indexOf('.', PLUGIN_PREFIX.length());
        if (pos < 0) {
            return null;
        }
        return optionName.substring(PLUGIN_PREFIX.length(), pos);
    }

    public SecretKey getEncryptionKey() {
        String key = config.getEncryptionKey();
        if (key == null) {
            return null;
        }
        return new SecretKeySpec(Base64.getDecoder().decode(key), "AES");
    }

}
