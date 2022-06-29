package org.dataone.cn.index.processor;

import org.apache.log4j.Logger;
import org.dataone.cn.hazelcast.HazelcastClientFactory;
import org.dataone.cn.indexer.object.ObjectManager;
import org.dataone.cn.indexer.resourcemap.IndexVisibilityDelegate;
import org.dataone.cn.indexer.solrhttp.SolrDoc;
import org.dataone.service.exceptions.InvalidToken;
import org.dataone.service.exceptions.NotAuthorized;
import org.dataone.service.exceptions.NotFound;
import org.dataone.service.exceptions.NotImplemented;
import org.dataone.service.exceptions.ServiceFailure;
import org.dataone.service.types.v1.Identifier;
import org.dataone.service.types.v2.SystemMetadata;

public class IndexVisibilityDelegateHazelcastImpl implements IndexVisibilityDelegate {

    private static Logger logger = Logger.getLogger(IndexVisibilityDelegateHazelcastImpl.class
            .getName());

    public IndexVisibilityDelegateHazelcastImpl() {
    }

    public boolean isDocumentVisible(Identifier pid) {
        boolean visible = false;
        try {

            //SystemMetadata systemMetadata = HazelcastClientFactory.getSystemMetadataMap().get(pid);
            SystemMetadata systemMetadata = ObjectManager.getInstance().getSystemMetadata(pid.getValue());
            // TODO: Is pid Identifier a SID?
            if (systemMetadata == null) {
                return true;
            }
            if (SolrDoc.visibleInIndex(systemMetadata)) {
                visible = true;
            }
        } catch (NullPointerException npe) {
            logger.warn("Could not get visible value for pid: " + pid.getValue() + " since " + npe.getMessage());
        } catch (InvalidToken e) {
            logger.warn("Could not get visible value for pid: " + pid.getValue() + " since " +e.getMessage());
        } catch (NotAuthorized e) {
            logger.warn("Could not get visible value for pid: " + pid.getValue() + " since " + e.getMessage());
        } catch (NotImplemented e) {
            logger.warn("Could not get visible value for pid: " + pid.getValue() + " since " +e.getMessage());
        } catch (ServiceFailure e) {
            logger.warn("Could not get visible value for pid: " + pid.getValue() + " since " +e.getMessage());
        } catch (NotFound e) {
            logger.warn("Could not get visible value for pid: " + pid.getValue() + " since " +e.getMessage());
        }
        return visible;
    }

    public boolean documentExists(Identifier pid) {
        boolean exists = false;
        try {
            //SystemMetadata systemMetadata = HazelcastClientFactory.getSystemMetadataMap().get(pid);
            SystemMetadata systemMetadata = ObjectManager.getInstance().getSystemMetadata(pid.getValue());
            if (systemMetadata != null) {
                exists = true;
            } else {
                // TODO: Is pid Identifier a SID?
                return true;
            }
        } catch (NullPointerException npe) {
            logger.warn("Could not get visible value for pid: " + pid.getValue());
        } catch (InvalidToken e) {
            logger.warn("Could not get visible value for pid: " + pid.getValue() + " since " +e.getMessage());
        } catch (NotAuthorized e) {
            logger.warn("Could not get visible value for pid: " + pid.getValue() + " since " +e.getMessage());
        } catch (NotImplemented e) {
            logger.warn("Could not get visible value for pid: " + pid.getValue() + " since " +e.getMessage());
        } catch (ServiceFailure e) {
            logger.warn("Could not get visible value for pid: " + pid.getValue() + " since " +e.getMessage());
        } catch (NotFound e) {
            logger.warn("Could not get visible value for pid: " + pid.getValue() + " since " +e.getMessage());
        }
        return exists;
    }
}
