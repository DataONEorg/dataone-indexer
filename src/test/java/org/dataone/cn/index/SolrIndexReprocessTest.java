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
import org.apache.log4j.Logger;
import org.apache.solr.common.SolrDocument;
import org.dataone.cn.hazelcast.HazelcastClientFactory;
import org.dataone.cn.index.generator.IndexTaskGenerator;
import org.dataone.cn.index.processor.IndexTaskProcessor;
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

    private static Logger logger = Logger.getLogger(SolrIndexReprocessTest.class.getName());

    private IndexTaskProcessor processor;
    private IndexTaskGenerator generator;

    private Resource peggym1271Sys;
    private Resource peggym1281Sys;
    private Resource peggym1281SysObsoletedBy;
    private Resource peggym1282Sys;
    private Resource peggym1291Sys;
    private Resource peggym1304Sys;
    private Resource peggym1305Sys;
    private Resource peggym1304SysObsoletedBy;
    private Resource peggymResourcemapSeriesSys;
    
    private static final int SLEEPTIME = 2000;

    @BeforeClass
    public static void init() {
        HazelcastClientFactoryTest.setUp();
    }

    @AfterClass
    public static void cleanup() throws Exception {
        HazelcastClientFactoryTest.shutDown();
    }

    /**
     * Test reprocessing when new version of object in a data package is updated
     */
    @Ignore
    @Test
    public void testReprocessDataPackage() throws Exception {
        // create/index data package
        deleteAll();
        indexTestDataPackage();
        //verify in index correct
        verifyTestDataPackageIndexed();
        System.out.println("&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&finished the initial index and everything is good.");
        indexNewRevision(peggym1304SysObsoletedBy);
        //Thread.sleep(2000);
        processor.processIndexTaskQueue();
        //Thread.sleep(2000);
        
        
        indexNewRevision(peggym1305Sys);
        Thread.sleep(2000);
        processor.processIndexTaskQueue();
        // verify data package info correct in index
        verifyDataPackageNewRevision();

        // add data revision
        indexNewRevision(peggym1281SysObsoletedBy);
        indexNewRevision(peggym1282Sys);
        processor.processIndexTaskQueue();
        // verify data package info correct in index
        verifyDataPackageNewDataRevision();
    }

    private void deleteAll() throws IOException {
        HTTPService httpService = (HTTPService) context.getBean("httpService");
        httpService.sendSolrDelete("peggym.130.4");
        httpService.sendSolrDelete("peggym.130.5");
        httpService.sendSolrDelete("peggym.127.1");
        httpService.sendSolrDelete("peggym.128.1");
        httpService.sendSolrDelete("peggym.128.2");
        httpService.sendSolrDelete("peggym.129.1");
        httpService.sendSolrDelete("peggym.resourcemap.series");
    }

    private void indexTestDataPackage() throws Exception {
        addSystemMetadata(peggym1271Sys);
        addSystemMetadata(peggym1281Sys);
        addSystemMetadata(peggym1291Sys);
        addSystemMetadata(peggym1304Sys);
        Thread.sleep(SLEEPTIME);
        processor.processIndexTaskQueue();
        Thread.sleep(SLEEPTIME);
        addSystemMetadata(peggymResourcemapSeriesSys);
        Thread.sleep(SLEEPTIME);
        processor.processIndexTaskQueue();
    }

    private void indexNewRevision(Resource resource) throws Exception{
        addSystemMetadata(resource);
        Thread.sleep(SLEEPTIME);
        processor.processIndexTaskQueue();
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
        HazelcastClientFactory.getSystemMetadataMap().put(sysmeta.getIdentifier(), sysmeta);
        //sysMetaMap.put(sysmeta.getIdentifier(), sysmeta);
        HazelcastClientFactory.getObjectPathMap().putAsync(sysmeta.getIdentifier(), path);
        generator.processSystemMetaDataUpdate(sysmeta, path);
    }

    public void setUp() throws Exception {
        super.setUp();
        configureSpringResources();
    }

    public void tearDown() throws Exception {
        super.tearDown();
    }

    private void configureSpringResources() {
        processor = (IndexTaskProcessor) context.getBean("indexTaskProcessor");
        generator = (IndexTaskGenerator) context.getBean("indexTaskGenerator");

        peggym1271Sys = (Resource) context.getBean("peggym1271Sys");
        peggym1281Sys = (Resource) context.getBean("peggym1281Sys");
        peggym1281SysObsoletedBy = (Resource) context.getBean("peggym1281SysObsoletedBy");
        peggym1282Sys = (Resource) context.getBean("peggym1282Sys");
        peggym1291Sys = (Resource) context.getBean("peggym1291Sys");
        peggym1304Sys = (Resource) context.getBean("peggym1304Sys");
        peggym1305Sys = (Resource) context.getBean("peggym1305Sys");
        
        peggym1304SysObsoletedBy = (Resource) context.getBean("peggym1304SysObsoletedBy");
        peggymResourcemapSeriesSys = (Resource) context.getBean("peggymResourcemapSeriesSys");

    }

}
