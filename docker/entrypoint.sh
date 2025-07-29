#!/bin/bash
figlet -ck DataONE Indexer

if [[ "$DEBUG" == "TRUE" ]]; then
    echo "Starting infinite loop, ctrl-c to interrupt..."
    sh -c 'trap "exit" TERM; while true; do sleep 1; done'
else
    echo "Starting DataONE Indexer from jar file: ./dataone-index-worker-${TAG}-shaded.jar,"
    echo "using cmd-line options for JAVA_MEM: [${IDX_JAVA_MEM}] and JAVA_OPTS: [${IDX_JAVA_OPTS}]"
    if [[ "$(echo "$IDX_JMX_ENABLED" | tr '[:upper:]' '[:lower:]')" == "true" ]]; then
        jmx_port=$(echo "$IDX_JAVA_OPTS" | \
            sed -n 's/.*-Dcom\.sun\.management\.jmxremote\.port=\([0-9]*\).*/\1/p')
        echo "Connect to JMX via port-forwarding to your localhost, from this pod's port: $jmx_port"
    fi
    java  $IDX_JAVA_MEM $IDX_JAVA_OPTS -cp /etc/dataone/:./dataone-index-worker-${TAG}-shaded.jar \
          org.dataone.cn.indexer.IndexWorker
fi
