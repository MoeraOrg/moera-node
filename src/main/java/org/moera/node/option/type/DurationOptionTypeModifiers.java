package org.moera.node.option.type;

public class DurationOptionTypeModifiers {

    private long minSeconds;
    private long maxSeconds;
    private boolean never;
    private boolean always;

    public long getMinSeconds() {
        return minSeconds;
    }

    public void setMinSeconds(long minSeconds) {
        this.minSeconds = minSeconds;
    }

    public long getMaxSeconds() {
        return maxSeconds;
    }

    public void setMaxSeconds(long maxSeconds) {
        this.maxSeconds = maxSeconds;
    }

    public boolean isNever() {
        return never;
    }

    public void setNever(boolean never) {
        this.never = never;
    }

    public boolean isAlways() {
        return always;
    }

    public void setAlways(boolean always) {
        this.always = always;
    }

}
