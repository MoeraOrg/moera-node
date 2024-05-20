package org.moera.node.global;

import java.util.concurrent.atomic.AtomicLong;

import org.moera.node.util.AtomicMedian;

public class RequestStatistics {

    public final AtomicLong minDuration = new AtomicLong(Long.MAX_VALUE);
    public final AtomicLong maxDuration = new AtomicLong(0);
    public final AtomicLong totalDuration = new AtomicLong(0);
    public final AtomicMedian medianDuration = new AtomicMedian(0, 5000, 20);
    public final AtomicLong count = new AtomicLong(0);
    public final AtomicLong slowCount = new AtomicLong(0);

}
