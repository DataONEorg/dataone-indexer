package org.dataone.cn.index;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.io.*;
import org.dataone.cn.indexer.IndexWorkerTest;
import org.dataone.cn.indexer.parser.ScienceMetadataDocumentSubprocessor;
import org.dataone.cn.indexer.solrhttp.SolrDoc;
import org.dataone.cn.indexer.solrhttp.SolrElementField;
import org.dataone.configuration.Settings;
import org.junit.Test;
import org.junit.Before;

import static org.junit.Assert.assertTrue;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

/**
 *
 */
public class PortalSubprocessorTest {
    static {
        try {
            Settings.augmentConfiguration(IndexWorkerTest.PORT_8985_PROPERTY_FILE_PATH);
        } catch (ConfigurationException e) {
            throw new RuntimeException(e);
        }
    }
    protected ApplicationContext context;
    protected ScienceMetadataDocumentSubprocessor processor;

    /**
     * Attempt to parse example 'portal' documents using the indexer PortalSubProcessor. See 'application-context-portals.xml'
     * for details on the indexer beans that are configured for parsing these types of documents.
     * @throws Exception something bad happened
     */
    @Test
    public void testProcessDocument() throws Exception {
        String id = "urn:uuid:349aa330-4645-4dab-a02d-3bf950cf708i";

        // Three different portal documents will be processed
        ArrayList<String> portalFiles = new ArrayList<>();
        portalFiles.add("src/test/resources/collection/portal-1.1.0-example.xml");
        portalFiles.add("src/test/resources/collection/collection-1.1.0-example-filterGroup.xml");
        portalFiles.add("src/test/resources/collection/collection-1.1.0-example2-filterGroup.xml");
        portalFiles.add("src/test/resources/collection/collection-1.1.0-example-filterGroup-operator.xml");
        portalFiles.add("src/test/resources/collection/collection-1.1.0-example-fieldsOperator.xml");
        portalFiles.add("src/test/resources/collection/portal-1.1.0-example-negation-only.xml");
        portalFiles.add("src/test/resources/collection/collection-example.xml");
        portalFiles.add("src/test/resources/collection/portal-example-simple.xml");
        portalFiles.add("src/test/resources/collection/portal-example-full.xml");   
        portalFiles.add("src/test/resources/collection/portal-example-seriesId.xml");
        portalFiles.add("src/test/resources/collection/portal-example-sasap.xml");
        portalFiles.add("src/test/resources/collection/portal-example-multiple-pids.xml");
        portalFiles.add("src/test/resources/collection/portal-example-only-pids.xml");
        portalFiles.add("src/test/resources/collection/portal-example-multiple-fields-values.xml");
        // The following 5 examples are from https://github.com/DataONEorg/collections-portals-schemas/tree/dev-1.1/docs/examples
        portalFiles.add("src/test/resources/collection/example-portal-A.xml");
        portalFiles.add("src/test/resources/collection/example-portal-B.xml");
        portalFiles.add("src/test/resources/collection/example-portal-C.xml");
        portalFiles.add("src/test/resources/collection/example-portal-D.xml");
        portalFiles.add("src/test/resources/collection/example-portal-E.xml");
        portalFiles.add("src/test/resources/collection/portal-example-multiple-fields-or-operator.xml");
        
        // The resulting 'collectionQuery' field will be compared to known values
        ArrayList<String> collectionQueryResultFiles = new ArrayList<>();
        collectionQueryResultFiles.add("src/test/resources/collection/collectionQuery-result-portal-1.1.0.txt");
        collectionQueryResultFiles.add("src/test/resources/collection/collectionQuery-result-example-filterGroup.txt");
        collectionQueryResultFiles.add("src/test/resources/collection/collectionQuery-result-example2-filterGroup.txt");
        collectionQueryResultFiles.add("src/test/resources/collection/collectionQuery-result-example-filterGroup-operator.txt");
        collectionQueryResultFiles.add("src/test/resources/collection/collectionQuery-result-example-fieldsOperator.txt");
        collectionQueryResultFiles.add("src/test/resources/collection/collectionQuery-result-portal-1.1.0-example-negation-only.txt");
        collectionQueryResultFiles.add("src/test/resources/collection/collectionQuery-result-example.txt");
        collectionQueryResultFiles.add("src/test/resources/collection/collectionQuery-result-example-simple.txt");
        collectionQueryResultFiles.add("src/test/resources/collection/collectionQuery-result-example-full.txt");
        collectionQueryResultFiles.add("src/test/resources/collection/collectionQuery-result-example-seriesId.txt");
        collectionQueryResultFiles.add("src/test/resources/collection/collectionQuery-result-example-sasap.txt");
        collectionQueryResultFiles.add("src/test/resources/collection/collectionQuery-result-example-multiple-pids.txt");
        collectionQueryResultFiles.add("src/test/resources/collection/collectionQuery-result-example-only-pids.txt");
        collectionQueryResultFiles.add("src/test/resources/collection/collectionQuery-result-example-multiple-fields-values.txt");
        collectionQueryResultFiles.add("src/test/resources/collection/collectionQuery-result-example-portal-A.txt");
        collectionQueryResultFiles.add("src/test/resources/collection/collectionQuery-result-example-portal-B.txt");
        collectionQueryResultFiles.add("src/test/resources/collection/collectionQuery-result-example-portal-C.txt");
        collectionQueryResultFiles.add("src/test/resources/collection/collectionQuery-result-example-portal-D.txt");
        collectionQueryResultFiles.add("src/test/resources/collection/collectionQuery-result-example-portal-E.txt");
        collectionQueryResultFiles.add("src/test/resources/collection/collectionQuery-result-example-multiple-fields-or-operator.txt");

        // Also test that the title is properly added and retrievable
        ArrayList<String> portalNames = new ArrayList<>();
        portalNames.add("portal-1.1.0-example");
        portalNames.add("filterGroup-example");
        portalNames.add("filterGroup-example2");
        portalNames.add("filterGroup-operator-example");
        portalNames.add("fieldsOperator-example");
        portalNames.add("portal-1.1.0-negation-only");
        portalNames.add("My saved search");
        portalNames.add("My Soil Portal");
        portalNames.add("My Portal");
        portalNames.add("Another test portal");
        portalNames.add("Lauren's test project - updated");
        portalNames.add("Multiple pids");
        portalNames.add("Only pids");
        portalNames.add("Multiple fields, multiple values");
        portalNames.add("Exclude a filter group with one rule");
        portalNames.add("Exclude a filter group with a negative filter");
        portalNames.add("ID filters with non-ID filters");
        portalNames.add("Exclude ID filters and non-ID filters");
        portalNames.add("High complexity query");
        portalNames.add("multiple fields using default fieldsOperator; negation, or operator test");

        for(int i=0; i < portalFiles.size(); i++) {
            String collectionQuery = null;
            InputStream is = getPortalDoc(portalFiles.get(i));
            System.out.println("Processing document: " + portalFiles.get(i));
            List<SolrElementField> sysSolrFields = new ArrayList<>();
            SolrDoc indexDocument = new SolrDoc(sysSolrFields);
            Map<String, SolrDoc> docs = new HashMap<>();
            docs.put(id, indexDocument);

            // Process the document, creating a Solr document from the input XML document
            //IDocumentSubprocessor processor = getPortalSubprocessor();

            String queryResult = null;
            try {
                // Read in the query string that the processor should create. This is read in
                // from disk so that we don't have to bother with special character escaping.
                File file = new File(collectionQueryResultFiles.get(i));
                collectionQuery = FileUtils.readFileToString(file).trim();
                docs = processor.processDocument(id, docs, is);
                // Extract the processed document we just created
                SolrDoc myDoc = docs.get("urn:uuid:349aa330-4645-4dab-a02d-3bf950cf708i");
                // Extract fields and check the values
                String title = myDoc.getField("title").getValue();
                String queryStr = myDoc.getField("collectionQuery").getValue();
                queryStr = queryStr.trim();

                System.out.println("correct collectionQuery: " + collectionQuery);
                System.out.println("actual collectionQuery: " + queryStr);

                // Did the index sub processor correctly extract the 'title' field from the portal document?
                //assertTrue("The portalSubprocessor correctly build the document with the correct value in the title field.", title.equalsIgnoreCase(portalNames.get(i)));
                // Did the index sub processor correctly extract the 'collectionQuery' field from the portal document?
                assertTrue("The portalSubprocessor correctly built the document with the correct value in the \"collectionQuery\" field.", queryStr.equalsIgnoreCase(collectionQuery));
            } catch (Exception e) {
                System.out.println("Error: " + e.getMessage());
            }
        }
    }

    /**
     * Get the document to test with the portal processor
     * @param filename the filename of the document to test
     * @return the document to test with the portal subprocessor
     * @throws Exception
     */
    private InputStream getPortalDoc(String filename) throws Exception{
        File file = new File(filename);
        InputStream is = new FileInputStream(file);
        return is;
    }

    /**
      * For each test, set up the Solr service and test data
      *
      * @throws Exception
    */
    @Before
    public void setUp() throws Exception {

        if (context == null) {
            context = new ClassPathXmlApplicationContext("org/dataone/cn/index/test-context.xml");
        }
        processor = (ScienceMetadataDocumentSubprocessor) context.getBean("portalsSubprocessor");
            
        List<String> matchDocuments = processor.getMatchDocuments();
        for (String doc : matchDocuments) {
            System.out.println("processor can process: " + doc);
        }
    }
}
