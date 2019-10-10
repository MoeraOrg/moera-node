package org.moera.node.option.type;

import java.util.UUID;
import java.util.function.Consumer;

import org.moera.node.option.exception.DeserializeOptionValueException;
import org.moera.node.option.exception.UnsuitableOptionValueException;

@OptionType("UUID")
public class UuidOptionType extends OptionTypeBase {

    private UUID parse(String value, Consumer<String> invalidValue) {
        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException e) {
            invalidValue.accept(value);
        }
        return null; // unreachable
    }

    @Override
    public Object deserializeValue(String value) {
        return parse(value, v -> {
            throw new DeserializeOptionValueException(getTypeName(), v);
        });
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
            return parse((String) value, v -> {
                throw new UnsuitableOptionValueException(v);
            });
        }
        return super.accept(value);
    }

}
