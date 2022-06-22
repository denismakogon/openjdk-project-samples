//
//  cpp_api.hpp
//  c_api
//
//  Created by Denis Makogon on 21.06.2022.
//

#ifndef cpp_api_hpp
#define cpp_api_hpp

#include "data_types.h"

#include <string>

using namespace std;

int imageToMatrix(string imagePath, int option, ExportableMat& exMat);
int runClassificationsOnImage(string classifierPath, string imagePath, PositionalFrameObjectDetectionDescriptor& pds);
int processVideoFile(string classifierPath, string videoFilePath, ExportableRectanglesPerFrame& exportableResult);
int runDetectionsOnVideo(string videoFilePath, string modelPath, string modelWeights);
int runDetectionsOnImage(string imagePath, string modelPath, string modelWeights, PositionalFrameObjectDetectionDescriptor& pds);

string toString(PositionalFrameObjectDetectionDescriptor& object);

int drawDetectionsOnImage(string sourceImagePath, string finalImagePath, PositionalFrameObjectDetectionDescriptor& pds, double scale = 1.2);


#endif /* cpp_api_hpp */
