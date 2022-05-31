package com.openjdk.samples.panama.ffmpeg;

import com.openjdk.samples.ffmpeg.libavutil.error.error_h;

import java.lang.foreign.MemorySegment;
import java.lang.foreign.MemorySession;
import java.lang.foreign.SegmentAllocator;

import static java.lang.foreign.ValueLayout.JAVA_BYTE;

public class ExitCodeException extends Exception {

    private ExitCodeException(String functionName, String errorMessage) {
        super(String.format(
                "[function: %s] error: %s",
                functionName, errorMessage
            )
        );
    }

    public ExitCodeException(String errorMessage) {
        super(errorMessage);
    }

    public static ExitCodeException fromReturnCode(String functionName, int errNum, MemorySession session) {
        System.out.printf("%s exit code: %s\n", functionName, errNum);
        var errBuffer = MemorySegment.allocateNative(
                error_h.AV_ERROR_MAX_STRING_SIZE(), session
        );
        SegmentAllocator.prefixAllocator(errBuffer).allocate(JAVA_BYTE, (byte) 0);
        error_h.av_strerror(errNum, errBuffer, error_h.AV_ERROR_MAX_STRING_SIZE());
        return new ExitCodeException(functionName, errBuffer.getUtf8String(0));
    }

}
