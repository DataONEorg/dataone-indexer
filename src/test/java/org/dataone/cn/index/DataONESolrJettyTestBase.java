package org.dataone.cn.index;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathExpressionException;

import jakarta.validation.constraints.AssertTrue;
import org.apache.commons.codec.EncoderException;
import org.apache.commons.collections.CollectionUtils;
import org.apache.solr.SolrJettyTestBase;
import org.apache.solr.SolrTestCaseJ4.SuppressSSL;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.embedded.JettyConfig;
import org.apache.solr.embedded.JettySolrRunner;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.client.solrj.util.ClientUtils;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.params.ModifiableSolrParams;
import org.dataone.cn.indexer.SolrIndex;
import org.dataone.cn.indexer.object.MockMNode;
import org.dataone.cn.indexer.object.ObjectManager;
import org.dataone.cn.indexer.parser.ISolrField;
import org.dataone.cn.indexer.solrhttp.SolrElementField;
import org.dataone.configuration.Settings;
import org.dataone.indexer.storage.Storage;
import org.dataone.service.exceptions.NotFound;
import org.dataone.service.exceptions.NotImplemented;
import org.dataone.service.exceptions.ServiceFailure;
import org.dataone.service.exceptions.UnsupportedType;
import org.dataone.service.types.v1.Identifier;
import org.dataone.service.util.DateTimeMarshaller;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.junit.Assert;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.core.io.Resource;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

/**
 * Solr unit test framework is dependent on JUnit 4.7. Later versions of junit
 * will break the base test classes.
 * 
 * @author sroseboo
 * 
 */
@SuppressSSL
public abstract class DataONESolrJettyTestBase extends SolrJettyTestBase {

    protected static ApplicationContext context;
    private SolrIndex solrIndexService;
    private int solrPort = Settings.getConfiguration().getInt("test.solr.port", 8985);
    private static final String DEFAULT_SOL_RHOME = "solr9home";
    private static final String SYSTEMMETA_FILE_NAME = "systemmetadata.xml";

    /**
     * Index the given object into solr
     * @param identifier  the identifier of the object which needs to be indexed
     * @param objectFile  the file path of the object which needs to be indexed
     * @throws Exception
     */
    protected void indexObjectToSolr(String identifier, Resource objectFile) throws Exception {
        boolean isSysmetaChangeOnly = false;
        String relativePath = objectFile.getFile().getPath();
        try (InputStream ignored = Storage.getInstance().retrieveObject(identifier)) {
            System.out.println("pid: " + identifier + " exists in hashstore.");
        } catch (FileNotFoundException e) {
            // The pid is not in the hash store, so we need to save the object into hashstore
            System.out.println("pid: " + identifier + " not found in hashstore. Saving object ["
                    + objectFile + "] into hashstore");
            try (InputStream object = objectFile.getInputStream()) {
                Storage.getInstance().storeObject(object, identifier);
            }
            File sysmetaFile = getSysmetaFile(relativePath);
            if (sysmetaFile != null) {
                try (InputStream sysmeta = new FileInputStream(sysmetaFile)) {
                    Storage.getInstance().storeMetadata(sysmeta, identifier);
                }
            }
        }
        Identifier pid = new Identifier();
        pid.setValue(identifier);
        //null is the value for docId
        solrIndexService.update(pid, isSysmetaChangeOnly, null);
    }

    /**
     * The convention method to get the system metadata file path from the objectPath.
     * We assume the object and system metadata file are in the same directory.
     * The system metadata file has a fixed name - systemmetadata.xml
     * @param  relativeObjPath  the relative path of the object
     * @return the file of system metadata. If it is null, this means the system metadata file
     * does not exist.
     */
    private static File getSysmetaFile(String relativeObjPath) {
        File sysmetaFile = null;
        String sysmetaPath = null;
        String relativeSysmetaPath = null;
        if (relativeObjPath != null) {
            if (relativeObjPath.contains(File.separator)) {
                relativeSysmetaPath = relativeObjPath.substring(0,
                            relativeObjPath.lastIndexOf(File.separator) + 1) + SYSTEMMETA_FILE_NAME;
            } else {
                // There is not path information in the object path ( it only has the file name).
                // So we just simply return systemmetadata.xml
                relativeSysmetaPath = SYSTEMMETA_FILE_NAME;
            }
        }
        if (relativeSysmetaPath != null) {
            sysmetaFile = new File(relativeSysmetaPath);
        }
        return sysmetaFile;
    }

    /**
     * Delete the given identifier from the solr server
     * @param identifier
     * @throws XPathExpressionException
     * @throws ServiceFailure
     * @throws NotImplemented
     * @throws NotFound
     * @throws UnsupportedType
     * @throws IOException
     * @throws EncoderException
     * @throws SolrServerException
     * @throws ParserConfigurationException
     * @throws SAXException
     */
    protected void deleteSolrDoc(String identifier)
        throws XPathExpressionException, ServiceFailure, NotImplemented, NotFound, UnsupportedType,
        IOException, EncoderException, SolrServerException, ParserConfigurationException,
        SAXException {
        Identifier pid = new Identifier();
        pid.setValue(identifier);
        solrIndexService.remove(pid);
    }


    protected SolrDocument assertPresentInSolrIndex(String pid) throws SolrServerException,
            IOException {
        ModifiableSolrParams solrParams = new ModifiableSolrParams();
        solrParams.set("q", "id:" + ClientUtils.escapeQueryChars(pid));
        solrParams.set("fl", "*");
        QueryResponse qr = getSolrClient().query(solrParams);
        Assert.assertFalse(qr.getResults().isEmpty());
        SolrDocument result = qr.getResults().get(0);
        String id = (String) result.getFieldValue("id");
        Assert.assertEquals(pid, id);
        return result;
    }

    protected SolrDocumentList findByField(String field, String value) throws SolrServerException,
            IOException {
        return findByQueryString(field + ":" + value);
    }

    protected SolrDocumentList findByQueryString(String query) throws SolrServerException,
            IOException {
        ModifiableSolrParams solrParams = new ModifiableSolrParams();
        solrParams.set("q", query);
        QueryResponse qr = getSolrClient().query(solrParams);
        return qr.getResults();
    }

    public void sendSolrDeleteAll() throws SolrServerException, IOException {
        getSolrClient().deleteByQuery("*:*");
        getSolrClient().commit();
    }

    protected void assertNotPresentInSolrIndex(String pid) throws SolrServerException, IOException {
        ModifiableSolrParams solrParams = new ModifiableSolrParams();
        solrParams.set("q", "id:" + pid);
        QueryResponse qr = getSolrClient().query(solrParams);
        Assert.assertTrue(qr.getResults().isEmpty());
    }

    protected SolrDocumentList getAllSolrDocuments() throws SolrServerException, IOException {
        ModifiableSolrParams solrParams = new ModifiableSolrParams();
        solrParams.set("q", "*:*");
        QueryResponse qr = getSolrClient().query(solrParams);
        return qr.getResults();
    }

    protected void compareFields(SolrDocument solrResult, Document metadataDoc,
            ISolrField fieldToCompare, String identifier) throws Exception {
        List<SolrElementField> fields = fieldToCompare.getFields(metadataDoc, identifier);
        if (fields.isEmpty() == false) {
            SolrElementField docField = fields.get(0);
            Object solrValueObject = solrResult.getFieldValue(docField.getName());

            System.out.println("Comparing value for field " + docField.getName());
            if (solrValueObject == null) {
                if (!"text".equals(docField.getName())) {
                    System.out.println("Null result value for field name:  " + docField.getName()
                            + ", actual: " + docField.getValue());
                    Assert.assertTrue(docField.getValue() == null || "".equals(docField.getValue()));
                }
            } else if (solrValueObject instanceof String) {
                String solrValue = (String) solrValueObject;
                String docValue = docField.getValue();
                System.out.println("Doc Value:  " + docValue);
                System.out.println("Solr Value: " + solrValue);
                Assert.assertEquals(docField.getValue(), solrValue);
            } else if (solrValueObject instanceof Boolean) {
                Boolean solrValue = (Boolean) solrValueObject;
                Boolean docValue = Boolean.valueOf(docField.getValue());
                System.out.println("Doc Value:  " + docValue);
                System.out.println("Solr Value: " + solrValue);
                Assert.assertEquals(docValue, solrValue);
            } else if (solrValueObject instanceof Integer) {
                Integer solrValue = (Integer) solrValueObject;
                Integer docValue = Integer.valueOf(docField.getValue());
                System.out.println("Doc Value:  " + docValue);
                System.out.println("Solr Value: " + solrValue);
                Assert.assertEquals(docValue, solrValue);
            } else if (solrValueObject instanceof Long) {
                Long solrValue = (Long) solrValueObject;
                Long docValue = Long.valueOf(docField.getValue());
                System.out.println("Doc Value:  " + docValue);
                System.out.println("Solr Value: " + solrValue);
                Assert.assertEquals(docValue, solrValue);
            } else if (solrValueObject instanceof Float) {
                Float solrValue = (Float) solrValueObject;
                Float docValue = Float.valueOf(docField.getValue());
                System.out.println("Doc Value:  " + docValue);
                System.out.println("Solr Value: " + solrValue);
                Assert.assertEquals(docValue, solrValue);
            } else if (solrValueObject instanceof Date) {
                Date solrValue = (Date) solrValueObject;
                Date docValue = DateTimeMarshaller.deserializeDateToUTC(docField.getValue());
                System.out.println("Doc Value:  " + docValue);
                System.out.println("Solr Value: " + solrValue);
                Assert.assertEquals(docValue.getTime(), solrValue.getTime());
            } else if (solrValueObject instanceof ArrayList) {
                ArrayList solrValueArray = (ArrayList) solrValueObject;
                ArrayList documentValueArray = new ArrayList();
                for (SolrElementField sef : fields) {
                    documentValueArray.add(sef.getValue());
                }
                System.out.println("Doc Value:  " + documentValueArray);
                System.out.println("Solr Value: " + solrValueArray);
                Assert.assertTrue(CollectionUtils.isEqualCollection(documentValueArray,
                        solrValueArray));
            } else {
                Assert.assertTrue(
                        "Unknown solr value object type for field: " + docField.getName(), false);
            }
            System.out.println("");
        }
    }

    public void setUp() throws Exception {
        super.setUp();
        loadSpringContext();
        startJettyAndSolr();
        //set up MockMnode for the ObjectManager
        MockMNode mockMNode = new MockMNode("http://mnode.foo");
        mockMNode.setContext(context);
        ObjectManager.setD1Node(mockMNode);
        System.out.println(
            "After setting mockNode for object manager in the test base setup method");
        sendSolrDeleteAll();
    }

    public void tearDown() throws Exception {
        super.tearDown();
    }

    protected void loadSpringContext() {
        if (context == null) {
            context = new ClassPathXmlApplicationContext("org/dataone/cn/index/test-context.xml");
        }
        solrIndexService = (SolrIndex) context.getBean("solrIndex");
    }

    protected void startJettyAndSolr() throws Exception {
        try {
            getJetty();
        } catch (IllegalStateException e) {
            String solrTestHome = System.getProperty("solrTestHome");
            System.out.println(
                "===========================The test solr home from the system property is "
                    + solrTestHome);
            if (solrTestHome == null || solrTestHome.trim().equals("")) {
                System.out.println("System property not set; using default instead...");
                solrTestHome = DEFAULT_SOL_RHOME;
            }
            System.out.println(
                "============================The final test solr home  is " + solrTestHome);
            JettyConfig jconfig = JettyConfig.builder().setPort(solrPort).build();
            File f = new File(".");
            String pathSt =
                f.getAbsolutePath() + "/src/test/resources/org/dataone/cn/index/resources/"
                    + solrTestHome;
            System.out.println(
                "============================The final full path to test-solr-home is " + pathSt);
            Path path = Paths.get(pathSt);
            assertTrue("Solr home directory not found! - " + pathSt, Files.isDirectory(path));
            createJettyWithPort(pathSt, jconfig);
        }
        waitForSolr();
    }

    private void waitForSolr() throws InterruptedException {
        int maxTries = 20;
        int delayMillis = 500;
        while (maxTries-- > 0) {
            try {
                HttpURLConnection conn = (HttpURLConnection) new URL(
                    "http://localhost:" + solrPort + "/solr/admin/ping").openConnection();
                conn.setConnectTimeout(1000);
                conn.setReadTimeout(1000);
                if (conn.getResponseCode() == 200) {
                    System.out.println("Solr is ready!");
                    return;
                }
            } catch (Exception e) {
                System.out.println(e.getMessage());
            }
            System.out.println("Waiting for Solr...");
            Thread.sleep(delayMillis);
        }
        fail("Solr did not start within " + ((maxTries * delayMillis)/1000) + " seconds; aborting");
    }

    // method is copied in from SolrJettyTestBase in order to set the port
    // number to match solr.properties (8983) for XPathDocumentParser to connect
    // to same solr server. If left unset, the port number is a random open
    // port.
    protected static JettySolrRunner createJettyWithPort(String solrHome, JettyConfig config)
            throws Exception {
        return createAndStartJetty(solrHome, config);
    }

    protected boolean compareFieldValue(String id, String fieldName, String expectedValue)
        throws SolrServerException, IOException {
        System.out.println("==================== start of compare");
        boolean equal = false;
        ModifiableSolrParams solrParams = new ModifiableSolrParams();
        solrParams.set("q", "id:" + ClientUtils.escapeQueryChars(id));
        solrParams.set("fl", "*");
        QueryResponse qr = getSolrClient().query(solrParams);
        SolrDocument result = qr.getResults().get(0);
        String value = (String)result.getFieldValue(fieldName);
        System.out.println(
            "+++++++++++++++++++The value of the field " + fieldName + " from Solr is " + value);
        System.out.println("The expected value of the field " + fieldName + " is " + expectedValue);
        return expectedValue.equals(value);
    }

    protected boolean compareFieldValue(String id, String fieldName, String[] expectedValues)
        throws SolrServerException, IOException {

        boolean equal = true;
        ModifiableSolrParams solrParams = new ModifiableSolrParams();
        solrParams.set("q", "id:" + ClientUtils.escapeQueryChars(id));
        solrParams.set("fl", "*");
        QueryResponse qr = getSolrClient().query(solrParams);
        SolrDocument result = qr.getResults().get(0);
        Collection<Object> solrValues = result.getFieldValues(fieldName);
        Object testResult = result.getFirstValue(fieldName);
        String[] solrValuesArray = new String[solrValues.size()];
        if(testResult instanceof Float) {
            // Solr returned a 'Float' value, so convert it to a string so that it can
            // be compared to the expected value.
            System.out.println("++++++++++++++++ Solr returned a 'Float'.");
            int iObj = 0;
            float fval;
            for (Object obj : solrValues) {
               fval = (Float) obj;
               solrValuesArray[iObj] = Float.toString(fval);
               iObj++;
            }
        } else if (testResult instanceof String) {
            System.out.println("++++++++++++++++ Solr returned a 'String'.");
            solrValuesArray = solrValues.toArray(new String[solrValues.size()]);
        } else if (testResult instanceof Date) {
            // Solr returned a 'Date' value, so convert it to a string so that it can
            // be compared to the expected value.
            System.out.println("++++++++++++++++ Solr returned a 'Date'.");
            TimeZone.setDefault(TimeZone.getTimeZone("GMT"));
            int iObj = 0;

            DateTimeZone.setDefault(DateTimeZone.UTC);
            DateTimeFormatter dtfOut = DateTimeFormat.forPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");

            Date dateObj;
            DateFormat dateFormat = new SimpleDateFormat("yyyy-mm-dd hh:mm:ss");
            for (Object obj : solrValues) {
                DateTime dateTime = new DateTime(obj);
                solrValuesArray[iObj] = dtfOut.print(dateTime);
                iObj++;
            }
        }

        System.out.println(
            "++++++++++++++++ the solr result array for the field " + fieldName + " is "
                + solrValuesArray);
        System.out.println(
            "++++++++++++++++ the expected values for the field " + fieldName + " is "
                + expectedValues);
        if (solrValuesArray.length != expectedValues.length) {
            equal = false;
            return equal;
        }
        if (solrValuesArray.length > 1) {
            Arrays.sort(expectedValues);
            Arrays.sort(solrValuesArray);
        }
        for (int i=0; i<solrValuesArray.length; i++) {
            System.out.println(
                "++++++++++++++++ compare values for field " + "\"" + fieldName + "\"" + " Solr: "
                    + solrValuesArray[i] + " expected value: " + expectedValues[i]);

            if (!solrValuesArray[i].equals(expectedValues[i])) {
                equal = false;
                break;
            }
        }
        return equal;
        
    }
    
    /**
     * Get the context
     * @return  the context
     */
    public static ApplicationContext getContext() {
        if (context == null) {
            context = new ClassPathXmlApplicationContext("org/dataone/cn/index/test-context.xml");
        }
        return context;
    }
}
