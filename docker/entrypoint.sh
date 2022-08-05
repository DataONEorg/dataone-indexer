#!/bin/sh
figlet -ck DataONE Indexer

if [[ "$DEBUG" == "TRUE" ]]
then
    echo "Starting infinite loop, ctrl-c to interrupt..."
    sh -c 'trap "exit" TERM; while true; do sleep 1; done'
else
    java -Dlog4j2.formatMsgNoLookups=true -XX:+UnlockExperimentalVMOptions -XX:+UseCGroupMemoryLimitForHeap -XX:+UseSerialGC -cp ./dataone-index-worker-${TAG}-shaded.jar org.dataone.cn.index.IndexWorker
fi
