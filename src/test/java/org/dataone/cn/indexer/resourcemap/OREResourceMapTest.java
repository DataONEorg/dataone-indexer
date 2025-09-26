package org.dataone.cn.indexer.resourcemap;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathExpressionException;

import org.apache.solr.common.SolrDocument;
import org.dataone.cn.index.DataONESolrJettyTestBase;
import org.dataone.cn.indexer.solrhttp.SolrElementField;
import org.dataone.service.types.v1.Identifier;
import org.dspace.foresite.OREException;
import org.dspace.foresite.OREParserException;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.core.io.Resource;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import com.carrotsearch.randomizedtesting.annotations.ThreadLeakScope;

@ThreadLeakScope(ThreadLeakScope.Scope.NONE)
//@RunWith(SpringJUnit4ClassRunner.class)
//@ContextConfiguration(locations = { "test-context.xml" })
public class OREResourceMapTest extends DataONESolrJettyTestBase{

    //@Autowired
    private Resource testDoc;

    //@Autowired
    private Resource incompleteResourceMap;

    //@Autowired
    private Resource dryadDoc;

    //@Autowired
    private Resource transitiveRelationshipsDoc;

    //@Autowired
    private Resource incompleteTransitiveRelationshipsDoc;

    //@Autowired
    private Resource missingComponentsResourcemap;

    //@Autowired
    private Resource peggym1321Sci;

    //@Autowired
    private Resource peggy1331Resourcemap;

    //@Autowired
    private Resource peggym1331Sci;

    //@Autowired
    private Resource data11;

    //@Autowired
    private Resource foo1271;

    //@Autowired
    private Resource missingComponentsResourcemap2;

    //@Autowired
    private Resource peggym1341Sci;

    public static final int WAIT_TIME_MILLI = 500;
    public static final int MAX_ATTEMPTS = 100;

    /**
     * Test to index a resourcemap object which has a component that will never be indexed by
     * another task. But the component object is in hashstore.
     */
    @Test
    public void testResourcemapWithUnindexedComponents() throws Exception {
        String missingDataId = "foo.127.1";
        String metadataId = "peggym.132.1";
        String resourcemapId = "missing.component.resourcemap";
        // Load the resource of foo1271 into the hash store with the given id
        loadToHashStore(missingDataId, foo1271);
        // Index the science metadata object
        indexObjectToSolr(metadataId, peggym1321Sci);
        SolrDocument data = null;
        boolean success = false;
        int count = 0;
        while (!success) {
            try {
                data = assertPresentInSolrIndex(metadataId);
                success = true;
            } catch (AssertionError e) {
                if (count < MAX_ATTEMPTS) {
                    Thread.sleep(WAIT_TIME_MILLI);
                } else {
                    throw e;
                }
            }
            count++;
        }
        Assert.assertEquals(1, ((List) data.getFieldValues(
            SolrElementField.FIELD_SIZE)).size());
        Assert.assertNull(data.getFieldValues(SolrElementField.FIELD_RESOURCEMAP));
        Assert.assertNull(data.getFieldValues(SolrElementField.FIELD_DOCUMENTS));
        Assert.assertNull(data.getFieldValues(SolrElementField.FIELD_ISDOCUMENTEDBY));

        try {
            data = assertPresentInSolrIndex(missingDataId);
            fail("Test can't reach here since the data object shouldn't be indexed now.");
        } catch (AssertionError e) {
        }

        //Index the resource map object
        indexObjectToSolr(resourcemapId, missingComponentsResourcemap);
        success = false;
        count = 0;
        while (!success) {
            try {
                data = assertPresentInSolrIndex(resourcemapId);
                success = true;
            } catch (AssertionError e) {
                if (count < MAX_ATTEMPTS) {
                    Thread.sleep(WAIT_TIME_MILLI);
                } else {
                    throw e;
                }
            }
            count++;
        }
        Assert.assertEquals(1, ((List) data.getFieldValues(
            SolrElementField.FIELD_SIZE)).size());

        // The missing data object should have a bare solr doc as well
        success = false;
        count = 0;
        while (!success) {
            try {
                data = assertPresentInSolrIndex(missingDataId);
                success = true;
            } catch (AssertionError e) {
                if (count < MAX_ATTEMPTS) {
                    Thread.sleep(WAIT_TIME_MILLI);
                } else {
                    throw e;
                }
            }
            count++;
        }
        Assert.assertNull(data.getFieldValues(SolrElementField.FIELD_SIZE));
        Assert.assertEquals(resourcemapId,
                            ((List) data.getFieldValues(SolrElementField.FIELD_RESOURCEMAP)).get(
                                0));
        Assert.assertEquals(metadataId, ((List) data.getFieldValues(
            SolrElementField.FIELD_ISDOCUMENTEDBY)).get(0));
        Assert.assertNull(data.getFieldValues(SolrElementField.FIELD_DOCUMENTS));

        // Check the metadata again and it should have the resourcemap and obsoletes fields
        data = assertPresentInSolrIndex(metadataId);
        Assert.assertEquals(resourcemapId, ((List) data.getFieldValues(
            SolrElementField.FIELD_RESOURCEMAP)).get(0));
        Assert.assertEquals(missingDataId, ((List) data.getFieldValues(
            SolrElementField.FIELD_DOCUMENTS)).get(0));
        Assert.assertNull(data.getFieldValues(SolrElementField.FIELD_ISDOCUMENTEDBY));
        Assert.assertEquals(1, ((List) data.getFieldValues(
            SolrElementField.FIELD_SIZE)).size());
    }

    /**
     * Test to index a resourcemap object which has a component which doesn't exist in the hashstore
     */
    @Test
    public void testResourcemapWithNonExistingComponents() throws Exception {
        String missingDataId = "foo.128.1";
        String metadataId = "peggym.134.1";
        String resourcemapId = "missing.component.resourcemap2";
        // Index the science metadata object
        indexObjectToSolr(metadataId, peggym1341Sci);
        SolrDocument data = null;
        boolean success = false;
        int count = 0;
        while (!success) {
            try {
                data = assertPresentInSolrIndex(metadataId);
                success = true;
            } catch (AssertionError e) {
                if (count < MAX_ATTEMPTS) {
                    Thread.sleep(WAIT_TIME_MILLI);
                } else {
                    throw e;
                }
            }
            count++;
        }
        Assert.assertEquals(1, ((List) data.getFieldValues(
            SolrElementField.FIELD_SIZE)).size());
        Assert.assertNull(data.getFieldValues(SolrElementField.FIELD_RESOURCEMAP));
        Assert.assertNull(data.getFieldValues(SolrElementField.FIELD_DOCUMENTS));
        Assert.assertNull(data.getFieldValues(SolrElementField.FIELD_ISDOCUMENTEDBY));

        try {
            data = assertPresentInSolrIndex(missingDataId);
            fail("Test can't reach here since the data object shouldn't be indexed now.");
        } catch (AssertionError e) {
        }

        //Index the resource map object
        indexObjectToSolr(resourcemapId, missingComponentsResourcemap2);
        success = false;
        count = 0;
        while (!success) {
            try {
                data = assertPresentInSolrIndex(resourcemapId);
                success = true;
            } catch (AssertionError e) {
                if (count < MAX_ATTEMPTS) {
                    Thread.sleep(WAIT_TIME_MILLI);
                } else {
                    throw e;
                }
            }
            count++;
        }
        Assert.assertEquals(1, ((List) data.getFieldValues(
            SolrElementField.FIELD_SIZE)).size());

        // The missing data object should have a bare solr doc as well
        success = false;
        count = 0;
        while (!success) {
            try {
                data = assertPresentInSolrIndex(missingDataId);
                success = true;
            } catch (AssertionError e) {
                if (count < MAX_ATTEMPTS) {
                    Thread.sleep(WAIT_TIME_MILLI);
                } else {
                    throw e;
                }
            }
            count++;
        }
        Assert.assertNull(data.getFieldValues(SolrElementField.FIELD_SIZE));
        Assert.assertEquals("A placeholding document",
                            ((List)data.getFieldValues("abstract")).get(0));
        Assert.assertEquals(resourcemapId,
                            ((List) data.getFieldValues(SolrElementField.FIELD_RESOURCEMAP)).get(
                                0));
        Assert.assertEquals(metadataId, ((List) data.getFieldValues(
            SolrElementField.FIELD_ISDOCUMENTEDBY)).get(0));
        Assert.assertNull(data.getFieldValues(SolrElementField.FIELD_DOCUMENTS));
        Assert.assertEquals("dataone_integration_test_user",
                            ((List) data.getFieldValues(SolrElementField.FIELD_RIGHTSHOLDER)).get(
                                0));
        Assert.assertEquals("dataone_public_user",
                            ((List) data.getFieldValues(SolrElementField.FIELD_READPERMISSION)).get(
                                0));
        Assert.assertEquals("dataone_integration_test_user", ((List) data.getFieldValues(
            SolrElementField.FIELD_WRITEPERMISSION)).get(0));
        Assert.assertEquals("dataone_integration_test_user2", ((List) data.getFieldValues(
            SolrElementField.FIELD_WRITEPERMISSION)).get(1));
        Assert.assertNull(data.getFieldValues(SolrElementField.FIELD_CHANGEPERMISSION));

        // Check the metadata again and it should have the resourcemap and obsoletes fields
        data = assertPresentInSolrIndex(metadataId);
        Assert.assertEquals(resourcemapId, ((List) data.getFieldValues(
            SolrElementField.FIELD_RESOURCEMAP)).get(0));
        Assert.assertEquals(missingDataId, ((List) data.getFieldValues(
            SolrElementField.FIELD_DOCUMENTS)).get(0));
        Assert.assertNull(data.getFieldValues(SolrElementField.FIELD_ISDOCUMENTEDBY));
        Assert.assertEquals(1, ((List) data.getFieldValues(
            SolrElementField.FIELD_SIZE)).size());
    }

    /**
     * Test to index a resourcemap object, which has everything ready
     */
    @Test
    public void testResourcemap() throws Exception {
        String metadataId = "peggym.133.1";
        String resourcemapId = "peggym.133.1.resourcemap";
        String dataId = "data.1.1";
        // Index the science metadata object
        indexObjectToSolr(metadataId, peggym1331Sci);
        SolrDocument data = null;
        boolean success = false;
        int count = 0;
        while (!success) {
            try {
                data = assertPresentInSolrIndex(metadataId);
                success = true;
            } catch (AssertionError e) {
                if (count < MAX_ATTEMPTS) {
                    Thread.sleep(WAIT_TIME_MILLI);
                } else {
                    throw e;
                }
            }
            count++;
        }
        Assert.assertEquals(1, ((List) data.getFieldValues(SolrElementField.FIELD_SIZE)).size());
        Assert.assertNull(data.getFieldValues(SolrElementField.FIELD_RESOURCEMAP));
        Assert.assertNull(data.getFieldValues(SolrElementField.FIELD_DOCUMENTS));
        Assert.assertNull(data.getFieldValues(SolrElementField.FIELD_ISDOCUMENTEDBY));

        // Index the data object
        indexObjectToSolr(dataId, data11);
        success = false;
        count = 0;
        while (!success) {
            try {
                data = assertPresentInSolrIndex(dataId);
                success = true;
            } catch (AssertionError e) {
                if (count < MAX_ATTEMPTS) {
                    Thread.sleep(WAIT_TIME_MILLI);
                } else {
                    throw e;
                }
            }
            count++;
        }
        Assert.assertEquals(1, ((List) data.getFieldValues(SolrElementField.FIELD_SIZE)).size());
        Assert.assertNull(data.getFieldValues(SolrElementField.FIELD_RESOURCEMAP));
        Assert.assertNull(data.getFieldValues(SolrElementField.FIELD_DOCUMENTS));
        Assert.assertNull(data.getFieldValues(SolrElementField.FIELD_ISDOCUMENTEDBY));

        //Index the resource map object
        indexObjectToSolr(resourcemapId, peggy1331Resourcemap);
        success = false;
        count = 0;
        while (!success) {
            try {
                data = assertPresentInSolrIndex(resourcemapId);
                success = true;
            } catch (AssertionError e) {
                if (count < MAX_ATTEMPTS) {
                    Thread.sleep(WAIT_TIME_MILLI);
                } else {
                    throw e;
                }
            }
            count++;
        }
        Assert.assertEquals(1, ((List) data.getFieldValues(SolrElementField.FIELD_SIZE)).size());

        // Check the data again and it should have the resourcemap and isDocumentedBy fields
        data = assertPresentInSolrIndex(dataId);
        Assert.assertNull(data.getFieldValues(SolrElementField.FIELD_DOCUMENTS));
        Assert.assertEquals(resourcemapId, ((List) data.getFieldValues(
            SolrElementField.FIELD_RESOURCEMAP)).get(0));
        Assert.assertEquals(metadataId, ((List) data.getFieldValues(
            SolrElementField.FIELD_ISDOCUMENTEDBY)).get(0));
        Assert.assertEquals(1, ((List) data.getFieldValues(SolrElementField.FIELD_SIZE)).size());

        // Check the metadata again and it should have the resourcemap and documents fields
        data = assertPresentInSolrIndex(metadataId);
        Assert.assertEquals(resourcemapId, ((List) data.getFieldValues(
            SolrElementField.FIELD_RESOURCEMAP)).get(0));
        Assert.assertEquals(dataId, ((List) data.getFieldValues(
            SolrElementField.FIELD_DOCUMENTS)).get(0));
        Assert.assertNull(data.getFieldValues(SolrElementField.FIELD_ISDOCUMENTEDBY));
        Assert.assertEquals(1, ((List) data.getFieldValues(SolrElementField.FIELD_SIZE)).size());
    }

    /**
     * Tests the foresite based resource map with transitive resource maps.
     * 
     * @throws OREException
     * @throws URISyntaxException
     * @throws OREParserException
     * @throws IOException
     */
    @Test
    public void testTransitiveRelationships() throws OREException, URISyntaxException,
            OREParserException, IOException {
        /* Resource map with all resources visible */
        ResourceMap resourceMap = new ForesiteResourceMap(transitiveRelationshipsDoc.getFile()
                .getAbsolutePath(), new IndexVisibilityDelegateTestImpl());

        List<String> docs = resourceMap.getAllDocumentIDs();

        Assert.assertEquals("Number of documents should be 5", 5, docs.size());

        Set<ResourceEntry> resources = resourceMap.getMappedReferences();

        Assert.assertEquals("Number of mapped references should be 4", 4, resources.size());

        for (ResourceEntry resource : resources) {

            if (resource.getIdentifier().equals("resource1")) {
                Set<String> documentedBy = resource.getDocumentedBy();

                Assert.assertEquals("Wrong number of documentedBy for " + resource.getIdentifier(),
                        0, documentedBy.size());

                Set<String> documents = resource.getDocuments();

                Assert.assertEquals("Wrong number of documents for " + resource.getIdentifier(), 1,
                        documents.size());

                Assert.assertTrue("Resource1 does not document resource 2",
                        documents.contains("resource2"));

            } else if (resource.getIdentifier().equals("resource2")) {
                Set<String> documentedBy = resource.getDocumentedBy();

                Assert.assertEquals("Wrong number of documentedBy for " + resource.getIdentifier(),
                        1, documentedBy.size());

                Assert.assertTrue("Resource2 isn't documented by resource1",
                        documentedBy.contains("resource1"));

                Set<String> documents = resource.getDocuments();

                Assert.assertEquals("Wrong number of documents for " + resource.getIdentifier(), 1,
                        documents.size());

                Assert.assertTrue("Resource2 does not document resource 3",
                        documents.contains("resource3"));

            } else if (resource.getIdentifier().equals("resource3")) {
                Set<String> documentedBy = resource.getDocumentedBy();

                Assert.assertEquals("Wrong number of documentedBy for " + resource.getIdentifier(),
                        1, documentedBy.size());

                Assert.assertTrue("Resource3 isn't documented by resource2",
                        documentedBy.contains("resource2"));

                Set<String> documents = resource.getDocuments();

                Assert.assertEquals("Wrong number of documents for " + resource.getIdentifier(), 1,
                        documents.size());

                Assert.assertTrue("Resource3 does not document resource 4",
                        documents.contains("resource4"));

            } else if (resource.getIdentifier().equals("resource4")) {
                Set<String> documentedBy = resource.getDocumentedBy();

                Assert.assertEquals("Wrong number of documentedBy for " + resource.getIdentifier(),
                        1, documentedBy.size());

                Assert.assertTrue("Resource4 isn't documented by resource3",
                        documentedBy.contains("resource3"));

                Set<String> documents = resource.getDocuments();

                Assert.assertEquals("Wrong number of documents for " + resource.getIdentifier(), 0,
                        documents.size());
            }
        }
    }

    /**
     * Tests the reasoner for the foresite resource map with incomplete
     * transitive documents/documentedBy relationships.
     * 
     * @throws OREException
     * @throws URISyntaxException
     * @throws OREParserException
     * @throws IOException
     */
    @Test
    public void testIncompleteTransitiveRelationships() throws OREException, URISyntaxException,
            OREParserException, IOException {

        /* Resource map with all resources visible */
        ResourceMap resourceMap = new ForesiteResourceMap(incompleteTransitiveRelationshipsDoc
                .getFile().getAbsolutePath(), new IndexVisibilityDelegateTestImpl());

        List<String> docs = resourceMap.getAllDocumentIDs();

        Assert.assertEquals("Number of documents should be 5", 5, docs.size());

        Set<ResourceEntry> resources = resourceMap.getMappedReferences();

        Assert.assertEquals("Number of mapped references should be 4", 4, resources.size());

        for (ResourceEntry resource : resources) {

            if (resource.getIdentifier().equals("resource1")) {
                Set<String> documentedBy = resource.getDocumentedBy();

                Assert.assertEquals("Wrong number of documentedBy for " + resource.getIdentifier(),
                        0, documentedBy.size());

                Set<String> documents = resource.getDocuments();

                Assert.assertEquals("Wrong number of documents for " + resource.getIdentifier(), 1,
                        documents.size());

                Assert.assertTrue("Resource1 does not document resource 2",
                        documents.contains("resource2"));

            } else if (resource.getIdentifier().equals("resource2")) {
                Set<String> documentedBy = resource.getDocumentedBy();

                Assert.assertEquals("Wrong number of documentedBy for " + resource.getIdentifier(),
                        1, documentedBy.size());

                Assert.assertTrue("Resource2 isn't documented by resource1",
                        documentedBy.contains("resource1"));

                Set<String> documents = resource.getDocuments();

                Assert.assertEquals("Wrong number of documents for " + resource.getIdentifier(), 1,
                        documents.size());

                Assert.assertTrue("Resource2 does not document resource 3",
                        documents.contains("resource3"));

            } else if (resource.getIdentifier().equals("resource3")) {
                Set<String> documentedBy = resource.getDocumentedBy();

                Assert.assertEquals("Wrong number of documentedBy for " + resource.getIdentifier(),
                        1, documentedBy.size());

                Assert.assertTrue("Resource3 isn't documented by resource2",
                        documentedBy.contains("resource2"));

                Set<String> documents = resource.getDocuments();

                Assert.assertEquals("Wrong number of documents for " + resource.getIdentifier(), 1,
                        documents.size());

                Assert.assertTrue("Resource3 does not document resource 4",
                        documents.contains("resource4"));

            } else if (resource.getIdentifier().equals("resource4")) {
                Set<String> documentedBy = resource.getDocumentedBy();

                Assert.assertEquals("Wrong number of documentedBy for " + resource.getIdentifier(),
                        1, documentedBy.size());

                Assert.assertTrue("Resource4 isn't documented by resource3",
                        documentedBy.contains("resource3"));

                Set<String> documents = resource.getDocuments();

                Assert.assertEquals("Wrong number of documents for " + resource.getIdentifier(), 0,
                        documents.size());
            }
        }
    }

    /**
     * Tests foresite based resource map on a dryad doc.
     * 
     * @throws OREException
     * @throws URISyntaxException
     * @throws OREParserException
     * @throws IOException
     */
    @Test
    public void testDryadDoc() throws OREException, URISyntaxException, OREParserException,
            IOException {
        /* Resource map with all resources visible */
        ResourceMap resourceMap = ResourceMapFactory.buildResourceMap(dryadDoc.getFile()
                .getAbsolutePath(), new IndexVisibilityDelegateTestImpl());

        /* Tests the identifer */
        Assert.assertEquals(
                "http://dx.doi.org/10.5061/dryad.12?format=d1rem&ver=2011-08-02T16:00:05.530-0400",
                resourceMap.getIdentifier());

        /* Tests the getAllDocumentIDs() method */
        Assert.assertEquals("Number of doc ids don't match", 14, resourceMap.getAllDocumentIDs()
                .size());

        /* Tests the getMappedReferences() method */
        Set<ResourceEntry> resources = resourceMap.getMappedReferences();

        Assert.assertEquals("Number of mapped references don't match", 13, resources.size());

        /* Checks the documents and documentedby for each resource */
        for (ResourceEntry resource : resources) {

            String resourceID = resource.getIdentifier();

            if (resourceID
                    .equals("http://dx.doi.org/10.5061/dryad.12&ver=2011-08-02T16:00:05.530-0400")) {
                Set<String> documentedBy = resource.getDocumentedBy();
                Assert.assertEquals("Wrong number of documentedBy for " + resourceID, 0,
                        documentedBy.size());

                Set<String> documents = resource.getDocuments();
                Assert.assertEquals("Wrong number of documents for " + resourceID, 12,
                        documents.size());

                for (String docID : new String[] {
                        "http://dx.doi.org/10.5061/dryad.12/6?ver=2011-08-02T16:00:05.530-0400",
                        "http://dx.doi.org/10.5061/dryad.12/4/bitstream",
                        "http://dx.doi.org/10.5061/dryad.12/6/bitstream",
                        "http://dx.doi.org/10.5061/dryad.12/2?ver=2011-08-02T16:00:05.530-0400",
                        "http://dx.doi.org/10.5061/dryad.12/1?ver=2011-08-02T16:00:05.530-0400",
                        "http://dx.doi.org/10.5061/dryad.12/4?ver=2011-08-02T16:00:05.530-0400",
                        "http://dx.doi.org/10.5061/dryad.12/5?ver=2011-08-02T16:00:05.530-0400",
                        "http://dx.doi.org/10.5061/dryad.12/3?ver=2011-08-02T16:00:05.530-0400",
                        "http://dx.doi.org/10.5061/dryad.12/3/bitstream",
                        "http://dx.doi.org/10.5061/dryad.12/1/bitstream",
                        "http://dx.doi.org/10.5061/dryad.12/5/bitstream",
                        "http://dx.doi.org/10.5061/dryad.12/2/bitstream" }) {
                    Assert.assertTrue(resourceID + " doesn't document " + docID,
                            documents.contains(docID));
                }

            } else if (resourceID.equals("http://dx.doi.org/10.5061/dryad.12/3/bitstream")) {
                Set<String> documentedBy = resource.getDocumentedBy();
                Assert.assertEquals("Wrong number of documentedBy for " + resourceID, 1,
                        documentedBy.size());

                Assert.assertTrue(
                        resourceID
                                + "is not doucmented by http://dx.doi.org/10.5061/dryad.12&ver=2011-08-02T16:00:05.530-0400",
                        documentedBy
                                .contains("http://dx.doi.org/10.5061/dryad.12&ver=2011-08-02T16:00:05.530-0400"));

                Set<String> documents = resource.getDocuments();
                Assert.assertEquals("Wrong number of documents for " + resourceID, 0,
                        documents.size());

            } else if (resourceID
                    .equals("http://dx.doi.org/10.5061/dryad.12/1?ver=2011-08-02T16:00:05.530-0400")) {
                Set<String> documentedBy = resource.getDocumentedBy();
                Assert.assertEquals("Wrong number of documentedBy for " + resourceID, 1,
                        documentedBy.size());

                Assert.assertTrue(
                        resourceID
                                + "is not doucmented by http://dx.doi.org/10.5061/dryad.12&ver=2011-08-02T16:00:05.530-0400",
                        documentedBy
                                .contains("http://dx.doi.org/10.5061/dryad.12&ver=2011-08-02T16:00:05.530-0400"));

                Set<String> documents = resource.getDocuments();
                Assert.assertEquals("Wrong number of documents for " + resourceID, 0,
                        documents.size());

            } else if (resourceID
                    .equals("http://dx.doi.org/10.5061/dryad.12/2?ver=2011-08-02T16:00:05.530-0400")) {
                Set<String> documentedBy = resource.getDocumentedBy();
                Assert.assertEquals("Wrong number of documentedBy for " + resourceID, 1,
                        documentedBy.size());

                Assert.assertTrue(
                        resourceID
                                + "is not doucmented by http://dx.doi.org/10.5061/dryad.12&ver=2011-08-02T16:00:05.530-0400",
                        documentedBy
                                .contains("http://dx.doi.org/10.5061/dryad.12&ver=2011-08-02T16:00:05.530-0400"));

                Set<String> documents = resource.getDocuments();
                Assert.assertEquals("Wrong number of documents for " + resourceID, 0,
                        documents.size());

            } else if (resourceID
                    .equals("http://dx.doi.org/10.5061/dryad.12/5?ver=2011-08-02T16:00:05.530-0400")) {
                Set<String> documentedBy = resource.getDocumentedBy();
                Assert.assertEquals("Wrong number of documentedBy for " + resourceID, 1,
                        documentedBy.size());

                Assert.assertTrue(
                        resourceID
                                + "is not doucmented by http://dx.doi.org/10.5061/dryad.12&ver=2011-08-02T16:00:05.530-0400",
                        documentedBy
                                .contains("http://dx.doi.org/10.5061/dryad.12&ver=2011-08-02T16:00:05.530-0400"));

                Set<String> documents = resource.getDocuments();
                Assert.assertEquals("Wrong number of documents for " + resourceID, 0,
                        documents.size());

            } else if (resourceID.equals("http://dx.doi.org/10.5061/dryad.12/1/bitstream")) {
                Set<String> documentedBy = resource.getDocumentedBy();
                Assert.assertEquals("Wrong number of documentedBy for " + resourceID, 1,
                        documentedBy.size());

                Assert.assertTrue(
                        resourceID
                                + "is not doucmented by http://dx.doi.org/10.5061/dryad.12&ver=2011-08-02T16:00:05.530-0400",
                        documentedBy
                                .contains("http://dx.doi.org/10.5061/dryad.12&ver=2011-08-02T16:00:05.530-0400"));

                Set<String> documents = resource.getDocuments();
                Assert.assertEquals("Wrong number of documents for " + resourceID, 0,
                        documents.size());

            } else if (resourceID.equals("http://dx.doi.org/10.5061/dryad.12/4/bitstream")) {
                Set<String> documentedBy = resource.getDocumentedBy();
                Assert.assertEquals("Wrong number of documentedBy for " + resourceID, 1,
                        documentedBy.size());

                Assert.assertTrue(
                        resourceID
                                + "is not doucmented by http://dx.doi.org/10.5061/dryad.12&ver=2011-08-02T16:00:05.530-0400",
                        documentedBy
                                .contains("http://dx.doi.org/10.5061/dryad.12&ver=2011-08-02T16:00:05.530-0400"));

                Set<String> documents = resource.getDocuments();
                Assert.assertEquals("Wrong number of documents for " + resourceID, 0,
                        documents.size());

            } else if (resourceID
                    .equals("http://dx.doi.org/10.5061/dryad.12/6?ver=2011-08-02T16:00:05.530-0400")) {
                Set<String> documentedBy = resource.getDocumentedBy();
                Assert.assertEquals("Wrong number of documentedBy for " + resourceID, 1,
                        documentedBy.size());

                Assert.assertTrue(
                        resourceID
                                + "is not doucmented by http://dx.doi.org/10.5061/dryad.12&ver=2011-08-02T16:00:05.530-0400",
                        documentedBy
                                .contains("http://dx.doi.org/10.5061/dryad.12&ver=2011-08-02T16:00:05.530-0400"));

                Set<String> documents = resource.getDocuments();
                Assert.assertEquals("Wrong number of documents for " + resourceID, 0,
                        documents.size());

            } else if (resourceID.equals("http://dx.doi.org/10.5061/dryad.12/5/bitstream")) {
                Set<String> documentedBy = resource.getDocumentedBy();
                Assert.assertEquals("Wrong number of documentedBy for " + resourceID, 1,
                        documentedBy.size());

                Assert.assertTrue(
                        resourceID
                                + "is not doucmented by http://dx.doi.org/10.5061/dryad.12&ver=2011-08-02T16:00:05.530-0400",
                        documentedBy
                                .contains("http://dx.doi.org/10.5061/dryad.12&ver=2011-08-02T16:00:05.530-0400"));

                Set<String> documents = resource.getDocuments();
                Assert.assertEquals("Wrong number of documents for " + resourceID, 0,
                        documents.size());

            } else if (resourceID.equals("http://dx.doi.org/10.5061/dryad.12/6/bitstream")) {
                Set<String> documentedBy = resource.getDocumentedBy();
                Assert.assertEquals("Wrong number of documentedBy for " + resourceID, 1,
                        documentedBy.size());

                Assert.assertTrue(
                        resourceID
                                + "is not doucmented by http://dx.doi.org/10.5061/dryad.12&ver=2011-08-02T16:00:05.530-0400",
                        documentedBy
                                .contains("http://dx.doi.org/10.5061/dryad.12&ver=2011-08-02T16:00:05.530-0400"));

                Set<String> documents = resource.getDocuments();
                Assert.assertEquals("Wrong number of documents for " + resourceID, 0,
                        documents.size());

            } else if (resourceID
                    .equals("http://dx.doi.org/10.5061/dryad.12/4?ver=2011-08-02T16:00:05.530-0400")) {
                Set<String> documentedBy = resource.getDocumentedBy();
                Assert.assertEquals("Wrong number of documentedBy for " + resourceID, 1,
                        documentedBy.size());

                Assert.assertTrue(
                        resourceID
                                + "is not doucmented by http://dx.doi.org/10.5061/dryad.12&ver=2011-08-02T16:00:05.530-0400",
                        documentedBy
                                .contains("http://dx.doi.org/10.5061/dryad.12&ver=2011-08-02T16:00:05.530-0400"));

                Set<String> documents = resource.getDocuments();
                Assert.assertEquals("Wrong number of documents for " + resourceID, 0,
                        documents.size());

            } else if (resourceID.equals("http://dx.doi.org/10.5061/dryad.12/2/bitstream")) {
                Set<String> documentedBy = resource.getDocumentedBy();
                Assert.assertEquals("Wrong number of documentedBy for " + resourceID, 1,
                        documentedBy.size());

                Assert.assertTrue(
                        resourceID
                                + "is not doucmented by http://dx.doi.org/10.5061/dryad.12&ver=2011-08-02T16:00:05.530-0400",
                        documentedBy
                                .contains("http://dx.doi.org/10.5061/dryad.12&ver=2011-08-02T16:00:05.530-0400"));

                Set<String> documents = resource.getDocuments();
                Assert.assertEquals("Wrong number of documents for " + resourceID, 0,
                        documents.size());

            } else if (resourceID
                    .equals("http://dx.doi.org/10.5061/dryad.12/3?ver=2011-08-02T16:00:05.530-0400")) {
                Set<String> documentedBy = resource.getDocumentedBy();
                Assert.assertEquals("Wrong number of documentedBy for " + resourceID, 1,
                        documentedBy.size());

                Assert.assertTrue(
                        resourceID
                                + "is not doucmented by http://dx.doi.org/10.5061/dryad.12&ver=2011-08-02T16:00:05.530-0400",
                        documentedBy
                                .contains("http://dx.doi.org/10.5061/dryad.12&ver=2011-08-02T16:00:05.530-0400"));

                Set<String> documents = resource.getDocuments();
                Assert.assertEquals("Wrong number of documents for " + resourceID, 0,
                        documents.size());
            }
        }
    }

    /**
     * Tests the foresite reasoner with a resource map that has incomplete 
     * documents/documentedBy relationships.
     * 
     * @throws OREException
     * @throws URISyntaxException
     * @throws OREParserException
     * @throws IOException
     */
    @Test
    public void testIncompleteResourceMap() throws OREException, URISyntaxException,
            OREParserException, IOException {
        /* Resource map with all resources visible */
        ResourceMap resourceMap = new ForesiteResourceMap(incompleteResourceMap.getFile()
                .getAbsolutePath(), new IndexVisibilityDelegateTestImpl());

        Set<ResourceEntry> resources = resourceMap.getMappedReferences();

        Assert.assertEquals("Number of mapped references don't match", 2, resources.size());

        for (ResourceEntry resource : resources) {

            if (resource.getIdentifier()
                    .equals("doi:10.6085/AA/ALEXXX_015MTBD009R00_20110122.50.1")) {
                Set<String> documentedBy = resource.getDocumentedBy();

                Assert.assertEquals("Wrong number of documentedBy", 0, documentedBy.size());

                Set<String> documents = resource.getDocuments();

                Assert.assertEquals(
                        "Wrong number of documents for doi:10.6085/AA/ALEXXX_015MTBD009R00_20110122.50.1",
                        1, documents.size());
                Assert.assertTrue(
                        "doi:10.6085/AA/ALEXXX_015MTBD009R00_20110122.50.1 should document doi:10.6085/AA/ALEXXX_015MTBD009R00_20110122.40.1",
                        documents.contains("doi:10.6085/AA/ALEXXX_015MTBD009R00_20110122.40.1"));

            } else if (resource.getIdentifier().equals(
                    "doi:10.6085/AA/ALEXXX_015MTBD009R00_20110122.40.1")) {
                Set<String> documentedBy = resource.getDocumentedBy();

                Assert.assertEquals("Wrong number of documentedBy", 1, documentedBy.size());
                Assert.assertTrue(
                        "doi:10.6085/AA/ALEXXX_015MTBD009R00_20110122.40.1 should be documented by doi:10.6085/AA/ALEXXX_015MTBD009R00_20110122.50.1",
                        documentedBy.contains("doi:10.6085/AA/ALEXXX_015MTBD009R00_20110122.50.1"));

                Set<String> documents = resource.getDocuments();

                Assert.assertEquals(
                        "Wrong number of documents doi:10.6085/AA/ALEXXX_015MTBD009R00_20110122.40.1",
                        0, documents.size());

            } else {
                Assert.fail("Unknown resource id: " + resource.getIdentifier());
            }
        }
    }

    /**
     * Tests the base case of foresite resource map parsing against the xpath
     * base resource map parser. 
     * 
     * @throws OREException
     * @throws URISyntaxException
     * @throws OREParserException
     * @throws IOException
     * @throws XPathExpressionException
     * @throws SAXException
     * @throws ParserConfigurationException
     */
    @Test
    public void testOREResourceMap() throws OREException, URISyntaxException, OREParserException,
            IOException, XPathExpressionException, SAXException, ParserConfigurationException {
        DocumentBuilderFactory domFactory = DocumentBuilderFactory.newInstance();
        domFactory.setNamespaceAware(true);
        DocumentBuilder builder = domFactory.newDocumentBuilder();
        Document doc = builder.parse(testDoc.getFile());

        ResourceMap foresiteResourceMap = new ForesiteResourceMap(testDoc.getFile()
                .getAbsolutePath(), new IndexVisibilityDelegateTestImpl());

        ResourceMap xpathResourceMap = new XPathResourceMap(doc,
                new IndexVisibilityDelegateTestImpl());

        /*** Checks that top level identifiers match ***/
        Assert.assertEquals("Identifiers do not match", foresiteResourceMap.getIdentifier(),
                xpathResourceMap.getIdentifier());

        /*** Tests getAllDocumentIDs() ***/
        List<String> foresiteDocs = foresiteResourceMap.getAllDocumentIDs();
        List<String> xpathDocs = xpathResourceMap.getAllDocumentIDs();

        /* Check the number of doc ids */
        Assert.assertEquals("Number of documents IDs don't match", foresiteDocs.size(),
                xpathDocs.size());

        Collections.sort(foresiteDocs);
        Collections.sort(xpathDocs);

        /* Check the individual elements match */
        for (int i = 0; i < foresiteDocs.size(); i++) {
            Assert.assertEquals("Document ID at " + i + "don't match", foresiteDocs.get(i),
                    xpathDocs.get(i));
        }

        /*** Tests the getContains method ***/
        List<String> foresiteContains = new LinkedList<String>(foresiteResourceMap.getContains());
        List<String> xpathContains = new LinkedList<String>(xpathResourceMap.getContains());

        Collections.sort(foresiteContains);
        Collections.sort(xpathContains);

        /* Checks that the number of resources is the same */
        Assert.assertEquals("Number of mapped references don't match", foresiteContains.size(),
                xpathContains.size());

        /* Check the individual resource ids match */
        for (int i = 0; i < foresiteContains.size(); i++) {
            Assert.assertEquals("Document ID at " + i + "don't match", foresiteContains.get(i),
                    xpathContains.get(i));
        }

        /*** Tests getIdentifierFromResource ***/

        /*** Tests getMappedReferences ***/

        /* Builds a sorted list of documents IDs for comparison */
        List<ResourceEntry> foresiteResourceMapDocs = new LinkedList<ResourceEntry>(
                foresiteResourceMap.getMappedReferences());
        List<ResourceEntry> xpathResourceMapDocs = new LinkedList<ResourceEntry>(
                xpathResourceMap.getMappedReferences());

        Collections.sort(foresiteResourceMapDocs, new Comparator<ResourceEntry>() {
            @Override
            public int compare(ResourceEntry o1, ResourceEntry o2) {
                return o1.getIdentifier().compareTo(o2.getIdentifier());
            }
        });

        Collections.sort(xpathResourceMapDocs, new Comparator<ResourceEntry>() {
            @Override
            public int compare(ResourceEntry o1, ResourceEntry o2) {
                return o1.getIdentifier().compareTo(o2.getIdentifier());
            }
        });

        /* Checks that the number of mapped references is the same */
        Assert.assertEquals("Number of mapped references don't match",
                foresiteResourceMapDocs.size(), xpathResourceMapDocs.size());

        /* Check the individual mapped references match */
        for (int i = 0; i < foresiteResourceMapDocs.size(); i++) {
            Assert.assertEquals("Document ID at " + i + "don't match", foresiteDocs.get(i),
                    xpathDocs.get(i));
        }
    }

    /**
     * Test scenario that ensures that pids that do not have system metadata in the 
     * system yet, still appear in the pid list as pids that are referenced by this resource map
     * and need to appear in the search index.
     * 
     * Test uses inner class - NullSmdVisibilityDelegate to simulate pids that do not have system
     * metadata records.
     */
    @Test
    public void testOREParsingWithNullSystemMetadataReferences()
            throws ParserConfigurationException, SAXException, IOException, OREException,
            URISyntaxException, OREParserException, XPathExpressionException {

        DocumentBuilderFactory domFactory = DocumentBuilderFactory.newInstance();
        domFactory.setNamespaceAware(true);
        DocumentBuilder builder = domFactory.newDocumentBuilder();
        Document doc = builder.parse(testDoc.getFile());

        ResourceMap foresiteResourceMap = new ForesiteResourceMap(testDoc.getFile()
                .getAbsolutePath(), new NullSmdVisibilityDelegate());

        ResourceMap xpathResourceMap = new XPathResourceMap(doc, new NullSmdVisibilityDelegate());

        Map<Identifier, Map<Identifier, List<Identifier>>> relations = org.dataone.ore.ResourceMapFactory
                .getInstance().parseResourceMap(testDoc.getInputStream());

        int pidCount = 1;
        Identifier identifier = relations.keySet().iterator().next();
        Map<Identifier, List<Identifier>> identiferMap = (Map<Identifier, List<Identifier>>) relations
                .get(identifier);
        for (Map.Entry<Identifier, List<Identifier>> entry : identiferMap.entrySet()) {
            pidCount++;
            for (Identifier documentedByIdentifier : entry.getValue()) {
                pidCount++;
            }
        }

        Assert.assertEquals("foresite pid count does not match actual pid count.", pidCount,
                foresiteResourceMap.getAllDocumentIDs().size());
        Assert.assertEquals("xpath pid count does not match actual pid count.", pidCount,
                xpathResourceMap.getAllDocumentIDs().size());
    }

    private class IndexVisibilityDelegateTestImpl implements IndexVisibilityDelegate {
        public boolean isDocumentVisible(Identifier pid) {
            return true;
        }

        public boolean documentExists(Identifier pid) {
            return true;
        }
    }

    private class NullSmdVisibilityDelegate implements IndexVisibilityDelegate {

        public boolean isDocumentVisible(Identifier pid) {
            return true;
        }

        public boolean documentExists(Identifier pid) {
            return false;
        }
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
    }
    
    
    /* Load the indexer and provenance context beans */
    protected void configureSpringResources() throws Exception {

        // Instantiate the generator and processor from the test-context beans
        //processor = (IndexTaskProcessor) context.getBean("indexTaskProcessor");
        //generator = (IndexTaskGenerator) context.getBean("indexTaskGenerator");

        // instantiate the RDF resource to be tested
      
        testDoc = (Resource) context.getBean("testDoc");

        incompleteResourceMap = (Resource) context.getBean("incompleteResourceMap");

        dryadDoc = (Resource) context.getBean("dryadDoc");

        transitiveRelationshipsDoc = (Resource) context.getBean("transitiveRelationshipsDoc");

        incompleteTransitiveRelationshipsDoc =
            (Resource) context.getBean("incompleteTransitiveRelationshipsDoc");

        missingComponentsResourcemap = (Resource) context.getBean("missingComponentResourcemap");

        peggym1321Sci = (Resource) context.getBean("peggym1321Sci");

        peggy1331Resourcemap = (Resource) context.getBean("peggym1331Resourcemap");

        peggym1331Sci = (Resource) context.getBean("peggym1331Sci");

        data11 = (Resource) context.getBean("data11");

        foo1271 = (Resource) context.getBean("foo1271");

        missingComponentsResourcemap2 = (Resource) context.getBean("missingComponentResourcemap2");

        peggym1341Sci = (Resource) context.getBean("peggym1341Sci");
    }


}
