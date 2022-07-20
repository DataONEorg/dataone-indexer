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
public class SolrIndexDeleteTest extends DataONESolrJettyTestBase {

    private static Logger logger = Logger.getLogger(SolrIndexDeleteTest.class.getName());

    private IndexTaskProcessor processor;
    private IndexTaskGenerator generator;

    private Resource peggym1271Sys;
    private Resource peggym1271SysArchived;
    private Resource peggym1281Sys;
    private Resource peggym1291Sys;
    private Resource peggym1304Sys;
    private Resource peggym1304SysArchived;
    private Resource peggymResourcemapSys;
    private Resource peggymResourcemap2Sys;
    private Resource peggymResourcemapComplicatedSys;
    private Resource peggymResourcemap2ComplicatedSys;
    private Resource peggymResourcemapSysArchived;
    private Resource peggymResourcemap2SysArchived;
    private Resource peggymResourcemap1OverlapSys;
    private Resource peggymResourcemap2OverlapSys;
    
    private static final int SLEEPTIME = 4000;


    /**
     * Unit test of the HTTPService.sendSolrDelete(pid) method. Inserts record
     * into solr index using XPathDocumentParser. Does not use index task
     * generation/processing.
     **/
    @Test
    public void testHttpServiceSolrDelete() throws Exception {
        String pid = "peggym.130.4";
        Resource systemMetadataResource = (Resource) context.getBean("peggym1304Sys");
        deleteAll();
        addEmlToSolrIndex(systemMetadataResource);
        Thread.sleep(SLEEPTIME);
        assertPresentInSolrIndex(pid);
        HTTPService httpService = (HTTPService) context.getBean("httpService");
        httpService.sendSolrDelete(pid);
        Thread.sleep(SLEEPTIME);
        assertNotPresentInSolrIndex(pid);
    }

    /**
     * Adds and removes a single science metadata document to the solr index.
     * Uses index task processing/generation to process add/delete.
     * 
     * @throws Exception
     */
    @Test
    public void testDeleteSingleDocFromIndex() throws Exception {
        String pid = "peggym.130.4";
        deleteAll();
        addSystemMetadata(peggym1304Sys);
        Thread.sleep(SLEEPTIME);
        processor.processIndexTaskQueue();
        Thread.sleep(SLEEPTIME);
        assertPresentInSolrIndex(pid);
        addSystemMetadata(peggym1304SysArchived);
        Thread.sleep(SLEEPTIME);
        processor.processIndexTaskQueue();
        Thread.sleep(SLEEPTIME);
        assertNotPresentInSolrIndex(pid);
    }

    /**
     * Adds a data package (see indexTestDataPackage) to solr index and then
     * removes a science data document from the package and verifies the state
     * of the data package is correct with respect to hiding the archived data
     * doc. The resource map document is then updated and the package is
     * verified to ensure science data document is still not present in the
     * package.
     * 
     * @throws Exception
     */
    @Test
    public void testArchiveDataInPackage() throws Exception {
        // create/index data package
        System.out.println("++++++++++++++++++++++++++ start of testArchiveDataInPackage");
        deleteAll();
        indexTestDataPackage();
        // verify in index correct
        verifyTestDataPackageIndexed();
        // remove a data object by adding system metadata to task queue with
        // archive=true
        addSystemMetadata(peggym1271SysArchived);
        Thread.sleep(SLEEPTIME);
        processor.processIndexTaskQueue();
        // verify data package info correct in index
        verifyDataPackageNo1271();
        // update package object (resource map)
        addSystemMetadata(peggymResourcemapSys);
        Thread.sleep(SLEEPTIME);
        processor.processIndexTaskQueue();
        // verify again
        Thread.sleep(SLEEPTIME);
        verifyDataPackageNo1271();
        System.out.println("++++++++++++++++++++++++++ end of testArchiveDataInPackage");
    }

    /**
     * Same scenario as testArchiveDataInPackage, but this time the file removed
     * is the science metadata document.
     * 
     * @throws Exception
     */
    @Test
    public void testArchiveScienceMetadataInPackage() throws Exception {
        System.out.println("++++++++++++++++++++++++++ start of testArchiveScienceMetadataInPackage");
        // create/index data package
        deleteAll();
        indexTestDataPackage();
        // verify in index correct
        verifyTestDataPackageIndexed();
        // remove a data object by adding system metadata to task queue with
        // archive=true
        addSystemMetadata(peggym1304SysArchived);
        Thread.sleep(SLEEPTIME);
        processor.processIndexTaskQueue();
        // verify data package info correct in index
        verifyDataPackageNo1304();
        // update package object (resource map)
        addSystemMetadata(peggymResourcemapSys);
        Thread.sleep(SLEEPTIME);
        processor.processIndexTaskQueue();
        // verify again
        verifyDataPackageNo1304();
        System.out.println("++++++++++++++++++++++++++ end of testArchiveScienceMetadataInPackage");
    }

    /**
     * Same scenario as testArchiveDataInPackage, but this time the data package
     * document itself is removed. This time the science metadata document is
     * updated and then the contents of the archived are verified.
     * 
     * @throws Exception
     */
    @Test
    public void testArchiveDataPackage() throws Exception {
        System.out.println("++++++++++++++++++++++++++ start of testArchiveDataPackage");
        // create/index data package
        deleteAll();
        indexTestDataPackage();
        // verify in index correct
        verifyTestDataPackageIndexed();
        // remove a data object by adding system metadata to task queue with
        // archive=true
        addSystemMetadata(peggymResourcemapSysArchived);
        Thread.sleep(SLEEPTIME);
        processor.processIndexTaskQueue();
        // verify data package info correct in index
        verifyDataPackageNoResourceMap();
        // update package object (resource map)
        addSystemMetadata(peggym1304Sys);
        Thread.sleep(SLEEPTIME);
        processor.processIndexTaskQueue();
        // verify again
        verifyDataPackageNoResourceMap();
        System.out.println("++++++++++++++++++++++++++ end of testArchiveDataPackage");
    }

    /**
     * Test to delete a data package by the removing event.
     */
    @Test
    public void testDeleteDataPackage() throws Exception {
        System.out.println("++++++++++++++++++++++++++ start of testDeleteDataPackage");
        // create/index data package
        deleteAll();
        indexTestDataPackage();
        //verify in index correct
        verifyTestDataPackageIndexed();
        deleteSystemMetadata(peggymResourcemapSys);
        Thread.sleep(SLEEPTIME);
        processor.processIndexTaskQueue();
        // verify data package info correct in index
        verifyDataPackageNoResourceMap();
        assertNotPresentInSolrIndex("peggym.resourcemap");
        System.out.println("++++++++++++++++++++++++++ end of testDeleteDataPackage");
    }

    /**
     * Test to delete a data package while there is another package specifies
     * the same relationship
     */
    @Test
    public void testDeleteDataPackageWithDuplicatedRelationship() throws Exception {
        System.out.println("++++++++++++++++++++++++++ start of testDeleteDataPackageWithDuplicatedRelationship");
        // create/index data package
        deleteAll();
        indexTestDataPackage();
        //verify in index correct
        verifyTestDataPackageIndexed();
        indexSecondTestDataPackage();
        verifySecondTestDataPackageIndexed();
        deleteSystemMetadata(peggymResourcemap2Sys);
        Thread.sleep(SLEEPTIME);
        processor.processIndexTaskQueue();
        // verify data package info correct in index.
        // we removed the second one. So it will recover 
        // to status that only has one resource map
        verifyTestDataPackageIndexed();
        assertNotPresentInSolrIndex("peggym.resourcemap2");
        deleteSystemMetadata(peggymResourcemapSys);
        Thread.sleep(SLEEPTIME);
        processor.processIndexTaskQueue();
        // verify data package info correct in index
        verifyDataPackageNoResourceMap();
        assertNotPresentInSolrIndex("peggym.resourcemap");
        System.out.println("++++++++++++++++++++++++++ end of testDeleteDataPackageWithDuplicatedRelationship");
    }

    /**
     * Test to delete data packages having complicated relationship.
     * DataPackage1 - peggym.resourcemap-complicated - describe the following relationship:
     *  peggym.130.4 documents peggym.128.1
     *  peggym.130.4 documents peggym.129.1
     *  peggym.127.1 documents peggym.130.4
     *  So peggym.130.4 is both metadata and data.
     * DataPackage2 - peggym.resourcemap2-complicated - describe the same relationship.
     * 
     */
    @Test
    public void testDeleteDataPackagesWithComplicatedRelation() throws Exception {
        System.out.println("++++++++++++++++++++++++++ start of testDeleteDataPackagesWithComplicatedRelation");
        deleteAll();
        indexComplicatedDataPackage();
        verifyComplicatedDataPackageIndexed();
        indexSecondComplicatedDataPackage();
        verifySecondComplicatedDataPackageIndexed();
        deleteSystemMetadata(peggymResourcemap2ComplicatedSys);
        Thread.sleep(SLEEPTIME);
        processor.processIndexTaskQueue();
        // verify data package info correct in index.
        // we removed the second one. So it will recover 
        // to status that only has one resource map
        verifyComplicatedDataPackageIndexed();
        assertNotPresentInSolrIndex("peggym.resourcemap2-complicated");
        deleteSystemMetadata(peggymResourcemapComplicatedSys);
        Thread.sleep(SLEEPTIME);
        processor.processIndexTaskQueue();
        // verify data package info correct in index
        verifyDataPackageNoResourceMap();
        assertNotPresentInSolrIndex("peggym.resourcemap-complicated");
        System.out.println("++++++++++++++++++++++++++ end of testDeleteDataPackagesWithComplicatedRelation");
    }

    /**
     * Two data packages:
     * The first one - peggym.resourcemap1-overlap: peggym.130.4 documents peggym.127.1
     * The second one - peggym.resourcemap2-overlap: peggym.130.4 documents peggym.128.1 and peggym.129.1. 
     * @throws Exception
     */
    @Test
    public void testDeleteTwoOverlappedDataPackage() throws Exception {
        System.out.println("++++++++++++++++++++++++++ start of testDeleteTwoOverlappedDataPackage");
        deleteAll();
        indexFirstOverlapDataPackage();
        verifyFirstOverlapDataPackageIndexed();
        indexSecondOverlapDataPackage();
        verifySecondOverlapDataPackageIndexed();
        deleteSystemMetadata(peggymResourcemap2OverlapSys);
        processor.processIndexTaskQueue();
        verifyFirstOverlapDataPackageIndexed();
        assertNotPresentInSolrIndex("peggym.resourcemap2-overlap");
        deleteSystemMetadata(peggymResourcemap1OverlapSys);
        processor.processIndexTaskQueue();
        verifyDataPackageNoResourceMap();
        assertNotPresentInSolrIndex("peggym.resourcemap1-overlap");
        System.out.println("++++++++++++++++++++++++++ end of testDeleteTwoOverlappedDataPackage");
    }

    /**
     * Verify that a data package will index when one (or more) documents in the
     * data package are archived.
     * 
     * @throws Exception
     */
    @Test
    public void testDataPackageWithArchivedDoc() throws Exception {
        System.out.println("++++++++++++++++++++++++++ start of testDataPackageWithArchivedDoc");
        deleteAll();
        indexTestDataPackageWithArchived1271Doc();
        verifyDataPackageNo1271();
        System.out.println("++++++++++++++++++++++++++ end of testDataPackageWithArchivedDoc");
    }

    private void verifyDataPackageNoResourceMap() throws Exception {
        Thread.sleep(SLEEPTIME);
        SolrDocument data = assertPresentInSolrIndex("peggym.127.1");
        Assert.assertNull(data.getFieldValues(SolrElementField.FIELD_RESOURCEMAP));
        Assert.assertNull(data.getFieldValues(SolrElementField.FIELD_ISDOCUMENTEDBY));
        Assert.assertNull(data.getFieldValues(SolrElementField.FIELD_DOCUMENTS));

        data = assertPresentInSolrIndex("peggym.128.1");
        Assert.assertNull(data.getFieldValues(SolrElementField.FIELD_RESOURCEMAP));
        Assert.assertNull(data.getFieldValues(SolrElementField.FIELD_ISDOCUMENTEDBY));
        Assert.assertNull(data.getFieldValues(SolrElementField.FIELD_DOCUMENTS));

        data = assertPresentInSolrIndex("peggym.129.1");
        Assert.assertNull(data.getFieldValues(SolrElementField.FIELD_RESOURCEMAP));
        Assert.assertNull(data.getFieldValues(SolrElementField.FIELD_ISDOCUMENTEDBY));
        Assert.assertNull(data.getFieldValues(SolrElementField.FIELD_DOCUMENTS));

        SolrDocument scienceMetadata = assertPresentInSolrIndex("peggym.130.4");
        Assert.assertNull(scienceMetadata.getFieldValues(SolrElementField.FIELD_RESOURCEMAP));
        Assert.assertNull(scienceMetadata.getFieldValues(SolrElementField.FIELD_ISDOCUMENTEDBY));
        Assert.assertNull(scienceMetadata.getFieldValues(SolrElementField.FIELD_DOCUMENTS));

        assertNotPresentInSolrIndex("peggym.resourcemap");
    }

    private void verifyDataPackageNo1304() throws Exception {
        Thread.sleep(SLEEPTIME);
        assertPresentInSolrIndex("peggym.127.1");

        SolrDocument data = assertPresentInSolrIndex("peggym.128.1");
        Assert.assertEquals(1,
                ((List) data.getFieldValues(SolrElementField.FIELD_RESOURCEMAP)).size());
        Assert.assertEquals("peggym.resourcemap",
                ((List) data.getFieldValues(SolrElementField.FIELD_RESOURCEMAP)).get(0));
        Assert.assertNull(data.getFieldValues(SolrElementField.FIELD_ISDOCUMENTEDBY));
        Assert.assertNull(data.getFieldValues(SolrElementField.FIELD_DOCUMENTS));

        assertPresentInSolrIndex("peggym.129.1");
        assertNotPresentInSolrIndex("peggym.130.4");
        assertPresentInSolrIndex("peggym.resourcemap");
    }

    private void deleteAll() throws IOException {
        HTTPService httpService = (HTTPService) context.getBean("httpService");
        httpService.sendSolrDelete("peggym.130.4");
        httpService.sendSolrDelete("peggym.127.1");
        httpService.sendSolrDelete("peggym.128.1");
        httpService.sendSolrDelete("peggym.129.1");
        httpService.sendSolrDelete("peggym.resourcemap");
        httpService.sendSolrDelete("peggym.resourcemap2");
        httpService.sendSolrDelete("peggym.resourcemap-complicated");
        httpService.sendSolrDelete("peggym.resourcemap2-complicated");
        httpService.sendSolrDelete("peggym.resourcemap1-overlap");
        httpService.sendSolrDelete("peggym.resourcemap2-overlap");
    }

    private void verifyDataPackageNo1271() throws Exception {
        Thread.sleep(SLEEPTIME);
        assertNotPresentInSolrIndex("peggym.127.1");
        SolrDocument data = assertPresentInSolrIndex("peggym.128.1");
        Assert.assertEquals(1,
                ((List) data.getFieldValues(SolrElementField.FIELD_RESOURCEMAP)).size());
        Assert.assertEquals("peggym.resourcemap",
                ((List) data.getFieldValues(SolrElementField.FIELD_RESOURCEMAP)).get(0));

        Assert.assertEquals(1,
                ((List) data.getFieldValues(SolrElementField.FIELD_ISDOCUMENTEDBY)).size());
        Assert.assertEquals("peggym.130.4",
                ((List) data.getFieldValue(SolrElementField.FIELD_ISDOCUMENTEDBY)).get(0));

        Assert.assertNull(data.getFieldValues(SolrElementField.FIELD_DOCUMENTS));

        assertPresentInSolrIndex("peggym.129.1");

        SolrDocument scienceMetadata = assertPresentInSolrIndex("peggym.130.4");
        Assert.assertEquals(1,
                ((List) scienceMetadata.getFieldValues(SolrElementField.FIELD_RESOURCEMAP)).size());
        Assert.assertEquals("peggym.resourcemap",
                ((List) scienceMetadata.getFieldValue(SolrElementField.FIELD_RESOURCEMAP)).get(0));

        Collection documentsCollection = scienceMetadata
                .getFieldValues(SolrElementField.FIELD_DOCUMENTS);
        Assert.assertEquals(2, documentsCollection.size());
        Assert.assertFalse(documentsCollection.contains("peggym.127.1"));
        Assert.assertTrue(documentsCollection.contains("peggym.128.1"));
        Assert.assertTrue(documentsCollection.contains("peggym.129.1"));

        assertPresentInSolrIndex("peggym.resourcemap");
    }

    private void indexFirstOverlapDataPackage() throws Exception {
        addSystemMetadata(peggym1271Sys);
        addSystemMetadata(peggym1304Sys);
        Thread.sleep(SLEEPTIME);
        processor.processIndexTaskQueue();
        addSystemMetadata(peggymResourcemap1OverlapSys);
        Thread.sleep(SLEEPTIME);
        processor.processIndexTaskQueue();
    }

    private void indexSecondOverlapDataPackage() throws Exception {
        addSystemMetadata(peggym1281Sys);
        addSystemMetadata(peggym1291Sys);
        addSystemMetadata(peggym1304Sys);
        Thread.sleep(SLEEPTIME);
        processor.processIndexTaskQueue();
        addSystemMetadata(peggymResourcemap2OverlapSys);
        Thread.sleep(SLEEPTIME);
        processor.processIndexTaskQueue();
    }

    private void indexTestDataPackage() throws Exception {
        addSystemMetadata(peggym1271Sys);
        addSystemMetadata(peggym1281Sys);
        addSystemMetadata(peggym1291Sys);
        addSystemMetadata(peggym1304Sys);
        Thread.sleep(SLEEPTIME);
        processor.processIndexTaskQueue();
        addSystemMetadata(peggymResourcemapSys);
        Thread.sleep(SLEEPTIME);
        processor.processIndexTaskQueue();
    }

    private void indexSecondTestDataPackage() throws Exception {
        addSystemMetadata(peggym1271Sys);
        addSystemMetadata(peggym1281Sys);
        addSystemMetadata(peggym1291Sys);
        addSystemMetadata(peggym1304Sys);
        Thread.sleep(SLEEPTIME);
        processor.processIndexTaskQueue();
        addSystemMetadata(peggymResourcemap2Sys);
        Thread.sleep(SLEEPTIME);
        processor.processIndexTaskQueue();
    }

    private void indexComplicatedDataPackage() throws Exception {
        addSystemMetadata(peggym1271Sys);
        addSystemMetadata(peggym1281Sys);
        addSystemMetadata(peggym1291Sys);
        addSystemMetadata(peggym1304Sys);
        Thread.sleep(SLEEPTIME);
        processor.processIndexTaskQueue();
        addSystemMetadata(peggymResourcemapComplicatedSys);
        Thread.sleep(SLEEPTIME);
        processor.processIndexTaskQueue();
    }

    private void indexSecondComplicatedDataPackage() throws Exception {
        addSystemMetadata(peggym1271Sys);
        addSystemMetadata(peggym1281Sys);
        addSystemMetadata(peggym1291Sys);
        addSystemMetadata(peggym1304Sys);
        Thread.sleep(SLEEPTIME);
        processor.processIndexTaskQueue();
        addSystemMetadata(peggymResourcemap2ComplicatedSys);
        Thread.sleep(SLEEPTIME);
        processor.processIndexTaskQueue();
    }

    private void indexTestDataPackageWithArchived1271Doc() throws Exception {
        addSystemMetadata(peggym1271SysArchived);
        addSystemMetadata(peggym1281Sys);
        addSystemMetadata(peggym1291Sys);
        addSystemMetadata(peggym1304Sys);
        Thread.sleep(SLEEPTIME);
        processor.processIndexTaskQueue();
        addSystemMetadata(peggymResourcemapSys);
        Thread.sleep(SLEEPTIME);
        processor.processIndexTaskQueue();
    }

    private void verifyFirstOverlapDataPackageIndexed() throws Exception {
        Thread.sleep(SLEEPTIME);
        SolrDocument data = assertPresentInSolrIndex("peggym.127.1");
        Assert.assertEquals(1,
                ((List) data.getFieldValues(SolrElementField.FIELD_RESOURCEMAP)).size());
        Assert.assertEquals("peggym.resourcemap1-overlap",
                ((List) data.getFieldValues(SolrElementField.FIELD_RESOURCEMAP)).get(0));

        Assert.assertEquals(1,
                ((List) data.getFieldValues(SolrElementField.FIELD_ISDOCUMENTEDBY)).size());
        Assert.assertEquals("peggym.130.4",
                ((List) data.getFieldValue(SolrElementField.FIELD_ISDOCUMENTEDBY)).get(0));

        Assert.assertNull(data.getFieldValues(SolrElementField.FIELD_DOCUMENTS));

        SolrDocument scienceMetadata = assertPresentInSolrIndex("peggym.130.4");
        Assert.assertEquals(1,
                ((List) scienceMetadata.getFieldValues(SolrElementField.FIELD_RESOURCEMAP)).size());
        Assert.assertEquals("peggym.resourcemap1-overlap",
                ((List) scienceMetadata.getFieldValue(SolrElementField.FIELD_RESOURCEMAP)).get(0));

        Collection documentsCollection = scienceMetadata
                .getFieldValues(SolrElementField.FIELD_DOCUMENTS);
        Assert.assertEquals(1, documentsCollection.size());
        Assert.assertTrue(documentsCollection.contains("peggym.127.1"));

        assertPresentInSolrIndex("peggym.resourcemap1-overlap");
    }

    private void verifySecondOverlapDataPackageIndexed() throws Exception {
        Thread.sleep(SLEEPTIME);
        SolrDocument data = assertPresentInSolrIndex("peggym.127.1");
        Assert.assertEquals(1,
                ((List) data.getFieldValues(SolrElementField.FIELD_RESOURCEMAP)).size());
        Assert.assertEquals("peggym.resourcemap1-overlap",
                ((List) data.getFieldValues(SolrElementField.FIELD_RESOURCEMAP)).get(0));

        Assert.assertEquals(1,
                ((List) data.getFieldValues(SolrElementField.FIELD_ISDOCUMENTEDBY)).size());
        Assert.assertEquals("peggym.130.4",
                ((List) data.getFieldValue(SolrElementField.FIELD_ISDOCUMENTEDBY)).get(0));
        Assert.assertNull(data.getFieldValues(SolrElementField.FIELD_DOCUMENTS));

        data = assertPresentInSolrIndex("peggym.128.1");
        Assert.assertEquals(1,
                ((List) data.getFieldValues(SolrElementField.FIELD_RESOURCEMAP)).size());
        Assert.assertEquals("peggym.resourcemap2-overlap",
                ((List) data.getFieldValues(SolrElementField.FIELD_RESOURCEMAP)).get(0));
        Assert.assertEquals(1,
                ((List) data.getFieldValues(SolrElementField.FIELD_ISDOCUMENTEDBY)).size());
        Assert.assertEquals("peggym.130.4",
                ((List) data.getFieldValue(SolrElementField.FIELD_ISDOCUMENTEDBY)).get(0));
        Assert.assertNull(data.getFieldValues(SolrElementField.FIELD_DOCUMENTS));

        data = assertPresentInSolrIndex("peggym.129.1");
        Assert.assertEquals(1,
                ((List) data.getFieldValues(SolrElementField.FIELD_RESOURCEMAP)).size());
        Assert.assertEquals("peggym.resourcemap2-overlap",
                ((List) data.getFieldValues(SolrElementField.FIELD_RESOURCEMAP)).get(0));
        Assert.assertEquals(1,
                ((List) data.getFieldValues(SolrElementField.FIELD_ISDOCUMENTEDBY)).size());
        Assert.assertEquals("peggym.130.4",
                ((List) data.getFieldValue(SolrElementField.FIELD_ISDOCUMENTEDBY)).get(0));
        Assert.assertNull(data.getFieldValues(SolrElementField.FIELD_DOCUMENTS));

        SolrDocument scienceMetadata = assertPresentInSolrIndex("peggym.130.4");
        Assert.assertEquals(2,
                ((List) scienceMetadata.getFieldValues(SolrElementField.FIELD_RESOURCEMAP)).size());
        Assert.assertEquals("peggym.resourcemap1-overlap",
                ((List) scienceMetadata.getFieldValue(SolrElementField.FIELD_RESOURCEMAP)).get(0));
        Assert.assertEquals("peggym.resourcemap2-overlap",
                ((List) scienceMetadata.getFieldValue(SolrElementField.FIELD_RESOURCEMAP)).get(1));

        Collection documentsCollection = scienceMetadata
                .getFieldValues(SolrElementField.FIELD_DOCUMENTS);
        Assert.assertEquals(3, documentsCollection.size());
        Assert.assertTrue(documentsCollection.contains("peggym.127.1"));
        Assert.assertTrue(documentsCollection.contains("peggym.128.1"));
        Assert.assertTrue(documentsCollection.contains("peggym.129.1"));

        assertPresentInSolrIndex("peggym.resourcemap1-overlap");
        assertPresentInSolrIndex("peggym.resourcemap2-overlap");
    }

    private void verifyComplicatedDataPackageIndexed() throws Exception {
        Thread.sleep(SLEEPTIME);
        SolrDocument data = assertPresentInSolrIndex("peggym.127.1");
        Assert.assertEquals(1,
                ((List) data.getFieldValues(SolrElementField.FIELD_RESOURCEMAP)).size());
        Assert.assertEquals("peggym.resourcemap-complicated",
                ((List) data.getFieldValues(SolrElementField.FIELD_RESOURCEMAP)).get(0));
        Assert.assertEquals(1,
                ((List) data.getFieldValues(SolrElementField.FIELD_DOCUMENTS)).size());
        Assert.assertEquals("peggym.130.4",
                ((List) data.getFieldValue(SolrElementField.FIELD_DOCUMENTS)).get(0));
        Assert.assertNull(data.getFieldValues(SolrElementField.FIELD_ISDOCUMENTEDBY));

        data = assertPresentInSolrIndex("peggym.128.1");
        Assert.assertEquals(1,
                ((List) data.getFieldValues(SolrElementField.FIELD_RESOURCEMAP)).size());
        Assert.assertEquals("peggym.resourcemap-complicated",
                ((List) data.getFieldValues(SolrElementField.FIELD_RESOURCEMAP)).get(0));
        Assert.assertEquals(1,
                ((List) data.getFieldValues(SolrElementField.FIELD_ISDOCUMENTEDBY)).size());
        Assert.assertEquals("peggym.130.4",
                ((List) data.getFieldValue(SolrElementField.FIELD_ISDOCUMENTEDBY)).get(0));
        Assert.assertNull(data.getFieldValues(SolrElementField.FIELD_DOCUMENTS));

        data = assertPresentInSolrIndex("peggym.129.1");
        Assert.assertEquals(1,
                ((List) data.getFieldValues(SolrElementField.FIELD_RESOURCEMAP)).size());
        Assert.assertEquals("peggym.resourcemap-complicated",
                ((List) data.getFieldValues(SolrElementField.FIELD_RESOURCEMAP)).get(0));
        Assert.assertEquals(1,
                ((List) data.getFieldValues(SolrElementField.FIELD_ISDOCUMENTEDBY)).size());
        Assert.assertEquals("peggym.130.4",
                ((List) data.getFieldValue(SolrElementField.FIELD_ISDOCUMENTEDBY)).get(0));
        Assert.assertNull(data.getFieldValues(SolrElementField.FIELD_DOCUMENTS));

        SolrDocument scienceMetadata = assertPresentInSolrIndex("peggym.130.4");
        Assert.assertEquals(1,
                ((List) scienceMetadata.getFieldValues(SolrElementField.FIELD_RESOURCEMAP)).size());
        Assert.assertEquals("peggym.resourcemap-complicated",
                ((List) scienceMetadata.getFieldValue(SolrElementField.FIELD_RESOURCEMAP)).get(0));

        Collection documentsCollection = scienceMetadata
                .getFieldValues(SolrElementField.FIELD_DOCUMENTS);
        Assert.assertEquals(2, documentsCollection.size());
        Assert.assertTrue(documentsCollection.contains("peggym.128.1"));
        Assert.assertTrue(documentsCollection.contains("peggym.129.1"));
        Assert.assertEquals(1, ((List) scienceMetadata
                .getFieldValues(SolrElementField.FIELD_ISDOCUMENTEDBY)).size());
        Assert.assertEquals("peggym.127.1", ((List) scienceMetadata
                .getFieldValue(SolrElementField.FIELD_ISDOCUMENTEDBY)).get(0));

        assertPresentInSolrIndex("peggym.resourcemap-complicated");
    }

    private void verifySecondComplicatedDataPackageIndexed() throws Exception {
        Thread.sleep(SLEEPTIME);
        SolrDocument data = assertPresentInSolrIndex("peggym.127.1");
        Assert.assertEquals(2,
                ((List) data.getFieldValues(SolrElementField.FIELD_RESOURCEMAP)).size());
        Assert.assertEquals("peggym.resourcemap-complicated",
                ((List) data.getFieldValues(SolrElementField.FIELD_RESOURCEMAP)).get(0));
        Assert.assertEquals("peggym.resourcemap2-complicated",
                ((List) data.getFieldValues(SolrElementField.FIELD_RESOURCEMAP)).get(1));
        Assert.assertEquals(1,
                ((List) data.getFieldValues(SolrElementField.FIELD_DOCUMENTS)).size());
        Assert.assertEquals("peggym.130.4",
                ((List) data.getFieldValue(SolrElementField.FIELD_DOCUMENTS)).get(0));
        Assert.assertNull(data.getFieldValues(SolrElementField.FIELD_ISDOCUMENTEDBY));

        data = assertPresentInSolrIndex("peggym.128.1");
        Assert.assertEquals(2,
                ((List) data.getFieldValues(SolrElementField.FIELD_RESOURCEMAP)).size());
        Assert.assertEquals("peggym.resourcemap-complicated",
                ((List) data.getFieldValues(SolrElementField.FIELD_RESOURCEMAP)).get(0));
        Assert.assertEquals("peggym.resourcemap2-complicated",
                ((List) data.getFieldValues(SolrElementField.FIELD_RESOURCEMAP)).get(1));
        Assert.assertEquals(1,
                ((List) data.getFieldValues(SolrElementField.FIELD_ISDOCUMENTEDBY)).size());
        Assert.assertEquals("peggym.130.4",
                ((List) data.getFieldValue(SolrElementField.FIELD_ISDOCUMENTEDBY)).get(0));
        Assert.assertNull(data.getFieldValues(SolrElementField.FIELD_DOCUMENTS));

        data = assertPresentInSolrIndex("peggym.129.1");
        Assert.assertEquals(2,
                ((List) data.getFieldValues(SolrElementField.FIELD_RESOURCEMAP)).size());
        Assert.assertEquals("peggym.resourcemap-complicated",
                ((List) data.getFieldValues(SolrElementField.FIELD_RESOURCEMAP)).get(0));
        Assert.assertEquals("peggym.resourcemap2-complicated",
                ((List) data.getFieldValues(SolrElementField.FIELD_RESOURCEMAP)).get(1));
        Assert.assertEquals(1,
                ((List) data.getFieldValues(SolrElementField.FIELD_ISDOCUMENTEDBY)).size());
        Assert.assertEquals("peggym.130.4",
                ((List) data.getFieldValue(SolrElementField.FIELD_ISDOCUMENTEDBY)).get(0));
        Assert.assertNull(data.getFieldValues(SolrElementField.FIELD_DOCUMENTS));

        SolrDocument scienceMetadata = assertPresentInSolrIndex("peggym.130.4");
        Assert.assertEquals(2,
                ((List) scienceMetadata.getFieldValues(SolrElementField.FIELD_RESOURCEMAP)).size());
        Assert.assertEquals("peggym.resourcemap-complicated",
                ((List) scienceMetadata.getFieldValue(SolrElementField.FIELD_RESOURCEMAP)).get(0));
        Assert.assertEquals("peggym.resourcemap2-complicated",
                ((List) scienceMetadata.getFieldValues(SolrElementField.FIELD_RESOURCEMAP)).get(1));

        Collection documentsCollection = scienceMetadata
                .getFieldValues(SolrElementField.FIELD_DOCUMENTS);
        Assert.assertEquals(2, documentsCollection.size());
        Assert.assertTrue(documentsCollection.contains("peggym.128.1"));
        Assert.assertTrue(documentsCollection.contains("peggym.129.1"));
        Assert.assertEquals(1, ((List) scienceMetadata
                .getFieldValues(SolrElementField.FIELD_ISDOCUMENTEDBY)).size());
        Assert.assertEquals("peggym.127.1", ((List) scienceMetadata
                .getFieldValue(SolrElementField.FIELD_ISDOCUMENTEDBY)).get(0));

        assertPresentInSolrIndex("peggym.resourcemap-complicated");
        assertPresentInSolrIndex("peggym.resourcemap2-complicated");
    }

    private void verifyTestDataPackageIndexed() throws Exception {
        Thread.sleep(SLEEPTIME);
        SolrDocument data = assertPresentInSolrIndex("peggym.127.1");
        Assert.assertEquals(1,
                ((List) data.getFieldValues(SolrElementField.FIELD_RESOURCEMAP)).size());
        Assert.assertEquals("peggym.resourcemap",
                ((List) data.getFieldValues(SolrElementField.FIELD_RESOURCEMAP)).get(0));

        Assert.assertEquals(1,
                ((List) data.getFieldValues(SolrElementField.FIELD_ISDOCUMENTEDBY)).size());
        Assert.assertEquals("peggym.130.4",
                ((List) data.getFieldValue(SolrElementField.FIELD_ISDOCUMENTEDBY)).get(0));

        Assert.assertNull(data.getFieldValues(SolrElementField.FIELD_DOCUMENTS));

        assertPresentInSolrIndex("peggym.128.1");
        assertPresentInSolrIndex("peggym.129.1");

        SolrDocument scienceMetadata = assertPresentInSolrIndex("peggym.130.4");
        Assert.assertEquals(1,
                ((List) scienceMetadata.getFieldValues(SolrElementField.FIELD_RESOURCEMAP)).size());
        Assert.assertEquals("peggym.resourcemap",
                ((List) scienceMetadata.getFieldValue(SolrElementField.FIELD_RESOURCEMAP)).get(0));

        Collection documentsCollection = scienceMetadata
                .getFieldValues(SolrElementField.FIELD_DOCUMENTS);
        Assert.assertEquals(3, documentsCollection.size());
        Assert.assertTrue(documentsCollection.contains("peggym.127.1"));
        Assert.assertTrue(documentsCollection.contains("peggym.128.1"));
        Assert.assertTrue(documentsCollection.contains("peggym.129.1"));

        assertPresentInSolrIndex("peggym.resourcemap");
    }

    private void verifySecondTestDataPackageIndexed() throws Exception {
        Thread.sleep(SLEEPTIME);
        SolrDocument data = assertPresentInSolrIndex("peggym.127.1");
        Assert.assertEquals(2,
                ((List) data.getFieldValues(SolrElementField.FIELD_RESOURCEMAP)).size());
        Assert.assertEquals("peggym.resourcemap",
                ((List) data.getFieldValues(SolrElementField.FIELD_RESOURCEMAP)).get(0));
        Assert.assertEquals("peggym.resourcemap2",
                ((List) data.getFieldValues(SolrElementField.FIELD_RESOURCEMAP)).get(1));

        Assert.assertEquals(1,
                ((List) data.getFieldValues(SolrElementField.FIELD_ISDOCUMENTEDBY)).size());
        Assert.assertEquals("peggym.130.4",
                ((List) data.getFieldValue(SolrElementField.FIELD_ISDOCUMENTEDBY)).get(0));

        Assert.assertNull(data.getFieldValues(SolrElementField.FIELD_DOCUMENTS));

        data = assertPresentInSolrIndex("peggym.128.1");
        Assert.assertEquals(2,
                ((List) data.getFieldValues(SolrElementField.FIELD_RESOURCEMAP)).size());
        Assert.assertEquals("peggym.resourcemap",
                ((List) data.getFieldValues(SolrElementField.FIELD_RESOURCEMAP)).get(0));
        Assert.assertEquals("peggym.resourcemap2",
                ((List) data.getFieldValues(SolrElementField.FIELD_RESOURCEMAP)).get(1));

        Assert.assertEquals(1,
                ((List) data.getFieldValues(SolrElementField.FIELD_ISDOCUMENTEDBY)).size());
        Assert.assertEquals("peggym.130.4",
                ((List) data.getFieldValue(SolrElementField.FIELD_ISDOCUMENTEDBY)).get(0));

        Assert.assertNull(data.getFieldValues(SolrElementField.FIELD_DOCUMENTS));

        data = assertPresentInSolrIndex("peggym.129.1");
        Assert.assertEquals(2,
                ((List) data.getFieldValues(SolrElementField.FIELD_RESOURCEMAP)).size());
        Assert.assertEquals("peggym.resourcemap",
                ((List) data.getFieldValues(SolrElementField.FIELD_RESOURCEMAP)).get(0));
        Assert.assertEquals("peggym.resourcemap2",
                ((List) data.getFieldValues(SolrElementField.FIELD_RESOURCEMAP)).get(1));

        Assert.assertEquals(1,
                ((List) data.getFieldValues(SolrElementField.FIELD_ISDOCUMENTEDBY)).size());
        Assert.assertEquals("peggym.130.4",
                ((List) data.getFieldValue(SolrElementField.FIELD_ISDOCUMENTEDBY)).get(0));

        Assert.assertNull(data.getFieldValues(SolrElementField.FIELD_DOCUMENTS));

        SolrDocument scienceMetadata = assertPresentInSolrIndex("peggym.130.4");
        Assert.assertEquals(2,
                ((List) scienceMetadata.getFieldValues(SolrElementField.FIELD_RESOURCEMAP)).size());
        Assert.assertEquals("peggym.resourcemap",
                ((List) scienceMetadata.getFieldValue(SolrElementField.FIELD_RESOURCEMAP)).get(0));
        Assert.assertEquals("peggym.resourcemap2",
                ((List) scienceMetadata.getFieldValue(SolrElementField.FIELD_RESOURCEMAP)).get(1));

        Collection documentsCollection = scienceMetadata
                .getFieldValues(SolrElementField.FIELD_DOCUMENTS);
        Assert.assertEquals(3, documentsCollection.size());
        Assert.assertTrue(documentsCollection.contains("peggym.127.1"));
        Assert.assertTrue(documentsCollection.contains("peggym.128.1"));
        Assert.assertTrue(documentsCollection.contains("peggym.129.1"));

        assertPresentInSolrIndex("peggym.resourcemap");
        assertPresentInSolrIndex("peggym.resourcemap2");
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

    private void deleteSystemMetadata(Resource systemMetadataResource) {
        SystemMetadata sysmeta = null;
        try {
            sysmeta = TypeMarshaller.unmarshalTypeFromStream(SystemMetadata.class,
                    systemMetadataResource.getInputStream());
        } catch (Exception ex) {
            logger.error(ex.getMessage(), ex);
            fail("Test SystemMetadata misconfiguration - Exception " + ex);
        }
        HazelcastClientFactory.getSystemMetadataMap().remove(sysmeta.getIdentifier());
        HazelcastClientFactory.getObjectPathMap().removeAsync(sysmeta.getIdentifier());
        generator.processSystemMetaDataDelete(sysmeta);
    }

    public void setUp() throws Exception {
        super.setUp();
        configureSpringResources();
    }

    public void tearDown() throws Exception {
        super.tearDown();
    }

    @BeforeClass
    public static void init() {
        HazelcastClientFactoryTest.setUp();
    }

    @AfterClass
    public static void cleanup() throws Exception {
        HazelcastClientFactoryTest.shutDown();
    }

    private void configureSpringResources() {
        processor = (IndexTaskProcessor) context.getBean("indexTaskProcessor");
        generator = (IndexTaskGenerator) context.getBean("indexTaskGenerator");

        peggym1271Sys = (Resource) context.getBean("peggym1271Sys");
        peggym1271SysArchived = (Resource) context.getBean("peggym1271SysArchived");
        peggym1281Sys = (Resource) context.getBean("peggym1281Sys");
        peggym1291Sys = (Resource) context.getBean("peggym1291Sys");
        peggym1304Sys = (Resource) context.getBean("peggym1304Sys");
        peggym1304SysArchived = (Resource) context.getBean("peggym1304SysArchived");
        peggymResourcemapSys = (Resource) context.getBean("peggymResourcemapSys");
        peggymResourcemap2Sys = (Resource) context.getBean("peggymResourcemap2Sys");
        peggymResourcemapSysArchived = (Resource) context.getBean("peggymResourcemapSysArchived");
        peggymResourcemap2SysArchived = (Resource) context.getBean("peggymResourcemap2SysArchived");
        peggymResourcemapComplicatedSys = (Resource) context
                .getBean("peggymResourcemapComplicatedSys");
        peggymResourcemap2ComplicatedSys = (Resource) context
                .getBean("peggymResourcemap2ComplicatedSys");
        peggymResourcemap1OverlapSys = (Resource) context.getBean("peggymResourcemap1OverlapSys");
        peggymResourcemap2OverlapSys = (Resource) context.getBean("peggymResourcemap2OverlapSys");
    }

}
