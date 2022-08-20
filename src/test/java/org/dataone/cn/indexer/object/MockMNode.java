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
    private ApplicationContext provenanceContext = null;
    private Resource peggym1271Sys = null;
    private Resource peggym1281Sys = null;
    private Resource peggym1291Sys = null;
    private Resource peggym1304Sys = null; 
    private Resource provAlaWaiNS02MatlabProcessingDataProcessor1mSys = null;
    private Resource provAlaWaiNS02MatlabProcessingConfigure1mSys = null;
    private Resource provAlaWaiNS02MatlabProcessingScheduleAW02XX_001CTDXXXXR00Processing1mSys = null;
    private Resource provAlaWaiNS02MatlabProcessingEML1xmlSys = null;
    
    /*
     * Constructor
     */
    public MockMNode(String nodeBaseServiceUrl) throws IOException, ClientSideException {
        super(nodeBaseServiceUrl);
        this.nodeType = NodeType.MN;
        provenanceContext = new ClassPathXmlApplicationContext("org/dataone/cn/indexer/resourcemap/test-context-provenance.xml");
        
    }
    
    /**
     * Get the system metadata from a resource with the specified pid. 
     * It is a hard-coded mapping between pids for system metadata files.
     */
    @Override
    public SystemMetadata getSystemMetadata(Session session, Identifier id)
                            throws InvalidToken, ServiceFailure, NotAuthorized, NotFound, NotImplemented {
        System.out.println("In the get System metadata method in the MockMNode for " + id.getValue());
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
             case "ala-wai-canal-ns02-matlab-processing-DataProcessor.1.m" :
                 resource = provAlaWaiNS02MatlabProcessingDataProcessor1mSys;
                 break;
             case "ala-wai-canal-ns02-matlab-processing.eml.1.xml" :
                 resource = provAlaWaiNS02MatlabProcessingEML1xmlSys;
                 break;
             case "ala-wai-canal-ns02-matlab-processing-Configure.1.m" :
                 resource = provAlaWaiNS02MatlabProcessingConfigure1mSys;
                 break;
             case "ala-wai-canal-ns02-matlab-processing-schedule_AW02XX_001CTDXXXXR00_processing.1.m" :
                 resource = provAlaWaiNS02MatlabProcessingScheduleAW02XX_001CTDXXXXR00Processing1mSys;
                 break;
             default :
                 throw new NotFound("0000", "MockMNode couldn't find the system metadata for identifier " + identifier);
        }
        try {
            sysmeta = TypeMarshaller.unmarshalTypeFromStream(SystemMetadata.class, resource.getInputStream());
        } catch (Exception e) {
            throw new ServiceFailure("0000", e.getMessage());
        }
        System.out.println("In the get System metadata method in the MockMNode for " + id.getValue() + " after successfully getting it.");
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
                provAlaWaiNS02MatlabProcessingDataProcessor1mSys = (Resource) provenanceContext
                        .getBean("provAlaWaiNS02MatlabProcessingDataProcessor1mSys");

                provAlaWaiNS02MatlabProcessingConfigure1mSys = (Resource) provenanceContext
                        .getBean("provAlaWaiNS02MatlabProcessingConfigure1mSys");

                provAlaWaiNS02MatlabProcessingScheduleAW02XX_001CTDXXXXR00Processing1mSys = (Resource) provenanceContext
                        .getBean("provAlaWaiNS02MatlabProcessingScheduleAW02XX_001CTDXXXXR00Processing1mSys");

                provAlaWaiNS02MatlabProcessingEML1xmlSys = (Resource) provenanceContext
                        .getBean("provAlaWaiNS02MatlabProcessingEML1xmlSys");
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
