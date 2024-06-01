package org.moera.node.util;

import java.sql.Timestamp;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;

public class MomentFinder {

    private final AtomicInteger nonce = new AtomicInteger(0);

    public long find(Predicate<Long> isFree, Timestamp timestamp) {
        return find(isFree, Util.toEpochSecond(timestamp) * 1000, SafeInteger.MAX_VALUE);
    }

    public long find(Predicate<Long> isFree, long momentBase, long momentLimit) {
        int n = 0;
        while (true) {
            long moment = momentBase + nonce.getAndIncrement() % 1000;
            if (moment < momentLimit) {
                if (isFree.test(moment)) {
                    return moment;
                }
            } else {
                int newStart = momentLimit - momentBase >= 200 ? 100 : (int) (momentLimit - momentBase) / 2;
                nonce.getAndAdd(1000 - nonce.get() % 1000 + newStart);
            }
            if (++n >= 1000) {
                n = 0;
                momentBase += 1000;
                if (momentBase >= momentLimit) {
                    momentLimit += 1000;
                    // ignore momentLimit, because 1000 iterations mean that there is no free slot in the given range
                }
            }
        }

    }

}
