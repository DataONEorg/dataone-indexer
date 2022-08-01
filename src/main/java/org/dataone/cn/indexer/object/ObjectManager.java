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

import java.io.IOException;

import org.apache.log4j.Logger;
import org.dataone.client.auth.AuthTokenSession;
import org.dataone.client.exception.ClientSideException;
import org.dataone.client.rest.HttpMultipartRestClient;
import org.dataone.client.rest.MultipartRestClient;
import org.dataone.client.v2.MNode;
import org.dataone.client.v2.formats.ObjectFormatCache;
import org.dataone.client.v2.impl.MultipartCNode;
import org.dataone.client.v2.impl.MultipartD1Node;
import org.dataone.client.v2.impl.MultipartMNode;
import org.dataone.client.v2.itk.D1Client;
import org.dataone.configuration.Settings;
import org.dataone.service.exceptions.InvalidToken;
import org.dataone.service.exceptions.NotAuthorized;
import org.dataone.service.exceptions.NotFound;
import org.dataone.service.exceptions.NotImplemented;
import org.dataone.service.exceptions.ServiceFailure;
import org.dataone.service.types.v1.Identifier;
import org.dataone.service.types.v1.Session;
import org.dataone.service.types.v1.Subject;
import org.dataone.service.types.v2.ObjectFormat;
import org.dataone.service.types.v2.SystemMetadata;


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

    private MultipartD1Node d1Node = null;
    private Session session = null;
  
    /**
     * Private constructor
     * @throws ServiceFailure 
     */
    private ObjectManager() throws ServiceFailure {
        if (dataRootDir.endsWith("/")) {
            dataRootDir = dataRootDir + "/";
        }
        if (documentRootDir.endsWith("/")) {
            documentRootDir = documentRootDir + "/";
        }
        logger.debug("ObjectManager.constructor - the root document directory is " + 
                        documentRootDir + " and the root data directory is " + dataRootDir);
      
        //get the token
        DataONEauthToken = System.getenv("DATAONE_AUTH_TOKEN");
        if (DataONEauthToken == null || DataONEauthToken.trim().equals("")) {
            DataONEauthToken =  Settings.getConfiguration().getString("DataONE.authToken");
            logger.debug("ObjectManager - Got token from properties file.");
        } else {
            logger.debug("ObjectManager - Got token from env.");
        }
        
        if (DataONEauthToken == null || DataONEauthToken.trim().equals("")) {
            logger.info("ObjectManager ------ Could NOT get a token from either env or a properties file");
        }
        Session session = createSession(DataONEauthToken);
        try {
            d1Node = getMultipartD1Node(session, nodeBaseURL);
        } catch (IOException | ClientSideException e) {
            logger.error("ObjectManager - couldn't create the d1node for the url " + nodeBaseURL + " since " + e.getMessage());
            throw new ServiceFailure("0000", e.getMessage());
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
        if (relativePath != null && !relativePath.trim().equals("") && 
                objectFormat != null && !objectFormat.trim().equals("")) {
            ObjectFormat format =ObjectFormatCache.getInstance().getFormat(objectFormat);
            if (format.getFormatType().equals("METADATA")) {
                absolutePath = documentRootDir + relativePath;
            } else {
                absolutePath = dataRootDir + relativePath;
            }
        }
        logger.debug("ObjectManager.getFilePath - the absolute file path for the relative file path " + 
                        relativePath + " is " + absolutePath);
        return absolutePath;
    }
    
    /**
     * Get the system metadata for the given id
     * @param id  the id to identify the system metadata
     * @return  the system metadata associated with the id
     * @throws InvalidToken
     * @throws NotAuthorized
     * @throws NotImplemented
     * @throws ServiceFailure
     * @throws NotFound
     */
    public SystemMetadata getSystemMetadata(String id) throws InvalidToken, NotAuthorized, NotImplemented, 
                                                                                    ServiceFailure, NotFound {
        Identifier identifier = new Identifier();
        identifier.setValue(id);
        SystemMetadata sysmeta = d1Node.getSystemMetadata(session, identifier);
        return sysmeta;
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
            logger.debug("ObjectManager.getSession - Creating public sessioni");
            session = new Session();
        } else {
            logger.debug("Creating authentication session from token: " + authToken.substring(0, 5) + "...");
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
            logger.debug("ObjectManager.getMultipartD1Node - creating cn MultipartMNode from the url " + serviceUrl);
            d1Node = new MultipartCNode(mrc, serviceUrl, session);
        } else {
            logger.debug("ObjectManager.getMultipartD1Node - creating mn MultipartMNode from the url " + serviceUrl);
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
