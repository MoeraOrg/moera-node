package org.moera.node.friends;

import java.util.Comparator;
import java.util.concurrent.PriorityBlockingQueue;

public class UsageQueue<T> extends PriorityBlockingQueue<Usage<T>> {

    public UsageQueue(int initialCapacity) {
        super(initialCapacity, Comparator.comparing(u -> u.usedAt));
    }

}
