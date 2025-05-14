package org.dataone.cn.indexer.object;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.security.NoSuchAlgorithmException;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.output.ByteArrayOutputStream;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.dataone.client.auth.AuthTokenSession;
import org.dataone.client.exception.ClientSideException;
import org.dataone.client.rest.HttpMultipartRestClient;
import org.dataone.client.rest.MultipartRestClient;
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
import org.dataone.service.types.v2.SystemMetadata;
import org.dataone.service.util.TypeMarshaller;


/**
 * A class to get the file path and system metadata for an identifier
 * @author tao
 *
 */
public abstract class ObjectManager {
    protected static String nodeBaseURL = Settings.getConfiguration().getString("dataone.mn.baseURL");
    private static String DataONEauthToken = null;
    private static Log logger = LogFactory.getLog(ObjectManager.class);
    private static final String TOKEN_VARIABLE_NAME = "DATAONE_AUTH_TOKEN";
    private static final String TOKEN_FILE_PATH_PROP_NAME = "dataone.nodeToken.file";

    protected static MultipartD1Node d1Node = null;
    protected static Session session = null;

    /**
     * Get the system metadata for the given id
     * @param id  the id to identify the system metadata
     * @return  the input stream of the system metadata associated with the id. It may be null.
     * @throws InvalidToken
     * @throws NotAuthorized
     * @throws NotImplemented
     * @throws ServiceFailure
     * @throws NotFound
     * @throws MarshallingException
     * @throws IOException
     * @throws NoSuchAlgorithmException
     */
    public abstract InputStream getSystemMetadataStream(String id)
        throws InvalidToken, NotAuthorized, NotImplemented, ServiceFailure, NotFound,
        NoSuchAlgorithmException, IOException, MarshallingException;

    /**
     * Get the system metadata object for the given identifier
     * @param id  the id to identify the system metadata
     * @return the system metadata object associated with the id. It may be null.
     * @throws InvalidToken
     * @throws NotAuthorized
     * @throws NotImplemented
     * @throws ServiceFailure
     * @throws NotFound
     * @throws InstantiationException
     * @throws IllegalAccessException
     * @throws IOException
     * @throws MarshallingException
     * @throws NoSuchAlgorithmException
     */
    public abstract org.dataone.service.types.v1.SystemMetadata getSystemMetadata(String id)
                                       throws InvalidToken, NotAuthorized, NoSuchAlgorithmException,
                                                NotImplemented, ServiceFailure, NotFound,
                                                InstantiationException, IllegalAccessException,
                                                IOException, MarshallingException;

    /**
     * Get the input stream of the content of the given pid
     * @param pid  the identifier of the content
     * @return the input stream of the content
     * @throws IllegalArgumentException
     * @throws FileNotFoundException
     * @throws NoSuchAlgorithmException
     * @throws IOException
     * @throws NotFound
     */
    public abstract InputStream getObject(String pid)
        throws IllegalArgumentException, FileNotFoundException, NoSuchAlgorithmException,
        IOException, NotFound;

    /**
     * Set the d1 node for this object manager.
     * We only use it for testing
     * @param node  the d1node will be assigned
     */
    public static void setD1Node(MultipartD1Node node) {
        d1Node = node;
    }

    /**
     * In case the token expired, the method will retrieve the token and create a new d1 node
     * @throws ServiceFailure 
     */
    protected static void refreshD1Node() throws ServiceFailure {
       //get the token
        DataONEauthToken = System.getenv(TOKEN_VARIABLE_NAME);
        if (DataONEauthToken == null || DataONEauthToken.isBlank()) {
            //can't get the token from the env variable. So try to get it from a file specified
            // in the property
            String tokenFilePath = Settings.getConfiguration().getString(TOKEN_FILE_PATH_PROP_NAME);
            if (tokenFilePath != null && !tokenFilePath.trim().equals("")) {
                logger.info(
                    "Can NOT get the token from the env variable so try to get the auth token "
                        + "from the file " + tokenFilePath);
                try {
                    DataONEauthToken = FileUtils.readFileToString(new File(tokenFilePath), "UTF-8");
                } catch (IOException e) {
                    DataONEauthToken = null;
                    logger.warn("Can NOT get the authen token from the file " + tokenFilePath +
                                    " since " + e.getMessage());
                }
                if (DataONEauthToken != null && !DataONEauthToken.isBlank()) {
                    logger.info("Got the auth token from the file "+ tokenFilePath);
                }
            }
        } else {
            logger.info("Got the auth token from an env. variable");
        }
        if (DataONEauthToken == null || DataONEauthToken.isBlank()) {
            logger.warn(
                "Could NOT get an auth token from either an env. variable or the properties file."
                    + " So it will act as the public user.");
        }
        session = createSession(DataONEauthToken);
        logger.info("Going to create the d1node with url " + nodeBaseURL);
        try {
            d1Node = getMultipartD1Node(session, nodeBaseURL);
        } catch (IOException | ClientSideException e) {
            logger.error("Couldn't create the d1node for the url " + nodeBaseURL + " since "
                             + e.getMessage());
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
    private static Session createSession(String authToken) {
        Session session = null;
        if (authToken == null || authToken.trim().equals("")) {
            logger.info("ObjectManager.createSession - Creating the public session");
            session = new Session();
        } else {
            logger.info("Creating authentication session from token: " + authToken.substring(0, 5)
                            + "...");
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
     */
    private static MultipartD1Node getMultipartD1Node(Session session, String serviceUrl)
        throws IOException, ClientSideException {
        MultipartRestClient mrc = null;
        MultipartD1Node d1Node = null;
        // First create a default HTTP client
        mrc = new HttpMultipartRestClient();
        // Now create a DataONE object that uses the rest client
        Boolean isCN = isCN(serviceUrl);
        // Now create a DataONE object that uses the rest client
        if (isCN) {
            logger.info("Creating cn MultipartMNode from the url " + serviceUrl);
            d1Node = new MultipartCNode(mrc, serviceUrl, session);
        } else {
            logger.info("Creating mn MultipartMNode from the url " + serviceUrl);
            d1Node = new MultipartMNode(mrc, serviceUrl, session);
        }
        return d1Node;
    }
    
    /*
     * Determine if the string represents a DataONE CN.
     * @param nodeStr either a DataONE node serviceURL (e.g. https://knb.ecoinformatics.org/knb/d1/mn)
     *      or a DataONE node identifier (e.g. urn:node:CN)
     */
    private static Boolean isCN(String nodeStr) {
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

    protected SystemMetadata getSystemMetadataByAPI(String id)
        throws ServiceFailure, InvalidToken, NotImplemented, NotAuthorized, NotFound {
        if (d1Node != null) {
            // Metacat can't find the system metadata from the storage system.
            // So try to get it from the dataone api
            Identifier identifier = new Identifier();
            identifier.setValue(id);
            return d1Node.getSystemMetadata(session, identifier);
        } else {
            throw new ServiceFailure("0000", "The d1Node is null and Indexer cannot get the "
                + "systemmetadata by a API call.");
        }
    }

}
