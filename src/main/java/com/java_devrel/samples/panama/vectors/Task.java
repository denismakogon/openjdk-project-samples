package com.java_devrel.samples.panama.vectors;

import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.Linker;
import java.lang.foreign.MemoryAddress;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.MemorySession;
import java.lang.foreign.SymbolLookup;
import java.lang.foreign.ValueLayout;
import java.lang.invoke.MethodHandle;
import java.util.Arrays;

// g++ -std=c++20 -shared -march=native -fPIC -o vector-simd.platform vector_simd.cpp
public class Task {
    
    static {
        System.load(System.getProperty("lib.path"));
    }

    private static final Linker linker = Linker.nativeLinker();
    private static final SymbolLookup linkerLookup = linker.defaultLookup();
    private static final SymbolLookup loaderLookup = SymbolLookup.loaderLookup();
    private static final SymbolLookup symbolLookup = name ->
            loaderLookup.lookup(name).or(() -> linkerLookup.lookup(name));

    // void* toVectorPointer(float* array, int size);
    private static final FunctionDescriptor toVoidPointerDescriptor = FunctionDescriptor.of(
        ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.JAVA_INT
    );
    // float* mm256_add_ps(void* vectorPtrOne, void* vectorPtrTwo);
    private static final FunctionDescriptor _mm_256_add_ps_Descriptor = FunctionDescriptor.of(
        ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.ADDRESS
    );

    private static final MethodHandle toVoidPointer = symbolLookup.lookup("toVectorPointer").map(
            memSeg -> linker.downcallHandle(memSeg, toVoidPointerDescriptor)
        ).orElseThrow();

    private static final MethodHandle _mm_256_add_ps = symbolLookup.lookup("mm256_add_ps").map(
        memSeg -> linker.downcallHandle(memSeg, _mm_256_add_ps_Descriptor)
    ).orElseThrow();

    public static void main(String[] args) throws Throwable {
        try(var memorySession = MemorySession.openConfined()) {
            float[] one = {1F, 2F, 3F, 4F, 5F, 6F, 7F, 8F, 9F, 10F};
            float[] two = {10F, 9F, 8F, 7F, 6F, 5F, 4F, 3F, 2F, 1F};

            var nativeOne = memorySession.allocateArray(ValueLayout.JAVA_FLOAT, one.length);
            var nativeTwo = memorySession.allocateArray(ValueLayout.JAVA_FLOAT, two.length);
            var oneSegment = MemorySegment.ofArray(one);
            var twoSegment = MemorySegment.ofArray(two);
            nativeOne.copyFrom(oneSegment);
            nativeTwo.copyFrom(twoSegment);

            // void* ptr, MemoryAddress
            var voidPtrOne = (MemoryAddress) toVoidPointer.invoke(nativeOne.address(), one.length);
            var voidPtrTwo = (MemoryAddress) toVoidPointer.invoke(nativeTwo.address(), two.length);

            // float*, MemoryAddress
            var floatArrayPtr = (MemoryAddress) _mm_256_add_ps.invoke(voidPtrOne, voidPtrTwo);
            var floatArraySegment = MemorySegment.ofAddress(
                floatArrayPtr, ValueLayout.JAVA_FLOAT.byteSize() * one.length, memorySession);
            var result = floatArraySegment.toArray(ValueLayout.JAVA_FLOAT);
            System.out.println(Arrays.toString(result));
        }
    }
}
