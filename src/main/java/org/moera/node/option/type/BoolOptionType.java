package org.moera.node.option.type;

import java.util.function.Consumer;

import org.moera.node.option.exception.DeserializeOptionValueException;
import org.moera.node.option.exception.UnsuitableOptionValueException;
import org.moera.node.util.Util;

@OptionType("bool")
public class BoolOptionType extends OptionTypeBase {

    private boolean parse(String value, Consumer<String> invalidValue) {
        Boolean boolValue = Util.toBoolean(value);
        if (boolValue != null) {
            return boolValue;
        }
        invalidValue.accept(value);
        return false; // unreachable
    }

    @Override
    public Object deserializeValue(String value) {
        return parse(value, v -> {
            throw new DeserializeOptionValueException(getTypeName(), v);
        });
    }

    @Override
    public Boolean getBool(Object value) {
        return (Boolean) value;
    }

    @Override
    public Integer getInt(Object value, Object typeModifiers) {
        return ((Boolean) value) ? 1 : 0;
    }

    @Override
    public Long getLong(Object value) {
        return ((Boolean) value) ? 1L : 0;
    }

    @Override
    protected Object accept(Object value) {
        if (value instanceof Boolean) {
            return value;
        }
        if (value instanceof Integer) {
            return ((Integer) value) != 0;
        }
        if (value instanceof Long) {
            return ((Long) value) != 0;
        }
        if (value instanceof String) {
            return parse((String) value, v -> {
                throw new UnsuitableOptionValueException(v);
            });
        }
        return super.accept(value);
    }

}
