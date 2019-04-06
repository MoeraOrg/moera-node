package org.moera.node.option;

import java.sql.Timestamp;

@OptionType("Timestamp")
public class TimestampOptionType extends OptionTypeBase {

    @Override
    public String serializeValue(Object value) {
        return Long.toString(((Timestamp) value).getTime());
    }

    @Override
    public Object deserializeValue(String value) {
        try {
            return new Timestamp(Long.parseLong(value));
        } catch (NumberFormatException e) {
            throw new DeserializeOptionValueException(
                    String.format("Invalid value of type '%s' for option", getTypeName()));
        }
    }

    @Override
    public Long getLong(Object value) {
        return value != null ? ((Timestamp) value).getTime() : null;
    }

    @Override
    public Timestamp getTimestamp(Object value) {
        return (Timestamp) value;
    }

    @Override
    public Object accept(Object value) {
        if (value instanceof Long) {
            return new Timestamp((Long) value);
        }
        if (value instanceof Timestamp) {
            return value;
        }
        return super.accept(value);
    }

}
