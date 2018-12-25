package org.moera.node.option;

@OptionType("int")
public class IntOptionType extends OptionTypeBase {

    @Override
    public Object deserializeValue(String value) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            throw new DeserializeOptionValueException(
                    String.format("Invalid value of type '%s' for option", getTypeName()));
        }
    }

    @Override
    public Integer getInt(Object value) {
        return (Integer) value;
    }

    @Override
    public Long getLong(Object value) {
        return ((Integer) value).longValue();
    }

    @Override
    public Object accept(Object value) {
        if (value instanceof Integer) {
            return value;
        } else if (value instanceof Long
                && ((Long) value) < Integer.MAX_VALUE
                && ((Long) value) > Integer.MIN_VALUE) {
            return ((Long) value).intValue();
        } else {
            return super.accept(value);
        }
    }

}
