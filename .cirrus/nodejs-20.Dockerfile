ARG CIRRUS_AWS_ACCOUNT=275878209202
FROM ${CIRRUS_AWS_ACCOUNT}.dkr.ecr.eu-central-1.amazonaws.com/base:j17-latest

USER root

# inspired by https://github.com/nodejs/help/wiki/Installation
ARG NODE_VERSION=20.10.0
ARG DISTRO=linux-x64
RUN curl -o node.tar.gz -fsSL "https://nodejs.org/dist/v${NODE_VERSION}/node-v${NODE_VERSION}-linux-x64.tar.gz"
RUN mkdir -p /usr/local/lib/nodejs
RUN tar -xzvf node.tar.gz -C /usr/local/lib/nodejs

USER sonarsource

RUN echo "NODE_VERSION=20.10.0" >> ~/.profile
RUN echo "DISTRO=linux-x64" >> ~/.profile
RUN echo "export PATH=/usr/local/lib/nodejs/node-v${NODE_VERSION}-${DISTRO}/bin:${PATH}" >> ~/.profile
