#!/usr/bin/env bash

set -xe

package="$(echo "com.java_devrel.samples.opencv" | tr '/' '.')"
package_dir="src/main/java/$(echo "${package}" | tr '.' '/')"
base_folder="${PWD}/src/main/java/com/java_devrel/samples/panama/third_party/opencv/c_api"
header_path="${base_folder}/c_api.h"

extension="so"

if [[ $OSTYPE == 'darwin'* ]]; then
  extension="dylib"
fi

lib_path="/usr/local/lib/libopencv_c_api.${extension}"


if [[ ! -d ${package_dir} ]]; then
  jextract --source -t "${package}" -I "${base_folder}" --output src/main/java "${header_path}" -l "${lib_path}"
else
  echo "package ${package} already exists!"
fi
