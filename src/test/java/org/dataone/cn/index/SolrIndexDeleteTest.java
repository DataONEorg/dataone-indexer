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
public class SolrIndexDeleteTest extends DataONESolrJettyTestBase {

    private static Logger logger = Logger.getLogger(SolrIndexDeleteTest.class.getName());

    //private IndexTaskProcessor processor;
    //private IndexTaskGenerator generator;

    private Resource peggym1271Sci;
    private String pid1271 = "peggym.127.1";
    private Resource peggym1271SysArchived;
    private Resource peggym1281Sci;
    private String pid1281 = "peggym.128.1";
    private Resource peggym1291Sci;
    private String pid1291 = "peggym.129.1";
    private Resource peggym1304Sci;
    private String pid1304 = "peggym.130.4";
    private Resource peggymResourcemapSci;
    private String peggyResourcemapId = "peggym.resourcemap";
    private Resource peggymResourcemap2Sci;
    private String peggyResourcemapId2 = "peggym.resourcemap2";
    private Resource peggymResourcemapComplicatedSci;
    private String peggymResourcemapComplicatedId = "peggym.resourcemap-complicated";
    private Resource peggymResourcemap2ComplicatedSci;
    private String peggymResourcemapComplicatedId2 = "peggym.resourcemap2-complicated";
    private Resource peggymResourcemap1OverlapSci;
    private String peggymResourcemap1OverlapSciId = "peggym.resourcemap1-overlap";
    private Resource peggymResourcemap2OverlapSci;
    private String peggymResourcemap2OverlapSciId = "peggym.resourcemap2-overlap";
    
    private static final int SLEEPTIME = 1000;


    /**
     * Unit test of the HTTPService.sendSolrDelete(pid) method. Inserts record
     * into solr index using XPathDocumentParser. Does not use index task
     * generation/processing.
     **/
    @Test
    public void testHttpServiceSolrDelete() throws Exception {
        String pid = "peggym.130.4";
        Resource scienceMetadataResource = (Resource) context.getBean("peggym1304Sci");
        indexObjectToSolr(pid, scienceMetadataResource);
        Thread.sleep(SLEEPTIME);
        assertPresentInSolrIndex(pid);
        deleteSolrDoc(pid);
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
    public void testDeleteDataInPackage() throws Exception {
        // create/index data package
        indexTestDataPackage();
        // verify in index correct
        verifyTestDataPackageIndexed();
        // remove a data object with id 1271
        deleteSolrDoc(pid1271);
        Thread.sleep(SLEEPTIME);
        //processor.processIndexTaskQueue();
        // verify data package info correct in index
        verifyDataPackageNo1271();
    }

    /**
     * Same scenario as testArchiveDataInPackage, but this time the file removed
     * is the science metadata document.
     * 
     * @throws Exception
     */
    @Test
    public void testDeleteScienceMetadataInPackage() throws Exception {
        // create/index data package
        indexTestDataPackage();
        // verify in index correct
        verifyTestDataPackageIndexed();
        deleteSolrDoc(pid1304);
        Thread.sleep(SLEEPTIME);
        //processor.processIndexTaskQueue();
        // verify data package info correct in index
        verifyDataPackageNo1304();
    }

    /**
     * Same scenario as testArchiveDataInPackage, but this time the data package
     * document itself is removed. This time the science metadata document is
     * updated and then the contents of the archived are verified.
     * 
     * @throws Exception
     */
    @Test
    public void testDeleteDataPackage() throws Exception {
        // create/index data package
        indexTestDataPackage();
        // verify in index correct
        verifyTestDataPackageIndexed();
        deleteSolrDoc(peggyResourcemapId);
        Thread.sleep(SLEEPTIME);
        //processor.processIndexTaskQueue();
        // verify data package info correct in index
        verifyDataPackageNoResourceMap();
        // update package object (resource map)
        indexObjectToSolr(pid1304, peggym1304Sci);
        Thread.sleep(SLEEPTIME);
        // verify again
        verifyDataPackageNoResourceMap();
    }


    /**
     * Test to delete a data package while there is another package specifies
     * the same relationship
     */
    @Test
    public void testDeleteDataPackageWithDuplicatedRelationship() throws Exception {
        // create/index data package
        indexTestDataPackage();
        //verify in index correct
        verifyTestDataPackageIndexed();
        indexSecondTestDataPackage();
        verifySecondTestDataPackageIndexed();
        deleteSolrDoc(peggyResourcemapId2);
        Thread.sleep(SLEEPTIME);
        verifyTestDataPackageIndexed();
        assertNotPresentInSolrIndex(peggyResourcemapId2);
        deleteSolrDoc(peggyResourcemapId);
        Thread.sleep(SLEEPTIME);
        //processor.processIndexTaskQueue();
        // verify data package info correct in index
        verifyDataPackageNoResourceMap();
        assertNotPresentInSolrIndex(peggyResourcemapId);
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
        indexComplicatedDataPackage();
        verifyComplicatedDataPackageIndexed();
        indexSecondComplicatedDataPackage();
        verifySecondComplicatedDataPackageIndexed();
        deleteSolrDoc(peggymResourcemapComplicatedId2);
        Thread.sleep(SLEEPTIME);
        //processor.processIndexTaskQueue();
        // verify data package info correct in index.
        // we removed the second one. So it will recover 
        // to status that only has one resource map
        verifyComplicatedDataPackageIndexed();
        assertNotPresentInSolrIndex(peggymResourcemapComplicatedId2);
        deleteSolrDoc(peggymResourcemapComplicatedId);
        Thread.sleep(SLEEPTIME);
        //processor.processIndexTaskQueue();
        // verify data package info correct in index
        verifyDataPackageNoResourceMap();
        assertNotPresentInSolrIndex(peggymResourcemapComplicatedId);
    }

    /**
     * Two data packages:
     * The first one - peggym.resourcemap1-overlap: peggym.130.4 documents peggym.127.1
     * The second one - peggym.resourcemap2-overlap: peggym.130.4 documents peggym.128.1 and peggym.129.1. 
     * @throws Exception
     */
    @Test
    public void testDeleteTwoOverlappedDataPackage() throws Exception {
        indexFirstOverlapDataPackage();
        verifyFirstOverlapDataPackageIndexed();
        indexSecondOverlapDataPackage();
        verifySecondOverlapDataPackageIndexed();
        deleteSolrDoc(peggymResourcemap2OverlapSciId);
        //processor.processIndexTaskQueue();
        verifyFirstOverlapDataPackageIndexed();
        assertNotPresentInSolrIndex(peggymResourcemap2OverlapSciId);
        deleteSolrDoc(peggymResourcemap1OverlapSciId);
        //processor.processIndexTaskQueue();
        verifyDataPackageNoResourceMap();
        assertNotPresentInSolrIndex(peggymResourcemap1OverlapSciId);
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
        indexObjectToSolr(pid1271, peggym1271Sci);
        indexObjectToSolr(pid1304, peggym1304Sci);
        Thread.sleep(SLEEPTIME);
        //processor.processIndexTaskQueue();
        indexObjectToSolr(peggymResourcemap1OverlapSciId, peggymResourcemap1OverlapSci);
        Thread.sleep(SLEEPTIME);
        //processor.processIndexTaskQueue();
    }

    private void indexSecondOverlapDataPackage() throws Exception {
        indexObjectToSolr(pid1281, peggym1281Sci);
        indexObjectToSolr(pid1291, peggym1291Sci);
        indexObjectToSolr(pid1304, peggym1304Sci);
        Thread.sleep(SLEEPTIME);
        //processor.processIndexTaskQueue();
        indexObjectToSolr(peggymResourcemap2OverlapSciId, peggymResourcemap2OverlapSci);
        Thread.sleep(SLEEPTIME);
        //processor.processIndexTaskQueue();
    }

    private void indexTestDataPackage() throws Exception {
        indexObjectToSolr(pid1271, peggym1271Sci);
        indexObjectToSolr(pid1281, peggym1281Sci);
        indexObjectToSolr(pid1291, peggym1291Sci);
        indexObjectToSolr(pid1304, peggym1304Sci);
        Thread.sleep(SLEEPTIME);
        //processor.processIndexTaskQueue();
        indexObjectToSolr(peggyResourcemapId, peggymResourcemapSci);
        Thread.sleep(4*SLEEPTIME);
        //processor.processIndexTaskQueue();
    }

    private void indexSecondTestDataPackage() throws Exception {
        indexObjectToSolr(pid1271, peggym1271Sci);
        indexObjectToSolr(pid1281, peggym1281Sci);
        indexObjectToSolr(pid1291, peggym1291Sci);
        indexObjectToSolr(pid1304, peggym1304Sci);
        Thread.sleep(SLEEPTIME);
        //processor.processIndexTaskQueue();
        indexObjectToSolr(peggyResourcemapId2, peggymResourcemap2Sci);
        Thread.sleep(SLEEPTIME);
        //processor.processIndexTaskQueue();
    }

    private void indexComplicatedDataPackage() throws Exception {
        indexObjectToSolr(pid1271, peggym1271Sci);
        indexObjectToSolr(pid1281, peggym1281Sci);
        indexObjectToSolr(pid1291, peggym1291Sci);
        indexObjectToSolr(pid1304, peggym1304Sci);
        Thread.sleep(SLEEPTIME);
        indexObjectToSolr(peggymResourcemapComplicatedId, peggymResourcemapComplicatedSci);
        Thread.sleep(SLEEPTIME);
    }

    private void indexSecondComplicatedDataPackage() throws Exception {
        indexObjectToSolr(pid1271, peggym1271Sci);
        indexObjectToSolr(pid1281, peggym1281Sci);
        indexObjectToSolr(pid1291, peggym1291Sci);
        indexObjectToSolr(pid1304, peggym1304Sci);
        Thread.sleep(SLEEPTIME);
        //processor.processIndexTaskQueue();
        indexObjectToSolr(peggymResourcemapComplicatedId2, peggymResourcemap2ComplicatedSci);
        Thread.sleep(SLEEPTIME);
        //processor.processIndexTaskQueue();
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
        //System.out.println("+++++++++++++++++++++\n" + data.toString());
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
        //HazelcastClientFactory.getSystemMetadataMap().put(sysmeta.getIdentifier(), sysmeta);
        //sysMetaMap.put(sysmeta.getIdentifier(), sysmeta);
        //HazelcastClientFactory.getObjectPathMap().putAsync(sysmeta.getIdentifier(), path);
        //generator.processSystemMetaDataUpdate(sysmeta, path);
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
        //HazelcastClientFactory.getSystemMetadataMap().remove(sysmeta.getIdentifier());
        //HazelcastClientFactory.getObjectPathMap().removeAsync(sysmeta.getIdentifier());
        //generator.processSystemMetaDataDelete(sysmeta);
    }

    public void setUp() throws Exception {
        super.setUp();
        configureSpringResources();
        sendSolrDeleteAll();
    }

    public void tearDown() throws Exception {
        super.tearDown();
    }

    @BeforeClass
    public static void init() {
        //HazelcastClientFactoryTest.setUp();
    }

    @AfterClass
    public static void cleanup() throws Exception {
        //HazelcastClientFactoryTest.shutDown();
    }

    private void configureSpringResources() {
        //processor = (IndexTaskProcessor) context.getBean("indexTaskProcessor");
        //generator = (IndexTaskGenerator) context.getBean("indexTaskGenerator");

        peggym1271Sci = (Resource) context.getBean("peggym1271Sci");
        peggym1271SysArchived = (Resource) context.getBean("peggym1271SysArchived");
        peggym1281Sci = (Resource) context.getBean("peggym1281Sci");
        peggym1291Sci = (Resource) context.getBean("peggym1291Sci");
        peggym1304Sci = (Resource) context.getBean("peggym1304Sci");
        peggymResourcemapSci = (Resource) context.getBean("peggymResourcemapSci");
        peggymResourcemap2Sci = (Resource) context.getBean("peggymResourcemap2Sci");
        peggymResourcemapComplicatedSci = (Resource) context
                .getBean("peggymResourcemapComplicatedSci");
        peggymResourcemap2ComplicatedSci = (Resource) context
                .getBean("peggymResourcemap2ComplicatedSci");
        peggymResourcemap1OverlapSci = (Resource) context.getBean("peggymResourcemap1OverlapSci");
        peggymResourcemap2OverlapSci = (Resource) context.getBean("peggymResourcemap2OverlapSci");
    }

}
