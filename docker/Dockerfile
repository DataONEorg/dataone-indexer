# Build from parent dir with a command like: `nerdctl build -t dataone-index-worker:2.4.0 -f docker/Dockerfile --build-arg TAG=2.4.0 .`
# Use an OpenJDK runtime as a parent image
# Note: the prior alpine-based openjdk image had network DNS issues, so replacing with Eclipse Temurin
FROM eclipse-temurin:17.0.8.1_1-jre-jammy

ARG TAG=2.0.0
ENV TAG=${TAG}
ENV DATAONE_INDEXER_CONFIG=/etc/dataone/dataone-indexer.properties

# Set the working directory 
WORKDIR /var/lib/dataone-indexer

RUN apt update && apt -y install \
    bash \
    figlet \
    curl \
    nano

#Add a user with name d1indexer
RUN useradd d1indexer

# The most recently built jar file is copied from the maven build directory to this dir by maven, so that
# it can be copied to the image.
COPY ../target/dataone-index-worker-${TAG}-shaded.jar .
COPY ./docker/entrypoint.sh .

# Change the ownership of the jar and sh files
RUN chown d1indexer dataone-index-worker-${TAG}-shaded.jar
RUN chown d1indexer entrypoint.sh

#Run Container as d1indexer
USER d1indexer

# Connect this image to a GitHub repository
LABEL org.opencontainers.image.source https://github.com/dataoneorg/dataone-indexer

# Run the Worker process
CMD ./entrypoint.sh
