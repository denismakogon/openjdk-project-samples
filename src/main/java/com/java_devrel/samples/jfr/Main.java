package com.java_devrel.samples.jfr;

import jdk.jfr.consumer.RecordingStream;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Optional;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

public class Main {

    public static void main(String... args) throws Exception {
        var kernNumTaskThreads = new AtomicInteger(1000);
        Optional.ofNullable(System.getProperty("sysctl.kern.num_taskthreads")).ifPresentOrElse(
                prop -> kernNumTaskThreads.set(Integer.parseInt(prop)), () -> {}
        );

        // we'll leave around 10% of all threads per process to the JVM.
        var maxThreads = Double.valueOf(kernNumTaskThreads.get() * 0.9).intValue();
        Callable<Void> task = () -> {
            var val = ThreadLocalRandom.current().nextInt(1, 10);
            System.out.printf("sleeping for %d seconds\n", val);
            Thread.sleep(Duration.ofSeconds(val));
            return null;
        };
        var threadEventManager = new ThreadPoolEventManager(maxThreads);
        var recordingStream = new RecordingStream();
        var accountableThreadPool = AccountableExecutorService.newExecutorFrom(
                () -> Executors.newFixedThreadPool(maxThreads), threadEventManager
        );
        try (recordingStream; accountableThreadPool) {
            recordingStream.enable(ThreadPoolStatusEvent.NAME);
            recordingStream.onEvent(ThreadPoolStatusEvent.NAME, event -> {
                ThreadPoolStatusEvent.attributeNames.forEach(
                        attr -> System.out.printf("%s threads: %s\n", attr, event.getInt(attr))
                );
                System.out.printf("%s threads: %f\n", "ratio", event.getFloat("ratio"));
            });
            recordingStream.startAsync();

            var futs = new ArrayList<>(IntStream.range(0, 300)
                    .mapToObj(i -> accountableThreadPool.submit(task)).toList());
            Thread.sleep(Duration.ofSeconds(4));
            futs.addAll(IntStream.range(0, 550)
                    .mapToObj(i -> accountableThreadPool.submit(task)).toList());
            Thread.sleep(Duration.ofSeconds(4));
            futs.addAll(IntStream.range(0, 100)
                    .mapToObj(i -> accountableThreadPool.submit(task)).toList());
            Thread.sleep(Duration.ofSeconds(4));
            futs.parallelStream().forEach(f -> {
                try {
                    f.get();
                } catch (InterruptedException | ExecutionException e) {
                    throw new RuntimeException(e);
                }
            });
        }
    }
}
