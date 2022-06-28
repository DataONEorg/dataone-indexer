# Build from parent dir with a command like: `nerdctl build -t dataone-index-worker:2.4.0 -f docker/Dockerfile --build-arg TAG=2.4.0 .`
# Use an OpenJDK runtime as a parent image
FROM openjdk:8-jre-alpine 

ARG TAG=2.0.0
ENV TAG=${TAG}
ENV DATAONE_INDEXER_CONFIG=/etc/dataone/dataone-indexer.properties

# Set the working directory 
WORKDIR /var/lib/dataone-indexer

RUN apk update
# bash is needed by the openssl install
RUN apk add bash
#RUN apk add g++ libc-dev openssl-dev libxml2 libxml2-dev

# The most recently built jar file is copied from the maven build directory to this dir by maven, so that
# it can be copyied to the image.
COPY ../target/dataone-index-worker-${TAG}-shaded.jar .

# Run the Worker process
# Note: docker --build-arg only allows one argument (one token only, multiple tokens inside quotes doesn't work, so have
#   to specify java options directly on command line.
# Set classpath to include /opt/local/metadig/log4j.properties, if it exists, so that logging can be changed without
# having to rebuild the container. Note that on k8s, this dir is mapped to the persistent volume, so will be /data/metadig/log4j.properties
# From Jing, the command he is using to launch locally
# java -cp target/dataone-index-worker-2.4.0-shaded.jar org.dataone.cn.index.IndexWorker
CMD java -Dlog4j2.formatMsgNoLookups=true -XX:+UnlockExperimentalVMOptions -XX:+UseCGroupMemoryLimitForHeap -XX:+UseSerialGC -cp ./dataone-index-worker-${TAG}-shaded.jar org.dataone.cn.index.IndexWorker
#CMD sh -c 'trap "exit" TERM; while true; do sleep 1; done'
