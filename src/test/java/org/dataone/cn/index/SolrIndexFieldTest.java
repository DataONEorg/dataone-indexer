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

import org.apache.log4j.Logger;
import org.apache.solr.common.SolrDocument;
import org.dataone.cn.indexer.XmlDocumentUtility;
import org.dataone.cn.indexer.parser.BaseXPathDocumentSubprocessor;
import org.dataone.cn.indexer.parser.ISolrField;
import org.dataone.cn.indexer.parser.ScienceMetadataDocumentSubprocessor;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.springframework.core.io.Resource;
import org.w3c.dom.Document;

import com.carrotsearch.randomizedtesting.annotations.ThreadLeakScope;

/**
 * Solr unit test framework is dependent on JUnit 4.7. Later versions of junit
 * will break the base test classes.
 * 
 * @author sroseboo
 * 
 */
@ThreadLeakScope(ThreadLeakScope.Scope.NONE)
public class SolrIndexFieldTest extends DataONESolrJettyTestBase {

    protected static Logger logger = Logger.getLogger(SolrIndexFieldTest.class.getName());

    protected BaseXPathDocumentSubprocessor systemMetadata200Subprocessor;

    @Test
    public void testLooping() throws Exception {
        testComplexSystemMetadataAndFgdcScienceData();
        testComplexSystemMetadataAndFgdcScienceData();
        testComplexSystemMetadataAndFgdcScienceData();
        testComplexSystemMetadataAndFgdcScienceData();
        testComplexSystemMetadataAndFgdcScienceData();
    }

    @Test
    public void testComplexSystemMetadataAndFgdcScienceData() throws Exception {
        String pid = "68e96cf6-fb14-42aa-bbea-6da546ccb507-scan_201407_2172.xml";
        Resource systemMetadataResource = (Resource) context.getBean("fgdc_scan_Sys");
        Resource sciMetadataResource = (Resource) context.getBean("fgdc_scan_Sci");
        indexObjectToSolr(pid, sciMetadataResource);

        SolrDocument result = assertPresentInSolrIndex(pid);

        ScienceMetadataDocumentSubprocessor fgdcSubProcessor = (ScienceMetadataDocumentSubprocessor) context
                .getBean("fgdcstd00111999Subprocessor");

        Resource scienceMetadataResource = (Resource) context.getBean("fgdc_scan_Sci");
        Document scienceMetadataDoc = XmlDocumentUtility
                .generateXmlDocument(scienceMetadataResource.getInputStream());
        for (ISolrField field : fgdcSubProcessor.getFieldList()) {
            compareFields(result, scienceMetadataDoc, field, pid);
        }

        // test system metadata fields in system metadata config match those
        // in solr index document
        Document systemMetadataDoc = XmlDocumentUtility.generateXmlDocument(scienceMetadataResource
                .getInputStream());
        for (ISolrField field : systemMetadata200Subprocessor.getFieldList()) {
            compareFields(result, systemMetadataDoc, field, pid);
        }
    }

    @Test
    public void testSystemMetadataAndEml210ScienceData() throws Exception {
        // peggym.130.4 system metadata document for eml2.1.0 science metadata
        // document
        String pid = "peggym.130.4";
        Resource systemMetadataResource = (Resource) context.getBean("peggym1304Sys");
        Resource scienceResource = (Resource) context.getBean("peggym1304Sci");

        // add peggym.130.4 to solr index, using XPathDocumentParser (used by
        // index-task-processor)
        indexObjectToSolr(pid, scienceResource);

        // retrieve solrDocument for peggym130.4 from solr server by pid
        SolrDocument result = assertPresentInSolrIndex(pid);

        // test science metadata fields in eml210 config match actual fields in
        // solr index document
        ScienceMetadataDocumentSubprocessor eml210 = (ScienceMetadataDocumentSubprocessor) context
                .getBean("eml210Subprocessor");

        Resource scienceMetadataResource = (Resource) context.getBean("peggym1304Sci");
        Document scienceMetadataDoc = XmlDocumentUtility
                .generateXmlDocument(scienceMetadataResource.getInputStream());
        for (ISolrField field : eml210.getFieldList()) {
            compareFields(result, scienceMetadataDoc, field, pid);
        }

        // test system metadata fields in system metadata config match those
        // in solr index document
        Document systemMetadataDoc = XmlDocumentUtility.generateXmlDocument(systemMetadataResource
                .getInputStream());
        for (ISolrField field : systemMetadata200Subprocessor.getFieldList()) {
            compareFields(result, systemMetadataDoc, field, pid);
        }
    }

    @Test
    public void testSystemMetadataAndFgdcScienceData() throws Exception {
        String pid = "www.nbii.gov_metadata_mdata_CSIRO_csiro_d_abayadultprawns";
        Resource systemMetadataResource = (Resource) context.getBean("fdgc01111999SysMeta");
        Resource sciMetadataResource = (Resource) context.getBean("fdgc01111999SciMeta");
        indexObjectToSolr(pid, sciMetadataResource);

        SolrDocument result = assertPresentInSolrIndex(pid);

        ScienceMetadataDocumentSubprocessor fgdcSubProcessor = (ScienceMetadataDocumentSubprocessor) context
                .getBean("fgdcstd00111999Subprocessor");

        Resource scienceMetadataResource = (Resource) context.getBean("fdgc01111999SciMeta");
        Document scienceMetadataDoc = XmlDocumentUtility
                .generateXmlDocument(scienceMetadataResource.getInputStream());
        for (ISolrField field : fgdcSubProcessor.getFieldList()) {
            compareFields(result, scienceMetadataDoc, field, pid);
        }

        // test system metadata fields in system metadata config match those
        // in solr index document
        Document systemMetadataDoc = XmlDocumentUtility.generateXmlDocument(systemMetadataResource
                .getInputStream());
        for (ISolrField field : systemMetadata200Subprocessor.getFieldList()) {
            compareFields(result, systemMetadataDoc, field, pid);
        }
    }

    @Before
    public void setUp() throws Exception {
        super.setUp();
        systemMetadata200Subprocessor = (BaseXPathDocumentSubprocessor) context
                .getBean("systemMetadata200Subprocessor");
        sendSolrDeleteAll();
    }

    @After
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
}
