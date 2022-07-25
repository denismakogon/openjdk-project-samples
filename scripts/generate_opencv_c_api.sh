#!/usr/bin/env bash

set -xe

package="$(echo "com.java_devrel.samples.opencv" | tr '/' '.')"
package_dir="src/main/java/$(echo "${package}" | tr '.' '/')"
base_folder="/Users/denismakogon/go/src/github.com/denismakogon/cpp-to-c-api/src"
include_folder="${base_folder}/includes"
header_path="${include_folder}/c_api.h"

extension="so"

if [[ $OSTYPE == 'darwin'* ]]; then
  extension="dylib"
fi

lib_path="/usr/local/lib/libopencv_c_api.${extension}"


if [[ ! -d ${package_dir} ]]; then
  jextract --source -t "${package}" -I "${include_folder}" -I "${base_folder}" --output src/main/java "${header_path}" -l "${lib_path}"
else
  echo "package ${package} already exists!"
fi
