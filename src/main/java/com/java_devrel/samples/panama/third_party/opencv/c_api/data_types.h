//
//  data_types.h
//  c_api
//
//  Created by Denis Makogon on 17.06.2022.
//

#ifndef data_types_h
#define data_types_h

struct ExportableMat {
    char* matContent;
    int width;
    int height;
};

struct ExportableRectangle {
    int x, y, width, height;
};

struct ExportableRectangles {
    struct ExportableRectangle *array;
    int size;
};

struct ExportableRectanglesPerFrame {
    struct ExportableRectangles *array;
    int size;
};

struct ObjectDetectionDescriptor {
    int classId;
    double confidence;
    struct ExportableRectangle rect;
};

struct PositionalFrameObjectDetectionDescriptor {
    int position;
    int size;
    struct ObjectDetectionDescriptor* detections;
};

#endif /* data_types_h */
