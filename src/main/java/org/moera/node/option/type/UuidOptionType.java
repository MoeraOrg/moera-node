package org.moera.node.option.type;

import java.util.UUID;

import org.moera.node.option.exception.DeserializeOptionValueException;

@OptionType("UUID")
public class UuidOptionType extends OptionTypeBase {

    @Override
    public Object deserializeValue(String value) {
        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException e) {
            throw new DeserializeOptionValueException(
                    String.format("Invalid value of type '%s' for option", getTypeName()));
        }
    }

    @Override
    public UUID getUuid(Object value) {
        return (UUID) value;
    }

    @Override
    public Object accept(Object value) {
        if (value instanceof UUID) {
            return value;
        }
        if (value instanceof String) {
            try {
                return UUID.fromString((String) value);
            } catch (IllegalArgumentException e) {
            }
        }
        return super.accept(value);
    }

}
