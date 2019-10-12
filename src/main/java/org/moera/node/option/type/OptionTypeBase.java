package org.moera.node.option.type;

import java.security.PrivateKey;
import java.sql.Timestamp;
import java.time.Duration;
import java.util.UUID;

import org.moera.node.option.OptionTypeModifiers;
import org.moera.node.option.exception.InvalidOptionTypeConversionException;
import org.moera.node.option.exception.UnsuitableOptionTypeException;

public abstract class OptionTypeBase {

    public String getTypeName() {
        OptionType optionType = getClass().getAnnotation(OptionType.class);
        if (optionType == null) {
            return "unknown";
        }
        return optionType.value();
    }

    public Object parseTypeModifiers(OptionTypeModifiers modifiers) {
        return modifiers;
    }

    public String serializeValue(Object value) {
        return value.toString();
    }

    public Object deserializeValue(String value) {
        return value;
    }

    public String getString(Object value) {
        return value.toString();
    }

    public Boolean getBool(Object value) {
        throw new InvalidOptionTypeConversionException(getTypeName(), Boolean.class);
    }

    public Integer getInt(Object value, Object typeModifiers) {
        throw new InvalidOptionTypeConversionException(getTypeName(), Integer.class);
    }

    public Long getLong(Object value) {
        throw new InvalidOptionTypeConversionException(getTypeName(), Long.class);
    }

    public PrivateKey getPrivateKey(Object value) {
        throw new InvalidOptionTypeConversionException(getTypeName(), PrivateKey.class);
    }

    public Duration getDuration(Object value) {
        throw new InvalidOptionTypeConversionException(getTypeName(), Duration.class);
    }

    public UUID getUuid(Object value) {
        throw new InvalidOptionTypeConversionException(getTypeName(), UUID.class);
    }

    public Timestamp getTimestamp(Object value) {
        throw new InvalidOptionTypeConversionException(getTypeName(), Timestamp.class);
    }

    public Object accept(Object value, Object typeModifiers) {
        return accept(value);
    }

    protected Object accept(Object value) {
        if (value == null) {
            return null;
        }
        throw new UnsuitableOptionTypeException(getTypeName(), value.getClass());
    }

}
