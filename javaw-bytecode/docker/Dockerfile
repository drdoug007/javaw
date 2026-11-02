# Local stand-in for .github/workflows/bytecode-tests.yml: the same Ubuntu release, Ubuntu's wabt package, Temurin 17
# (the JDK 17 oracle) and 21 (the build JDK), and Node 24. Build and run it with docker-tests.sh.
FROM eclipse-temurin:17-jdk-noble AS jdk17
FROM eclipse-temurin:21-jdk-noble AS jdk21
FROM node:24-bookworm-slim AS node

FROM ubuntu:24.04
ARG MAVEN_VERSION=3.9.9
RUN apt-get update && apt-get install -y --no-install-recommends wabt python3 curl ca-certificates git \
    && rm -rf /var/lib/apt/lists/*
COPY --from=jdk17 /opt/java/openjdk /opt/jdk17
COPY --from=jdk21 /opt/java/openjdk /opt/jdk21
COPY --from=node /usr/local/bin/node /usr/local/bin/node
RUN curl -fsSL "https://archive.apache.org/dist/maven/maven-3/${MAVEN_VERSION}/binaries/apache-maven-${MAVEN_VERSION}-bin.tar.gz" \
        | tar -xz -C /opt && ln -s "/opt/apache-maven-${MAVEN_VERSION}/bin/mvn" /usr/local/bin/mvn
# Hosted runners use a UTF-8 locale; without one the JVM encodes file names as ASCII and non-ASCII fixtures fail.
ENV LANG=C.UTF-8 LC_ALL=C.UTF-8 JAVA_HOME=/opt/jdk21 JAVA_HOME_17=/opt/jdk17 PATH=/opt/jdk21/bin:$PATH
WORKDIR /work
