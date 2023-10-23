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
COPY --chown=d1indexer ../target/dataone-index-worker-${TAG}-shaded.jar .
COPY --chown=d1indexer ./docker/entrypoint.sh .

#Run Container as d1indexer
USER d1indexer

# Connect this image to a GitHub repository
LABEL org.opencontainers.image.source="https://github.com/dataoneorg/dataone-indexer"
LABEL org.opencontainers.image.title="DataONE Indexer"
LABEL org.opencontainers.image.version=${TAG}

# Run the Worker process
CMD ./entrypoint.sh
