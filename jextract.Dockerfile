FROM openjdk:19-bullseye as jdk19-source
FROM openjdk:18-bullseye as jextract-build-stage

COPY --from=jdk19-source /usr/local/openjdk-19 /usr/local/openjdk-19

WORKDIR tmp/
RUN apt-get update && apt-get install wget tar xz-utils git -qy --no-install-recommends
RUN mkdir deps && \
    wget -O deps/LLVM.tar.gz https://github.com/llvm/llvm-project/releases/download/llvmorg-13.0.1/clang+llvm-13.0.1-x86_64-linux-gnu-ubuntu-20.04.tar.xz && \
    tar -xvf deps/LLVM.tar.gz -C deps && rm -fr deps/LLVM.tar.gz && \
    git clone https://github.com/openjdk/jextract.git && \
    cd jextract && \
    sh ./gradlew -Pjdk19_home=/usr/local/openjdk-19 -Pllvm_home=../deps/clang+llvm-13.0.1-x86_64-linux-gnu-ubuntu-20.04 clean verify && \
    rm -fr /tmp/deps

FROM openjdk:19-bullseye as jdk19-custom-runtime

COPY --from=jextract-build-stage /tmp/jextract/build/jextract/ /usr/local/jextract
ENV PATH=${PATH}:/usr/local/jextract/bin

# installing additional necessary dev packages
RUN curl "https://raw.githubusercontent.com/denismakogon/openjdk-project-samples/master/scripts/install_build_packages.sh" | /bin/bash
# installing maven
RUN curl "https://raw.githubusercontent.com/denismakogon/openjdk-project-samples/master/scripts/install_maven.sh" | /bin/bash
