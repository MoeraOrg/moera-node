package org.moera.node.option.type;

import org.moera.node.option.exception.DeserializeOptionValueException;

@OptionType("long")
public class LongOptionType extends OptionTypeBase {

    @Override
    public Object deserializeValue(String value) {
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            throw new DeserializeOptionValueException(getTypeName(), value);
        }
    }

    @Override
    public Long getLong(Object value) {
        return (Long) value;
    }

    @Override
    public Object accept(Object value) {
        if (value instanceof Integer) {
            return ((Integer) value).longValue();
        }
        if (value instanceof Long) {
            return value;
        }
        return super.accept(value);
    }

}
