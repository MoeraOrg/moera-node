package org.moera.node.option.type;

import java.util.UUID;

import org.moera.node.option.exception.DeserializeOptionValueException;
import org.moera.node.option.exception.UnsuitableOptionValueException;
import org.moera.node.util.Util;

@OptionType("UUID")
public class UuidOptionType extends OptionTypeBase {

    @Override
    public Object deserializeValue(String value) {
        return Util.uuid(value).orElseThrow(() -> new DeserializeOptionValueException(getTypeName(), value));
    }

    @Override
    public UUID getUuid(Object value) {
        return (UUID) value;
    }

    @Override
    protected Object accept(Object value) {
        if (value instanceof UUID) {
            return value;
        }
        if (value instanceof String v) {
            return Util.uuid(v).orElseThrow(() -> new UnsuitableOptionValueException(v));
        }
        return super.accept(value);
    }

}
