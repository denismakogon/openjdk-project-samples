package com.java_devrel.samples.panama.part_5;

import com.java_devrel.samples.opencv.*;

import java.lang.foreign.MemorySegment;
import java.lang.foreign.MemorySession;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.LongStream;
import java.util.stream.Stream;

public class Main {
    private static int opencvAPI(MemorySession memorySession,
                                 MemorySegment carClassifier$ms,
                                 MemorySegment sourcePath$ms,
                                 MemorySegment finalPath$ms) {
        var retCode = 0;

        var pds$ms = PositionalFrameObjectDetectionDescriptor.allocate(memorySession);
        retCode = c_api_h.runClassificationsOnImage(
                carClassifier$ms, sourcePath$ms, pds$ms
        );
        if (retCode != 0 ) {
            return retCode;
        }

        // long size;
        var size = PositionalFrameObjectDetectionDescriptor.size$get(pds$ms);
        System.out.printf("Number of detections: %d\n", size);
        // ObjectDetectionDescriptor* detections;
        var detections$ms = PositionalFrameObjectDetectionDescriptor.detections$get(pds$ms);
        // memory segment behind 'ObjectDetectionDescriptor* detections'
        var arraySegment = MemorySegment.ofAddress(
                detections$ms, size*ObjectDetectionDescriptor.sizeof(), memorySession);

        LongStream.range(0, size).forEach(i -> {
            var classID = ObjectDetectionDescriptor.classId$get(arraySegment, i);
            var x = ExportableRectangle.x$get(arraySegment, i);
            var y = ExportableRectangle.y$get(arraySegment, i);
            var width = ExportableRectangle.width$get(arraySegment, i);
            var height = ExportableRectangle.height$get(arraySegment, i);
            System.out.printf("[iteration: %d] class ID: %d, x=%d, y=%d  width=%d heights=%d\n",
                    i, classID, x, y, width, height);
        });

        c_api_h.drawDetectionsOnImage(
                sourcePath$ms, finalPath$ms, pds$ms, 1.0
        );
        return retCode;
    }
    public static void main(String[] args) {
        var cwd = Path.of("").toAbsolutePath().toString();
        var baseDir = cwd + "/assets/";
        var sourceImagePathOne = baseDir + "images/1.jpeg";
        var sourceImagePathTwo = baseDir + "images/2.jpeg";
        var finalImagePathOne = baseDir + "images/1-final.jpeg";
        var finalImagePathTwo = baseDir + "images/2-final.jpeg";

        var carClassifier = baseDir + "car_classfier.xml";
        var modelPath = baseDir + "yolov3-320.cfg";
        var modelWeightsPath = baseDir + "yolov3-320.weights";

        try(var memorySession = MemorySession.openConfined()) {
            var carClassifier$ms = memorySession.allocateUtf8String(carClassifier);
            var modelPath$ms = memorySession.allocateUtf8String(modelPath);
            var modelWeightsPath$ms = memorySession.allocateUtf8String(modelWeightsPath);
            Stream.of(
                    List.of(sourceImagePathOne, finalImagePathOne),
                    List.of(sourceImagePathTwo, finalImagePathTwo)
            ).forEach(lst -> {
                var source = memorySession.allocateUtf8String(lst.get(0));
                var target = memorySession.allocateUtf8String(lst.get(1));
                int retCode;
                var pds$ms = PositionalFrameObjectDetectionDescriptor.allocate(memorySession);
                retCode = c_api_h.runDetectionsOnImage(
                        source, modelPath$ms, modelWeightsPath$ms, pds$ms, 0.2
                );
                if (retCode != 0 ) {
                    System.exit(retCode);
                }
                c_api_h.drawDetectionsOnImage(source, target, pds$ms, 1.2);
            });
        }
    }
}
