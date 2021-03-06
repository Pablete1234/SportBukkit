FROM openjdk:8-jdk-alpine

RUN apk add --no-cache curl tar bash git ruby-rake unzip && apk add patch --update

ARG MAVEN_VERSION=3.5.2
ARG USER_HOME_DIR=/
ARG SHA=707b1f6e390a65bde4af4cdaf2a24d45fc19a6ded00fff02e91626e3e42ceaff
ARG BASE_URL=https://apache.osuosl.org/maven/maven-3/${MAVEN_VERSION}/binaries

RUN mkdir -p /usr/share/maven /usr/share/maven/ref \
  && curl -fsSL -o /tmp/apache-maven.tar.gz ${BASE_URL}/apache-maven-${MAVEN_VERSION}-bin.tar.gz \
  && echo "${SHA}  /tmp/apache-maven.tar.gz" | sha256sum -c - \
  && tar -xzf /tmp/apache-maven.tar.gz -C /usr/share/maven --strip-components=1 \
  && rm -f /tmp/apache-maven.tar.gz \
  && ln -s /usr/share/maven/bin/mvn /usr/bin/mvn

ENV MAVEN_HOME /usr/share/maven
ENV MAVEN_OPTS "-XX:+TieredCompilation -XX:TieredStopAtLevel=1 -Dmaven.repo.local=/workspace/.m2"
ENV SPORTBUKKIT_CLOUD=true

WORKDIR /workspace
ENTRYPOINT rake
