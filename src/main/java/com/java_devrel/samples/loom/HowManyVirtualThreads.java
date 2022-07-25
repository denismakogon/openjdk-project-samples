package com.java_devrel.samples.loom;

import java.time.Duration;
import java.time.Instant;
import java.util.stream.IntStream;

public class HowManyVirtualThreads extends Util {
    public static void main(String[] args) {
        var threads = IntStream.range(0, 10_000_000).mapToObj(
                i -> Thread.ofVirtual().unstarted(Util::updatePlatformOrVirtualThreadMap)).toList();

        var start = Instant.now();
        threads.parallelStream().forEach(Thread::start);
        for (Thread thread : threads) {
            try {
                thread.join();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
        var stop = Instant.now();

        System.out.println("execution time: " + Duration.between(start, stop).toMillis() + "ms");
        System.out.println("platform threads: " + getVirtualThreadsMap().keySet().size());
    }
}
