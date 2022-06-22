#!/usr/bin/env bash

set -ex

MAVEN_VERSION=${MAVEN_VERSION:-"3.8.6"}
BASE_URL="https://downloads.apache.org/maven/maven-3/${MAVEN_VERSION}/binaries"
MAVEN_BINARY_URL=${BASE_URL}/apache-maven-${MAVEN_VERSION}-bin.tar.gz
MAVEN_BINARY_SHA512_URL=${MAVEN_BINARY_URL}.sha512

mkdir -p /usr/share/maven /usr/share/maven/ref \
  && curl --output /tmp/apache-maven.tar.gz "${MAVEN_BINARY_URL}" \
  && echo "$(curl "$MAVEN_BINARY_SHA512_URL")  /tmp/apache-maven.tar.gz" | sha512sum -c - \
  && tar -xzf /tmp/apache-maven.tar.gz -C /usr/share/maven --strip-components=1 \
  && rm -f /tmp/apache-maven.tar.gz \
  && ln -s /usr/share/maven/bin/mvn /usr/bin/mvn

export MAVEN_HOME=/usr/share/maven
export MAVEN_CONFIG="/root/.m2"
