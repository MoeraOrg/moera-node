package org.moera.node.push;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class PushSource {

    private static final int QUEUE_CAPACITY = 20;

    private BlockingQueue<PushPacket> queue;
    private Thread thread;

    public PushSource() {
        queue = new LinkedBlockingQueue<>(QUEUE_CAPACITY);
        thread = Thread.currentThread();
    }

    public BlockingQueue<PushPacket> getQueue() {
        return queue;
    }

    public void setQueue(BlockingQueue<PushPacket> queue) {
        this.queue = queue;
    }

    public Thread getThread() {
        return thread;
    }

    public void setThread(Thread thread) {
        this.thread = thread;
    }

}
