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
    private static final FunctionDescriptor variadicPrintfDescriptor =
            printfDescriptor.asVariadic(ADDRESS, JAVA_INT);

    private static int printf(String formatter, Object...varargs) {
        AtomicInteger res = new AtomicInteger(-1);
        symbolLookup.lookup("printf").ifPresent(addr -> {
            MethodHandle printfHandle = linker.downcallHandle(addr, variadicPrintfDescriptor);
            try {
                var cString = SegmentAllocator.implicitAllocator().allocateUtf8String(formatter);
                var args = new ArrayList<>(varargs.length + 1);
                Collections.addAll(args, varargs);
                Collections.addAll(args, List.of(cString));
                res.set(
                        (int) printfHandle.asSpreader(Object[].class, args.size()).invoke(args)
                );
            } catch (Throwable e) {
                throw new RuntimeException(e);
            }
        });
        return res.get();
    }

    public static void main(String[] args) throws Throwable {
        try (var memorySession = MemorySession.openConfined()) {
            symbolLookup.lookup("printf").ifPresent(addr -> {
                var variadicPrintfDescriptor = printfDescriptor.asVariadic(ADDRESS, JAVA_INT);
                MethodHandle printfHandle = linker.downcallHandle(addr, variadicPrintfDescriptor);
                try {
                    var age = 31;
                    var cString = memorySession.allocateUtf8String("My name is %s, age %d\n");
                    var name = memorySession.allocateUtf8String("Denis");
                    System.out.println(
                            (int) printfHandle.invoke(cString, name, age)
                    );
                } catch (Throwable e) {
                    throw new RuntimeException(e);
                }
            });

        }
    }

}
