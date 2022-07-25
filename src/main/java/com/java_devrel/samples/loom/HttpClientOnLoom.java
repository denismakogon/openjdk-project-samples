package com.java_devrel.samples.loom;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Collections;
import java.util.concurrent.*;
import java.util.stream.Collectors;


public class HttpClientOnLoom {

    public static void main(String...args) throws Exception {
        var tasksNo = 500_000;
        var request = HttpRequest.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .GET();
        var client = HttpClient.newHttpClient();
        try(var executor = Executors.newThreadPerTaskExecutor(
                Thread.ofVirtual().name("virtual-", 0).allowSetThreadLocals(false).factory())) {
            Callable<Integer> task = () -> {
                var currThread = Thread.currentThread();
                System.out.println(currThread.threadId());
                var resp = client.send(
                        request.uri(new URI(String.format(
                                        "http://localhost:8080/get?virtualThread=%s&counter=%s",
                                        currThread.isVirtual(), currThread.getName())))
                                .build(),
                        HttpResponse.BodyHandlers.ofString()
                );
                return resp.statusCode();
            };
            var statusCodeCounter = executor.invokeAll(
                    Collections.nCopies(tasksNo, task)).parallelStream().map(t -> {
                var currThread = Thread.currentThread();
                System.out.printf("running [thread: %s], virtual - %s\n",
                        currThread.getName(), currThread.isVirtual()
                );
                var statusCode = 0;
                try {
                    statusCode = t.get();
                } catch (ExecutionException | InterruptedException e) {
                    throw new RuntimeException(e);
                }
                return statusCode;
            }).collect(Collectors.groupingBy(Integer::valueOf, Collectors.counting()));
            System.out.println(statusCodeCounter);
        }
    }
}
