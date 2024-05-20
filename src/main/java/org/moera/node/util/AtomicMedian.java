package org.moera.node.util;

import java.util.concurrent.atomic.AtomicLong;

public class AtomicMedian {

    private final long min;
    private final long max;
    private final long step;

    private final AtomicLong[] counts;

    public AtomicMedian(long min, long max, long step) {
        if (min > max) {
            throw new IllegalArgumentException("Minimal value should be less or equal to the maximum one");
        }
        if (step <= 0) {
            throw new IllegalArgumentException("Step should be positive");
        }

        this.min = min;
        this.max = max;
        this.step = step;

        int cells = (int) Math.ceil((double) (max - min) / step);
        counts = new AtomicLong[cells];
        for (int i = 0; i < cells; i++) {
            counts[i] = new AtomicLong(0);
        }
    }

    public void add(long value) {
        int cell;
        if (value < min) {
            cell = 0;
        } else if (value >= max) {
            cell = counts.length - 1;
        } else {
            cell = (int) ((value - min) / step);
        }
        counts[cell].incrementAndGet();
    }

    public long getMedian() {
        long total = 0;
        for (var count : counts) {
            total += count.get();
        }

        long n = 0;
        for (int i = 0; i < counts.length; i++) {
            n += counts[i].get();
            if (n >= total / 2) {
                return min + i * step + step / 2;
            }
        }
        return max; // this should never happen
    }

    public void clear() {
        for (var count : counts) {
            count.set(0);
        }
    }

}
