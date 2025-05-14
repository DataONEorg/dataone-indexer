package org.dataone.cn.indexer.resourcemap;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.security.NoSuchAlgorithmException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.dataone.cn.indexer.object.ObjectManagerFactory;
import org.dataone.cn.indexer.solrhttp.SolrDoc;
import org.dataone.exceptions.MarshallingException;
import org.dataone.service.exceptions.InvalidToken;
import org.dataone.service.exceptions.NotAuthorized;
import org.dataone.service.exceptions.NotFound;
import org.dataone.service.exceptions.NotImplemented;
import org.dataone.service.exceptions.ServiceFailure;
import org.dataone.service.types.v1.Identifier;
import org.dataone.service.types.v1.SystemMetadata;

public class IndexVisibilityDelegateImpl implements IndexVisibilityDelegate {

    private static Log logger = LogFactory.getLog(IndexVisibilityDelegateImpl.class
            .getName());

    public IndexVisibilityDelegateImpl() {
    }

    public boolean isDocumentVisible(Identifier pid) {
        boolean visible = false;
        try {
            SystemMetadata systemMetadata = ObjectManagerFactory.getObjectManager()
                                                            .getSystemMetadata(pid.getValue());
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
        } catch (InstantiationException e) {
            logger.warn("Could not get visible value for pid: " + pid.getValue() + " since " +e.getMessage());
        } catch (IllegalAccessException e) {
            logger.warn("Could not get visible value for pid: " + pid.getValue() + " since " +e.getMessage());
        } catch (IOException e) {
            logger.warn("Could not get visible value for pid: " + pid.getValue() + " since " +e.getMessage());
        } catch (MarshallingException e) {
            logger.warn("Could not get visible value for pid: " + pid.getValue() + " since " +e.getMessage());
        } catch (NoSuchAlgorithmException e) {
            logger.warn("Could not get visible value for pid: " + pid.getValue() + " since " +e.getMessage());
        } catch (ClassNotFoundException e) {
            logger.warn("Could not get visible value for pid: " + pid.getValue() + " since "
                            + e.getMessage());
        } catch (InvocationTargetException e) {
            logger.warn("Could not get visible value for pid: " + pid.getValue() + " since "
                            + e.getMessage());
        } catch (NoSuchMethodException e) {
            logger.warn("Could not get visible value for pid: " + pid.getValue() + " since "
                            + e.getMessage());
        }
        return visible;
    }

    public boolean documentExists(Identifier pid) {
        boolean exists = false;
        try {
            SystemMetadata systemMetadata =
                ObjectManagerFactory.getObjectManager().getSystemMetadata(pid.getValue());
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
        } catch (InstantiationException e) {
            logger.warn("Could not get visible value for pid: " + pid.getValue() + " since " +e.getMessage());
        } catch (IllegalAccessException e) {
            logger.warn("Could not get visible value for pid: " + pid.getValue() + " since " +e.getMessage());
        } catch (IOException e) {
            logger.warn("Could not get visible value for pid: " + pid.getValue() + " since " +e.getMessage());
        } catch (MarshallingException e) {
            logger.warn("Could not get visible value for pid: " + pid.getValue() + " since " +e.getMessage());
        } catch (NoSuchAlgorithmException e) {
            logger.warn("Could not get visible value for pid: " + pid.getValue() + " since " +e.getMessage());
        } catch (ClassNotFoundException e) {
            logger.warn("Could not get visible value for pid: " + pid.getValue() + " since "
                            + e.getMessage());
        } catch (InvocationTargetException e) {
            logger.warn("Could not get visible value for pid: " + pid.getValue() + " since "
                            + e.getMessage());
        } catch (NoSuchMethodException e) {
            logger.warn("Could not get visible value for pid: " + pid.getValue() + " since "
                            + e.getMessage());
        }
        return exists;
    }
}
