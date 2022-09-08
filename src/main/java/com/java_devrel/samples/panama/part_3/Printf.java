package com.java_devrel.samples.panama.part_3;


import java.lang.foreign.MemorySegment;
import java.lang.foreign.MemorySession;
import java.lang.foreign.SegmentAllocator;
import java.nio.charset.StandardCharsets;

import static com.java_devrel.samples.stdlib.stdio.stdio_h.printf;
import static java.lang.foreign.ValueLayout.JAVA_BYTE;

public class Printf {
    public static void main(String[] args) {
        var bytes = "Welcome from the other side!\n".getBytes(StandardCharsets.UTF_8);
        try (var memorySession = MemorySession.openConfined()) {
<<<<<<< Updated upstream
            var cString = memorySession.allocateUtf8String("Welcome from the other side!\n");
            printf(cString);
=======
            var segmentAllocator = SegmentAllocator.newNativeArena(
                    bytes.length + 1, memorySession);
            MemorySegment addr = segmentAllocator.allocate(bytes.length + 1);
            var heapSegment = MemorySegment.ofArray(bytes);
            addr.copyFrom(heapSegment);
            addr.set(JAVA_BYTE, bytes.length, (byte)0);
            printf(addr);
>>>>>>> Stashed changes
        }
    }
}
