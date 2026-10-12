package org.dataone.cn.index;

import com.carrotsearch.randomizedtesting.annotations.ThreadLeakScope;
import org.dataone.cn.indexer.solrhttp.SolrElementField;
import org.dataone.service.exceptions.InvalidRequest;
import org.dataone.service.types.v1.Identifier;
import org.dataone.service.types.v2.SystemMetadata;
import org.dataone.service.util.TypeMarshaller;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.Date;

/**
 * Test the scenarios that the RabbitMQ messages contains the embedded system metadata
 */
@ThreadLeakScope(ThreadLeakScope.Scope.NONE)
public class EmbeddedSysMetaIndexingTest extends DataONESolrJettyTestBase {
    private final static String DATA_SYSMETA_Path =
        "src/test/resources/org/dataone/cn/index/resources/d1_testdocs/data/data-system"
            + "-metadata.xml";
    private static final int SLEEP = 200;
    private static final int TIMES = 100;


    @Before
    public void setUp() throws Exception {
        // Start up the embedded Jetty server and Solr service
        super.setUp();
    }

    @After
    public void tearDown() throws Exception {
        super.tearDown();
    }

    /**
     * Successfully index a data object with embedded system metadata.
     * Since indexing a data object only needs the system metadata, we don't need to load the
     * object into hashtore.
     * @throws Exception
     */
    @Test
    public void testIndexWithEmbeddedSysMeta() throws Exception {
        Boolean isSysmetaChangeOnly = false;
        SystemMetadata embeddedSysMeta =
            TypeMarshaller.unmarshalTypeFromFile(SystemMetadata.class, DATA_SYSMETA_Path);
        String id = "testIndexWithEmbeddedSysMeta" + System.currentTimeMillis();
        Identifier pid = new Identifier();
        pid.setValue(id);
        embeddedSysMeta.setIdentifier(pid);
        // Null is for docid
        solrIndexService.update(pid, isSysmetaChangeOnly, null, embeddedSysMeta);
        for (int i = 0; i < TIMES; i++) {
            try {
                Thread.sleep(SLEEP);
                assertPresentInSolrIndex(id);
                break;
            } catch (Throwable e) {
            }
        }
        assertTrue(compareFieldValue(id, "id", id));
        assertTrue(compareFieldValue(id, "submitter","http://orcid.org/0000-0002-1209-5268"));
        assertTrue(compareFieldValue(id, "checksum", "c5e0815b9d729df135968d535bd4a6de"));
        assertTrue(compareFieldValue(id, "fileName", "letter"));
    }

    /**
     * Since the embedded system metadata can carry an older version than the one in solr, we
     * need to test that the scenario will cause a rejection.
     * @throws Exception
     */
    @Test
    public void olderVersionOverwriteTest() throws Exception {
        // Add a solr doc to the server
        boolean isSysmetaChangeOnly = false;
        SystemMetadata embeddedSysMeta =
            TypeMarshaller.unmarshalTypeFromFile(SystemMetadata.class, DATA_SYSMETA_Path);
        String id = "olderVersionOverwriteTest" + System.currentTimeMillis();
        Identifier pid = new Identifier();
        pid.setValue(id);
        embeddedSysMeta.setIdentifier(pid);
        // Null is for docid
        solrIndexService.update(pid, isSysmetaChangeOnly, null, embeddedSysMeta);
        for (int i = 0; i < TIMES; i++) {
            try {
                Thread.sleep(SLEEP);
                assertPresentInSolrIndex(id);
                break;
            } catch (Throwable e) {
            }
        }
        assertTrue(compareFieldValue(id, "id", id));
        assertTrue(compareFieldValue(id, "submitter","http://orcid.org/0000-0002-1209-5268"));
        assertTrue(compareFieldValue(id, "checksum", "c5e0815b9d729df135968d535bd4a6de"));
        assertTrue(compareFieldValue(id, "fileName", "letter"));
        String originalVersion = solrIndexService.getHttpService().getSolrDocumentById(id)
            .getField(SolrElementField.FIELD_VERSION).getValue();
        // Index again with the same modification date. It should succeed.
        solrIndexService.update(pid, isSysmetaChangeOnly, null, embeddedSysMeta);
        String newVersion = solrIndexService.getHttpService().getSolrDocumentById(id)
            .getField(SolrElementField.FIELD_VERSION).getValue();
        for (int i = 0; i < TIMES; i++) {
            try {
                Thread.sleep(SLEEP);
                newVersion = solrIndexService.getHttpService().getSolrDocumentById(id)
                    .getField(SolrElementField.FIELD_VERSION).getValue();
                assertNotEquals(originalVersion, newVersion);
                break;
            } catch (Throwable e) {
            }
        }
        //It has a new version in solr.
        assertNotEquals(originalVersion, newVersion);
        assertTrue(compareFieldValue(id, "checksum", "c5e0815b9d729df135968d535bd4a6de"));
        // Index again with an older modification date. It should fail.
        Date date = embeddedSysMeta.getDateSysMetadataModified();
        // Subtract 1 millisecond from the original time and create a new Date
        Date olderDate = new Date(date.getTime() - 1);
        embeddedSysMeta.setDateSysMetadataModified(olderDate);
        InvalidRequest exception = assertThrows(InvalidRequest.class, () ->
            solrIndexService.update(pid, isSysmetaChangeOnly, null, embeddedSysMeta)
        );
        assertTrue(exception.getMessage().contains("older modification date"));
    }
}
