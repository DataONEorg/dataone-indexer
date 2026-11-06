package org.dataone.cn.indexer.solrhttp;

import com.carrotsearch.randomizedtesting.annotations.ThreadLeakScope;
import org.dataone.cn.index.DataONESolrJettyTestBase;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.core.io.Resource;

/**
 * Test the methods in the HttpService class
 */
@ThreadLeakScope(ThreadLeakScope.Scope.NONE)
public class HttpServiceTest extends DataONESolrJettyTestBase {
    private Resource peggym1351Sci;
    private String peggym1351SciPid = "peggym.135.1";
    private Resource specialCharacterIdSci;
    private String specialCharacterIdSciPid = "https://foo.com/?action=*";


    private static final int SLEEP = 200;
    private static final int TIMES = 100;

    @Before
    public void setUp() throws Exception {
        // Start up the embedded Jetty server and Solr service
        super.setUp();
        peggym1351Sci = (Resource) context.getBean("peggym1351Sci");
        specialCharacterIdSci = (Resource) context.getBean("specialCharacterIdSci");
    }

    /**
     * For each test, clean up, bring down the Solr service
     */
    @After
    public void tearDown() throws Exception {
        super.tearDown();
    }

    /**
     * Test the getSolrDocById for an archived object
     */
    @Test
    public void testGetSolrDocByIdForArchivedObj() throws Exception {
        //index the object
        String id = peggym1351SciPid;
        indexObjectToSolr(id, peggym1351Sci);
        // Make sure the object has been indexed
        for (int i=0; i<TIMES; i++) {
            try {
                Thread.sleep(SLEEP);
                assertPresentInSolrIndex(id);
                break;
            } catch (Throwable e) {

            }
        }
        SolrDoc doc = solrIndexService.getHttpService().getSolrDocumentById(id);
        assertNotNull(doc);
        assertEquals(id, doc.getIdentifier());
        assertTrue(Long.parseLong(doc.getFirstFieldValue(SolrElementField.FIELD_VERSION)) > 1);
        assertTrue(Boolean.parseBoolean(doc.getFirstFieldValue("archived")));
    }

    /**
     * Test the getSolrDocById with a non-existing id
     */
    @Test
    public void testGetSolrDocByIdWithNonExistId() throws Exception {
        String id = "fooo.jing.1";
        SolrDoc doc = solrIndexService.getHttpService().getSolrDocumentById(id);
        assertNull(doc);
    }

    /**
     * Test the getSolrDocById with an id having special characters
     */
    @Test
    public void testGetSolrDocByIdWithSpecialCharacter() throws Exception {
        //index the object
        String id = specialCharacterIdSciPid;
        indexObjectToSolr(id, specialCharacterIdSci);
        // Make sure the object has been indexed
        for (int i=0; i<TIMES; i++) {
            try {
                Thread.sleep(SLEEP);
                assertPresentInSolrIndex(id);
                break;
            } catch (Throwable e) {

            }
        }
        SolrDoc doc = solrIndexService.getHttpService().getSolrDocumentById(id);
        assertNotNull(doc);
        assertEquals(id, doc.getIdentifier());
        assertTrue(Long.parseLong(doc.getField(SolrElementField.FIELD_VERSION).getValue()) > 1);
    }

}
