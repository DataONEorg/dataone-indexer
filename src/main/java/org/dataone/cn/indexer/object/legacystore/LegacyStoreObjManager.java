package org.dataone.cn.indexer.object.legacystore;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileSystems;
import java.nio.file.Files;

import org.apache.commons.io.output.ByteArrayOutputStream;
import org.apache.log4j.Logger;
import org.dataone.client.v2.formats.ObjectFormatCache;
import org.dataone.cn.indexer.object.ObjectManager;
import org.dataone.configuration.Settings;
import org.dataone.exceptions.MarshallingException;
import org.dataone.service.exceptions.InvalidToken;
import org.dataone.service.exceptions.NotAuthorized;
import org.dataone.service.exceptions.NotFound;
import org.dataone.service.exceptions.NotImplemented;
import org.dataone.service.exceptions.ServiceFailure;
import org.dataone.service.types.v2.ObjectFormat;
import org.dataone.service.types.v2.SystemMetadata;
import org.dataone.service.util.TypeMarshaller;

/**
 * The class to get objects and system metadata from Metacat legacy store
 * @author Tao
 */
public class LegacyStoreObjManager extends ObjectManager {
    // environmental variables' names
    private static final String DATA_ROOT_DIR_ENV_NAME = "DATAONE_INDEXER_METACAT_DATA_ROOT_DIR";
    private static final String DOCUMENT_ROOT_DIR_ENV_NAME =
        "DATAONE_INDEXER_METACAT_DOCUMENT_ROOT_DIR";

    private static final String DATA_ROOT_DIR_PROPERTY_NAME = "index.data.root.directory";
    private static final String DOCUMENT_ROOT_DIR_PROPERTY_NAME = "index.document.root.directory";
    private static String dataRootDir;
    private static String documentRootDir;
    private static Logger logger = Logger.getLogger(LegacyStoreObjManager.class);

    private static boolean ifDataAndDocRootSame = false;

    /**
     * Constructor
     * Read the Metacat legacy data and document directories from the environmental variables and
     * the properties file. The values in the environmental variables overwrite the properties ones.
     * @throws ServiceFailure
     */
    public LegacyStoreObjManager() throws ServiceFailure {
        dataRootDir = System.getenv(DATA_ROOT_DIR_ENV_NAME);
        logger.debug("The data root dir from env " + DATA_ROOT_DIR_ENV_NAME + " is " + dataRootDir);
        if (dataRootDir == null || dataRootDir.isBlank()) {
            dataRootDir = Settings.getConfiguration().getString(DATA_ROOT_DIR_PROPERTY_NAME);
            logger.debug("The data root dir from the properties is " + dataRootDir);
        }
        if (dataRootDir == null || dataRootDir.isBlank()) {
            throw new ServiceFailure("0000",
                                     "The data root directory specified by the env " + "variable "
                                         + DATA_ROOT_DIR_ENV_NAME + " or the property "
                                         + DATA_ROOT_DIR_PROPERTY_NAME
                                         + " in the properties file is null/blank");
        }
        documentRootDir = System.getenv(DOCUMENT_ROOT_DIR_ENV_NAME);
        logger.debug("The document root dir from env " + DOCUMENT_ROOT_DIR_ENV_NAME + " is "
                         + documentRootDir);
        if (documentRootDir == null || documentRootDir.isBlank()) {
            documentRootDir =
                Settings.getConfiguration().getString(DOCUMENT_ROOT_DIR_PROPERTY_NAME);
            logger.debug("The document root dir from the properties is " + documentRootDir);
        }
        if (documentRootDir == null || documentRootDir.isBlank()) {
            throw new ServiceFailure("0000",
                                     "The document root directory specified by the env variable "
                                         + DOCUMENT_ROOT_DIR_ENV_NAME + " or the property "
                                         + DOCUMENT_ROOT_DIR_PROPERTY_NAME
                                         + " in the properties file is blank.");
        }
        if (!Files.exists(FileSystems.getDefault().getPath(dataRootDir))) {
            throw new ServiceFailure("0000", "The data root directory " + dataRootDir +
                " specified in the env variable or the properties file doesn't exist");
        }
        if (!Files.exists(FileSystems.getDefault().getPath(documentRootDir))) {
            throw new ServiceFailure("0000", "The document root directory " + documentRootDir +
                " specified in the env variable or the properties file doesn't exist");
        }
        if (!dataRootDir.endsWith("/")) {
            dataRootDir = dataRootDir + "/";
        }
        if (!documentRootDir.endsWith("/")) {
            documentRootDir = documentRootDir + "/";
        }

        if (documentRootDir.equals(dataRootDir)) {
            ifDataAndDocRootSame = true;
        }
        logger.info("ObjectManager.constructor - the root document directory is " +
                        documentRootDir + " and the root data directory is " + dataRootDir +
                        " Are they same?" + ifDataAndDocRootSame);

    }


    @Override
    public InputStream getObject(String pid)
        throws IllegalArgumentException, IOException, NotFound {
        File object = new File(documentRootDir + pid);
        if (!object.exists()) {
            object = new File(dataRootDir + pid);
            if (object.exists()) {
                return new FileInputStream(object);
            } else {
                throw new FileNotFoundException(
                    "Neither " + documentRootDir + " nor " + dataRootDir + " have the docid "
                        + pid);
            }
        } else {
            return new FileInputStream(object);
        }

    }

    /**
     * Get the absolute file path for a given relative path. If the relativePath is null or blank,
     * null will be returned
     * @param relativePath
     * @param objectFormat
     * @return  the absolute file path
     * @throws NotFound
     */
    private String getFilePath(String relativePath, String objectFormat) throws NotFound {
        String absolutePath = null;
        if (relativePath != null && !relativePath.isBlank()) {
            if (ifDataAndDocRootSame) {
                absolutePath = documentRootDir + relativePath;
            } else if (objectFormat != null && !objectFormat.isBlank()) {
                ObjectFormat format =ObjectFormatCache.getInstance().getFormat(objectFormat);
                if (format.getFormatType().equals("METADATA")) {
                    absolutePath = documentRootDir + relativePath;
                } else {
                    absolutePath = dataRootDir + relativePath;
                }
            }
        }
        logger.debug("The absolute file path for the relative file path " +
                         relativePath + " is " + absolutePath);
        return absolutePath;
    }

    @Override
    public org.dataone.service.types.v1.SystemMetadata getSystemMetadata(String id)
        throws InvalidToken, NotAuthorized, NotImplemented, ServiceFailure, NotFound {
        SystemMetadata sysmeta = null;
        long start = System.currentTimeMillis();
        try {
            for (int i=0; i<5; i++) {
                try {
                    sysmeta = getSystemMetadataByAPI(id);
                    break;
                } catch (ServiceFailure ee) {
                    logger.warn("The DataONE api call doesn't get the system metadata since "
                                    + ee.getMessage() + ". This is " + i
                                    + " try and Indexer will try again.");
                    try {
                        Thread.sleep(300);
                    } catch (InterruptedException ie) {
                        logger.info("The sleep of the thread was interrupted.");
                    }
                }
            }
            logger.debug(
                "ObjectManager.getSystemMetadata - finish getting the system metadata via the "
                    + "DataONE API call for the pid " + id);
        } catch (NotAuthorized e) {
            logger.info(
                "ObjectManager.getSystemMetadata - failed to get the system metadata via the "
                    + "DataONE API call for the pid "
                    + id + " since it is not authorized. We will refresh the token and try again");
            refreshD1Node();
            sysmeta = getSystemMetadataByAPI(id);
        }
        long end = System.currentTimeMillis();
        logger.info(
            "ObjectManager.getSystemMetadata - finish getting the system metadata via DataONE API"
                + " for the pid "
                + id + " and it took " + (end - start) + "milliseconds");

        return sysmeta;
    }

    @Override
    public InputStream getSystemMetadataStream(String id) throws InvalidToken, NotAuthorized,
        NotImplemented, ServiceFailure, NotFound, IOException, MarshallingException {
        long start = System.currentTimeMillis();
        //try to get the system metadata from the storage system first
        InputStream sysmetaInputStream = null;
        SystemMetadata sysmeta = (SystemMetadata) getSystemMetadata(id);
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
        return sysmetaInputStream;
    }


}
