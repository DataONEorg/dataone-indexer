#!/bin/bash

# Configure the SOLR server with helm-provided env variables to install
# the schema desired for this SOLR instance

set -o errexit
set -o nounset
set -o pipefail

export LOG='/tmp/poststart.log'
echo "** Starting postStart Hook... **" > ${LOG}

# Load libraries
. /opt/bitnami/scripts/libbitnami.sh
. /opt/bitnami/scripts/liblog.sh
. /opt/bitnami/scripts/libsolr.sh

# Load solr environment variables
. /opt/bitnami/scripts/solr-env.sh

echo "** Sourced Bitnami scripts... **" >> ${LOG}

export SOLR_AUTH_TYPE=basic
export SOLR_AUTHENTICATION_OPTS="-Dbasicauth=${SOLR_ADMIN_USERNAME}:${SOLR_ADMIN_PASSWORD}"
export CONFIG='/bitnami/solr/server/solr/configsets'
export SOLR_COLLECTION=dataone_index
export SOLR_CMD='/opt/bitnami/solr/bin/solr'
while [ ! -d "${CONFIG}"/sample_techproducts_configs ] ;
do
      sleep 1
      echo "** Sleeping while sample config is created... **" >> ${LOG}
done

echo "** Sample configs found. Copying files... **" >> ${LOG}
cp -R ${CONFIG}/sample_techproducts_configs ${CONFIG}/${SOLR_COLLECTION}
cp /solrconfig/schema.xml ${CONFIG}/${SOLR_COLLECTION}/conf/
cp /solrconfig/solrconfig.xml ${CONFIG}/${SOLR_COLLECTION}/conf/
rm -f ${CONFIG}/${SOLR_COLLECTION}/conf/managed-schema

echo "** Waiting for Zookeeper **" >> ${LOG}
if ! solr_wait_for_zookeeper; then
    echo "Zookeeper not detected" >> ${LOG}
    exit 1
fi
echo "** Zookeeper is up **" >> ${LOG}

echo "** Creating configset in Zookeeper... **" >> ${LOG}
# Create a custom SOLR configset in Zookeeper
${SOLR_CMD} zk upconfig -n ${SOLR_COLLECTION} -d ${CONFIG}/${SOLR_COLLECTION} -z ${SOLR_ZK_HOSTS}/solr
${SOLR_CMD} zk ls /configs -z ${SOLR_ZK_HOSTS}/solr >> ${LOG}

# Now create the collection if it doesn't exist
# http://localhost:8983/solr/admin/collections?action=list
echo "** Creating collection... **" >> ${LOG}
if ! solr_collection_exists "$SOLR_COLLECTION"; then
    ${SOLR_CMD} create_collection -c ${SOLR_COLLECTION} -n ${SOLR_COLLECTION} -replicationFactor ${SOLR_NUMBER_OF_NODES}
    echo "Collection created" >> ${LOG}
else
    echo "Skipping. Collection already exists." >> ${LOG}
fi
echo "** Finished postStart. **" >> ${LOG}
