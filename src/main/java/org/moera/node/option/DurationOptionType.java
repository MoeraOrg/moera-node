package org.moera.node.option;

import java.time.Duration;

import org.moera.commons.util.DurationFormatException;
import org.moera.commons.util.Util;

@OptionType("Duration")
public class DurationOptionType extends OptionTypeBase {

    @Override
    public String getString(Object value) {
        return (String) value;
    }

    @Override
    public Duration getDuration(Object value) {
        return Util.toDuration((String) value);
    }

    @Override
    public Object accept(Object value) {
        if (value instanceof String) {
            try {
                Util.toDuration((String) value);
                return value;
            } catch (DurationFormatException e) {
            }
        }
        return super.accept(value);
    }

}
