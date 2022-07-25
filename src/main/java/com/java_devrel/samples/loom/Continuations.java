package com.java_devrel.samples.loom;

import jdk.internal.vm.Continuation;
import jdk.internal.vm.ContinuationScope;

public class Continuations {
    public static void main(String[] args) {
        ContinuationScope scope = new ContinuationScope("scope");
        Continuation c = new Continuation(scope, () -> {
            System.out.println("stage one");
            // unmounts a continuation
            Continuation.yield(scope);
            System.out.println("stage two");
        });
        System.out.println("start");
        // mounts a continuation to a carrier thread
        c.run();
        System.out.println("after start");
        c.run();
        System.out.println("stop");
    }
}
