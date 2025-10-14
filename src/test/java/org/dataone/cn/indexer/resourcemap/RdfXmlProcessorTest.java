package org.dataone.cn.indexer.resourcemap;


import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.dataone.cn.index.DataONESolrJettyTestBase;
import org.dataone.cn.indexer.annotation.RdfXmlSubprocessor;
import org.dataone.cn.indexer.convert.SolrDateConverter;
import org.dataone.cn.indexer.solrhttp.SolrDoc;
import org.dataone.cn.indexer.solrhttp.SolrElementField;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.core.io.Resource;

import com.carrotsearch.randomizedtesting.annotations.ThreadLeakScope;

/**
 * RDF/XML Subprocessor test for provenance field handling
 */
@ThreadLeakScope(ThreadLeakScope.Scope.NONE)
public class RdfXmlProcessorTest extends DataONESolrJettyTestBase {

    /* Log it */
    private static Log log = LogFactory.getLog(RdfXmlProcessorTest.class);

    /* the conext with provenance-specific bean definitions */
    private ApplicationContext provenanceContext = null;

    /* The index task processor used to process tasks in the queue */
    //protected IndexTaskProcessor processor;

    /* The task generator that adds tasks to the queue */
    //private IndexTaskGenerator generator;

    /* the RDF/XML resource map to parse */
    private Resource provAlaWaiNS02MatlabProcessing2RDF;
    private String provAlaWaiNS02MatlabProcessing2RDFPid = "ala-wai-ns02-matlab-processing.2.rdf";

    /* The three Matlab scripts involved in the processing */
    private Resource provAlaWaiNS02MatlabProcessingDataProcessor1m;
    private String provAlaWaiNS02MatlabProcessingDataProcessor1mPid =
        "ala-wai-canal-ns02-matlab-processing-DataProcessor.1.m";
    private Resource provAlaWaiNS02MatlabProcessingConfigure1m;
    private String provAlaWaiNS02MatlabProcessingConfigure1mPid =
        "ala-wai-canal-ns02-matlab-processing-Configure.1.m";
    private Resource provAlaWaiNS02MatlabProcessingScheduleAW02XX_001CTDXXXXR00Processing1m;
    private String provAlaWaiNS02MatlabProcessingScheduleAW02XX_001CTDXXXXR00Processing1mPid =
        "ala-wai-canal-ns02-matlab-processing-schedule_AW02XX_001CTDXXXXR00_processing.1.m";

    /* The EML science metadata describing the processing */
    private Resource provAlaWaiNS02MatlabProcessingEML1xml;
    private String provAlaWaiNS02MatlabProcessingEML1xmlPid = "ala-wai-canal-ns02-matlab-processing.eml.1.xml";

    /* The input data being processed */
    private Resource provAlaWaiNS02CTDData1txt;
    private String provAlaWaiNS02CTDData1txtPid = "ala-wai-canal-ns02-ctd-data.1.txt";

    /* The processed output image */
    private Resource provAlaWaiNS02ImageDataAW02XX_001CTDXXXXR00_20150203_10day1jpg;
    private String provAlaWaiNS02ImageDataAW02XX_001CTDXXXXR00_20150203_10day1jpgPid =
        "ala-wai-canal-ns02-image-data-AW02XX_001CTDXXXXR00_20150203_10day.1.jpg";
    
    /* The eml 2.2.0 object is part of a portal */
    private Resource partEml220;
    private String partEml220Pid = "eml-test-doc";
    
    /* The portal object which has the part eml 220 object*/
    private Resource partPortal;
    private String partPortalPid = "urn:uuid:b210adf0-f08a-4cae-aa86-5b64605e9297";
    
    /* The resource map describing the hasPart/isPartOf relationship */
    private Resource partResourcemap;
    private String partResourcemapPid = "resourcemap-with-part.xml";

    /* An instance of the RDF/XML Subprocessor */
    private RdfXmlSubprocessor provRdfXmlSubprocessor;

    /* A date converter for many date strings */
    private SolrDateConverter dateConverter = new SolrDateConverter();

    /* Store a map of expected Solr fields and their values for testing */
    private HashMap<String, String> expectedFields = new HashMap<String, String>();

    /* Define provenance field names used in the index */
    private String WAS_DERIVED_FROM_FIELD = "prov_wasDerivedFrom";
    private String WAS_GENERATED_BY_FIELD = "prov_wasGeneratedBy";
    private String WAS_INFORMED_BY_FIELD = "prov_wasInformedBy";
    private String USED_FIELD = "prov_used";
    private String GENERATED_FIELD = "prov_generated";
    private String GENERATED_BY_PROGRAM_FIELD = "prov_generatedByProgram";
    private String GENERATED_BY_EXECUTION_FIELD = "prov_generatedByExecution";
    private String GENERATED_BY_USER_FIELD = "prov_generatedByUser";
    private String USED_BY_PROGRAM_FIELD = "prov_usedByProgram";
    private String USED_BY_EXECUTION_FIELD = "prov_usedByExecution";
    private String USED_BY_USER_FIELD = "prov_usedByUser";
    private String WAS_EXECUTED_BY_EXECUTION_FIELD = "prov_wasExecutedByExecution";
    private String WAS_EXECUTED_BY_USER_FIELD = "prov_wasExecutedByUser";
    private String HAS_SOURCES_FIELD = "prov_hasSources";
    private String HAS_DERIVATIONS_FIELD = "prov_hasDerivations";
    private String INSTANCE_OF_CLASS_FIELD = "prov_instanceOfClass";
    
    private static final int SLEEPTIME = 5000;

    @BeforeClass
    public static void init() {
        //HazelcastClientFactoryTest.setUp();
    }

    @AfterClass
    public static void cleanup() throws Exception {
        //HazelcastClientFactoryTest.shutDown();
    }

    /**
     * For each test, set up the Solr service and test data
     * 
     * @throws Exception
     */
    @Before
    public void setUp() throws Exception {

        // Start up the embedded Jetty server and Solr service
        super.setUp();

        // load the prov context beans
        configureSpringResources();

        // instantiate the subprocessor
        provRdfXmlSubprocessor = (RdfXmlSubprocessor) context.getBean("rdfXMLSubprocessor");
        sendSolrDeleteAll();

    }

    /**
     * For each test, clean up, bring down the Solr service
     */
    @After
    public void tearDown() throws Exception {
        super.tearDown();

    }

    /* 
     * Compares the indexed provenance Solr fields to the expected fields
     */
    protected boolean compareFields(HashMap<String, String> expectedFields,
            InputStream resourceMap, RdfXmlSubprocessor provRdfXmlSubProcessor, String identifier,
            String referencedPid) throws Exception {

        // A map for the sub processor to populate
        Map<String, SolrDoc> docs = new TreeMap<String, SolrDoc>();

        // Build a minimal SolrDoc keyed by the id
        SolrDoc solrDoc = new SolrDoc();
        SolrElementField identifierField = new SolrElementField();
        identifierField.setName(SolrElementField.FIELD_ID);
        identifierField.setValue(identifier);
        solrDoc.addField(identifierField);
        docs.put(identifier, solrDoc);

        // The returned map with processed Solr documents
        Map<String, SolrDoc> solrDocs = provRdfXmlSubProcessor.processDocument(identifier, docs,
                resourceMap);

        // A list of Solr fields filtered by the target object identifier
        System.out.println("referencedPid: " + referencedPid);
        
        Set<String> keys = solrDocs.keySet();
        for (String key : keys) {
            System.out.println("============= the key is " + key);
        }

        SolrDoc referencedPidDoc = solrDocs.get(referencedPid);

        if (referencedPidDoc == null) {
            fail("Failed to find a SolrDoc for referencedPid: " + referencedPid);

        }
        List<SolrElementField> fields = referencedPidDoc.getFieldList();

        // compare the expected and processed fields
        for (SolrElementField field : fields) {
            String name = field.getName();
            String value = field.getValue();
            log.debug("Field name: " + name);
            String expectedValue = expectedFields.get(name);

            if (expectedValue != null) {
                List<String> expectedValues = Arrays.asList(StringUtils.split(expectedValue, "||"));
                if (expectedValues != null && !expectedValues.isEmpty()) {
                    System.out.println("Checking value:\t" + value);
                    System.out.println("in expected: \t" + expectedValues);
                    Assert.assertTrue(expectedValues.contains(value));
                }
            }

        }
        return true;

    }

    /**
     * Test if the provenance fields in resource maps are processed correctly with the 
     * RdfXmlsubprocessor. This test does not add content to Hazelcast or Solr.
     * 
     * @throws Exception
     */
    //@Ignore
    @Test
    public void testProvenanceFields() throws Exception {

        log.debug("Testing RDF/XML provenance indexing of ala-wai-ns02-matlab-processing.2.rdf: ");

        // Ensure fields associated with the data input objects are indexed
        expectedFields.clear();
        expectedFields.put(USED_BY_PROGRAM_FIELD,
                "ala-wai-canal-ns02-matlab-processing-schedule_AW02XX_001CTDXXXXR00_processing.1.m"
                        + "||" + "ala-wai-canal-ns02-matlab-processing-Configure.1.m" + "||"
                        + "ala-wai-canal-ns02-matlab-processing-DataProcessor.1.m");
        expectedFields
                .put(USED_BY_EXECUTION_FIELD, "urn:uuid:6EC8CAB7-2063-4440-BA23-364313C145FC");
        expectedFields
                .put(INSTANCE_OF_CLASS_FIELD, "http://purl.dataone.org/provone/2015/01/15/ontology#Data");
        expectedFields.put(USED_BY_USER_FIELD, "urn:uuid:D89221AD-E251-4CCB-B515-09D869DB1A61");
        compareFields(expectedFields, provAlaWaiNS02MatlabProcessing2RDF.getInputStream(),
                provRdfXmlSubprocessor, "ala-wai-ns02-matlab-processing.2.rdf",
                "ala-wai-canal-ns02-ctd-data.1.txt");

        // Ensure fields associated with the data output objects are indexed
        expectedFields.clear();
        expectedFields.put(WAS_DERIVED_FROM_FIELD, "ala-wai-canal-ns02-ctd-data.1.txt");
        expectedFields.put(GENERATED_BY_PROGRAM_FIELD,
                "ala-wai-canal-ns02-matlab-processing-schedule_AW02XX_001CTDXXXXR00_processing.1.m"
                        + "||" + "ala-wai-canal-ns02-matlab-processing-Configure.1.m" + "||"
                        + "ala-wai-canal-ns02-matlab-processing-DataProcessor.1.m");
        expectedFields.put(GENERATED_BY_EXECUTION_FIELD,
                "urn:uuid:6EC8CAB7-2063-4440-BA23-364313C145FC");
        expectedFields
                .put(GENERATED_BY_USER_FIELD, "urn:uuid:D89221AD-E251-4CCB-B515-09D869DB1A61");
        expectedFields
                .put(INSTANCE_OF_CLASS_FIELD, "http://purl.dataone.org/provone/2015/01/15/ontology#Data");
        compareFields(expectedFields, provAlaWaiNS02MatlabProcessing2RDF.getInputStream(),
                provRdfXmlSubprocessor, "ala-wai-ns02-matlab-processing.2.rdf",
                "ala-wai-canal-ns02-image-data-AW02XX_001CTDXXXXR00_20150203_10day.1.jpg");
        // Ensure fields associated with program objects are indexed
        expectedFields.clear();
        expectedFields.put(USED_FIELD, "ala-wai-canal-ns02-ctd-data.1.txt");
        expectedFields.put(GENERATED_FIELD,
                "ala-wai-canal-ns02-image-data-AW02XX_001CTDXXXXR00_20150203_10day.1.jpg");
        compareFields(expectedFields, provAlaWaiNS02MatlabProcessing2RDF.getInputStream(),
                provRdfXmlSubprocessor, "ala-wai-ns02-matlab-processing.2.rdf",
                "ala-wai-canal-ns02-matlab-processing-schedule_AW02XX_001CTDXXXXR00_processing.1.m");

        // Ensure fields associated with the data input object's metadata are indexed
        expectedFields.clear();
        expectedFields.put(
            HAS_DERIVATIONS_FIELD,
            "ala-wai-canal-ns02-image-data-AW02XX_001CTDXXXXR00_20150203_10day.1.jpg");
        compareFields(expectedFields, provAlaWaiNS02MatlabProcessing2RDF.getInputStream(), 
            provRdfXmlSubprocessor, "ala-wai-ns02-matlab-processing.2.rdf", 
            "ala-wai-canal-ns02-ctd-data.1.txt");

        // Ensure fields associated with the data output object's metadata are indexed
        //expectedFields.clear();
        //expectedFields.put(HAS_SOURCES_FIELD, "ala-wai-canal-ns02-ctd-data.1.txt");
        //compareFields(expectedFields, provAlaWaiNS02MatlabProcessing2RDF.getInputStream(), 
        //    provRdfXmlSubprocessor, "ala-wai-ns02-matlab-processing.2.rdf", 
        //    "ala-wai-canal-ns02-image-data.eml.1.xml");

    }
    
    /**
     * Test if the hasPart/isPartOf fields in resource maps are processed correctly with the 
     * RdfXmlsubprocessor. This test does not add content to Hazelcast or Solr.
     * 
     * @throws Exception
     */
    @Test
    public void testPartFields() throws Exception {

        log.debug("Testing RDF/XML parts indexing of resourcemap-with-part.xml: ");
        // Ensure fields associated with the data input objects are indexed
        expectedFields.clear();
        expectedFields.put("isPartOf", "clown-portal");
        compareFields(expectedFields, partResourcemap.getInputStream(),
                provRdfXmlSubprocessor, "resource_map_urn:uuid:cd489c7e-be88-4d57-b13a-911b25a0b47f",
                "urn:uuid:f18812ac-7f4f-496c-82cc-3f4f54830274");
        expectedFields.clear();
        expectedFields.put("isPartOf", "https://doi.org/10.5063/12345");
        compareFields(expectedFields, partResourcemap.getInputStream(),
                      provRdfXmlSubprocessor, "resource_map_urn:uuid:cd489c7e-be88-4d57-b13a-911b25a0b47f",
                      "urn:uuid:f18812ac-7f4f-496c-82cc-12345");
    }


    /**
     * Test the end to end index processing of a resource map with provenance statements
     * 
     * @throws Exception
     */
    //@Ignore
    @Test
    public void testInsertProvResourceMap() throws Exception {
        String script1 = provAlaWaiNS02MatlabProcessingDataProcessor1mPid;
        indexObjectToSolr(script1, provAlaWaiNS02MatlabProcessingDataProcessor1m);

        String script2 = provAlaWaiNS02MatlabProcessingConfigure1mPid;
        indexObjectToSolr(script2,provAlaWaiNS02MatlabProcessingConfigure1m);

        String script3 = provAlaWaiNS02MatlabProcessingScheduleAW02XX_001CTDXXXXR00Processing1mPid;
        indexObjectToSolr(script3, provAlaWaiNS02MatlabProcessingScheduleAW02XX_001CTDXXXXR00Processing1m);

        // Insert the EML file into the task queue
        String emlDoc = provAlaWaiNS02MatlabProcessingEML1xmlPid;
        indexObjectToSolr(emlDoc, provAlaWaiNS02MatlabProcessingEML1xml);

        // Insert the output image into the task queue
        String jpgImage = provAlaWaiNS02ImageDataAW02XX_001CTDXXXXR00_20150203_10day1jpgPid;
        indexObjectToSolr(jpgImage, provAlaWaiNS02ImageDataAW02XX_001CTDXXXXR00_20150203_10day1jpg);

        // Insert the CTD data into the task queue
        String ctdData = provAlaWaiNS02CTDData1txtPid;
        indexObjectToSolr(ctdData, provAlaWaiNS02CTDData1txt);
        
        Thread.sleep(SLEEPTIME);
        // now process the tasks
        //processor.processIndexTaskQueue();

        // Insert the resource map into the task queue
        String resourceMap = provAlaWaiNS02MatlabProcessing2RDFPid;
        //formatId = "http://www.openarchives.org/ore/terms";
        indexObjectToSolr(resourceMap, provAlaWaiNS02MatlabProcessing2RDF);

        Thread.sleep(SLEEPTIME);
        // now process the tasks
        //processor.processIndexTaskQueue();
        

        Thread.sleep(SLEEPTIME);
        Thread.sleep(SLEEPTIME);
        Thread.sleep(SLEEPTIME);
        // ensure everything indexed properly
        assertPresentInSolrIndex(script1);
        assertPresentInSolrIndex(script2);
        assertPresentInSolrIndex(script3);
        assertPresentInSolrIndex(emlDoc);
        assertPresentInSolrIndex(jpgImage);
        assertPresentInSolrIndex(ctdData);
        assertPresentInSolrIndex(resourceMap);

    }
    
    /**
     * Test the end to end index processing of a resource map with provenance statements
     * 
     * @throws Exception
     */
    //@Ignore
    @Test
    public void testInsertPartsResourceMap() throws Exception {
        // Insert the EML file into the task queue
        String emlId = partEml220Pid;
        //formatId = "https://eml.ecoinformatics.org/eml-2.2.0";
        indexObjectToSolr(emlId, partEml220);

        // Insert the portal document into the task queue
        String portalId = partPortalPid;
        //formatId = "https://purl.dataone.org/portals-1.0.0";
        String serieId = "urn:uuid:27ae3627-be62-4963-859a-8c96d940cadc";
        indexObjectToSolr(portalId, partPortal);

        Thread.sleep(SLEEPTIME);
        // now process the tasks
        //processor.processIndexTaskQueue();

        // Insert the resource map into the task queue
        String resourceMapId = "resource_map_urn:uuid:cd489c7e-be88-4d57-b13a-911b25a0b47f";
        //formatId = "http://www.openarchives.org/ore/terms";
        //The following statement was commented before we created the dataone-indexer project
        //insertResource(resourceMapId, formatId, partResourcemap, nodeid, userDN);

        Thread.sleep(SLEEPTIME);
        Thread.sleep(SLEEPTIME);
        assertPresentInSolrIndex(emlId);
        assertPresentInSolrIndex(portalId);
        //assertPresentInSolrIndex(resourceMapId);
        assertTrue(compareFieldValue(emlId, "title", "EML Annotation Example"));

    }
    

    /**
     *  Default test - is JUnit working as expected?
     */
    @Test
    public void testInit() {
        Assert.assertTrue(1 == 1);

    }

    /* Load the indexer and provenance context beans */
    protected void configureSpringResources() throws IOException {

        // Instantiate the generator and processor from the test-context beans
        //processor = (IndexTaskProcessor) context.getBean("indexTaskProcessor");
        //generator = (IndexTaskGenerator) context.getBean("indexTaskGenerator");

        // instantiate the RDF resource to be tested
        if (provenanceContext == null) {
            provenanceContext = new ClassPathXmlApplicationContext(
                    "org/dataone/cn/indexer/resourcemap/test-context-provenance.xml");
        }
        provAlaWaiNS02MatlabProcessing2RDF = (Resource) provenanceContext
                .getBean("provAlaWaiNS02MatlabProcessing2RDF");

        provAlaWaiNS02MatlabProcessingDataProcessor1m = (Resource) provenanceContext
                .getBean("provAlaWaiNS02MatlabProcessingDataProcessor1m");

        provAlaWaiNS02MatlabProcessingConfigure1m = (Resource) provenanceContext
                .getBean("provAlaWaiNS02MatlabProcessingConfigure1m");

        provAlaWaiNS02MatlabProcessingScheduleAW02XX_001CTDXXXXR00Processing1m = (Resource) provenanceContext
                .getBean("provAlaWaiNS02MatlabProcessingScheduleAW02XX_001CTDXXXXR00Processing1m");

        provAlaWaiNS02MatlabProcessingEML1xml = (Resource) provenanceContext
                .getBean("provAlaWaiNS02MatlabProcessingEML1xml");

        provAlaWaiNS02CTDData1txt = (Resource) provenanceContext
                .getBean("provAlaWaiNS02CTDData1txt");

        provAlaWaiNS02ImageDataAW02XX_001CTDXXXXR00_20150203_10day1jpg = (Resource) provenanceContext
                .getBean("provAlaWaiNS02ImageDataAW02XX_001CTDXXXXR00_20150203_10day1jpg");
        
        partEml220 = (Resource) provenanceContext.getBean("eml220TestDocSciMeta");
        
        partPortal = (Resource) provenanceContext.getBean("portalTestDoc");
        
        partResourcemap = (Resource) provenanceContext.getBean("partResourceMap");
    }

  
}
