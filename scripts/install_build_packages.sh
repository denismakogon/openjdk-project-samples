#!/usr/bin/env bash

set -ex


apt-get install -yq --no-install-recommends \
    linux-libc-dev \
    libstdc++-10-dev \
    libmfx-dev \
    libvdpau-dev \
    libc6-dev-i386 #\
    #gcc-multilib
