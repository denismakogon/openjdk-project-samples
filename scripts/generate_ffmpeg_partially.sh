#!/usr/bin/env bash

set -xe

package="com.openjdk.samples.ffmpeg"
package_dir="src/main/java/$(echo ${package} | tr '.' '/')"
include_path="/usr/local/include"

if [[ ! -d ${package_dir} ]]; then
  if [[ $OSTYPE == 'darwin'* ]]; then
    echo "macos is not supported yet"
    exit 1
  fi

  # ffmpeg.FilesWalk requirements
  jextract --source -t ${package}.libavcoded.avcodec -I ${include_path}/libavcodec/ ${include_path}/libavcodec/avcodec.h --output src/main/java -l "${LD_LIBRARY_PATH}/libavcodec.so"
  jextract --source -t ${package}.libavformat.avformat -I ${include_path}/libavformat/ ${include_path}/libavformat/avformat.h --output src/main/java  -l "${LD_LIBRARY_PATH}/libavformat.so"
  jextract --source -t ${package}.libavformat.avio -I ${include_path}/libavformat/ ${include_path}/libavformat/avio.h --output src/main/java -l "${LD_LIBRARY_PATH}/libavformat.so"

  # ffmpeg.Versions requirements
  jextract --source -t ${package}.libavcoded.version -I ${include_path}/libavcodec/ ${include_path}/libavcodec/version.h --output src/main/java -l "${LD_LIBRARY_PATH}/libavcodec.so"
  jextract --source -t ${package}.libavformat.version -I ${include_path}/libavformat/ ${include_path}/libavformat/version.h --output src/main/java -l "${LD_LIBRARY_PATH}/libavformat.so"
  jextract --source -t ${package}.libavutil.error  -I ${include_path}/libavutil  ${include_path}/libavutil/error.h --output src/main/java -l "${LD_LIBRARY_PATH}/libavutil.so"

else
  echo "package ${package} already exists!"
fi
