package com.java_devrel.samples.loom;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class Util {
    private static final Map<String, List<String>> vThreadNames = Collections.synchronizedMap(new HashMap<>());
    private static final Map<String, List<String>> pThreadNames = Collections.synchronizedMap(new HashMap<>());

    static Map<String, List<String>> getVirtualThreadsMap() {
        return vThreadNames;
    }
    public static void updatePlatformThreadMap() {
        var thread = Thread.currentThread();
        var threadId = thread.threadId();
        var threadName = thread.getName();
        var _threadNames = pThreadNames.getOrDefault(threadName, new ArrayList<>());
        _threadNames.add(String.format("PlatformThread[%d]", threadId));
        pThreadNames.put(threadName, _threadNames);
    }

    public static void updateVirtualThreadMap() {
        var thread = Thread.currentThread();
        var threadName = thread.toString();
        var vThreadName = threadName.split("/")[0];
        var carrierThread = threadName.split("@")[1];
        var _threadNames = vThreadNames.getOrDefault(carrierThread, new ArrayList<>());
        _threadNames.add(vThreadName);
        vThreadNames.put(carrierThread, _threadNames);
    }

    public static void updatePlatformOrVirtualThreadMap() {
        if (Thread.currentThread().isVirtual()) {
            updateVirtualThreadMap();
            return;
        }
        updatePlatformThreadMap();
    }

    static void _printMap(Map<String, List<String>> m) {
        m.forEach(
                (k, v) -> {
                    System.out.printf("%s: [\n", k);
                    v.stream().collect(Collectors.groupingBy(p -> p, Collectors.counting())).forEach(
                            (k1, v1) -> System.out.printf("\t%s was mounted %d times\n", k1, v1)
                    );
                    System.out.println("]");
                }
        );

    }

    public static void printVirtualThreadMap() {
        _printMap(vThreadNames);
    }

    public static void printPlatformThreadMap() {
        _printMap(pThreadNames);
    }

    public static MethodHandle getMethod(Class<?> clz, String name) {
        MethodHandle mh;
        try {
            mh = MethodHandles.lookup().findStatic(
                    clz, name,
                    MethodType.methodType(void.class));
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }

        return mh;

    }

    public static void runMethod(Class<?> clz, String name) {
        try {
            getMethod(clz, name).invoke();
        } catch (Throwable e) {
            System.err.println(e.getMessage());
            System.exit(1);
        }
    }
}
