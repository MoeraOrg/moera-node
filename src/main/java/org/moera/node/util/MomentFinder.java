package org.moera.node.util;

import java.sql.Timestamp;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

public class MomentFinder {

    private AtomicInteger nonce = new AtomicInteger(0);

    public long find(Function<Long, Boolean> isFree, Timestamp timestamp) {
        long base = Util.toEpochSecond(timestamp) * 100;
        int n = 0;
        while (true) {
            long moment = base + nonce.getAndIncrement() % 100;
            if (isFree.apply(moment)) {
                return moment;
            }
            if (++n >= 100) {
                n = 0;
                base += 100;
            }
        }

    }

}
