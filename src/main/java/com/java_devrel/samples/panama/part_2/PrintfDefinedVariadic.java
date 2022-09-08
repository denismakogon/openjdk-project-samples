package com.java_devrel.samples.panama.part_2;

import java.lang.foreign.*;
import java.lang.invoke.MethodHandle;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static java.lang.foreign.ValueLayout.*;


public class PrintfDefinedVariadic {
    private static final Linker linker = Linker.nativeLinker();
    private static final SymbolLookup linkerLookup = linker.defaultLookup();
    private static final SymbolLookup systemLookup = SymbolLookup.loaderLookup();
    private static final SymbolLookup symbolLookup = name ->
            systemLookup.lookup(name).or(() -> linkerLookup.lookup(name));
    private static final FunctionDescriptor printfDescriptor =
            FunctionDescriptor.of(JAVA_INT, ADDRESS);

    private static final MemorySegment printf$ms = symbolLookup.lookup("printf").orElseThrow();

    private static int printf(MemorySession ms, FunctionDescriptor fd, String formatter, Object...varargs) {
        AtomicInteger res = new AtomicInteger(-1);
        MethodHandle printfHandle = linker.downcallHandle(printf$ms, fd);
        try {
            var cString = ms.allocateUtf8String(formatter);
            var args = new ArrayList<>(varargs.length + 1);
            Collections.addAll(args, varargs);
            Collections.addAll(args, List.of(cString));
            res.set(
                    (int) printfHandle.asSpreader(Object[].class, args.size()).invoke(args)
            );
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
        return res.get();
    }

    public static void main(String[] args) throws Throwable {
        try (var memorySession = MemorySession.openConfined()) {
            var formatter = "My name is %s, age %d\n";
            var name = "Denis";
            var age = 31;
            printf(memorySession, printfDescriptor.asVariadic(ADDRESS, JAVA_INT),
                    formatter, memorySession.allocateUtf8String(name), age);
        }
    }

}
