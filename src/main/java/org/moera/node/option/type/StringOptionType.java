package org.moera.node.option.type;

import org.moera.node.option.OptionTypeModifiers;
import org.moera.node.option.exception.DeserializeOptionValueException;
import org.moera.node.util.Util;

@OptionType("string")
public class StringOptionType extends OptionTypeBase {

    @Override
    public StringOptionTypeModifiers parseTypeModifiers(OptionTypeModifiers modifiers) {
        StringOptionTypeModifiers stringMods = new StringOptionTypeModifiers();
        if (modifiers.getMultiline() != null) {
            Boolean multiline = Util.toBoolean(modifiers.getMultiline());
            if (multiline == null) {
                throw new DeserializeOptionValueException("bool", modifiers.getMultiline());
            }
            stringMods.setMultiline(multiline);
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
