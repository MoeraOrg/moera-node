package org.moera.node.util;

import java.sql.Timestamp;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;

public class MomentFinder {

    private AtomicInteger nonce = new AtomicInteger(0);

    public long find(Predicate<Long> isFree, Timestamp timestamp) {
        long base = Util.toEpochSecond(timestamp) * 1000;
        int n = 0;
        while (true) {
            long moment = base + nonce.getAndIncrement() % 1000;
            if (isFree.test(moment)) {
                return moment;
            }
            if (++n >= 1000) {
                n = 0;
                base += 1000;
            }
        }

    }

}
