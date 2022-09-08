package com.java_devrel.samples.panama.part_2;

import java.lang.foreign.*;
import java.lang.invoke.MethodHandle;

import static java.lang.foreign.ValueLayout.*;

public class PrintfImpls {
    private static final FunctionDescriptor printfDescriptor =
            FunctionDescriptor.of(JAVA_INT, ADDRESS);
    static final Linker linker = Linker.nativeLinker();
    private static final SymbolLookup linkerLookup = linker.defaultLookup();
    private static final SymbolLookup systemLookup = SymbolLookup.loaderLookup();
    private static final SymbolLookup symbolLookup = name ->
            systemLookup.lookup(name).or(() -> linkerLookup.lookup(name));

    static final MemorySegment printf$ms = symbolLookup.lookup("printf").orElseThrow();

    static MethodHandle specializedPrintf(MemoryLayout... varargLayouts) {
        FunctionDescriptor specialized = printfDescriptor.asVariadic(varargLayouts);
        return linker.downcallHandle(printf$ms, specialized);
    }

    public static final MethodHandle WithInt = specializedPrintf(JAVA_INT);
    public static final MethodHandle WithString = specializedPrintf(ADDRESS);
    public static final MethodHandle WithStringAndInt = specializedPrintf(JAVA_INT, ADDRESS);

    public static void main(String[] args) throws Throwable {
        try (var ms = MemorySession.openConfined()) {
            WithString.invoke(ms.allocateUtf8String("My name is %s.\n"),
                    ms.allocateUtf8String("Denis"));
            WithInt.invoke(ms.allocateUtf8String("I'm %dyo old.\n"), 31);
            WithStringAndInt.invoke(ms.allocateUtf8String("My name is %s, I'm %dyo old.\n"),
                    ms.allocateUtf8String("Denis"), 31);
        }
    }
}
