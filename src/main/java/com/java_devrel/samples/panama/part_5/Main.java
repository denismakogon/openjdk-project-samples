package com.java_devrel.samples.panama.part_5;

import com.java_devrel.samples.opencv.*;

import java.lang.foreign.MemorySession;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

public class Main {

    public static void main(String[] args) {
        var cwd = Path.of("").toAbsolutePath().toString();
        var baseDir = cwd + "/assets/";

        var sourceImagePathOne = baseDir + "images/1.jpeg";
        var sourceImagePathTwo = baseDir + "images/2.jpeg";
        var finalImagePathOne = baseDir + "images/1-final.jpeg";
        var finalImagePathTwo = baseDir + "images/2-final.jpeg";

        var modelPath = baseDir + "yolov3-320.cfg";
        var modelWeightsPath = baseDir + "yolov3-320.weights";
        var cocoaClassesFilePath = baseDir + "coco.names";

        try(var memorySession = MemorySession.openConfined()) {
            var modelPath$ms = memorySession.allocateUtf8String(modelPath);
            var modelWeightsPath$ms = memorySession.allocateUtf8String(modelWeightsPath);
            var cocoaClassesFilePath$ms = memorySession.allocateUtf8String(cocoaClassesFilePath);

            Stream.of(
                    List.of(sourceImagePathOne, finalImagePathOne, 0.0, 0.8),
                    List.of(sourceImagePathTwo, finalImagePathTwo, 0.2, 0.8)
            ).forEach(lst -> {
                var source = memorySession.allocateUtf8String((String)lst.get(0));
                var target = memorySession.allocateUtf8String((String)lst.get(1));
                int retCode;
                var pds$ms = PositionalFrameObjectDetectionDescriptor.allocate(memorySession);

                retCode = c_api_h.runDetectionsOnImage(
                        source, modelPath$ms, modelWeightsPath$ms,
                        cocoaClassesFilePath$ms, pds$ms, (Double)lst.get(2), (Double)lst.get(3)
                );
                if (retCode != 0 ) {
                    System.exit(retCode);
                }

                System.out.println(c_api_h.PositionalFrameObjectDetectionDescriptor_toString(pds$ms).getUtf8String(0));
                c_api_h.drawDetectionsOnImage(source, target, pds$ms, 1);
            });
        }
    }
}
