package org.moera.node.option;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.annotation.PostConstruct;
import javax.inject.Inject;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

@Component
public class OptionsMetadata {

    private static Logger log = LoggerFactory.getLogger(OptionsMetadata.class);

    private Map<String, OptionTypeBase> types;
    private Map<String, OptionDescriptor> descriptors;

    @Inject
    private ApplicationContext applicationContext;

    @PostConstruct
    public void init() throws IOException {
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
    }

    public OptionTypeBase getType(String type) {
        OptionTypeBase optionType = types.get(type);
        if (optionType == null) {
            throw new UnknownOptionTypeException(type);
        }
        return optionType;
    }

    public OptionDescriptor getDescriptor(String name) {
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

    public Map<String, OptionDescriptor> getDescriptors() {
        return descriptors;
    }

}
