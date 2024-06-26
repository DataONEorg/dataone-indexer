package org.dataone.cn.indexer.parser.utility;

import java.io.IOException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.dataone.client.exception.ClientSideException;
import org.dataone.client.rest.DefaultHttpMultipartRestClient;
import org.dataone.client.v2.impl.MultipartMNode;
import org.dataone.client.v2.itk.D1Client;
import org.dataone.cn.indexer.object.ObjectManager;
import org.dataone.configuration.Settings;
import org.dataone.exceptions.MarshallingException;
import org.dataone.service.exceptions.InvalidToken;
import org.dataone.service.exceptions.NotAuthorized;
import org.dataone.service.exceptions.NotFound;
import org.dataone.service.exceptions.NotImplemented;
import org.dataone.service.exceptions.ServiceFailure;
import org.dataone.service.types.v1.Identifier;
import org.dataone.service.types.v2.SystemMetadata;

public class SeriesIdResolver {

    public static Log log = LogFactory.getLog(SeriesIdResolver.class);

    /**
     * Method to find HEAD PID for a given SID. If the provided identifier is already a PID, then it
     * will simply be returned. If the provided identifier is a SID, then the latest SystemMetadata
     * will be fetched and the PID for this latest revision will be returned.
     *
     * @param identifier the SID to look up (if a PID is provided it will simply be returned)
     * @return the HEAD PID for the given identifier
     * @throws InvalidToken
     * @throws ServiceFailure
     * @throws NotAuthorized
     * @throws NotFound
     * @throws NotImplemented
     * @throws ClientSideException
     * @throws IOException
     */
    public static Identifier getPid(Identifier identifier)
        throws InvalidToken, ServiceFailure, NotAuthorized, NotFound, NotImplemented, IOException,
        ClientSideException {
        // check if this is this a sid
        Identifier pid = identifier;
        log.debug("pid===" + pid.getValue());
        String mnBaseURL = Settings.getConfiguration().getString("dataone.mn.baseURL");
        String nodeType = Settings.getConfiguration().getString("dataone.nodeType");
        log.info("SeriesIdResolver.getPid - the current node type is " + nodeType);
        SystemMetadata fetchedSysmeta = null;
        if ((nodeType == null || !nodeType.equalsIgnoreCase("cn")) && mnBaseURL != null
            && !mnBaseURL.trim().equals("")) {
            log.info(
                "SeriesIdResolver.getPid - get the system metadata from the mn base url" + mnBaseURL
                    + " for the object " + identifier.getValue());
            try {
                MultipartMNode mnode =
                    new MultipartMNode(new DefaultHttpMultipartRestClient(), mnBaseURL);
                fetchedSysmeta = mnode.getSystemMetadata(null, identifier);
            } catch (Exception e) {
                log.warn("SeriesIdResolver.getPid - can't get the system metadata from the mn "
                             + mnBaseURL + " for the object " + pid.getValue() + " since "
                             + e.getMessage() + ". We will try to get it from cn.");
            }
        }
        if (fetchedSysmeta == null) {
            log.info("SeriesIdResolver.getPid - get the system metadata for the object "
                         + identifier.getValue()
                         + " from the cn since the current node is cn or the systemmetadata is not"
                         + " available on a mn with baseurl " + mnBaseURL);
            fetchedSysmeta = D1Client.getCN().getSystemMetadata(null, identifier);
        }
        if (fetchedSysmeta != null && !fetchedSysmeta.getIdentifier().getValue()
            .equals(identifier.getValue())) {
            log.debug("Found pid: " + fetchedSysmeta.getIdentifier().getValue() + " for sid: "
                          + identifier.getValue());
            pid = fetchedSysmeta.getIdentifier();
        }

        return pid;
    }



}
