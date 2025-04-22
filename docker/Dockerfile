# Build from parent dir with a command like:
#     `nerdctl build -t dataone-index-worker:2.4.0 -f docker/Dockerfile --build-arg TAG=2.4.0 .`
# Use an OpenJDK runtime as a parent image
FROM eclipse-temurin:17.0.14_7-jre-noble

ARG TAG="NO-TAG"
ENV TAG=${TAG}
ENV DATAONE_INDEXER_CONFIG=/etc/dataone/dataone-indexer.properties

# Set the working directory 
WORKDIR /var/lib/dataone-indexer

RUN apt update && apt -y install \
    bash \
    figlet \
    curl \
    vim

RUN groupadd -g 59997 d1indexer && useradd -u 59997 -g 59997 d1indexer \
    && touch ./livenessprobe && touch ./readinessprobe

# The most recently built jar file is copied from the maven build directory to this dir by maven, so that
# it can be copied to the image.
COPY ./target/dataone-index-worker-${TAG}-shaded.jar .
COPY ./docker/entrypoint.sh .

# Change the ownership of the jar and sh files
RUN chown d1indexer dataone-index-worker-${TAG}-shaded.jar
RUN chown d1indexer entrypoint.sh
RUN chown d1indexer livenessprobe
RUN chown d1indexer readinessprobe

#Run Container as d1indexer
USER 59997:59997

# Connect this image to a GitHub repository
LABEL org.opencontainers.image.source="https://github.com/dataoneorg/dataone-indexer"

# Run the Worker process
CMD ["./entrypoint.sh"]
