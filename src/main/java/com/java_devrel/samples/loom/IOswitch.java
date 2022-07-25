package com.java_devrel.samples.loom;

import java.util.stream.IntStream;

public class IOswitch extends Util {
    public static void main(String[] args) throws InterruptedException {
        var threads = IntStream.range(0, 10).mapToObj(
            index -> Thread.ofVirtual().unstarted(() -> {
            try {
                updatePlatformOrVirtualThreadMap();
                System.out.println(Thread.currentThread());
                updatePlatformOrVirtualThreadMap();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        })).toList();

        threads.parallelStream().forEach(Thread::start);
        for (Thread thread : threads) {
            thread.join();
        }

        printVirtualThreadMap();
    }
}
