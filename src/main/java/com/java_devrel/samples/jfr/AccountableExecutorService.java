package com.java_devrel.samples.jfr;

import java.time.Duration;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.function.Supplier;

public class AccountableExecutorService implements ExecutorService {

    /*
    * It could be any instance of the ExecutorService
    * */
    protected ExecutorService service;

    /*
    * A number of currently active threads
    * */
    protected AtomicInteger counter;
    protected ThreadPoolEventManager eventManager;
    protected Thread backgroundEventPublisher;

    public static ExecutorService newExecutorFrom(
            Supplier<ExecutorService> supplier, ThreadPoolEventManager eventManager) {
        return new AccountableExecutorService(supplier.get(), eventManager);
    }

    Thread backgroundEventPublisherTask() {
        return Thread.ofPlatform().unstarted(() -> {
            while (true) {
                try {
                    Thread.sleep(Duration.ofSeconds(1));
                    eventManager.record(counter.get());
                } catch (InterruptedException e) {
                    return;
                }
            }
        });
    }

    AccountableExecutorService(ExecutorService service, ThreadPoolEventManager eventManager) {
        this.service = service;
        this.counter = new AtomicInteger(0);
        this.eventManager = eventManager;
        backgroundEventPublisher = backgroundEventPublisherTask();
        backgroundEventPublisher.start();
    }

    <T> T call(Function<Void, T> invoke) {
        T res = null;
        Throwable throwable = null;
        try {
            this.counter.incrementAndGet();
            res = invoke.apply(null);
        } catch (Throwable ex) {
            throwable = ex;
        } finally {
            this.counter.decrementAndGet();
        }
        if (throwable != null) {
            throw new RuntimeException(throwable);
        }
        return res;
    }

    <T> Callable<T> wrap(Callable<T> task) {
       return () -> {
           Function<Void, T> fn = (var _void) -> {
               T res;
               try {
                   res = task.call();
               } catch (Exception e) {
                   throw new RuntimeException(e);
               }
               return res;
           };
           return call(fn);
        };
    }

    Runnable wrap(Runnable task) {
        return () -> {
            Function<Void, Void> fn = (var _void) -> {
                try {
                    task.run();
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
                return null;
            };
            call(fn);
        };
    }

    @Override
    public void shutdown() {
        this.service.shutdown();
        counter = new AtomicInteger(0);
    }

    @Override
    public List<Runnable> shutdownNow() {
        return this.service.shutdownNow();
    }

    @Override
    public boolean isShutdown() {
        return this.service.isShutdown();
    }

    @Override
    public boolean isTerminated() {
        return this.service.isTerminated();
    }

    @Override
    public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
        return this.service.awaitTermination(timeout, unit);
    }

    @Override
    public <T> Future<T> submit(Callable<T> task) {
        return this.service.submit(wrap(task));
    }

    @Override
    public <T> Future<T> submit(Runnable task, T result) {
        return this.service.submit(wrap(task), result);
    }

    @Override
    public Future<?> submit(Runnable task) {
        return this.service.submit(wrap(task));
    }

    @Override
    public <T> List<Future<T>> invokeAll(
            Collection<? extends Callable<T>> tasks) throws InterruptedException {
        return this.service.invokeAll(tasks.stream().map(this::wrap).toList());
    }

    @Override
    public <T> List<Future<T>> invokeAll(
            Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit)
            throws InterruptedException {
        return this.service.invokeAll(tasks.stream().map(this::wrap).toList(), timeout, unit);
    }

    @Override
    public <T> T invokeAny(
            Collection<? extends Callable<T>> tasks)
            throws InterruptedException, ExecutionException {
        return this.service.invokeAny(tasks.stream().map(this::wrap).toList());
    }

    @Override
    public <T> T invokeAny(
            Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit)
            throws InterruptedException, ExecutionException, TimeoutException {
        return this.service.invokeAny(tasks.stream().map(this::wrap).toList(), timeout, unit);
    }

    @Override
    public void execute(Runnable command) {
        this.service.execute(wrap(command));
    }

    @Override
    public void close() {
        this.backgroundEventPublisher.interrupt();
        this.service.close();
        counter = new AtomicInteger(0);
    }
}
