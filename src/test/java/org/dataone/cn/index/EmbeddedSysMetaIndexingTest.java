package org.dataone.cn.index;

import com.carrotsearch.randomizedtesting.annotations.ThreadLeakScope;
import org.dataone.service.types.v1.Identifier;
import org.dataone.service.types.v2.SystemMetadata;
import org.dataone.service.util.TypeMarshaller;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

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
        for (int i=0; i<TIMES; i++) {
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
}
