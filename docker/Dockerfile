# Build from parent dir with a command like: `nerdctl build -t dataone-index-worker:2.4.0 -f docker/Dockerfile --build-arg TAG=2.4.0 .`
# Use an OpenJDK runtime as a parent image
# Note: the prior alpine-based openjdk image had network DNS issues, so replacing with Eclipse Temurin
FROM eclipse-temurin:17.0.4.1_1-jre-jammy

ARG TAG=2.0.0
ENV TAG=${TAG}
ENV DATAONE_INDEXER_CONFIG=/etc/dataone/dataone-indexer.properties

# Set the working directory 
WORKDIR /var/lib/dataone-indexer

RUN apt update
RUN apt install bash 
RUN apt install figlet
RUN apt install curl
RUN apt install nano

# The most recently built jar file is copied from the maven build directory to this dir by maven, so that
# it can be copied to the image.
COPY ../target/dataone-index-worker-${TAG}-shaded.jar .
COPY ./docker/entrypoint.sh .

# Connect this image to a GitHub repository
LABEL org.opencontainers.image.source https://github.com/dataoneorg/dataone-indexer

# Run the Worker process
CMD ./entrypoint.sh
