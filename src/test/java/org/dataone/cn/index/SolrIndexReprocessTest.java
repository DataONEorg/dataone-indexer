/**
 * This work was created by participants in the DataONE project, and is
 * jointly copyrighted by participating institutions in DataONE. For 
 * more information on DataONE, see our web site at http://dataone.org.
 *
 *   Copyright ${year}
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and 
 * limitations under the License.
 * 
 * $Id$
 */

package org.dataone.cn.index;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.solr.common.SolrDocument;
import org.dataone.cn.indexer.solrhttp.HTTPService;
import org.dataone.cn.indexer.solrhttp.SolrElementField;
import org.dataone.service.types.v2.SystemMetadata;
import org.dataone.service.util.TypeMarshaller;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.core.io.Resource;

import com.carrotsearch.randomizedtesting.annotations.ThreadLeakScope;

/**
 * Solr unit test framework is dependent on JUnit 4.7. Later versions of junit
 * will break the base test classes.
 * 
 * @author sroseboo
 * 
 */
@ThreadLeakScope(ThreadLeakScope.Scope.NONE)
public class SolrIndexReprocessTest extends DataONESolrJettyTestBase {

    private static Log logger = LogFactory.getLog(SolrIndexReprocessTest.class.getName());
    private Resource peggym1271Sci;
    private String pid1271 = "peggym.127.1";
    private Resource peggym1281Sci;
    private Resource peggym1281SciObsoletedBy;
    private String pid1281 = "peggym.128.1";
    private Resource peggym1282Sci;
    private String pid1282 = "peggym.128.2";
    private Resource peggym1291Sci;
    private String pid1291 = "peggym.129.1";
    private Resource peggym1304Sci;
    private Resource peggym1304SciObsoletedBy;
    private String pid1304 = "peggym.130.4";
    private Resource peggym1305Sci;
    private String pid1305 = "peggym.130.5";
    private Resource peggymResourcemapSeriesSci;
    private String pidresourcemap = "peggym.resourcemap.series";
    
    private static final int SLEEPTIME = 2000;

    @BeforeClass
    public static void init() {
        //HazelcastClientFactoryTest.setUp();
    }

    @AfterClass
    public static void cleanup() throws Exception {
        //HazelcastClientFactoryTest.shutDown();
    }

    /**
     * Test reprocessing when new version of object in a data package is updated
     */
    @Ignore
    @Test
    public void testReprocessDataPackage() throws Exception {
        indexTestDataPackage();
        //verify in index correct
        verifyTestDataPackageIndexed();
        System.out.println("&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&finished the initial index and everything is good.");
        indexNewRevision(pid1304, peggym1304SciObsoletedBy);      
        indexNewRevision(pid1305, peggym1305Sci);
        Thread.sleep(2000);
        //processor.processIndexTaskQueue();
        // verify data package info correct in index
        verifyDataPackageNewRevision();

        // add data revision
        indexNewRevision(pid1281, peggym1281SciObsoletedBy);
        indexNewRevision(pid1282, peggym1282Sci);
        //processor.processIndexTaskQueue();
        // verify data package info correct in index
        verifyDataPackageNewDataRevision();
    }

   

    private void indexTestDataPackage() throws Exception {
        indexObjectToSolr(pid1271, peggym1271Sci);
        indexObjectToSolr(pid1281, peggym1281Sci);
        indexObjectToSolr(pid1291, peggym1291Sci);
        indexObjectToSolr(pid1304, peggym1304Sci);
        Thread.sleep(SLEEPTIME);
        Thread.sleep(SLEEPTIME);
        indexObjectToSolr(pidresourcemap, peggymResourcemapSeriesSci);
        Thread.sleep(SLEEPTIME);
    }

    private void indexNewRevision(String pid, Resource resource) throws Exception{
        indexObjectToSolr(pid, resource);
        Thread.sleep(SLEEPTIME);
    }

    private void verifyDataPackageNewRevision() throws Exception {
        Thread.sleep(20000);
        SolrDocument data = assertPresentInSolrIndex("peggym.127.1");
        Assert.assertEquals(1,
                ((List) data.getFieldValues(SolrElementField.FIELD_RESOURCEMAP)).size());
        Assert.assertEquals("peggym.resourcemap.series",
                ((List) data.getFieldValues(SolrElementField.FIELD_RESOURCEMAP)).get(0));

        Assert.assertEquals(1,
                ((List) data.getFieldValues(SolrElementField.FIELD_ISDOCUMENTEDBY)).size());
        Assert.assertEquals("peggym.130",
                ((List) data.getFieldValue(SolrElementField.FIELD_ISDOCUMENTEDBY)).get(0));

        Assert.assertNull(data.getFieldValues(SolrElementField.FIELD_DOCUMENTS));

        assertPresentInSolrIndex("peggym.128.1");
        assertPresentInSolrIndex("peggym.129.1");

        // older revision of sciMeta should be taken out of data package
        SolrDocument scienceMetadata = assertPresentInSolrIndex("peggym.130.4");
        Assert.assertNull(scienceMetadata.getFieldValues(SolrElementField.FIELD_RESOURCEMAP));
        
        // and documents relationships removed
        Collection documentsCollection = scienceMetadata
                .getFieldValues(SolrElementField.FIELD_DOCUMENTS);
        Assert.assertNull(documentsCollection);
        
        // check that the new revision has the resource map/documents values on it
        SolrDocument scienceMetadataRevision = assertPresentInSolrIndex("peggym.130.5");
        System.out.println("scienceMetadataRevision=====" + scienceMetadataRevision);
        Assert.assertEquals(1, ((List) scienceMetadataRevision
                .getFieldValues(SolrElementField.FIELD_RESOURCEMAP)).size());
        Assert.assertEquals("peggym.resourcemap.series", ((List) scienceMetadataRevision
                .getFieldValue(SolrElementField.FIELD_RESOURCEMAP)).get(0));
        
        // make sure the documents values are in place on the new scimeta record
        Collection documentsUpdatedCollection = scienceMetadataRevision
                .getFieldValues(SolrElementField.FIELD_DOCUMENTS);
        Assert.assertEquals(3, documentsUpdatedCollection.size());
        Assert.assertTrue(documentsUpdatedCollection.contains("peggym.127.1"));
        Assert.assertTrue(documentsUpdatedCollection.contains("peggym.128"));
        Assert.assertTrue(documentsUpdatedCollection.contains("peggym.129.1"));
        
        // and of course, the ORE is still there
        assertPresentInSolrIndex("peggym.resourcemap.series");

    }

    private void verifyDataPackageNewDataRevision() throws Exception {
        Thread.sleep(20000);
    	// make sure the original data is not in the package now
        SolrDocument dataOrig = assertPresentInSolrIndex("peggym.128.1");
        Assert.assertNull(dataOrig.getFieldValues(SolrElementField.FIELD_RESOURCEMAP));
        Assert.assertNull(dataOrig.getFieldValues(SolrElementField.FIELD_ISDOCUMENTEDBY));

        SolrDocument dataNew = assertPresentInSolrIndex("peggym.128.2");
        Assert.assertEquals(1,
                ((List) dataNew.getFieldValues(SolrElementField.FIELD_RESOURCEMAP)).size());
        Assert.assertEquals("peggym.resourcemap.series",
                ((List) dataNew.getFieldValues(SolrElementField.FIELD_RESOURCEMAP)).get(0));
        Assert.assertEquals(1,
                ((List) dataNew.getFieldValues(SolrElementField.FIELD_ISDOCUMENTEDBY)).size());
        Assert.assertEquals("peggym.130",
                ((List) dataNew.getFieldValue(SolrElementField.FIELD_ISDOCUMENTEDBY)).get(0));
        Assert.assertNull(dataNew.getFieldValues(SolrElementField.FIELD_DOCUMENTS));

        // check other items that have not changed
        SolrDocument data = assertPresentInSolrIndex("peggym.127.1");
        Assert.assertEquals(1,
                ((List) data.getFieldValues(SolrElementField.FIELD_RESOURCEMAP)).size());
        Assert.assertEquals("peggym.resourcemap.series",
                ((List) data.getFieldValues(SolrElementField.FIELD_RESOURCEMAP)).get(0));
        Assert.assertEquals(1,
                ((List) data.getFieldValues(SolrElementField.FIELD_ISDOCUMENTEDBY)).size());
        Assert.assertEquals("peggym.130",
                ((List) data.getFieldValue(SolrElementField.FIELD_ISDOCUMENTEDBY)).get(0));
        Assert.assertNull(data.getFieldValues(SolrElementField.FIELD_DOCUMENTS));

        assertPresentInSolrIndex("peggym.129.1");

        // make sure the older revision of scimeta is not in package
        SolrDocument scienceMetadata = assertPresentInSolrIndex("peggym.130.4");
        Assert.assertNull(scienceMetadata.getFieldValues(SolrElementField.FIELD_RESOURCEMAP));

        Collection documentsCollection = scienceMetadata
                .getFieldValues(SolrElementField.FIELD_DOCUMENTS);
        Assert.assertNull(documentsCollection);

        // check that the new revision also has the resource map value on it
        SolrDocument scienceMetadataRevision = assertPresentInSolrIndex("peggym.130.5");
        System.out.println("scienceMetadataRevision=====" + scienceMetadataRevision);
        Assert.assertEquals(1, ((List) scienceMetadataRevision
                .getFieldValues(SolrElementField.FIELD_RESOURCEMAP)).size());
        Assert.assertEquals("peggym.resourcemap.series", ((List) scienceMetadataRevision
                .getFieldValue(SolrElementField.FIELD_RESOURCEMAP)).get(0));
        
        Collection documentsCollectionRevision = scienceMetadataRevision
                .getFieldValues(SolrElementField.FIELD_DOCUMENTS);
        Assert.assertEquals(3, documentsCollectionRevision.size());
        Assert.assertTrue(documentsCollectionRevision.contains("peggym.127.1"));
        Assert.assertTrue(documentsCollectionRevision.contains("peggym.128"));
        Assert.assertTrue(documentsCollectionRevision.contains("peggym.129.1"));

        assertPresentInSolrIndex("peggym.resourcemap.series");

    }

    private void verifyTestDataPackageIndexed() throws Exception {
        Thread.sleep(20000);
        SolrDocument data = assertPresentInSolrIndex("peggym.127.1");
        logger.debug("DATA=" + data);
        Assert.assertEquals(1,
                ((List) data.getFieldValues(SolrElementField.FIELD_RESOURCEMAP)).size());
        Assert.assertEquals("peggym.resourcemap.series",
                ((List) data.getFieldValues(SolrElementField.FIELD_RESOURCEMAP)).get(0));

        Assert.assertEquals(1,
                ((List) data.getFieldValues(SolrElementField.FIELD_ISDOCUMENTEDBY)).size());
        Assert.assertEquals("peggym.130",
                ((List) data.getFieldValue(SolrElementField.FIELD_ISDOCUMENTEDBY)).get(0));

        Assert.assertNull(data.getFieldValues(SolrElementField.FIELD_DOCUMENTS));

        assertPresentInSolrIndex("peggym.128.1");
        assertPresentInSolrIndex("peggym.129.1");

        SolrDocument scienceMetadata = assertPresentInSolrIndex("peggym.130.4");
        Assert.assertEquals("peggym.130",
                scienceMetadata.getFieldValue(SolrElementField.FIELD_SERIES_ID));
        Assert.assertEquals(1,
                ((List) scienceMetadata.getFieldValues(SolrElementField.FIELD_RESOURCEMAP)).size());
        Assert.assertEquals("peggym.resourcemap.series",
                ((List) scienceMetadata.getFieldValue(SolrElementField.FIELD_RESOURCEMAP)).get(0));

        Collection documentsCollection = scienceMetadata
                .getFieldValues(SolrElementField.FIELD_DOCUMENTS);
        Assert.assertEquals(3, documentsCollection.size());
        Assert.assertTrue(documentsCollection.contains("peggym.127.1"));
        Assert.assertTrue(documentsCollection.contains("peggym.128"));
        Assert.assertTrue(documentsCollection.contains("peggym.129.1"));

        assertPresentInSolrIndex("peggym.resourcemap.series");
    }

    private void addSystemMetadata(Resource systemMetadataResource) {
        SystemMetadata sysmeta = null;
        try {
            sysmeta = TypeMarshaller.unmarshalTypeFromStream(SystemMetadata.class,
                    systemMetadataResource.getInputStream());
        } catch (Exception ex) {
            logger.error(ex.getMessage(), ex);
            fail("Test SystemMetadata misconfiguration - Exception " + ex);
        }
        String path = null;
        try {
            path = StringUtils
                    .remove(systemMetadataResource.getFile().getPath(), File.separator + "SystemMetadata");
        } catch (IOException e) {
            logger.error(e.getMessage(), e);
        }
    }

    public void setUp() throws Exception {
        super.setUp();
        configureSpringResources();
    }

    public void tearDown() throws Exception {
        super.tearDown();
    }

    private void configureSpringResources() {
        peggym1271Sci = (Resource) context.getBean("peggym1271Sci");
        peggym1281Sci = (Resource) context.getBean("peggym1281Sci");
        peggym1281SciObsoletedBy = (Resource) context.getBean("peggym1281SciObsoletedBy");
        peggym1282Sci = (Resource) context.getBean("peggym1282Sci");
        peggym1291Sci = (Resource) context.getBean("peggym1291Sci");
        peggym1304Sci = (Resource) context.getBean("peggym1304Sci");
        peggym1304SciObsoletedBy = (Resource) context.getBean("peggym1304SciObsoletedBy");
        peggym1305Sci = (Resource) context.getBean("peggym1305Sci");
        peggymResourcemapSeriesSci = (Resource) context.getBean("peggymResourcemapSeriesSci");

    }

}
