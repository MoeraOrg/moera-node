package org.moera.node.option.type;

import java.sql.Timestamp;
import java.util.function.Consumer;

import org.moera.node.option.exception.DeserializeOptionValueException;
import org.moera.node.option.exception.UnsuitableOptionValueException;
import org.moera.node.util.Util;

@OptionType("Timestamp")
public class TimestampOptionType extends OptionTypeBase {

    private Timestamp parse(String value, Consumer<String> invalidValue) {
        try {
            return Util.toTimestamp(Long.parseLong(value));
        } catch (NumberFormatException e) {
            invalidValue.accept(value);
        }
        return null; // unreachable
    }

    @Override
    public String serializeValue(Object value) {
        return Long.toString(Util.toEpochSecond((Timestamp) value));
    }

    @Override
    public Object deserializeValue(String value) {
        return parse(value, v -> {
            throw new DeserializeOptionValueException(getTypeName(), v);
        });
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
    protected Object accept(Object value) {
        if (value instanceof Long) {
            return Util.toTimestamp((Long) value);
        }
        if (value instanceof Timestamp) {
            return value;
        }
        if (value instanceof String) {
            return parse((String) value, v -> {
                throw new UnsuitableOptionValueException(v);
            });
        }
        return super.accept(value);
    }

}
