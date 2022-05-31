package com.openjdk.samples.loom;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;

public class CalculateThreadMax {

    public static void main(String...args) {
        var counter = new AtomicInteger(0);
        try{
            while (true) {
                Thread.ofPlatform().start(() -> {
                    counter.incrementAndGet();
                    var threadID = Thread.currentThread().threadId();
                    if (threadID == 1_000_001L) {
                        System.out.println("No. of threads is already beyond 1M!");
                    }
                    if (threadID == 2_000_001L) {
                        System.out.println("No. of threads is already beyond 2M and 1! That's enough!");
                        System.exit(0);
                    }

                    try {
                        Thread.sleep(Duration.ofSeconds(threadID));
                    } catch (Exception e) {
                        System.out.println("MAX threads: " + (counter.get() - 1));
                        System.exit(0);
                    }
                });
            }
        } catch (OutOfMemoryError ex) {
            System.out.println("MAX threads: " + (counter.get() - 1));
            System.exit(0);
        }
    }
}
