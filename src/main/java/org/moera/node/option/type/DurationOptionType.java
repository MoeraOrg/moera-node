package org.moera.node.option.type;

import org.moera.node.option.OptionTypeModifiers;
import org.moera.node.option.exception.DeserializeOptionValueException;
import org.moera.node.option.exception.UnsuitableOptionValueException;
import org.moera.node.util.DurationFormatException;
import org.moera.node.util.ExtendedDuration;
import org.moera.node.util.Util;

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
        if (modifiers == null) {
            return new DurationOptionTypeModifiers();
        }

        DurationOptionTypeModifiers durMods = new DurationOptionTypeModifiers();
        if (modifiers.getMin() != null) {
            durMods.setMinSeconds(parseSeconds(modifiers.getMin()));
        } else {
            durMods.setMinSeconds(0);
        }
        if (modifiers.getMax() != null) {
            durMods.setMaxSeconds(parseSeconds(modifiers.getMax()));
        } else {
            durMods.setMaxSeconds(Long.MAX_VALUE);
        }
        if (modifiers.getNever() != null) {
            Boolean never = Util.toBoolean(modifiers.getNever());
            if (never == null) {
                throw new DeserializeOptionValueException("bool", modifiers.getNever());
            }
            durMods.setNever(never);
        }
        if (modifiers.getAlways() != null) {
            Boolean always = Util.toBoolean(modifiers.getAlways());
            if (always == null) {
                throw new DeserializeOptionValueException("bool", modifiers.getAlways());
            }
            durMods.setAlways(always);
        }
        return durMods;
    }

    @Override
    public String getString(Object value) {
        return (String) value;
    }

    @Override
    public ExtendedDuration getDuration(Object value) {
        return ExtendedDuration.parse((String) value);
    }

    @Override
    public Object accept(Object value, Object typeModifiers) {
        if (value instanceof String) {
            return acceptString((String) value, (DurationOptionTypeModifiers) typeModifiers);
        }
        return super.accept(value);
    }

    private String acceptString(String value, DurationOptionTypeModifiers typeModifiers) {
        ExtendedDuration duration;
        try {
            duration = ExtendedDuration.parse(value);
        } catch (DurationFormatException e) {
            throw new UnsuitableOptionValueException(value);
        }
        if (typeModifiers != null) {
            switch (duration.getZone()) {
                case NEVER:
                    if (!typeModifiers.isNever()) {
                        throw new UnsuitableOptionValueException(value);
                    }
                    break;
                case ALWAYS:
                    if (!typeModifiers.isAlways()) {
                        throw new UnsuitableOptionValueException(value);
                    }
                    break;
                case FIXED:
                default:
                    long seconds = duration.getSeconds();
                    if (seconds < typeModifiers.getMinSeconds() || seconds > typeModifiers.getMaxSeconds()) {
                        throw new UnsuitableOptionValueException(value);
                    }
                    break;
            }
        }
        return value;
    }

}
