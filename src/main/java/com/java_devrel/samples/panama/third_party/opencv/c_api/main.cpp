//
//  main.cpp
//  c_api
//
//  Created by Denis Makogon on 17.06.2022.
//

#include "cpp_api.hpp"
#include "data_types.h"

#include <iostream>
#include <cstdio>


using namespace std;

int main(int argc, const char * argv[]) {
    string baseDir = "/Users/denismakogon/go/src/github.com/denismakogon/openjdk-project-samples/assets/";
    
    string imagePath = baseDir + "images/2.jpeg";
    string carClassifier = baseDir + "car_classfier.xml";
    string modelPath = baseDir + "yolov3-320.cfg";
    string modelWeightsPath = baseDir + "yolov3-320.weights";
    
    ExportableMat exMat;
    imageToMatrix(imagePath, 0, exMat);

//    PositionalFrameObjectDetectionDescriptor one;
//    runClassificationsOnImage(carClassifier, imagePath, one);
//    puts(toString(one).c_str());
    
//    PositionalFrameObjectDetectionDescriptor two;
//    runDetectionsOnImage(imagePath, modelPath, modelWeightsPath, two);
//    puts(toString(two).data());

    return 0;
}
