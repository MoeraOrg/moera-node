package org.moera.node.util;

import java.sql.Timestamp;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

public class MomentFinder {

    private Function<Long, Boolean> isFree;
    private AtomicInteger nonce = new AtomicInteger(0);

    public MomentFinder(Function<Long, Boolean> isFree) {
        this.isFree = isFree;
    }

    public long find(Timestamp timestamp) {
        int prevM = -1;
        while (true) {
            int m = nonce.getAndIncrement() % 100;
            if (m <= prevM) {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                }
            }
            long moment = Util.toEpochSecond(timestamp) * 100 + m;
            if (isFree.apply(moment)) {
                return moment;
            }
        }

    }

}
