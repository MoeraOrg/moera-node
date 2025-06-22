package org.moera.node.option.type;

import org.moera.lib.node.types.SettingTypeModifiers;

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
        return stringMods;
    }

    @Override
    public String getString(Object value) {
        return (String) value;
    }

    protected Object accept(Object value) {
        return value.toString();
    }

}
