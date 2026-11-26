package org.moera.node.option.type;

import java.util.Arrays;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.moera.lib.naming.NodeName;
import org.moera.lib.node.types.SettingTypeModifiers;
import org.springframework.util.ObjectUtils;

@OptionType("string")
public class StringOptionType extends OptionTypeBase {

    @Override
    public StringOptionTypeModifiers parseTypeModifiers(SettingTypeModifiers modifiers) {
        StringOptionTypeModifiers stringMods = new StringOptionTypeModifiers();
        if (modifiers.getMultiline() != null) {
            stringMods.setMultiline(modifiers.getMultiline());
        }
        if (modifiers.getItems() != null) {
            stringMods.setItems(modifiers.getItems());
        }
        stringMods.setNodeName(modifiers.getNodeName());
        stringMods.setSeparator(modifiers.getSeparator());
        return stringMods;
    }

    @Override
    public String getString(Object value) {
        return (String) value;
    }

    @Override
    public Object accept(Object value, Object typeModifiers) {
        if (value == null) {
            return null;
        }

        String valueS = value.toString();
        var modifiers = (StringOptionTypeModifiers) typeModifiers;
        if (modifiers != null && Boolean.TRUE.equals(modifiers.getNodeName())) {
            if (ObjectUtils.isEmpty(modifiers.getSeparator())) {
                valueS = NodeName.expand(valueS.trim());
            } else {
                valueS = Arrays.stream(valueS.split(Pattern.quote(modifiers.getSeparator())))
                    .filter(StringUtils::isNotBlank)
                    .map(String::trim)
                    .map(NodeName::expand)
                    .collect(Collectors.joining(modifiers.getSeparator()));
            }
        }
        return valueS;
    }

}
