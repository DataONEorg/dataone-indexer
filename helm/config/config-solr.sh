#!/bin/bash

# Configure the SOLR server with helm-provided env variables to install
# the schema desired for this SOLR instance

set -o errexit
set -o nounset
set -o pipefail
#set -o xtrace # Uncomment this line for debugging purposes

# Load libraries
. /opt/bitnami/scripts/libbitnami.sh
. /opt/bitnami/scripts/liblog.sh
. /opt/bitnami/scripts/libsolr.sh

# Load solr environment variables
. /opt/bitnami/scripts/solr-env.sh

export SOLR_AUTH_TYPE=basic
export SOLR_AUTHENTICATION_OPTS="-Dbasicauth=${SOLR_ADMIN_USERNAME}:${SOLR_ADMIN_PASSWORD}"
export CONFIG='/bitnami/solr/server/solr/configsets'
export SOLR_COLLECTION=dataone_index
while [ ! -d "${CONFIG}"/sample_techproducts_configs ] ;
do
      sleep 1
      info "** Sleeping while sample config is created... **"
done

info "** Sample configs found. Creating collection... **"
cp -R ${CONFIG}/sample_techproducts_configs ${CONFIG}/${SOLR_COLLECTION}
cp /solrconfig/schema.xml ${CONFIG}/${SOLR_COLLECTION}/conf/
cp /solrconfig/solrconfig.xml ${CONFIG}/${SOLR_COLLECTION}/conf/
rm -f ${CONFIG}/${SOLR_COLLECTION}/conf/managed-schema

# Create a custom SOLR configset in Zookeeper
/opt/bitnami/solr/bin/solr zk upconfig -n ${SOLR_COLLECTION} -d ${CONFIG}/${SOLR_COLLECTION} -z ${SOLR_ZK_HOSTS}

# Now create the collection if it doesn't exist
solr_create_collection
