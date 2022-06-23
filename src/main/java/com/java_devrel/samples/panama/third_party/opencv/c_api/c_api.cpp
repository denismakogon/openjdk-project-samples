//
//  c_api.cpp
//  c_api
//
//  Created by Denis Makogon on 17.06.2022.
//

#include "opencv2/core/mat.hpp"
#include "opencv2/core/utility.hpp"
#include "opencv2/imgproc.hpp"
#include "opencv2/imgcodecs.hpp"

#include "opencv2/videoio.hpp"

#include "opencv2/dnn.hpp"
#include "opencv2/objdetect.hpp"


#include "data_types.h"

#include <iostream>
#include <cstdio>
#include <iterator>
#include <fstream>


using namespace std;
using namespace cv;
using namespace cv::samples;
using namespace cv::detail;
using namespace cv::details;
using namespace cv::dnn;

/*-----------------------------------------------------------------------*/
/*------------------------------Data structs-----------------------------*/
/*-----------------------------------------------------------------------*/

const static Scalar colors[] = {
    Scalar(255,0,0),
    Scalar(255,128,0),
    Scalar(255,255,0),
    Scalar(0,255,0),
    Scalar(0,128,255),
    Scalar(0,255,255),
    Scalar(0,0,255),
    Scalar(255,0,255)
};

/*-----------------------------------------------------------------------*/
/*-----------------------------------etc---------------------------------*/
/*-----------------------------------------------------------------------*/

bool checkElementByIndexAt(vector<string>& vt, int index) {
    bool isPresent;
    try {
        vt.at(index);
        isPresent = true;
    } catch (out_of_range& ex) {
        isPresent = false;
    }
    return isPresent;
}

const char* stringToChar(string str) {
    const char* charPtrClassName = new char[str.length()];
    charPtrClassName = str.data();
    return charPtrClassName;
}

/*-----------------------------------------------------------------------*/
/*---------------------------------DEBUG---------------------------------*/
/*-----------------------------------------------------------------------*/

void debug(string message) {
    string flag = getenv("DEBUG") ? getenv("DEBUG") : string("0");
    bool debugEnabled = string("1").compare(flag) == 0;
    if (debugEnabled) {
        puts(message.c_str());
    }
}

/*-----------------------------------------------------------------------*/
/*------------------------------toString API-----------------------------*/
/*-----------------------------------------------------------------------*/

string toString(ExportableRectangle& object) {
    return format("x=%d y=%d width=%d heights=%d",
                  object.x, object.y, object.width, object.height);
}

string toString(ObjectDetectionDescriptor& object) {
    return format("confidence: %6.4lf, rectange: [%s] class: %s\n",
                  object.confidence, toString(object.rect).data(), object.className);
}

string toString(PositionalFrameObjectDetectionDescriptor& object) {
    string res = "";
    for (int i = 0; i < object.size; i++) {
        res += toString(object.detections[i]);
    }
    return format("position: %d, detections: %s", object.position, res.c_str());
}

/*-----------------------------------------------------------------------*/
/*-------------------------------file API--------------------------------*/
/*-----------------------------------------------------------------------*/

int iterateFile(string filePath, function<void (const string&, int )> callback) {
    debug("in iterateFile");
    try {
        findFile(filePath);
    } catch (Exception& ex) {
        debug(ex.what());
        return -1;
    }
    
    ifstream inputFile(filePath.c_str());
    
    string tmp;
    int indexOf = 0;
    while (std::getline(inputFile, tmp)) {
        if (tmp.size() > 0) {
            callback(tmp, indexOf);
            indexOf++;
        }
    }
    
    inputFile.close();
    debug("done with iterateFile");
    return 0;
}

int readFileToVectorOf(string filePath, vector<string>& lines) {
    debug("in readFileToVectorOf");
    
    int retCode = iterateFile(filePath, [&](const string & line, int indexOf) {
        lines.push_back(line);
    });
    
    debug(format("done with readFileToVectorOf, retCode: %d", retCode));
    return retCode;
}

int readFileToMapOf(string filePath, map<string, int>& mapOf) {
    debug("in readFileToMapOf");
    
    int retCode = iterateFile(filePath, [&](const string& line, int indexOf) {
        mapOf[line] = indexOf;
    });
    
    debug(format("done with readFileToMapOf, retCode: %d", retCode));
    return retCode;
}

/*-----------------------------------------------------------------------*/
/*---------------------------vector export API---------------------------*/
/*-----------------------------------------------------------------------*/

void exportVectorOf(vector<Rect>& detections, ExportableRectangle* result) {
    for(int i = 0; i < detections.size(); i++) {
        Rect r = detections[i];
        result[i] = (ExportableRectangle) {
            .x = r.x, .y = r.y,
            .width = r.width,
            .height = r.height
        };
    }
}

void exportVectorOf(vector<ExportableRectangles>& rectsPerFrame, ExportableRectanglesPerFrame& result) {
    result = (ExportableRectanglesPerFrame) {};
    result.size = rectsPerFrame.size();
    result.array = rectsPerFrame.data();
}

void exportVectorOf(vector<Rect>& intermediate, PositionalFrameObjectDetectionDescriptor& pds) {
    vector<ObjectDetectionDescriptor> ds;
    for (auto r: intermediate) {
        ds.push_back((ObjectDetectionDescriptor) {
                        .className = strdup("unknown"),
                        .confidence = 0,
                        .rect = (ExportableRectangle) {
                            .x = r.x, .y = r.y,
                            .width = r.width,
                            .height = r.height
                        }
        });
    }

    pds = (PositionalFrameObjectDetectionDescriptor) {
        .position = 0,
        .size = intermediate.size(),
        .detections = ds.data()
    };
}

/*-----------------------------------------------------------------------*/
/*------------------------------Matrix API-------------------------------*/
/*-----------------------------------------------------------------------*/

void matToByteArray(Mat& image, ExportableMat& exMat) {
    unsigned long size = image.total() * image.elemSize();
    char * bytes = new char[size];
    memcpy(bytes,image.data,size * sizeof(char));
    exMat = (ExportableMat) {
        .matContent = bytes,
        .width = image.cols,
        .height = image.rows
    };
}

void byteArrayToMat(ExportableMat& exMat, Mat& mat) {
    mat = Mat(exMat.height, exMat.width, CV_8UC3, exMat.matContent).clone();
}

int readImageFile(Mat& image, string imagePath, int option) {
    debug("in readImageFile");
    int retCode = 0;
    try {
        image = imread(findFile(imagePath, true, true), option);
        if( image.empty() ) {
            retCode = -1;
        }
    } catch (Exception& ex) {
        debug(ex.what());
        retCode = -1;
    }
    debug(format("done with readImageFile, retCode: %d", retCode));
    return retCode;
}

int imageToMatrix(string imagePath, int option, ExportableMat& exMat) {
    debug("in imageToMatrix");
    Mat img;
    
    int retCode = 0;
    retCode = readImageFile(img, imagePath, option);
    if (retCode != 0) {
        return retCode;
    }
    
    matToByteArray(img, exMat);
    debug(format("done with imageToMatrix, retCode: %d", retCode));
    return retCode;
}

/*-----------------------------------------------------------------------*/
/*--------------------------Classfiers API-------------------------------*/
/*-----------------------------------------------------------------------*/

void prepareImageForClassification(Mat& img, Mat& result) {
    debug("in prepareImageForClassification");
    Mat blurred, dilated, kernel;
    GaussianBlur(img, blurred, Size(5, 5), 0);
    dilate(blurred, dilated, Mat(), Point(-1, -1));
    kernel = getStructuringElement(MORPH_ELLIPSE, Size(2, 2));
    morphologyEx(dilated, result, MORPH_CLOSE, kernel);
    debug("done with prepareImageForClassification");
}

void runClassifier(CascadeClassifier& cascade, Mat& source, Mat& result, vector<Rect>& detections) {
    debug("in runClassifier");
    source.copyTo(result);

    Mat preprocessedSource;
    prepareImageForClassification(source, preprocessedSource);

    cascade.detectMultiScale(preprocessedSource, detections, 1.1, 1);
    debug("done with runClassifier");
}

int runClassification(string classifierPath, string imagePath, vector<Rect>& results) {
    debug("in runClassification");
    CascadeClassifier cascade;
    int retCode = 0;
    try {
        retCode = cascade.load(classifierPath);
        debug("done reading classifier from a file" + classifierPath + format("%d", retCode));
        if (retCode != 1) {
            return retCode;
        }
    } catch (Exception& ex) {
        debug(ex.what());
        return -1;
    }

    Mat source, finalImg;
    retCode = readImageFile(source, imagePath, IMREAD_COLOR);
    if (retCode != 0) {
        return retCode;
    }

    runClassifier(cascade, source, finalImg, results);
    debug(format("done with runClassification, retCode: %d", retCode));
    return retCode;
}

int runClassificationsOnImage(string classifierPath, string imagePath, PositionalFrameObjectDetectionDescriptor& pds) {
    debug("in runClassificationsOnImage");
    vector<Rect> detections;
    int retCode = 0;
    
    retCode = runClassification(string(classifierPath), string(imagePath), detections);
    if ( retCode != 0 ) {
        return retCode;
    }
    
    exportVectorOf(detections, pds);
    debug(format("done with runClassificationsOnImage, retCode: %d", retCode));
    return retCode;
}

/*-----------------------------------------------------------------------*/
/*-----------------------------Video API---------------------------------*/
/*-----------------------------------------------------------------------*/

int readAllFrames(string videoFilePath, vector<Mat>& frames) {
    debug("in readAllFrames");
    VideoCapture capture;
    try {
        capture = VideoCapture(findFile(videoFilePath));
        if ( !capture.isOpened() ) {
            return -1;
        }
    } catch (Exception& ex) {
        debug(ex.what());
        return -1;
    }

    int frameCount = capture.get(CAP_PROP_FRAME_COUNT);

    frames = vector<Mat>(frameCount);
    Mat currentFrame;
    for (int i = 0; i < frameCount; i ++) {
        capture >> currentFrame;
        if (currentFrame.empty()) {
            break;
        }
        frames[i] = currentFrame;
    }
    debug(format("done with readAllFrames, retCode: %d", 0));
    return 0;
}

int processVideoFile(string classifierPath, string videoFilePath, vector<ExportableRectangles>& result) {
    debug("in processVideoFile");
    CascadeClassifier cascade;
    if ( !cascade.load(findFile(classifierPath)) ) {
        debug("classifier not found!");
        return -1;
    };
    
    vector<Mat> frames;
    int retCode = 0;
    
    retCode = readAllFrames(videoFilePath, frames);
    if (retCode != 0) {
        return retCode;
    }

    Mat currentFrame, finalImg;
    for (long i = 0; i < frames.size(); i++) {
        vector<Rect> detections;
        runClassifier(cascade, frames[i], finalImg, detections);
        ExportableRectangle* res = new ExportableRectangle[detections.size()];

        exportVectorOf(detections, res);
        result.push_back((ExportableRectangles) {
            .array = res,
            .size = detections.size()
        });
    }
    debug(format("done with processVideoFile, retCode: %d", retCode));
    return retCode;
}

int processVideoFile(string classifierPath, string videoFilePath, ExportableRectanglesPerFrame& exportableResult) {
    debug("in processVideoFile");
    vector<ExportableRectangles> result;
    int retCode = 0;
    retCode = processVideoFile(string(classifierPath), string(videoFilePath), result);
    if ( retCode != 0 ) {
        return retCode;
    }
    exportVectorOf(result, exportableResult);
    debug(format("done with processVideoFile, retCode: %d", retCode));
    return retCode;
}

/*-----------------------------------------------------------------------*/
/*-------------------------------DNN API---------------------------------*/
/*-----------------------------------------------------------------------*/

int setupDNN(string modelPath, string modelWeights, Net& net, int backend=DNN_BACKEND_DEFAULT, int target=DNN_TARGET_CPU) {
    debug("in setupDNN");
    try {
        net = readNetFromDarknet(findFile(modelPath), findFile(modelWeights));
        net.setPreferableBackend(backend);
        net.setPreferableTarget(target);
    } catch (Exception& ex) {
        debug(ex.what());
        return -1;
    }
    debug(format("done with setupDNN, retCode: %d", 0));
    return 0;
}


void inputPreprocess(const Mat& frame, Net& net, Size inpSize, float scale,
                       const Scalar& mean, bool swapRB) {
    debug("in inputPreprocess");
    static Mat blob;
    if (inpSize.width <= 0) inpSize.width = frame.cols;
    if (inpSize.height <= 0) inpSize.height = frame.rows;
    blobFromImage(frame, blob, 1.0, inpSize, Scalar(), swapRB, false, CV_8U);
    net.setInput(blob, "", scale, mean);
    if (net.getLayer(0)->outputNameToIndex("im_info") != -1)  // Faster-RCNN or R-FCN
    {
        resize(frame, frame, inpSize);
        Mat imInfo = (Mat_<float>(1, 3) << inpSize.height, inpSize.width, 1.6f);
        net.setInput(imInfo, "im_info");
    }
    debug("done with inputPreprocess");
}

void formatDetections(Mat& frame, vector<Mat>& outs, Net& net, vector<ObjectDetectionDescriptor>& ds,
                      vector<string>& cocoaClasses,
                      double confidenceThresholdMax=1.0,
                      double confidenceThresholdMin=0.1,
                      int backend=DNN_BACKEND_DEFAULT) {
    debug("in formatDetections");
    static std::vector<int> outLayers = net.getUnconnectedOutLayers();
    static std::string outLayerType = net.getLayer(outLayers[0])->type;

    if (outLayerType == "DetectionOutput") {
        for (size_t k = 0; k < outs.size(); k++) {
            float* data = (float*)outs[k].data;
            for (size_t i = 0; i < outs[k].total(); i += 7) {
                float confidence = data[i + 2];
                int classID = (int)(data[i + 1]) - 1;
                
                checkElementByIndexAt(cocoaClasses, classID);
                bool isMatch =
                    (confidence > confidenceThresholdMin && confidence < confidenceThresholdMax) && checkElementByIndexAt(cocoaClasses, classID);
                
                if (isMatch) {
                    int left   = (int)data[i + 3];
                    int top    = (int)data[i + 4];
                    int right  = (int)data[i + 5];
                    int bottom = (int)data[i + 6];
                    int width  = right - left + 1;
                    int height = bottom - top + 1;
                    if (width <= 2 || height <= 2) {
                        left   = (int)(data[i + 3] * frame.cols);
                        top    = (int)(data[i + 4] * frame.rows);
                        right  = (int)(data[i + 5] * frame.cols);
                        bottom = (int)(data[i + 6] * frame.rows);
                        width  = right - left + 1;
                        height = bottom - top + 1;
                    }
                    
                    ds.push_back((ObjectDetectionDescriptor) {
                        .className = strdup(cocoaClasses[classID].c_str()),
                        .confidence = confidence,
                        .rect = (ExportableRectangle) {
                            .x = left,
                            .y = top,
                            .width = width,
                            .height = height
                        }
                    });
                }
            }
        }
    } else if (outLayerType == "Region") {
        for (long i = 0; i < outs.size(); ++i) {
            float* data = (float*)outs[i].data;
            for (int j = 0; j < outs[i].rows; ++j, data += outs[i].cols) {
                Mat scores = outs[i].row(j).colRange(5, outs[i].cols);
                Point classIdPoint;
                double confidence;
                minMaxLoc(scores, 0, &confidence, 0, &classIdPoint);

                bool isMatch =
                    (confidence > confidenceThresholdMin && confidence < confidenceThresholdMax) && checkElementByIndexAt(cocoaClasses, classIdPoint.x);

                if (isMatch) {
                    int centerX = (int)(data[0] * frame.cols);
                    int centerY = (int)(data[1] * frame.rows);
                    int width = (int)(data[2] * frame.cols);
                    int height = (int)(data[3] * frame.rows);
                    
                    int left = centerX - width / 2;
                    int top = centerY - height / 2;

                    ds.push_back((ObjectDetectionDescriptor) {
                        .className = strdup(cocoaClasses[classIdPoint.x].c_str()),
                        .confidence = confidence,
                        .rect = (ExportableRectangle) {
                            .x = left,
                            .y = top,
                            .width = width,
                            .height = height
                        }
                    });
                }
            }
        }
    } else
        CV_Error(Error::StsNotImplemented, "Unknown output layer type: " + outLayerType);

    debug("done with formatDetections");
}

void runtObjectDetectionsOn(Mat& img, Net& net, vector<ObjectDetectionDescriptor>& ds,
                            vector<string>& cocoaClasses,
                            double confidenceThresholdMax=1.0,
                            double confidenceThresholdMin=0.1) {
    
    debug("in runtObjectDetectionsOn");
    int inputSize = 320;
    vector<Mat> outputs;

    inputPreprocess(img, net, Size(inputSize, inputSize), (float) 1/255, Scalar(0, 0, 0), false);
    net.forward(outputs, net.getUnconnectedOutLayersNames());
    
    formatDetections(img, outputs, net, ds, cocoaClasses,
                     confidenceThresholdMax=confidenceThresholdMax,
                     confidenceThresholdMin=confidenceThresholdMin);
    debug("done with runtObjectDetectionsOn");
}

/*-----------------------------------------------------------------------*/
/*---------------------------DNN detections API--------------------------*/
/*-----------------------------------------------------------------------*/

int runDetectionsOnImage(string imagePath, string modelPath, string modelWeights,
                         string cocoaClassesFilePath,
                         PositionalFrameObjectDetectionDescriptor& pds,
                         double confidenceThresholdMin=0.1,
                         double confidenceThresholdMax=1.0) {
    debug("in runDetectionsOn");
    dnn::Net net;
    Mat frame;
    vector<ObjectDetectionDescriptor> ds;

    int retCode = 0;
    retCode = readImageFile(frame, imagePath, IMREAD_COLOR);
    if ( retCode != 0 ) {
        return retCode;
    }
    
    retCode = setupDNN(modelPath, modelWeights, net);
    if ( retCode != 0 ) {
        return retCode;
    }
    
    vector<string> cocoaClasses;
    retCode = readFileToVectorOf(cocoaClassesFilePath, cocoaClasses);
    if ( retCode != 0 ) {
        return retCode;
    }
    
    runtObjectDetectionsOn(frame, net, ds, cocoaClasses,
                           confidenceThresholdMax=confidenceThresholdMax,
                           confidenceThresholdMin=confidenceThresholdMin);

    sort(
         ds.begin(), ds.end(),
         [](ObjectDetectionDescriptor a, ObjectDetectionDescriptor b) {
             return a.rect.x <= b.rect.x;
         }
     );
    pds = (PositionalFrameObjectDetectionDescriptor) {
        .position = 0,
        .size = ds.size(),
        .detections = ds.data()
    };

    debug(format("done with runDetectionsOn, retCode: %d", retCode));
    return retCode;
}

int runDetectionsOnVideo(string videoFilePath, string modelPath,
                         string modelWeights, string cocoaClassesFilePath,
                         double confidenceThresholdMax=1.0,
                         double confidenceThresholdMin=0.1) {
    debug("in runDetectionsOnVideo");
    dnn::Net net;
    vector<Mat> frames;
    vector<PositionalFrameObjectDetectionDescriptor> detectionsPerFrame;
    vector<string> cocoaClasses;

    int retCode = 0;
    retCode = readAllFrames(videoFilePath, frames);
    if ( retCode != 0 ) {
        return retCode;
    }
    retCode = setupDNN(modelPath, modelWeights, net);
    if ( retCode != 0 ) {
        return retCode;
    }
    
    retCode = readFileToVectorOf(cocoaClassesFilePath, cocoaClasses);
    if ( retCode != 0 ) {
        return retCode;
    }
    
    for (long i = 0; i < frames.size(); i++ ) {
        vector<ObjectDetectionDescriptor> ds;
        runtObjectDetectionsOn(frames[i], net, ds, cocoaClasses,
                               confidenceThresholdMax=confidenceThresholdMax,
                               confidenceThresholdMin=confidenceThresholdMin);
        detectionsPerFrame.push_back((PositionalFrameObjectDetectionDescriptor) {
            .position = static_cast<int>(i),
            .size = ds.size(),
            .detections = ds.data()
        });
    }
    debug(format("done with runDetectionsOnVideo, retCode: %d", retCode));
    return retCode;
}

/*-----------------------------------------------------------------------*/
/*-----------------------------Drawing API-------------------------------*/
/*-----------------------------------------------------------------------*/

int drawDetectionsOnImage(string sourceImagePath, string finalImagePath,
                          PositionalFrameObjectDetectionDescriptor& pds,
                          double scale = 1.2) {
    debug("in drawDetectionsOnImage");
    Mat source, result;
    int retCode = readImageFile(source, sourceImagePath, IMREAD_COLOR);
    if ( retCode != 0 ) {
        return retCode;
    }
    
    source.copyTo(result);
    
    for (long i = 0; i < pds.size; i++) {
        Scalar color = colors[i%8];
        ObjectDetectionDescriptor d = pds.detections[i];
        ExportableRectangle r = d.rect;
        rectangle(result, Point(cvRound(r.x * scale), cvRound(r.y * scale)),
                  Point(cvRound((r.x + r.width-1) * scale),
                        cvRound((r.y + r.height-1) * scale)),
                  color, 3, 8, 0);

    }
    
    debug(format("done with drawDetectionsOnImage, retCode: %d", retCode));
    return static_cast<int>(imwrite(finalImagePath, result));
}

/*-----------------------------------------------------------------------*/
/*---------------------------C API collection----------------------------*/
/*-----------------------------------------------------------------------*/

extern "C" const char* ExportableRectangle_toString(struct ExportableRectangle *ExportableRectangle);
const char* ExportableRectangle_toString(struct ExportableRectangle *ExportableRectangle) {
    return stringToChar(toString(*ExportableRectangle));
}

extern "C" const char* ObjectDetectionDescriptor_toString(struct ObjectDetectionDescriptor *ObjectDetectionDescriptor);
const char* ObjectDetectionDescriptor_toString(struct ObjectDetectionDescriptor *ObjectDetectionDescriptor) {
    return stringToChar(toString(*ObjectDetectionDescriptor));
}

extern "C" const char * PositionalFrameObjectDetectionDescriptor_toString(struct PositionalFrameObjectDetectionDescriptor* PositionalFrameObjectDetectionDescriptor);
const char * PositionalFrameObjectDetectionDescriptor_toString(struct PositionalFrameObjectDetectionDescriptor* PositionalFrameObjectDetectionDescriptor) {
    return stringToChar(toString(*PositionalFrameObjectDetectionDescriptor));
}

extern "C" int imageToMatrix(const char* imagePath, int option, struct ExportableMat *exMat);
int imageToMatrix(const char* imagePath, int option, struct ExportableMat *ExportableMat) {
    return imageToMatrix(string(imagePath), option, *ExportableMat);
}

extern "C" int runClassificationsOnImage(const char* classifierPath, const char* imagePath, struct PositionalFrameObjectDetectionDescriptor *pds);
int runClassificationsOnImage(const char* classifierPath, const char* imagePath, struct PositionalFrameObjectDetectionDescriptor *PositionalFrameObjectDetectionDescriptor) {
    return runClassificationsOnImage(string(classifierPath), string(imagePath), *PositionalFrameObjectDetectionDescriptor);
}

extern "C" int processVideoFile(const char* classifierPath, const char* videoFilePath, struct ExportableRectanglesPerFrame* exportableResult);
int processVideoFile(const char* classifierPath, const char* videoFilePath, struct ExportableRectanglesPerFrame* ExportableRectanglesPerFrame) {
    return processVideoFile(string(classifierPath), string(videoFilePath), *ExportableRectanglesPerFrame);
}

extern "C" int runDetectionsOnVideo(const char* videoFilePath, const char* modelPath,
                                    const char* modelWeights, const char* cocoaClassesFilePath,
                                    double confidenceThresholdMin,
                                    double confidenceThresholdMax);
int runDetectionsOnVideo(const char* videoFilePath, const char* modelPath,
                         const char* modelWeights, const char* cocoaClassesFilePath,
                         double confidenceThresholdMin,
                         double confidenceThresholdMax) {
    return runDetectionsOnVideo(string(videoFilePath), string(modelPath),
                                string(modelWeights), string(cocoaClassesFilePath),
                                confidenceThresholdMin=confidenceThresholdMin,
                                confidenceThresholdMax=confidenceThresholdMax);
}

extern "C" int runDetectionsOnImage(const char* imagePath, const char* modelPath,
                                    const char* modelWeights, const char* cocoaClassesFilePath,
                                    struct PositionalFrameObjectDetectionDescriptor* PositionalFrameObjectDetectionDescriptor,
                                    double confidenceThresholdMin,
                                    double confidenceThresholdMax);
int runDetectionsOnImage(const char* imagePath, const char* modelPath,
                         const char* modelWeights, const char* cocoaClassesFilePath,
                         struct PositionalFrameObjectDetectionDescriptor* PositionalFrameObjectDetectionDescriptor,
                         double confidenceThresholdMin,
                         double confidenceThresholdMax) {

    return runDetectionsOnImage(string(imagePath), string(modelPath), string(modelWeights),
                                string(cocoaClassesFilePath),
                                *PositionalFrameObjectDetectionDescriptor,
                                confidenceThresholdMin=confidenceThresholdMin,
                                confidenceThresholdMax=confidenceThresholdMax);
}

extern "C" int drawDetectionsOnImage(const char* sourceImagePath, const char* finalImagePath, struct PositionalFrameObjectDetectionDescriptor *PositionalFrameObjectDetectionDescriptor, double scale);
int drawDetectionsOnImage(const char* sourceImagePath, const char* finalImagePath, struct PositionalFrameObjectDetectionDescriptor *PositionalFrameObjectDetectionDescriptor, double scale) {
    return drawDetectionsOnImage(string(sourceImagePath), string(finalImagePath), *PositionalFrameObjectDetectionDescriptor, scale);
}
