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
/*------------------------------toString API-----------------------------*/
/*-----------------------------------------------------------------------*/

string toString(ExportableRectangle& object) {
    return format("x=%d y=%d width=%d heights=%d",
                  object.x, object.y, object.width, object.height);
}

string toString(ObjectDetectionDescriptor& object) {
    int classId = object.classId;
    float confidence = object.confidence;
    return format("class ID: %d, confidence: %f, rectange: [%s]\n", classId, confidence, toString(object.rect).c_str());
}

string toString(PositionalFrameObjectDetectionDescriptor& object) {
    string res = "\n";
    for (size_t i = 0; i < object.size; i++) {
        res += toString(object.detections[i]);
    }
    return format("position: %d, detections: %s", object.position, res.c_str());
}

/*-----------------------------------------------------------------------*/
/*----------------------------------etc----------------------------------*/
/*-----------------------------------------------------------------------*/

void exportVectorOf(vector<Rect> detections, ExportableRectangle* result) {
    for(size_t i = 0; i < detections.size(); i++) {
        Rect r = detections[i];
        result[i] = (ExportableRectangle) {
            .x = r.x, .y = r.y,
            .width = r.width,
            .height = r.height
        };
    }
}

void exportVectorOf(vector<ExportableRectangles> rectsPerFrame, ExportableRectanglesPerFrame& result) {
    result = (ExportableRectanglesPerFrame) {};
    result.size = static_cast<int>(rectsPerFrame.size());
    result.array = rectsPerFrame.data();
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
    string imageFile = findFile(imagePath, true, true);
    image = imread(imageFile, option);
    if( image.empty() ) {
        return -1;
    }
    return 0;
}

int imageToMatrix(string imagePath, int option, ExportableMat& exMat) {
    Mat img;
    
    if (!readImageFile(img, imagePath, option)) {
        return -1;
    }
    
    matToByteArray(img, exMat);
    return 0;
}

/*-----------------------------------------------------------------------*/
/*--------------------------Classfiers API-------------------------------*/
/*-----------------------------------------------------------------------*/

void prepareImageForClassification(Mat& img, Mat& result) {
    Mat blurred, dilated, kernel;
    GaussianBlur(img, blurred, Size(5, 5), 0);
    dilate(blurred, dilated, Mat(), Point(-1, -1));
    kernel = getStructuringElement(MORPH_ELLIPSE, Size(2, 2));
    morphologyEx(dilated, result, MORPH_CLOSE, kernel);
}

int prepareImageForClassification(string imagePath, Mat& resultingImage) {
    Mat img;
    if ( !readImageFile(img, imagePath, COLOR_BGR2GRAY) ) {
        return -1;
    }
    prepareImageForClassification(img, resultingImage);
    return 0;
}

vector<Rect> runClassifier(CascadeClassifier& cascade, Mat& source, Mat& result) {
    source.copyTo(result);

    Mat preprocessedSource;
    prepareImageForClassification(source, preprocessedSource);

    vector<Rect> detections;
    cascade.detectMultiScale(preprocessedSource, detections, 1.1, 1);
    return detections;
}

int runClassification(string classifierPath, string imagePath, vector<Rect>& results) {
    cout << "in runClassification" << endl;
    CascadeClassifier cascade;
    if ( !cascade.load(findFile(classifierPath)) ) {
        cout << "classifier not found!" << endl;
        return -1;
    };
    cout << "classifier loaded" << endl;

    Mat source, finalImg;
    readImageFile(source, imagePath, IMREAD_COLOR);
    cout << "image loaded" << endl;

    results = runClassifier(cascade, source, finalImg);
    cout << "classification completed" << endl;
    return 0;
}

vector<Rect> runClassificationsOnImage(string classifierPath, string imagePath) {
    vector<Rect> results;
    int res = runClassification(classifierPath, imagePath, results);
    if ( res != 0 ) {
        cerr << "something went wrong!" << endl;
    }
    return results;
}

void runClassificationsOnImage(string classifierPath, string imagePath, PositionalFrameObjectDetectionDescriptor& pds) {
    vector<Rect> intermediate = runClassificationsOnImage(string(classifierPath), string(imagePath));

    sort(
         intermediate.begin(), intermediate.end(),
         [](Rect a, Rect b) {
             return a.x < b.x;
         }
     );
    
    vector<ObjectDetectionDescriptor> dss;
    for (auto r: intermediate) {
        dss.push_back((ObjectDetectionDescriptor) {
            .classId = -1,
            .confidence = -1.0,
            .rect = (ExportableRectangle) {
                .x = r.x, .y = r.y,
                .width = r.width,
                .height = r.height
            }
        });
    }
    
    pds = (PositionalFrameObjectDetectionDescriptor) {
        .position = 0,
        .size = static_cast<int>(dss.size()),
        .detections = dss.data()
    };

}

/*-----------------------------------------------------------------------*/
/*-----------------------------Video API---------------------------------*/
/*-----------------------------------------------------------------------*/

vector<Mat> readAllFrames(string videoFilePath, vector<Mat>& frames) {
    VideoCapture capture(findFile(videoFilePath));
    if ( !capture.isOpened() ) {
        return frames;
    }
    int frameCount = capture.get(CAP_PROP_FRAME_COUNT);

    frames = vector<Mat>(frameCount);
    Mat currentFrame;
    for (size_t i = 0; i < frameCount; i ++) {
        capture >> currentFrame;
        if (currentFrame.empty()) {
            break;
        }
        frames[i] = currentFrame;
    }
    return frames;
}

int processVideoFile(string classifierPath, string videoFilePath, vector<ExportableRectangles>& result) {
    CascadeClassifier cascade;
    if ( !cascade.load(findFile(classifierPath)) ) {
        cout << "classifier not found!" << endl;
        return -1;
    };
    vector<Mat> frames;
    readAllFrames(videoFilePath, frames);

    Mat currentFrame, finalImg;
    result = vector<ExportableRectangles>(frames.size());
    for (size_t i = 0; i < frames.size(); i++) {
        vector<Rect> intermediate = runClassifier(cascade, frames[i], finalImg);
        ExportableRectangle* res = new ExportableRectangle[intermediate.size()];

        exportVectorOf(intermediate, res);
        result[i] = (ExportableRectangles) {
            .array = res,
            .size = static_cast<int>(intermediate.size())
        };

    }
    return 0;
}

int processVideoFile(string classifierPath, string videoFilePath, ExportableRectanglesPerFrame& exportableResult) {
    vector<ExportableRectangles> result;
    int res = processVideoFile(string(classifierPath), string(videoFilePath), result);
    if ( res != 0 ) {
        return res;
    }
    exportVectorOf(result, exportableResult);
    return 0;
}

/*-----------------------------------------------------------------------*/
/*-------------------------------DNN API---------------------------------*/
/*-----------------------------------------------------------------------*/

void setupDNN(string modelPath, string modelWeights, Net& net, int backend=DNN_BACKEND_DEFAULT, int target=DNN_TARGET_CPU) {
    net = readNetFromDarknet(findFile(modelPath), findFile(modelWeights));
    net.setPreferableBackend(backend);
    net.setPreferableTarget(target);
}


void inputPreprocess(const Mat& frame, Net& net, Size inpSize, float scale,
                       const Scalar& mean, bool swapRB) {
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
}

void formatDetections(Mat& frame, vector<Mat>& outs, Net& net, vector<ObjectDetectionDescriptor>& ds, float confidenceThreshold=0.4, int backend=DNN_BACKEND_DEFAULT) {
    static std::vector<int> outLayers = net.getUnconnectedOutLayers();
    static std::string outLayerType = net.getLayer(outLayers[0])->type;

    for (size_t i = 0; i < outs.size(); ++i) {
        float* data = (float*)outs[i].data;
        for (int j = 0; j < outs[i].rows; ++j, data += outs[i].cols) {
            Mat scores = outs[i].row(j).colRange(5, outs[i].cols);
            Point classIdPoint;
            double confidence;
            minMaxLoc(scores, 0, &confidence, 0, &classIdPoint);
            
            if (confidence > confidenceThreshold) {
                int centerX = (int)(data[0] * frame.cols);
                int centerY = (int)(data[1] * frame.rows);
                int width = (int)(data[2] * frame.cols);
                int height = (int)(data[3] * frame.rows);
                
                int left = centerX - width / 2;
                int top = centerY - height / 2;

                ObjectDetectionDescriptor d = (ObjectDetectionDescriptor) {
                    .classId = classIdPoint.x,
                    .confidence = confidence,
                    .rect = (ExportableRectangle) {
                        .x = left,
                        .y = top,
                        .width = width,
                        .height = height
                    }
                };
                ds.push_back(d);
            }
        }
    }
}

void runtObjectDetectionsOn(Mat& img, Net& net, vector<ObjectDetectionDescriptor>& ds) {
    int inputSize = 320;
    vector<Mat> outputs;

    inputPreprocess(img, net, Size(inputSize, inputSize), (float) 1/255, Scalar(0, 0, 0), false);
    net.forward(outputs, net.getUnconnectedOutLayersNames());
    formatDetections(img, outputs, net, ds);
}

/*-----------------------------------------------------------------------*/
/*---------------------------DNN detections API--------------------------*/
/*-----------------------------------------------------------------------*/

void runDetectionsOn(string imagePath, string modelPath, string modelWeights, PositionalFrameObjectDetectionDescriptor& pds) {
    dnn::Net net;
    Mat frame;
    vector<ObjectDetectionDescriptor> ds;

    readImageFile(frame, imagePath, IMREAD_COLOR);
    setupDNN(modelPath, modelWeights, net);
    runtObjectDetectionsOn(frame, net, ds);

    sort(
         ds.begin(), ds.end(),
         [](ObjectDetectionDescriptor a, ObjectDetectionDescriptor b) {
             return a.rect.x <= b.rect.x;
         }
     );
    pds = (PositionalFrameObjectDetectionDescriptor) {
        .position = 0,
        .size = static_cast<int>(ds.size()),
        .detections = ds.data()
    };
}

void runDetectionsOnVideo(string videoFilePath, string modelPath, string modelWeights) {
    dnn::Net net;
    vector<Mat> frames;
    vector<PositionalFrameObjectDetectionDescriptor> detectionsPerFrame;

    readAllFrames(videoFilePath, frames);
    setupDNN(modelPath, modelWeights, net);

    for (size_t i = 0; i < frames.size(); i++ ) {
        vector<ObjectDetectionDescriptor> ds;
        runtObjectDetectionsOn(frames[i], net, ds);
        detectionsPerFrame[i] = (PositionalFrameObjectDetectionDescriptor) {
            .position = static_cast<int>(i),
            .size = static_cast<int>(ds.size()),
            .detections = ds.data()
        };
    }
}

void runDetectionsOnImage(string imagePath, string modelPath, string modelWeights, PositionalFrameObjectDetectionDescriptor& pds) {
    runDetectionsOn(imagePath, modelPath, modelWeights, pds);
}

/*-----------------------------------------------------------------------*/
/*-----------------------------Drawing API-------------------------------*/
/*-----------------------------------------------------------------------*/

bool drawDetectionsOnImage(string sourceImagePath, string finalImagePath, PositionalFrameObjectDetectionDescriptor& pds, double scale = 1.2) {
    Mat source, result;
    readImageFile(source, sourceImagePath, IMREAD_COLOR);
    source.copyTo(result);
    
    for (size_t i = 0; i < pds.size; i++) {
        Scalar color = colors[i%8];
        ObjectDetectionDescriptor d = pds.detections[i];
        ExportableRectangle r = d.rect;
        rectangle(result, Point(cvRound(r.x * scale), cvRound(r.y * scale)),
                  Point(cvRound((r.x + r.width-1) * scale),
                        cvRound((r.y + r.height-1) * scale)),
                  color, 3, 8, 0);

    }
    return imwrite(finalImagePath, result);
}

/*-----------------------------------------------------------------------*/
/*---------------------------C API collection----------------------------*/
/*-----------------------------------------------------------------------*/

extern "C" const char* ExportableRectangle_toString(struct ExportableRectangle *object);
const char* ExportableRectangle_toString(struct ExportableRectangle *object) {
    string str = toString(*object);
    const char* s = new char[str.length()];
    s = str.data();
    return s;
}

extern "C" const char* ObjectDetectionDescriptor_toString(struct ObjectDetectionDescriptor *object);
const char* ObjectDetectionDescriptor_toString(struct ObjectDetectionDescriptor *object) {
    string str = toString(*object);
    const char* s = new char[str.length()];
    s = str.data();
    return s;
}

extern "C" const char * PositionalFrameObjectDetectionDescriptor_toString(struct PositionalFrameObjectDetectionDescriptor* object);
const char * PositionalFrameObjectDetectionDescriptor_toString(struct PositionalFrameObjectDetectionDescriptor* object) {
    string str = toString(*object);
    const char* s = new char[str.length()];
    s = str.data();
    return s;
}

extern "C" int imageToMatrix(const char* imagePath, int option, struct ExportableMat *exMat);
int imageToMatrix(const char* imagePath, int option, struct ExportableMat *exMat) {
    return imageToMatrix(string(imagePath), option, *exMat);
}

extern "C" void runClassificationsOnImage(const char* classifierPath, const char* imagePath, struct PositionalFrameObjectDetectionDescriptor *pds);
void runClassificationsOnImage(const char* classifierPath, const char* imagePath, struct PositionalFrameObjectDetectionDescriptor *pds) {
    runClassificationsOnImage(string(classifierPath), string(imagePath), *pds);
}

extern "C" int processVideoFile(const char* classifierPath, const char* videoFilePath, struct ExportableRectanglesPerFrame* exportableResult);
int processVideoFile(const char* classifierPath, const char* videoFilePath, struct ExportableRectanglesPerFrame* exportableResult) {
    return processVideoFile(string(classifierPath), string(videoFilePath), *exportableResult);
}

extern "C" void runDetectionsOnVideo(const char* videoFilePath, const char* modelPath, const char* modelWeights);
void runDetectionsOnVideo(const char* videoFilePath, const char* modelPath, const char* modelWeights) {
    runDetectionsOnVideo(string(videoFilePath), string(modelPath), string(modelWeights));
}

extern "C" void runDetectionsOnImage(const char* imagePath, const char* modelPath, const char* modelWeights,
                                     struct PositionalFrameObjectDetectionDescriptor* pds);
void runDetectionsOnImage(const char* imagePath, const char* modelPath, const char* modelWeights,
                          struct PositionalFrameObjectDetectionDescriptor* pds) {
    runDetectionsOnImage(string(imagePath), string(modelPath), string(modelWeights), *pds);
}

extern "C" bool drawDetectionsOnImage(const char* sourceImagePath, const char* finalImagePath, struct PositionalFrameObjectDetectionDescriptor *pds, double scale);
bool drawDetectionsOnImage(const char* sourceImagePath, const char* finalImagePath, struct PositionalFrameObjectDetectionDescriptor *pds, double scale) {
    return drawDetectionsOnImage(string(sourceImagePath), string(finalImagePath), *pds, scale);
}
