package org.moera.node.option.type;

import java.sql.Timestamp;

import org.moera.node.option.exception.DeserializeOptionValueException;
import org.moera.node.util.Util;

@OptionType("Timestamp")
public class TimestampOptionType extends OptionTypeBase {

    @Override
    public String serializeValue(Object value) {
        return Long.toString(Util.toEpochSecond((Timestamp) value));
    }

    @Override
    public Object deserializeValue(String value) {
        try {
            return Util.toTimestamp(Long.parseLong(value));
        } catch (NumberFormatException e) {
            throw new DeserializeOptionValueException(
                    String.format("Invalid value of type '%s' for option", getTypeName()));
        }
    }

    @Override
    public String getString(Object value) {
        return serializeValue(value);
    }

    @Override
    public Long getLong(Object value) {
        return Util.toEpochSecond((Timestamp) value);
    }

    @Override
    public Timestamp getTimestamp(Object value) {
        return (Timestamp) value;
    }

    @Override
    public Object accept(Object value) {
        if (value instanceof Long) {
            return Util.toTimestamp((Long) value);
        }
        if (value instanceof Timestamp) {
            return value;
        }
        return super.accept(value);
    }

}
