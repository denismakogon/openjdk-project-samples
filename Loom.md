# Project Loom

## How many threads we can launch?

```shell
java --enable-preview -Djdk.virtualThreadScheduler.parallelism=10 -cp target/openjdk-samples-1.0.jar com.java_devrel.samples.loom.HowManyVirtualThreads
```

## I/O thread switch

```shell
java --enable-preview -cp target/openjdk-samples-1.0.jar com.java_devrel.samples.loom.IOswitch
```


## Thread pinning

Supported `threadType`:
 - virtual
 - platform

```shell
java --enable-preview -cp target/openjdk-samples-1.0.jar -DthreadType=virtual com.java_devrel.samples.loom.Synchronization
```

## Continuations

```shell
java --add-opens=java.base/jdk.internal.vm=ALL-UNNAMED --enable-preview -cp target/openjdk-samples-1.0.jar com.java_devrel.samples.loom.Continuations
```

## Synchronization

Tasks:
 - synchronizedTask
 - reentrantLockTask

```shell
java --enable-preview -cp target/openjdk-samples-1.0.jar -Dtask=synchronizedTask com.java_devrel.samples.loom.Synchronization
```

## Structured concurrency

Samples:
 - simpleScope
 - nestedScope
 - complexScope
 - unboundForkInScope
 - syncStreamInScope
 - parallelStreamInScope

```shell
java --add-modules jdk.incubator.concurrent --add-opens=java.base/jdk.internal.vm=ALL-UNNAMED --enable-preview -cp target/openjdk-samples-1.0.jar -Dsample=goodThreadScope com.java_devrel.samples.loom.StructuredConcurrency
```
