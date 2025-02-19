package org.moera.node.option.type;

import java.util.function.Consumer;

import org.moera.lib.node.types.SettingTypeModifiers;
import org.moera.node.option.exception.DeserializeOptionValueException;
import org.moera.node.option.exception.UnsuitableOptionValueException;

@OptionType("int")
public class IntOptionType extends OptionTypeBase {

    @Override
    public IntOptionTypeModifiers parseTypeModifiers(SettingTypeModifiers modifiers) {
        IntOptionTypeModifiers intMods = new IntOptionTypeModifiers();
        if (modifiers != null && modifiers.getMin() != null) {
            intMods.setMin((Long) deserializeValue(modifiers.getMin()));
        } else {
            intMods.setMin(Long.MIN_VALUE);
        }
        if (modifiers != null && modifiers.getMax() != null) {
            intMods.setMax((Long) deserializeValue(modifiers.getMax()));
        } else {
            intMods.setMax(Long.MAX_VALUE);
        }
        return intMods;
    }

    private long parse(String value, Consumer<String> invalidValue) {
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            invalidValue.accept(value);
        }
        return 0; // unreachable
    }

    @Override
    public Object deserializeValue(String value) {
        return parse(value, v -> {
            throw new DeserializeOptionValueException(getTypeName(), v);
        });
    }

    @Override
    public Integer getInt(Object value, Object typeModifiers) {
        Long longValue = (Long) value;
        if (longValue == null) {
            return null;
        }
        if (typeModifiers == null || !((IntOptionTypeModifiers) typeModifiers).isFitsIntoInt()) {
            return super.getInt(value, typeModifiers);
        }
        return longValue.intValue();
    }

    @Override
    public Long getLong(Object value) {
        return (Long) value;
    }

    @Override
    public Object accept(Object value, Object typeModifiers) {
        if (value instanceof Integer) {
            return ((Integer) value).longValue();
        }
        if (value instanceof Long) {
            return value;
        }
        if (value instanceof String) {
            return acceptString((String) value, (IntOptionTypeModifiers) typeModifiers);
        }
        return super.accept(value);
    }

    private Object acceptString(String value, IntOptionTypeModifiers typeModifiers) {
        long longValue = parse(value, v -> {
            throw new UnsuitableOptionValueException(v);
        });
        if (typeModifiers != null) {
            if (longValue < typeModifiers.getMin() || longValue > typeModifiers.getMax()) {
                throw new UnsuitableOptionValueException(value);
            }
        }
        return longValue;
    }

}
