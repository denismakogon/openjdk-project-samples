package com.java_devrel.samples.panama.part_6;

import java.lang.foreign.*;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;

import static java.lang.foreign.ValueLayout.ADDRESS;

public class Upcall {
    static final Linker linker = Linker.nativeLinker();
    private static MethodHandle callback$mh;

    static {
        try {
            callback$mh = MethodHandles
                    .lookup()
                    .findStatic(Upcall.class,
                            "callback",
                            MethodType.methodType(void.class));
        } catch (IllegalAccessException | NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }
    // void callback_function(void (*callablePtr)());
    static final MethodHandle nativeFunctionWithCallback = Linker.nativeLinker()
            .downcallHandle(addr, FunctionDescriptor.ofVoid(ADDRESS));


    static void callback() {
        System.out.println("I'm inside of a callback");
    }

    public static void main(String[] args) throws Throwable {
        try(var ms = MemorySession.openConfined()) {
            MemorySegment callbackNativeSymbol = linker.upcallStub(
                    callback$mh, FunctionDescriptor.ofVoid(), ms);
            nativeFunctionWithCallback.invokeExact((Addressable) callbackNativeSymbol);
        }
    }

}
