package org.moera.node.util;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class ParametrizedLock<K> {

    private static final class CountedLock {

        public int counter;
        public Lock lock = new ReentrantLock();

    }

    private final Map<K, CountedLock> locks = new HashMap<>();
    private final Object mapLock = new Object();

    public void lock(K key) {
        CountedLock lock;
        synchronized (mapLock) {
            lock = locks.computeIfAbsent(key, k -> new CountedLock());
            lock.counter++;
        }
        lock.lock.lock();
    }

    public void unlock(K key) {
        CountedLock lock;
        synchronized (mapLock) {
            lock = locks.get(key);
            if (lock == null) {
                throw new LockUnderflowException(key.toString());
            }
            lock.counter--;
            if (lock.counter < 0) {
                throw new LockUnderflowException(key.toString());
            }
            if (lock.counter == 0) {
                locks.remove(key);
            }
        }
        lock.lock.unlock();
    }

    public static void main(String[] args) {
        BlockingQueue<Integer> queue = new LinkedBlockingQueue<>();
        ParametrizedLock<Integer> locks = new ParametrizedLock<>();
        long startTime = System.currentTimeMillis();

        new Thread(() -> {
            generate(locks, queue, startTime, 10);
        }).start();

        new Thread(() -> {
            generate(locks, queue, startTime, 20);
        }).start();

        try {
            int[] current = new int [2];
            while (System.currentTimeMillis() - startTime < 600000) {
                Integer code = queue.poll(3, TimeUnit.SECONDS);
                if (code == null) {
                    return;
                }
                int resource = code / 100;
                int thread = code / 10 % 10;
                int phase = code % 10;
                System.out.printf("%d: thread %d resource %d %s%n", System.currentTimeMillis() - startTime, thread,
                        resource, phase == 0 ? "begin" : "end");

                if (phase == 0) {
                    if (current[resource] != 0) {
                        System.out.println("Error!");
                        System.exit(1);
                    } else {
                        current[resource] = thread;
                    }
                } else {
                    if (current[resource] != thread) {
                        System.out.println("Error!");
                        System.exit(1);
                    } else {
                        current[resource] = 0;
                    }
                }
            }
        } catch (InterruptedException e) {
        }
    }

    private static void generate(ParametrizedLock<Integer> locks, BlockingQueue<Integer> queue, long startTime,
                                 int code) {
        try {
            while (System.currentTimeMillis() - startTime < 600000) {
                int resource = (int) (Math.random() * 2);
                locks.lock(resource);
                queue.put(resource * 100 + code);
                Thread.sleep((long) (Math.random() * 1000));
                queue.put(resource * 100 + code + 1);
                locks.unlock(resource);
                Thread.sleep((long) (Math.random() * 1000));
            }
        } catch (InterruptedException e) {
        }
    }

}
