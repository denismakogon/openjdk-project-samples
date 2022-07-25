package com.java_devrel.samples.loom;

import java.lang.invoke.MethodHandle;
import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.IntStream;

public class Synchronization extends Util {

    static final ReentrantLock lock = new ReentrantLock();

    static void synchronizedTask() {
        synchronized (lock) {
            try {
                Thread.sleep(Duration.ofSeconds(1));
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }

    static void reentrantLockTask() {
        lock.lock();
        try {
            Thread.sleep(Duration.ofSeconds(1));
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } finally {
            lock.unlock();
        }
    }

    private static void runExample(MethodHandle task$mh, int times) {
        var threads = IntStream.range(0, 10).mapToObj(index -> Thread.ofVirtual().unstarted(() -> {
            try {
                for (int i = 0; i < times; i++) {
                    updateVirtualThreadMap();
                    task$mh.invoke();
                }
            } catch (Throwable e) {
                throw new RuntimeException(e);
            }
        })).toList();

        threads.parallelStream().forEach(Thread::start);
        for (Thread thread : threads) {
            try {
                thread.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
                System.exit(1);
            }
        }

        printVirtualThreadMap();
    }

    public static void main(String[] args) throws InterruptedException {
        int times = 3;
        Optional.of(System.getProperty("task")).ifPresentOrElse(
                t -> runExample(getMethod(Synchronization.class, t), times),
                () -> runExample(getMethod(Synchronization.class, "synchronizedTask"), times)
        );
    }
}
