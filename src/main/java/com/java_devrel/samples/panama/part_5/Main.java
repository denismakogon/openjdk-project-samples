package com.java_devrel.samples.panama.part_5;

import com.java_devrel.samples.opencv.ExportableMat;
import com.java_devrel.samples.opencv.PositionalFrameObjectDetectionDescriptor;
import com.java_devrel.samples.opencv.c_api_h;

import java.lang.foreign.MemorySession;
import java.nio.file.Path;

public class Main {

    public static void main(String[] args) {
        var cwd = Path.of("").toAbsolutePath().toString();
        var baseDir = cwd + "/assets/";
        var sourceImagePath = baseDir + "images/2.jpeg";
        var finalImagePath = baseDir + "images/2-final.jpeg";
        var carClassifier = baseDir + "car_classfier.xml";
        var modelPath = baseDir + "yolov3-320.cfg";
        var modelWeightsPath = baseDir + "yolov3-320.weights";

        try(var memorySession = MemorySession.openConfined()) {
            var sourceImagePath$ms = memorySession.allocateUtf8String(sourceImagePath);
            var finalImagePath$ms = memorySession.allocateUtf8String(finalImagePath);
            var carClassifier$ms = memorySession.allocateUtf8String(carClassifier);

            var exMat$ms = ExportableMat.allocate(memorySession);
            var pds$ms = PositionalFrameObjectDetectionDescriptor.allocate(memorySession);

            var retCode = c_api_h.imageToMatrix(sourceImagePath$ms, c_api_h.IMREAD_COLOR(), exMat$ms);
            if (retCode != 0 ) {
                System.err.println("something went wrong on C++ side");
                System.exit(retCode);
            }
            var h= ExportableMat.height$get(exMat$ms);
            var w = ExportableMat.width$get(exMat$ms);
            System.out.printf("HxW = %dx%d\n", h, w);


            c_api_h.runClassificationsOnImage(
                    carClassifier$ms, sourceImagePath$ms, pds$ms
            );
            var constChar$ms = c_api_h.PositionalFrameObjectDetectionDescriptor_toString(pds$ms);
            System.out.println(constChar$ms.getUtf8String(0));
            
            c_api_h.drawDetectionsOnImage(
                    sourceImagePath$ms, finalImagePath$ms, pds$ms, 1.0
            );
        }
    }
}
