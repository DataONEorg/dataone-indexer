/**
 * This work was created by participants in the DataONE project, and is
 * jointly copyrighted by participating institutions in DataONE. For 
 * more information on DataONE, see our web site at http://dataone.org.
 *
 *   Copyright 2022
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and 
 * limitations under the License.
 * 
 */
package org.dataone.cn.indexer.object;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;

import org.apache.log4j.Logger;
import org.dataone.client.auth.AuthTokenSession;
import org.dataone.client.exception.ClientSideException;
import org.dataone.client.rest.HttpMultipartRestClient;
import org.dataone.client.rest.MultipartRestClient;
import org.dataone.client.v2.formats.ObjectFormatCache;
import org.dataone.client.v2.impl.MultipartCNode;
import org.dataone.client.v2.impl.MultipartD1Node;
import org.dataone.client.v2.impl.MultipartMNode;
import org.dataone.configuration.Settings;
import org.dataone.exceptions.MarshallingException;
import org.dataone.service.exceptions.InvalidToken;
import org.dataone.service.exceptions.NotAuthorized;
import org.dataone.service.exceptions.NotFound;
import org.dataone.service.exceptions.NotImplemented;
import org.dataone.service.exceptions.ServiceFailure;
import org.dataone.service.types.v1.Identifier;
import org.dataone.service.types.v1.Session;
import org.dataone.service.types.v2.ObjectFormat;
import org.dataone.service.types.v2.SystemMetadata;
import org.dataone.service.util.TypeMarshaller;


/**
 * A class to get the file path and system metadata for an identifier
 * @author tao
 *
 */
public class ObjectManager {
    private static ObjectManager manager = null;
    private static String dataRootDir = Settings.getConfiguration().getString("index.data.root.directory");
    private static String documentRootDir = Settings.getConfiguration().getString("index.document.root.directory");
    private static String nodeBaseURL = Settings.getConfiguration().getString("index.d1node.baseURL");
    private static String DataONEauthToken = null;
    private static Logger logger = Logger.getLogger(ObjectManager.class);
    private static final String TOKEN_VARIABLE_NAME = "DATAONE_AUTH_TOKEN";
    private static final String SYSTEMMETA_FILE_NAME = "systemmetadata.xml";

    private static MultipartD1Node d1Node = null;
    private static Session session = null;
    private static boolean ifDataAndDocRootSame = false;
  
    /**
     * Private constructor
     * @throws ServiceFailure 
     */
    private ObjectManager() throws ServiceFailure {
        if (dataRootDir == null || dataRootDir.trim().equals("")) {
            throw new ServiceFailure("0000", "The data root directory specified by the property index.data.root.directory is blank in the properties file");
        }
        if (documentRootDir == null || documentRootDir.trim().equals("")) {
            throw new ServiceFailure("0000", "The metadata root directory specified by the property index.document.root.directory is blank in the properties file");
        }
        if (!Files.exists(FileSystems.getDefault().getPath(dataRootDir))) {
            throw new ServiceFailure("0000", "The data root directory " + dataRootDir + 
                                    " specified in the properties file doesn't exist");
        }
        if (!Files.exists(FileSystems.getDefault().getPath(documentRootDir))) {
            throw new ServiceFailure("0000", "The document root directory " + documentRootDir + 
                                    " specified in the properties file doesn't exist");
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
        if (d1Node == null) {
            refreshD1Node();
        } else {
            logger.info("ObjectManager ---NOT going to create the d1node with the url " + nodeBaseURL + 
                       " since the ObjectManager already was assigned a d1node with the url " + d1Node.getNodeBaseServiceUrl());
        }
    }
    
    /**
     * Get an ObjectManager instance through the singleton pattern.
     * @return  the instance of ObjectManager
     * @throws ServiceFailure 
     */
    public static ObjectManager getInstance() throws ServiceFailure {
        if (manager == null) {
            synchronized (ObjectManager.class) {
                if (manager == null)  {
                    manager = new ObjectManager();
                }
            }
        }
        return manager;
    }
    
    /**
     * Get the absolute file path for a given relative path. If the relativePath is null or blank,
     * null will be returned
     * @param relativePath
     * @param objectFormat
     * @return  the absolute file path 
     * @throws NotFound
     */
    public String getFilePath(String relativePath, String objectFormat) throws NotFound {
        String absolutePath = null;
        if (relativePath != null && !relativePath.trim().equals("")) {
            if (ifDataAndDocRootSame) {
                absolutePath = documentRootDir + relativePath;
            } else if (objectFormat != null && !objectFormat.trim().equals("")) {
                ObjectFormat format =ObjectFormatCache.getInstance().getFormat(objectFormat);
                if (format.getFormatType().equals("METADATA")) {
                    absolutePath = documentRootDir + relativePath;
                } else {
                    absolutePath = dataRootDir + relativePath;
                }
            }
        }
        logger.debug("ObjectManager.getFilePath - the absolute file path for the relative file path " + 
                        relativePath + " is " + absolutePath);
        return absolutePath;
    }
    
    /**
     * Get the system metadata for the given id
     * @param id  the id to identify the system metadata
     * @param objectRelativePath  the object path for this id. It can help to determine 
     * the system metadata file if the system metadata file exists.
     * @return  the system metadata associated with the id
     * @throws InvalidToken
     * @throws NotAuthorized
     * @throws NotImplemented
     * @throws ServiceFailure
     * @throws NotFound
     * @throws MarshallingException 
     * @throws IOException 
     * @throws IllegalAccessException 
     * @throws InstantiationException 
     */
    public SystemMetadata getSystemMetadata(String id, String relativeObjPath) throws InvalidToken, NotAuthorized, NotImplemented, 
                                                                                    ServiceFailure, NotFound, InstantiationException, IllegalAccessException, IOException, MarshallingException {
        SystemMetadata sysmeta = null;
        //try to get the system metadata from the file system first
        File sysmetaFile = getSysmetaFile(relativeObjPath);
        if (sysmetaFile != null) {
            sysmeta = TypeMarshaller.unmarshalTypeFromFile(SystemMetadata.class, sysmetaFile);
            logger.debug("ObjectManager.getSystemMetadata - finish getting the system metadata via the file system for the pid " + id);
        } else {
            //if we can't get it from the file system, get it from dataone API
            Identifier identifier = new Identifier();
            identifier.setValue(id);
            try {
                sysmeta = d1Node.getSystemMetadata(session, identifier);
                logger.debug("ObjectManager.getSystemMetadata - finish getting the system metadata via the DataONE API call for the pid " + id);
            } catch (NotAuthorized e) {
                logger.info("ObjectManager.getSystemMetadata - failed to get the system metadata via the DataONE API call for the pid " + id +
                            " since it is not authorized. We will refresh the token and try again");
                refreshD1Node();
                sysmeta = d1Node.getSystemMetadata(session, identifier);
            }
            logger.debug("ObjectManager.getSystemMetadata - finish getting the system metadata via DataONE API for the pid " + id);
        }
        return sysmeta;
    }
    
    /**
     * Set the d1 node for this object manager.
     * We only use it for testing
     * @param node  the d1node will be assigned
     */
    public static void setD1Node(MultipartD1Node node) {
        d1Node = node;
    }
    
    /**
     * Get the system metadata file path from the objectPath.
     * We assume the object and system metadata file are in the same directory. 
     * The system metadata file has a fixed name - systemmetadata.xml
     * @param  relativeObjPath  the relative path of the object
     * @return  the file of system metadata. If it is null, this means the system metadata file does not exist.
     */
    protected static File getSysmetaFile(String relativeObjPath) {
        File sysmetaFile = null;
        String sysmetaPath = null;
        String relativeSysmetaPath = null;
        if (relativeObjPath != null) {
            if (relativeObjPath.contains(File.separator)) {
                logger.debug("ObjectManager.getSysmetaFile - the object file path " + relativeObjPath + " has at least one path separator " + File.pathSeparator);
                relativeSysmetaPath = relativeObjPath.substring(0, relativeObjPath.lastIndexOf(File.separator) + 1) + SYSTEMMETA_FILE_NAME;
            } else {
                logger.debug("ObjectManager.getSysmetaFile - the object file path " + relativeObjPath + " doesnot have any path separator " + File.pathSeparator);
                //There is not path information in the object path ( it only has the file name). So we just simply return systemmetadata.xml
                relativeSysmetaPath = SYSTEMMETA_FILE_NAME;
            }
            logger.debug("ObjectManager.getSysmetaFile - the relative system metadata file path for the object path " + 
                        relativeObjPath + " is " + relativeSysmetaPath);
            if (ifDataAndDocRootSame) {
                sysmetaPath = documentRootDir + relativeSysmetaPath;
                sysmetaFile = new File(sysmetaPath);
                if (!sysmetaFile.exists()) {
                    //the system metadata file doesn't exist and we set it to null
                    sysmetaPath = null;
                    sysmetaFile = null;
                }
            } else {
                //try if this object is a document first since we have no idea if the object is metadata or data.
                sysmetaPath = documentRootDir + relativeSysmetaPath;
                sysmetaFile = new File(sysmetaPath);
                if (!sysmetaFile.exists()) {
                    // try data 
                    sysmetaPath = dataRootDir + relativeSysmetaPath;
                    sysmetaFile = new File(sysmetaPath);
                    if (!sysmetaFile.exists()) {
                        //the system metadata file doesn't exist and we set it to null
                        sysmetaPath = null;
                        sysmetaFile = null;
                    }
                }
            }
        }
        logger.debug("ObjectManager.getSysmetaFile - the final system metadata file path for the object path " + 
                relativeObjPath + " is " + sysmetaPath + ". Null means that not system metadata file exists.");
        return sysmetaFile;
    }
    
    /**
     * In case the token expired, the method will retrieve the token and create a new d1 node
     * @throws ServiceFailure 
     */
    private void refreshD1Node() throws ServiceFailure {
       //get the token
        DataONEauthToken = System.getenv(TOKEN_VARIABLE_NAME);
        if (DataONEauthToken == null || DataONEauthToken.trim().equals("")) {
            DataONEauthToken =  Settings.getConfiguration().getString(TOKEN_VARIABLE_NAME);
            if (DataONEauthToken != null && !DataONEauthToken.trim().equals("")) {
                logger.info("ObjectManager.refreshD1Node - Got the auth token from the properties file");
            }
        } else {
            logger.info("ObjectManager.refreshD1Node - Got the auth token from an env. variable");
        }
        
        if (DataONEauthToken == null || DataONEauthToken.trim().equals("")) {
            logger.warn("ObjectManager.refreshD1Node ------ Could NOT get an auth token from either an env. variable or the properties file. So it will act as the public user.");
        }
        session = createSession(DataONEauthToken);
        logger.info("ObjectManager.refreshD1Node ------ going to create the d1node with url " + nodeBaseURL);
        try {
            d1Node = getMultipartD1Node(session, nodeBaseURL);
        } catch (IOException | ClientSideException e) {
            logger.error("ObjectManager.refreshD1Node - couldn't create the d1node for the url " + nodeBaseURL + " since " + e.getMessage());
            throw new ServiceFailure("0000", e.getMessage());
        }
    }
    
    /**
     * Get a DataONE authenticated session
     * <p>
     *     If no subject or authentication token are provided, a public session is returned
     * </p>
     * @param authToken the authentication token
     * @return the DataONE session
     */
    private Session createSession(String authToken) {
        Session session = null;
        if (authToken == null || authToken.trim().equals("")) {
            logger.info("ObjectManager.createSession - Creating the public session");
            session = new Session();
        } else {
            logger.info("ObjectManger.createSession - Creating authentication session from token: " + authToken.substring(0, 5) + "...");
            session = new AuthTokenSession(authToken);
        }
        return session;
    }
    
    /**
     * Get a DataONE MultipartCNode object, which will be used to communication with a CN
     *
     * @param session a DataONE authentication session
     * @param serviceUrl the service URL for the node we are connecting to
     * @return a DataONE MultipartCNode object
     * @throws ClientSideException 
     * @throws IOException 
     * @throws MetadigException
     */
    private MultipartD1Node getMultipartD1Node(Session session, String serviceUrl) throws IOException, ClientSideException {
        MultipartRestClient mrc = null;
        MultipartD1Node d1Node = null;
        // First create a default HTTP client
        mrc = new HttpMultipartRestClient();
        // Now create a DataONE object that uses the rest client
        Boolean isCN = isCN(serviceUrl);
        // Now create a DataONE object that uses the rest client
        if (isCN) {
            logger.info("ObjectManager.getMultipartD1Node - creating cn MultipartMNode from the url " + serviceUrl);
            d1Node = new MultipartCNode(mrc, serviceUrl, session);
        } else {
            logger.info("ObjectManager.getMultipartD1Node - creating mn MultipartMNode from the url " + serviceUrl);
            d1Node = new MultipartMNode(mrc, serviceUrl, session);
        }
        return d1Node;
    }
    
    /*
     * Determine if the string represents a DataONE CN.
     * @param nodeStr either a DataONE node serviceURL (e.g. https://knb.ecoinformatics.org/knb/d1/mn)
     *      or a DataONE node identifier (e.g. urn:node:CN)
     */
    private Boolean isCN(String nodeStr) {
        Boolean isCN = false;
        // match node urn, e.g. "https://cn.dataone.org/cn"
        if (nodeStr.matches("^\\s*urn:node:.*")) {
            if (nodeStr.matches("^\\s*urn:node:CN.*$|^\\s*urn:node:cn.*$")) {
                isCN = true;
                logger.debug("ObjectManager.isCN - The nodeId is for a CN: " + nodeStr);
            } else {
                logger.debug("ObjectManager.isCN - The nodeId is not for a CN: " + nodeStr);
                isCN = false;
            }
        } else {
            // match cn service url e.g. "https://cn.dataone.org/cn"
            if (nodeStr.matches("^\\s*https*://cn.*?\\.dataone\\.org.*$|https*://cn.*?\\.test\\.dataone\\.org.*$")) {
                isCN = true;
                logger.debug("ObjectManager.isCN - The service URL is for a CN: " + nodeStr);
            } else {
                logger.debug("ObjectManager.isCN - The service URL is not for a CN: " + nodeStr);
                isCN = false;
            }
        }
        return isCN;
    }

}
