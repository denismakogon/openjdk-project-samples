package com.java_devrel.samples.loom;

import jdk.incubator.concurrent.StructuredTaskScope;

import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;


public class StructuredConcurrency extends Util {
    static int taskNo = 10;
    static Callable<Integer> task = () -> {
        System.out.println(Thread.currentThread());
        Thread.sleep(Duration.ofSeconds(1));
        updatePlatformOrVirtualThreadMap();
        return 42;
    };

    static void simpleScope() throws InterruptedException {
        try (var scope = new StructuredTaskScope<>()) {
            scope.fork(task);
            scope.join();
        }
    }

    static void nestedScope() throws InterruptedException {
        try(var scopeOne = new StructuredTaskScope<>()) {
            Collections.nCopies(10, task).forEach(scopeOne::fork);

            try(var scopeTwo = new StructuredTaskScope<>()) {
                var scopeTwoTasks = Collections.nCopies(taskNo, task)
                        .stream().map(scopeTwo::fork).toList();
                scopeTwo.joinUntil(Instant.now().plusSeconds((long) (scopeTwoTasks.size() * 1.5)));
            } catch (TimeoutException e) {
                throw new RuntimeException(e);
            }
            scopeOne.join();
        }
    }

    static void complexScope() {
        try(var scopeGlobal = new StructuredTaskScope<>("platform", Thread.ofPlatform().factory())) {
            var foo = scopeGlobal.fork(() -> {
                try(var scopeOne = new StructuredTaskScope.ShutdownOnFailure()) {
                    var listOfFutures = Collections.nCopies(taskNo, task)
                            .stream().map(scopeOne::fork).toList();
                    scopeOne.joinUntil(Instant.now().plusSeconds((long)(listOfFutures.size() * 1.5)));
                    var results = listOfFutures.stream().map(Future::resultNow).toList();
                    System.out.println("completed in scopeOne");
                    return results;
                } catch (Throwable e) {
                    throw new RuntimeException(e);
                }
            });
            while (!foo.isDone()) Thread.sleep(10);

            var bar = scopeGlobal.fork(() -> foo.resultNow().parallelStream()
                    .collect(Collectors.groupingBy(Integer::valueOf, Collectors.counting())));

            scopeGlobal.join();
            bar.resultNow();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    static void unboundForkInScope() {
        try(var scopeJoinUntil = new StructuredTaskScope<>()) {
            Collections.nCopies(15, task)
                    .parallelStream().forEach(scopeJoinUntil::fork);
            scopeJoinUntil.joinUntil(Instant.now().plusSeconds(taskNo));
        } catch (WrongThreadException | TimeoutException | InterruptedException e) {
            System.err.println(e.getMessage());
        }
    }

    static void boundStream() throws InterruptedException {
        try(var scope = new StructuredTaskScope<Integer>()) {
            for (Callable<Integer> integerCallable : Collections.nCopies(100, task)) {
                scope.fork(integerCallable);
            }
            scope.join();
        }
    }

    /*
    * You can use parallel streams however you like.
    * But you can only call the fork method from threads that are part of the scope.
    *
    * The reason for that is that if any thread could call fork,
    * there would be a race between knowing when the scope is done (all forks are complete)
    * and starting a new fork. I.e. all forks could be done and a new one started at exactly the same time.
    * That is why that particular method (and others, such as shutdown) is constrained to certain threads.
    *
    * So the issue is not “using parallel streams” but calling the fork method from an external thread.
    */
    static void _streamInScope(boolean runParallelStream) {
        try(var scopeJoinUntil = new StructuredTaskScope<>()) {
            scopeJoinUntil.fork(() -> {
                updatePlatformOrVirtualThreadMap();
                var stream = Collections.nCopies(taskNo, task).stream();
                if (runParallelStream) {
                    stream = stream.parallel();
                }
                return stream.map(scopeJoinUntil::fork).toList();
            });
            scopeJoinUntil.join();
        } catch (WrongThreadException | InterruptedException e) {
            System.err.println(e.getMessage());
        }
    }

    static void syncStreamInScope() {
        _streamInScope(false);
    }

    static void parallelStreamInScope() {
        _streamInScope(true);
    }

    public static void main(String[] args) throws InterruptedException, ExecutionException {
        Optional.of(System.getProperty("sample")).ifPresentOrElse(name ->
                runMethod(StructuredConcurrency.class, name),
                () -> runMethod(StructuredConcurrency.class, "simpleScope")
        );
        printVirtualThreadMap();
        printPlatformThreadMap();
    }
}
