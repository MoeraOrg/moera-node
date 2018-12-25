package org.moera.node.option;

import java.security.PrivateKey;
import java.time.Duration;

public abstract class OptionTypeBase {

    protected String getTypeName() {
        OptionType optionType = getClass().getAnnotation(OptionType.class);
        if (optionType == null) {
            return "unknown";
        }
        return optionType.value();
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

    public Integer getInt(Object value) {
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

    public Object accept(Object value) {
        if (value == null) {
            return null;
        }
        throw new UnsuitableOptionValueException(getTypeName(), value.getClass());
    }

}
