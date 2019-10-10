package org.moera.node.option.type;

import org.moera.node.option.exception.DeserializeOptionValueException;

@OptionType("bool")
public class BoolOptionType extends OptionTypeBase {

    @Override
    public Object deserializeValue(String value) {
        if ("true".equalsIgnoreCase(value) || "yes".equalsIgnoreCase(value) || "1".equals(value)) {
            return true;
        }
        if ("false".equalsIgnoreCase(value) || "no".equalsIgnoreCase(value) || "0".equals(value)) {
            return false;
        }
        throw new DeserializeOptionValueException(
                String.format("Invalid value of type '%s' for option", getTypeName()));
    }

    @Override
    public Boolean getBool(Object value) {
        return (Boolean) value;
    }

    @Override
    public Integer getInt(Object value) {
        return ((Boolean) value) ? 1 : 0;
    }

    @Override
    public Long getLong(Object value) {
        return ((Boolean) value) ? 1L : 0;
    }

    @Override
    public Object accept(Object value) {
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
            return deserializeValue((String) value);
        }
        return super.accept(value);
    }

}
