package org.dataone.cn.indexer.object.hashstore;

import org.apache.commons.io.output.ByteArrayOutputStream;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.dataone.cn.indexer.object.ObjectManager;
import org.dataone.exceptions.MarshallingException;
import org.dataone.indexer.storage.Storage;
import org.dataone.service.exceptions.InvalidToken;
import org.dataone.service.exceptions.NotAuthorized;
import org.dataone.service.exceptions.NotFound;
import org.dataone.service.exceptions.NotImplemented;
import org.dataone.service.exceptions.ServiceFailure;
import org.dataone.service.types.v2.SystemMetadata;
import org.dataone.service.util.TypeMarshaller;

import java.io.ByteArrayInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.security.NoSuchAlgorithmException;

/**
 * Implementation of ObjectManager based on a hash store
 * @author Tao
 */
public class HashStoreObjManager extends ObjectManager {
    private static Storage storage = null;
    private static Log logger = LogFactory.getLog(ObjectManager.class);
    static {
        try {
            refreshD1Node();
        } catch (ServiceFailure e) {
            logger.warn("Metacat cannot initialize the d1Node since " + e.getMessage());
        }
        storage = Storage.getInstance();
    }

    /**
     * Constructor
     */
    public HashStoreObjManager() {

    }

    @Override
    public InputStream getSystemMetadataStream(String id) throws InvalidToken, NotAuthorized,
        NotImplemented, ServiceFailure, NotFound, NoSuchAlgorithmException, IOException,
        MarshallingException {
        long start = System.currentTimeMillis();
        //try to get the system metadata from the storage system first
        InputStream sysmetaInputStream = null;
        try {
            sysmetaInputStream = storage.retrieveSystemMetadata(id);
            long end = System.currentTimeMillis();
            logger.info("Finish getting the system metadata via the file system for the pid " + id
                            + " and it took " + (end - start) + "milliseconds");
        } catch (FileNotFoundException exception ) {
            if (d1Node != null) {
                // Metacat can't find the system metadata from the storage system.
                // So try to get it from the dataone api
                SystemMetadata sysmeta = getSystemMetadataByAPI(id);
                logger.debug("Finish getting the system metadata via the DataONE API call for the"
                                 + " pid " + id);
                if (sysmeta != null) {
                    ByteArrayOutputStream systemMetadataOutputStream = new ByteArrayOutputStream();
                    TypeMarshaller.marshalTypeToOutputStream(sysmeta, systemMetadataOutputStream);
                    sysmetaInputStream =
                        new ByteArrayInputStream(systemMetadataOutputStream.toByteArray());
                }
                long end = System.currentTimeMillis();
                logger.info("Finish getting the system metadata via DataONE API for the pid " + id
                                + " and it took " + (end - start) + "milliseconds");
            }
        }
        return sysmetaInputStream;
    }

    @Override
    public org.dataone.service.types.v1.SystemMetadata getSystemMetadata(String id)
        throws InvalidToken, NotAuthorized, NoSuchAlgorithmException,
        NotImplemented, ServiceFailure, NotFound,
        InstantiationException, IllegalAccessException,
        IOException, MarshallingException {
        org.dataone.service.types.v1.SystemMetadata sysmeta = null;
        try (InputStream input = getSystemMetadataStream(id)) {
            if (input != null) {
                try {
                    SystemMetadata sysmeta2 = TypeMarshaller
                        .unmarshalTypeFromStream(SystemMetadata.class, input);
                    sysmeta = sysmeta2;
                } catch (Exception e) {
                    try (InputStream input2 = getSystemMetadataStream(id)) {
                        if (input2 != null) {
                            sysmeta = TypeMarshaller.unmarshalTypeFromStream(
                                org.dataone.service.types.v1.SystemMetadata.class, input2);
                        }
                    }
                }
            }
        }
        return sysmeta;
    }

    @Override
    public InputStream getObject(String pid)
        throws IllegalArgumentException, NoSuchAlgorithmException, IOException {
        return storage.retrieveObject(pid);
    }

}
