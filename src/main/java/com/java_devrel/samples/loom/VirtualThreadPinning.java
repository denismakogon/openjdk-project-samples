package com.java_devrel.samples.loom;

import java.time.Duration;
import java.util.Optional;
import java.util.stream.IntStream;

public class VirtualThreadPinning extends Util {

    static void _runTask(Thread.Builder builder) throws Exception {
        var threads = IntStream.range(0, 10).mapToObj(index -> builder.unstarted(() -> {
            try {
                updatePlatformOrVirtualThreadMap();
                System.out.println(Thread.currentThread());
                updatePlatformOrVirtualThreadMap();
                Thread.sleep(Duration.ofSeconds(1));
                updatePlatformOrVirtualThreadMap();
                System.out.println(Thread.currentThread());
                updatePlatformOrVirtualThreadMap();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        })).toList();

        // mounts a virtual thread to a carrier thread and runs in
        threads.parallelStream().forEach(Thread::start);
        for (Thread thread : threads) {
            thread.join();
        }
    }

    static void runWith_virtual() {
        try {
            _runTask(Thread.ofVirtual());
            printVirtualThreadMap();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    static void runWith_platform() {
        try {
            _runTask(Thread.ofPlatform());
            printPlatformThreadMap();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static void main(String[] args) throws InterruptedException {
        Optional.of(System.getProperty("threadType")).ifPresentOrElse(
                threadType -> runMethod(VirtualThreadPinning.class, "runWith_" + threadType),
                () -> runMethod(VirtualThreadPinning.class, "runWith_platform"));
    }
}
