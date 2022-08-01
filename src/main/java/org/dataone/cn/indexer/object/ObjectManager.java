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

import org.apache.log4j.Logger;
import org.dataone.client.v2.MNode;
import org.dataone.client.v2.formats.ObjectFormatCache;
import org.dataone.client.v2.itk.D1Client;
import org.dataone.configuration.Settings;
import org.dataone.service.exceptions.InvalidToken;
import org.dataone.service.exceptions.NotAuthorized;
import org.dataone.service.exceptions.NotFound;
import org.dataone.service.exceptions.NotImplemented;
import org.dataone.service.exceptions.ServiceFailure;
import org.dataone.service.types.v1.Identifier;
import org.dataone.service.types.v1.Session;
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
    private static String mnBaseURL = Settings.getConfiguration().getString("index.mn.baseURL");
    private static String DataONEauthToken = null;
    private static Logger logger = Logger.getLogger(ObjectManager.class);
    
    
    private MNode mn = null;
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
        mn = D1Client.getMN(mnBaseURL);
        DataONEauthToken = System.getenv("DATAONE_AUTH_TOKEN");
        if (DataONEauthToken == null || DataONEauthToken.trim().equals("")) {
            DataONEauthToken =  Settings.getConfiguration().getString("DataONE.authToken");
            logger.debug("ObjectManager - Got token from properties file.");
        } else {
            logger.debug("ObjectManager - Got token from env.");
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
        SystemMetadata sysmeta = mn.getSystemMetadata(session, identifier);
        return sysmeta;
    }

}
