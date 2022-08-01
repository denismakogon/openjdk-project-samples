package com.java_devrel.samples.jfr;

import com.java_devrel.samples.http.Server;
import jdk.jfr.consumer.RecordingStream;

import java.io.IOException;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

public class HTTPServer {

    public static void main(String... args) throws IOException {
        var kernNumTaskThreads = new AtomicInteger(1000);
        Optional.ofNullable(System.getProperty("sysctl.kern.num_taskthreads")).ifPresentOrElse(
                prop -> kernNumTaskThreads.set(Integer.parseInt(prop)), () -> {}
        );
        var maxThreads = Double.valueOf(kernNumTaskThreads.get() * 0.9).intValue();

        var threadEventManager = new ThreadPoolEventManager(maxThreads);
        var recordingStream = new RecordingStream();
        recordingStream.enable(ThreadPoolStatusEvent.NAME);
        recordingStream.startAsync();
        var accountableThreadPool = AccountableExecutorService.newExecutorFrom(
                () -> Executors.newFixedThreadPool(maxThreads), threadEventManager
        );

        recordingStream.onEvent(ThreadPoolStatusEvent.NAME, event -> {
            ThreadPoolStatusEvent.attributeNames.forEach(
                    attr -> System.out.printf("%s threads: %s\n", attr, event.getInt(attr))
            );
            System.out.printf("%s threads: %f\n", "ratio", event.getFloat("ratio"));
        });

        var server = Server.serverWithExecutor(accountableThreadPool, Server.defaultRouter());

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            Server.logger.info("Server is shutting down...");
            recordingStream.close();
            accountableThreadPool.close();
            server.stop(0);
            Server.logger.info("Server stopped");
        }));

        server.start();
    }

}
