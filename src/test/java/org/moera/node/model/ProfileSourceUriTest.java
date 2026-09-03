package org.moera.node.model;

import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.moera.lib.node.types.ProfileAttributes;
import org.moera.lib.node.types.SettingDescriptor;
import org.moera.lib.node.types.WhoAmI;
import org.moera.lib.node.types.validate.ValidationFailure;
import org.moera.node.data.OptionRepository;
import org.moera.node.global.RequestContextImpl;
import org.moera.node.option.Options;
import org.moera.node.option.OptionsMetadata;
import org.moera.node.option.OptionHookManager;
import org.moera.node.option.type.BoolOptionType;
import org.moera.node.option.type.OptionTypeBase;
import org.moera.node.option.type.StringOptionType;
import org.moera.node.plugin.Plugins;
import org.springframework.test.util.ReflectionTestUtils;

class ProfileSourceUriTest {

    @Test
    void whoAmIReturnsProfileSourceUri() {
        Options options = options("https://example.org/alice");
        RequestContextImpl requestContext = new RequestContextImpl();
        requestContext.setOptions(options);

        WhoAmI whoAmI = WhoAmIiUtil.build(requestContext, null);

        Assertions.assertEquals("https://example.org/alice", whoAmI.getSourceUri());
    }

    @Test
    void sourceUriValidationChecksOnlyLength() {
        ProfileAttributes attributes = new ProfileAttributes();
        attributes.setSourceUri("not a URI");

        Assertions.assertDoesNotThrow(attributes::validate);

        attributes.setSourceUri("x".repeat(1025));
        Assertions.assertThrows(ValidationFailure.class, attributes::validate);
    }

    private static Options options(String sourceUri) {
        SettingDescriptor sourceUriDescriptor = descriptor("profile.source-uri", "string", null);
        SettingDescriptor frozenDescriptor = descriptor("frozen", "bool", "false");

        OptionsMetadata metadata = new OptionsMetadata();
        ReflectionTestUtils.setField(metadata, "types", Map.of(
            "string", (OptionTypeBase) new StringOptionType(),
            "bool", new BoolOptionType()
        ));
        ReflectionTestUtils.setField(metadata, "descriptors", Map.of(
            sourceUriDescriptor.getName(), sourceUriDescriptor,
            frozenDescriptor.getName(), frozenDescriptor
        ));
        ReflectionTestUtils.setField(metadata, "typeModifiers", new HashMap<>());
        ReflectionTestUtils.setField(metadata, "plugins", new Plugins());

        OptionRepository repository = (OptionRepository) Proxy.newProxyInstance(
            OptionRepository.class.getClassLoader(),
            new Class<?>[]{OptionRepository.class},
            (proxy, method, args) -> method.getName().equals("findAllByNodeId") ? List.of() : null
        );
        Options options = new Options(UUID.randomUUID(), metadata, repository, new OptionHookManager());
        @SuppressWarnings("unchecked")
        Map<String, Object> values = (Map<String, Object>) ReflectionTestUtils.getField(options, "values");
        values.put("profile.source-uri", sourceUri);
        return options;
    }

    private static SettingDescriptor descriptor(String name, String type, String defaultValue) {
        SettingDescriptor descriptor = new SettingDescriptor();
        descriptor.setName(name);
        descriptor.setType(type);
        descriptor.setDefaultValue(defaultValue);
        return descriptor;
    }

}
