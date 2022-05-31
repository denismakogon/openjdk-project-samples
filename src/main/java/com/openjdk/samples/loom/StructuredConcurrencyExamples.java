package com.openjdk.samples.loom;

import jdk.incubator.concurrent.ScopeLocal;
import jdk.incubator.concurrent.StructuredTaskScope;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Instant;
import java.util.Collections;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class StructuredConcurrencyExamples {

    private static final ScopeLocal<String> scoped = ScopeLocal.newInstance();

    private static void scopeLocal() throws InterruptedException {
        var virtualThread = Thread.ofVirtual()
                .allowSetThreadLocals(false)
                .start(
                        () -> ScopeLocal.where(
                                scoped, "world!",
                                () -> System.out.println("Hello " + scoped.get())
                        )
                );
        virtualThread.join();
    }

    public static void main(String[] args) throws InterruptedException {
        var taskNo = 500000;
        var request = HttpRequest.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .GET();
        var client = HttpClient.newHttpClient();

        Callable<Integer> task = () -> {
            var currThread = Thread.currentThread();
            var resp = client.send(
                    request.uri(new URI(String.format(
                                    "http://localhost:8080/get?virtualThread=%s&counter=%s",
                                    currThread.isVirtual(), currThread.getName())))
                            .build(),
                    HttpResponse.BodyHandlers.ofString()
            );
            if (200 != resp.statusCode()) {
                throw new Exception("bad request: " + resp.body());
            }
            return resp.statusCode();
        };

        try(var scopeOne = new StructuredTaskScope<>()) {
            Collections.nCopies(10, task)
                    .parallelStream().forEach(scopeOne::fork);

            try(var scopeTwo = new StructuredTaskScope<>()) {
                var scopeTwoTasks = Collections.nCopies(20, task)
                        .parallelStream().map(scopeTwo::fork).toList();
                scopeTwo.joinUntil(Instant.now().plusSeconds(scopeTwoTasks.size() * 10L));
            } catch (TimeoutException e) {
                throw new RuntimeException(e);
            }

            scopeOne.join();
        }

        try(var scopeJoinUntil = new StructuredTaskScope<>()) {

            scopeJoinUntil.joinUntil(Instant.now().plusSeconds(10));
        } catch (TimeoutException e) {
            throw new RuntimeException(e);
        }

        try(var scopeShutdown = new StructuredTaskScope<>()) {
            scopeShutdown.shutdown();
        }


        try(var scopeGlobal = new StructuredTaskScope<>("platform", Thread.ofPlatform().factory())) {

            var foo = scopeGlobal.fork(() -> {
                try(var scopeOne = new StructuredTaskScope.ShutdownOnFailure()) {
                    var listOfFutures = Collections.nCopies(20, task)
                            .parallelStream().map(scopeOne::fork).toList();
                    scopeOne.joinUntil(Instant.now().plusSeconds(taskNo * 3));
                    scopeOne.throwIfFailed(e -> e);
                    return listOfFutures.stream().map(Future::resultNow).toList();
                } catch (Throwable e) {
                    throw new RuntimeException(e);
                }
            });

            var bar = scopeGlobal.fork(() -> foo.resultNow().stream().parallel()
                    .collect(Collectors.groupingBy(Integer::valueOf, Collectors.counting())));

            scopeGlobal.join();
            System.out.println(bar.resultNow());
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

    }
}
