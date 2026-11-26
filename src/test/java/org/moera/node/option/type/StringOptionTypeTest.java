package org.moera.node.option.type;

import java.io.InputStream;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.moera.lib.naming.NodeNameParsingException;
import org.moera.lib.node.types.SettingDescriptor;
import org.moera.node.model.SettingMetaInfoUtil;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.dataformat.yaml.YAMLFactory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StringOptionTypeTest {

    private final StringOptionType optionType = new StringOptionType();

    @Test
    void acceptReturnsNullForNullValue() {
        assertNull(optionType.accept(null, null));
        assertNull(optionType.accept(null, modifiers(true, ",")));
    }

    @Test
    void acceptConvertsValueToStringWithoutModifiers() {
        assertEquals("42", optionType.accept(42, null));
    }

    @ParameterizedTest
    @CsvSource(value = {"false", "NULL"}, nullValues = "NULL")
    void acceptPreservesTextWhenNodeNameIsDisabled(Boolean nodeName) {
        assertEquals(" alice, bob_2 ", optionType.accept(" alice, bob_2 ", modifiers(nodeName, ",")));
    }

    @ParameterizedTest
    @NullAndEmptySource
    void acceptNormalizesSingleNodeName(String separator) {
        StringOptionTypeModifiers modifiers = modifiers(true, separator);

        assertEquals("alice_0", optionType.accept(" alice ", modifiers));
        assertEquals("bob_2", optionType.accept(" bob_2 ", modifiers));
        assertEquals("", optionType.accept(" \t ", modifiers));
    }

    @ParameterizedTest
    @CsvSource(value = {
        ", : 'alice,bob_2,carol' : 'alice_0,bob_2,carol_0'",
        ", : ' alice , bob_2, carol ' : 'alice_0,bob_2,carol_0'",
        "; : 'alice;bob_2' : 'alice_0;bob_2'",
        "| : 'alice|bob_2' : 'alice_0|bob_2'",
        "' ' : '  alice   bob_2  ' : 'alice_0 bob_2'"
    }, delimiter = ':')
    void acceptNormalizesNodeNameList(String separator, String value, String expected) {
        assertEquals(expected, optionType.accept(value, modifiers(true, separator)));
    }

    @Test
    void acceptRejectsInvalidNodeNameGeneration() {
        assertThrows(NodeNameParsingException.class, () -> optionType.accept("alice_invalid", modifiers(true, null)));
    }

    @ParameterizedTest
    @CsvSource({"recommendations.source,", "search.known, ','"})
    void nodeNameModifiersAreAvailableInOptionsAndApiMetadata(String optionName, String separator) throws Exception {
        ObjectMapper yamlMapper = new ObjectMapper(new YAMLFactory());
        List<SettingDescriptor> descriptors;
        try (InputStream input = getClass().getResourceAsStream("/options.yaml")) {
            descriptors = yamlMapper.readValue(input, new TypeReference<>() {
            });
        }
        SettingDescriptor descriptor = descriptors.stream()
            .filter(option -> optionName.equals(option.getName()))
            .findFirst()
            .orElseThrow();
        assertNotNull(descriptor.getModifiers());

        StringOptionTypeModifiers parsed = optionType.parseTypeModifiers(descriptor.getModifiers());
        assertEquals(Boolean.TRUE, parsed.getNodeName());
        assertEquals(separator, parsed.getSeparator());

        ObjectMapper jsonMapper = new ObjectMapper();
        JsonNode metadata = jsonMapper.valueToTree(SettingMetaInfoUtil.build(descriptor));
        JsonNode modifiers = metadata.path("modifiers");
        JsonNode expectedSeparator = separator != null ? jsonMapper.valueToTree(separator) : null;
        assertTrue(modifiers.path("nodeName").asBoolean());
        assertEquals(expectedSeparator, modifiers.get("separator"));
    }

    private static StringOptionTypeModifiers modifiers(Boolean nodeName, String separator) {
        StringOptionTypeModifiers modifiers = new StringOptionTypeModifiers();
        modifiers.setNodeName(nodeName);
        modifiers.setSeparator(separator);
        return modifiers;
    }

}
