package org.moera.node.option;

@OptionType("long")
public class LongOptionType extends OptionTypeBase {

    @Override
    public Object deserializeValue(String value) {
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            throw new DeserializeOptionValueException(
                    String.format("Invalid value of type '%s' for option", getTypeName()));
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
        } else if (value instanceof Long) {
            return value;
        } else {
            return super.accept(value);
        }
    }

}
