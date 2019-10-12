package org.moera.node.option.type;

import java.time.Duration;

import org.moera.commons.util.DurationFormatException;
import org.moera.commons.util.Util;
import org.moera.node.option.OptionTypeModifiers;
import org.moera.node.option.exception.DeserializeOptionValueException;
import org.moera.node.option.exception.UnsuitableOptionValueException;

@OptionType("Duration")
public class DurationOptionType extends OptionTypeBase {

    private long parseSeconds(String value) {
        try {
            return Util.toDuration(value).getSeconds();
        } catch (DurationFormatException e) {
            throw new DeserializeOptionValueException(getTypeName(), value);
        }
    }

    @Override
    public DurationOptionTypeModifiers parseTypeModifiers(OptionTypeModifiers modifiers) {
        DurationOptionTypeModifiers durMods = new DurationOptionTypeModifiers();
        if (modifiers != null && modifiers.getMin() != null) {
            durMods.setMinSeconds(parseSeconds(modifiers.getMin()));
        } else {
            durMods.setMinSeconds(0);
        }
        if (modifiers != null && modifiers.getMax() != null) {
            durMods.setMaxSeconds(parseSeconds(modifiers.getMax()));
        } else {
            durMods.setMaxSeconds(Long.MAX_VALUE);
        }
        return durMods;
    }

    @Override
    public String getString(Object value) {
        return (String) value;
    }

    @Override
    public Duration getDuration(Object value) {
        return Util.toDuration((String) value);
    }

    @Override
    public Object accept(Object value, Object typeModifiers) {
        if (value instanceof String) {
            return acceptString((String) value, (DurationOptionTypeModifiers) typeModifiers);
        }
        return super.accept(value);
    }

    private String acceptString(String value, DurationOptionTypeModifiers typeModifiers) {
        long seconds;
        try {
            seconds = Util.toDuration(value).getSeconds();
        } catch (DurationFormatException e) {
            throw new UnsuitableOptionValueException(value);
        }
        if (typeModifiers != null) {
            if (seconds < typeModifiers.getMinSeconds() || seconds > typeModifiers.getMaxSeconds()) {
                throw new UnsuitableOptionValueException(value);
            }
        }
        return value;
    }

}
