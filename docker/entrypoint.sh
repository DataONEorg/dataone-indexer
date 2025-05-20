#!/bin/bash
figlet -ck DataONE Indexer

if [[ "$DEBUG" == "TRUE" ]]
then
    echo "Starting infinite loop, ctrl-c to interrupt..."
    sh -c 'trap "exit" TERM; while true; do sleep 1; done'
else
    echo "Starting DataONE Indexer from jar file: ./dataone-index-worker-${TAG}-shaded.jar,"
    echo "using memory options: [$IDX_JAVA_MEM]"

    java  -Dlog4j2.formatMsgNoLookups=true \
          -XX:+UnlockExperimentalVMOptions \
          -XX:+UseContainerSupport  "$IDX_JAVA_MEM" \
          -XX:+UseSerialGC \
          -cp /etc/dataone/:./dataone-index-worker-${TAG}-shaded.jar \
          org.dataone.cn.indexer.IndexWorker
fi
