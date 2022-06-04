package com.java_devrel.samples.panama.part_1;

import java.lang.foreign.*;
import java.lang.invoke.MethodHandle;
import java.util.Objects;

import static java.lang.foreign.ValueLayout.ADDRESS;
import static java.lang.foreign.ValueLayout.JAVA_INT;


public class PrintfSimplified {
    private static final Linker linker = Linker.nativeLinker();
    private static final SymbolLookup linkerLookup = linker.defaultLookup();
    private static final SymbolLookup systemLookup = SymbolLookup.loaderLookup();
    private static final SymbolLookup symbolLookup = name ->
            systemLookup.lookup(name).or(() -> linkerLookup.lookup(name));
    private static final FunctionDescriptor printfDescriptor = FunctionDescriptor.of(
            JAVA_INT.withBitAlignment(32), ADDRESS.withBitAlignment(64)
    );

    private static final MethodHandle printfMethodHandle = symbolLookup.lookup("printf").map(
            addr -> linker.downcallHandle(addr, printfDescriptor)
    ).orElse(null);

    private static int printf(String str, MemorySession memorySession) throws Throwable {
        Objects.requireNonNull(printfMethodHandle);
        var cString = memorySession.allocateUtf8String(str + "\n");
        return (int) printfMethodHandle.invoke(cString);
    }

    public static void main(String[] args) throws Throwable {
        var str = "hello world";
        try (var memorySession = MemorySession.openConfined()) {
            System.out.println(printf(str, memorySession));
        }
    }
}
