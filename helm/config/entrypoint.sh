#!/bin/bash

# shellcheck disable=SC1091

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

print_welcome_page

if [[ "$*" = *"/opt/bitnami/scripts/solr/run.sh"* ]]; then

    info "** Starting solr setup **"
    /opt/bitnami/scripts/solr/setup.sh
    info "** solr setup finished! **"
    
    info "** Start creating custom collection **"
    if ! solr_wait_for_zookeeper; then
        error "Zookeeper not detected"
        exit 1
    fi
    #/solrconfig/config-solr.sh
    info "** Collection created **"
fi

echo ""
exec "$@"
