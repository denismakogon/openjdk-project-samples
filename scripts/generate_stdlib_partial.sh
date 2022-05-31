#!/usr/bin/env bash

set -xe

stdlib_partial=${1:-"stdio"}


package="$(echo "com.openjdk.samples.stdlib.${stdlib_partial}" | tr '/' '.')"
package_dir="src/main/java/$(echo "${package}" | tr '.' '/')"
include_path="/usr/include"


if [[ ! -d ${package_dir} ]]; then
  if [[ $OSTYPE == 'darwin'* ]]; then
    include_path="/Applications/Xcode.app/Contents/Developer/Platforms/MacOSX.platform/Developer/SDKs/MacOSX.sdk/usr/include"
  fi
  jextract --source -t "${package}" -I ${include_path} "${include_path}/${stdlib_partial}.h" --output src/main/java
else
  echo "package ${package} already exists!"
fi
