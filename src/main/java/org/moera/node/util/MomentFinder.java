package org.moera.node.util;

import java.sql.Timestamp;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;

public class MomentFinder {

    private final AtomicInteger nonce = new AtomicInteger(0);

    public long find(Predicate<Long> isFree, Timestamp timestamp) {
        return find(isFree, Util.toEpochSecond(timestamp) * 1000);
    }

    public long find(Predicate<Long> isFree, long momentBase) {
        int n = 0;
        while (true) {
            long moment = momentBase + nonce.getAndIncrement() % 1000;
            if (isFree.test(moment)) {
                return moment;
            }
            if (++n >= 1000) {
                n = 0;
                momentBase += 1000;
            }
        }

    }

}
