package org.dataone.cn.indexer.object;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.io.input.AutoCloseInputStream;
import org.dataone.client.exception.ClientSideException;
import org.dataone.client.v2.impl.MultipartMNode;
import org.dataone.cn.indexer.SolrIndex;
import org.dataone.exceptions.MarshallingException;
import org.dataone.service.exceptions.InsufficientResources;
import org.dataone.service.exceptions.InvalidToken;
import org.dataone.service.exceptions.NotAuthorized;
import org.dataone.service.exceptions.NotFound;
import org.dataone.service.exceptions.NotImplemented;
import org.dataone.service.exceptions.ServiceFailure;
import org.dataone.service.types.v1.Identifier;
import org.dataone.service.types.v1.NodeType;
import org.dataone.service.types.v1.Session;
import org.dataone.service.types.v2.SystemMetadata;
import org.dataone.service.util.TypeMarshaller;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.core.io.Resource;

/**
 * MockMNode mimics a DataONE Member Node, and should be used only for testing
 * when there is a dependency on MN services
 */
public class MockMNode extends MultipartMNode {
    public static final String NODE_ID = "urn:node:MNMetacat_Test";
    private ApplicationContext context = null;
    private Resource peggym1271Sys = null;
    private Resource peggym1281Sys = null;
    private Resource peggym1291Sys = null;
    private Resource peggym1304Sys = null; 
    /*
     * Constructor
     */
    public MockMNode(String nodeBaseServiceUrl) throws IOException, ClientSideException {
        super(nodeBaseServiceUrl);
        this.nodeType = NodeType.MN;
    }
    
    /**
     * Get the system metadata from a resource with the specified pid. 
     * It is a hard-coded mapping between pids for system metadata files.
     */
    @Override
    public SystemMetadata getSystemMetadata(Session session, Identifier id)
                            throws InvalidToken, ServiceFailure, NotAuthorized, NotFound, NotImplemented {
        System.out.println("in the get System metadata in the MockMNode.");
        SystemMetadata sysmeta = null;
        initialize();
        Resource resource = null;
        if (id == null || id.getValue().trim().equals("")) {
            throw new InvalidToken("0000", "In the getSystemMetadata method, the id can't be null or blank");
        }
        String identifier = id.getValue();
        switch (identifier) {
            case "peggym.127.1" :
                resource = peggym1271Sys;
                break;
             case "peggym.128.1" :
                 resource = peggym1281Sys;
                 break;
             case "peggym.129.1" :
                 resource = peggym1291Sys;
                 break;
             case "peggym.130.4" :
                 resource = peggym1304Sys;
                 break;
             default :
                 throw new NotFound("0000", "MockMNode couldn't find the system metadata for identifier " + identifier);
        }
        try {
            sysmeta = TypeMarshaller.unmarshalTypeFromStream(SystemMetadata.class, resource.getInputStream());
        } catch (Exception e) {
            throw new ServiceFailure("0000", e.getMessage());
        }
        return sysmeta;
    }
    
    /**
     * Initialize the resource (only once)
     * @throws ServiceFailure
     */
    private void initialize() throws ServiceFailure{
        if (peggym1271Sys == null) {
            synchronized (MockMNode.class){
                if (context == null) {
                    throw new ServiceFailure("0000", "The context should be set before calling the getSystemMetadata method.");
                }
                peggym1271Sys = (Resource) context.getBean("peggym1271Sys");
                peggym1281Sys = (Resource) context.getBean("peggym1281Sys");
                peggym1291Sys = (Resource) context.getBean("peggym1291Sys");
                peggym1304Sys = (Resource) context.getBean("peggym1304Sys");
                System.out.println("finished to intialize the resources in the MockMNode");
            }
        }
    }
    
    /**
     * Set the context first
     * @param context
     */
    public void setContext(ApplicationContext context) {
        this.context = context;
    }
}
