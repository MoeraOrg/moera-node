package org.moera.node.global;

import java.util.concurrent.atomic.AtomicLong;

public class RequestStatistics {

    public final AtomicLong minDuration = new AtomicLong(Long.MAX_VALUE);
    public final AtomicLong maxDuration = new AtomicLong(0);
    public final AtomicLong totalDuration = new AtomicLong(0);
    public final AtomicLong count = new AtomicLong(0);

}
