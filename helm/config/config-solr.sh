#!/bin/sh

# Configure the SOLR server with helm-provided env variables to install
# the schema desired for this SOLR instance

export SOLR_AUTH_TYPE=basic
export SOLR_AUTHENTICATION_OPTS="-Dbasicauth=${SOLR_ADMIN_USERNAME}:${SOLR_ADMIN_PASSWORD}"
export CONFIG='/bitnami/solr/server/solr/configsets'
cp -R ${CONFIG}/sample_techproducts_configs ${CONFIG}/${SOLR_COLLECTION}
cp /solrconfig/schema.xml ${CONFIG}/${SOLR_COLLECTION}/conf/
cp /solrconfig/solrconfig.xml ${CONFIG}/${SOLR_COLLECTION}/conf/
rm ${CONFIG}/${SOLR_COLLECTION}/conf/managed-schema
/opt/bitnami/solr/bin/solr delete -c ${SOLR_COLLECTION}
/opt/bitnami/solr/bin/solr create_collection -c ${SOLR_COLLECTION} -d ${CONFIG}/${SOLR_COLLECTION}/ -rf ${SOLR_NUMBER_OF_NODES}
