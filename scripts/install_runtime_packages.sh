#!/usr/bin/env bash

set -ex


apt-get update && apt-get install -yq --no-install-recommends \
    gcc-10-base \
    libc6 \
    libc-dev-bin \
    libstdc++6 \
    libcrypt1 \
    libnsl2 \
    libtirpc3 \
    linux-libc-dev \
    libmfx1 \
    libvdpau1 \
    libx11-6 \
    libxau6 \
    libxcb1 \
    libxdmcp6
