FROM openjdk:19-bullseye as jdk19-source
FROM openjdk:18-bullseye as jextract-build-stage

COPY --from=jdk19-source /usr/local/openjdk-19 /usr/local/openjdk-19

WORKDIR tmp/
RUN apt-get update && apt-get install wget tar xz-utils git -qy --no-install-recommends
RUN mkdir deps && \
    wget -O deps/LLVM.tar.gz https://github.com/llvm/llvm-project/releases/download/llvmorg-13.0.0/clang+llvm-13.0.0-x86_64-linux-gnu-ubuntu-20.04.tar.xz && \
    tar -xvf deps/LLVM.tar.gz -C deps && rm -fr deps/LLVM.tar.gz && \
    git clone https://github.com/openjdk/jextract.git && \
    cd jextract && \
    sh ./gradlew -Pjdk19_home=/usr/local/openjdk-19 -Pllvm_home=../deps/clang+llvm-13.0.0-x86_64-linux-gnu-ubuntu-20.04 clean verify && \
    rm -fr /tmp/deps

FROM ghcr.io/denismakogon/ffmpeg-debian:5.0.1-build as ffmpeg-shared-libs
FROM openjdk:19-bullseye as jdk19-custom-runtime

# jextract.jmod
#COPY --from=jextract-build-stage /tmp/jextract/build/jmods ${JAVA_HOME}/jmods/
# jextract binary
WORKDIR /tmp/workdir

COPY --from=jextract-build-stage /tmp/jextract/build/jextract/ /usr/local/jextract
COPY --from=ffmpeg-shared-libs /opt/ffmpeg/lib/pkgconfig /usr/local/lib/pkgconfig
COPY --from=ffmpeg-shared-libs /usr/local /usr/local/

ADD scripts/install_maven.sh scripts/install_maven.sh
ADD scripts/install_build_packages.sh scripts/install_build_packages.sh

RUN /bin/bash scripts/install_build_packages.sh && /bin/bash scripts/install_maven.sh

ENV LD_LIBRARY_PATH=/usr/local/lib
ENV PKG_CONFIG_PATH=/usr/local/lib/pkgconfig
ENV PATH=${PATH}:/usr/local/jextract/bin

# building a new custom runtime with jextract module
#RUN jlink --module-path=${JAVA_HOME}/jmods --add-modules=$(java --list-modules | tr '@' ' ' | awk '{print $1}' | tr '\n' ',')org.openjdk.jextract --output /usr/local/jdk
#
#FROM ghcr.io/denismakogon/ffmpeg-debian:5.0.1-build as ffmpeg-shared-libs
#FROM debian:bullseye-slim
#
#COPY --from=jdk19-custom-runtime /usr/local/jdk /usr/local/jdk
#COPY --from=ffmpeg-shared-libs /opt/ffmpeg/lib/pkgconfig /usr/local/lib/pkgconfig
#COPY --from=ffmpeg-shared-libs /usr/local /usr/local/
#
#ENV LD_LIBRARY_PATH=/usr/local/lib
#ENV PKG_CONFIG_PATH=/usr/local/lib/pkgconfig
#ENV JAVA_HOME=/usr/local/jdk
#ENV PATH=${PATH}:${JAVA_HOME}/bin
