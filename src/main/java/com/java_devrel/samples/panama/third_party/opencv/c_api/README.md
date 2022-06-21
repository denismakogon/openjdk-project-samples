#  Attempt to build a car speed detection tool

## Tools

- [OpenCV 4.5+](https://github.com/opencv/opencv)
- [DLib](https://github.com/davisking/dlib)

### OpenCV

```shell
brew install opencv
```

### OpenCV C API library

Build a library:
```shell
g++ -I/usr/local/Cellar/opencv/4.5.5_2/include/opencv4/ \
    -L/usr/local/Cellar/opencv/4.5.5_2/lib \
    -lopencv_gapi -lopencv_stitching -lopencv_aruco \
    -lopencv_bgsegm -lopencv_bioinspired -lopencv_ccalib \
    -lopencv_dnn_objdetect -lopencv_dpm -lopencv_face \
    -lopencv_fuzzy -lopencv_hfs -lopencv_img_hash \
    -lopencv_line_descriptor -lopencv_quality \
    -lopencv_reg -lopencv_rgbd -lopencv_saliency \
    -lopencv_stereo -lopencv_structured_light \
    -lopencv_phase_unwrapping -lopencv_superres \
    -lopencv_optflow -lopencv_surface_matching \
    -lopencv_tracking -lopencv_datasets -lopencv_text \
    -lopencv_dnn -lopencv_plot -lopencv_videostab \
    -lopencv_video -lopencv_xfeatures2d -lopencv_shape \
    -lopencv_ml -lopencv_ximgproc -lopencv_xobjdetect \
    -lopencv_objdetect -lopencv_calib3d -lopencv_features2d \
    -lopencv_highgui -lopencv_videoio -lopencv_imgcodecs \
    -lopencv_flann -lopencv_xphoto -lopencv_photo \
    -lopencv_imgproc -lopencv_core \
    -std=c++11  -dynamiclib c_api.cpp \
    -o /usr/local/lib/libopencv_c_api.dylib \
    -current_version 1.0 -compatibility_version 1.0
```

a library will be stored at:
```shell
/usr/local/lib/libopencv_c_api.dylib
```

## Test CPP Main

```shell
g++ -std=c++11 \
  -I. -L. /usr/local/lib/libopencv_c_api.dylib \
  -Wall -Werror \
  -ferror-limit=1 \
  main.cpp
```
```shell
./a.out
```

## Assets folder

```shell 
assets
├── car_classfier.xml
├── coco.names
├── images
│   ├── 1.png
│   └── 2.jpeg
├── yolov3-320.cfg
└── yolov3-320.weights
```
