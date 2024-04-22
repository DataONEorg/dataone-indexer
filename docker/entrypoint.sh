#!/bin/bash
figlet -ck DataONE Indexer

if [[ "$DEBUG" == "TRUE" ]]
then
    echo "Starting infinite loop, ctrl-c to interrupt..."
    sh -c 'trap "exit" TERM; while true; do sleep 1; done'
else
    jar -xf dataone-index-worker-${TAG}-shaded.jar

    java  -Dlog4j2.formatMsgNoLookups=true \
          -XX:+UnlockExperimentalVMOptions \
          -XX:+UseContainerSupport \
          -XX:+UseSerialGC \
          -cp ./config/:. \
          org.dataone.cn.indexer.IndexWorker
fi
