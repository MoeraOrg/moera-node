package org.moera.node.util;

import java.time.Duration;

public class ExtendedDuration {

    public enum Zone {
        FIXED, NEVER, ALWAYS
    }

    public static final ExtendedDuration NEVER = new ExtendedDuration(null, Zone.NEVER);
    public static final ExtendedDuration ALWAYS = new ExtendedDuration(null, Zone.ALWAYS);

    private Duration duration;
    private Zone zone;

    protected ExtendedDuration() {
        this(null, Zone.NEVER);
    }

    public ExtendedDuration(Duration duration) {
        this(duration, Zone.FIXED);
    }

    private ExtendedDuration(Duration duration, Zone zone) {
        this.duration = duration;
        this.zone = zone;
    }

    public Duration getDuration() {
        return duration;
    }

    public long getSeconds() {
        return duration.getSeconds();
    }

    public Zone getZone() {
        return zone;
    }

    public boolean isNever() {
        return getZone() == Zone.NEVER;
    }

    public boolean isAlways() {
        return getZone() == Zone.ALWAYS;
    }

    public boolean isFixed() {
        return getZone() == Zone.FIXED;
    }

    public static ExtendedDuration parse(String value) {
        if (value.equals("never")) {
            return NEVER;
        }
        if (value.equals("always")) {
            return ALWAYS;
        }
        return new ExtendedDuration(Util.toDuration(value));
    }

}
