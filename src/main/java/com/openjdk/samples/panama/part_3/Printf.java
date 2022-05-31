package com.openjdk.samples.panama.part_3;


import java.lang.foreign.MemorySession;
import java.lang.foreign.SegmentAllocator;

import static com.openjdk.samples.stdlib.stdio.stdio_h.printf;


public class Printf {
    public static void main(String[] args) {
        try (var memorySession = MemorySession.openConfined()) {
            var allocator = SegmentAllocator.newNativeArena(memorySession);
            var cString = allocator.allocateUtf8String("Welcome from the other side!\n");
            printf(cString);
        }
    }
}
