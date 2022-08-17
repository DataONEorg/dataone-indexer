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
 */

package org.dataone.cn.indexer.annotation;

import java.util.Collection;

import org.apache.solr.common.SolrDocument;
import org.dataone.cn.index.DataONESolrJettyTestBase;
import org.dataone.cn.indexer.XmlDocumentUtility;
import org.dataone.cn.indexer.parser.BaseXPathDocumentSubprocessor;
import org.dataone.cn.indexer.parser.ISolrField;
import org.dataone.cn.indexer.parser.ScienceMetadataDocumentSubprocessor;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.core.io.Resource;
import org.w3c.dom.Document;

import com.carrotsearch.randomizedtesting.annotations.ThreadLeakScope;

@ThreadLeakScope(ThreadLeakScope.Scope.NONE)
public class SolrIndexEmlAnnotationTest extends DataONESolrJettyTestBase {

    protected BaseXPathDocumentSubprocessor systemMetadata200Subprocessor;

    @Test
    public void testSystemMetadataEml220AndAnnotation() throws Exception {
        String pid = "eml-test-doc";
        Resource systemMetadataResource = (Resource) context.getBean("eml220TestDocSysMeta");
        Resource scienceMetadataResource = (Resource) context.getBean("eml220TestDocSciMeta");

        indexObjectToSolr(pid, scienceMetadataResource);
        SolrDocument result = assertPresentInSolrIndex(pid);

        // Compare EML 2.2.0 fields
        ScienceMetadataDocumentSubprocessor eml220 = (ScienceMetadataDocumentSubprocessor) context
                .getBean("eml220Subprocessor");

        Document scienceMetadataDoc = XmlDocumentUtility
                .generateXmlDocument(scienceMetadataResource.getInputStream());
                
        for (ISolrField field : eml220.getFieldList()) {
            compareFields(result, scienceMetadataDoc, field, pid);
        }

        // Compare System Metadata fields
        Document systemMetadataDoc = XmlDocumentUtility.generateXmlDocument(systemMetadataResource
                .getInputStream());
        for (ISolrField field : systemMetadata200Subprocessor.getFieldList()) {
            compareFields(result, systemMetadataDoc, field, pid);
        }
        
        // Test the annotation were extracted and expanded
        //
        // Manually assert the set of concepts. When I wrote this, I felt like I should use 
        // `compareFields` but I didn't see a natural way without changing the way other things
        // work so here I've just asserted the values directly
        Collection<Object> values = result.getFieldValues("sem_annotation");

        assertEquals(true, values.contains("http://purl.dataone.org/odo/ECSO_00000512"));
        assertEquals(true, values.contains(
            "http://ecoinformatics.org/oboe/oboe.1.2/oboe-core.owl#MeasurementType"
            ));
        assertEquals(true, values.contains("http://purl.dataone.org/odo/ECSO_00001243"));
        assertEquals(true, values.contains("http://purl.dataone.org/odo/ECSO_00000518"));
        assertEquals(true, values.contains("http://www.w3.org/2000/01/rdf-schema#Resource"));
        assertEquals(true, values.contains("http://purl.dataone.org/odo/ECSO_00000516"));
        assertEquals(true, values.contains("http://purl.obolibrary.org/obo/UO_0000301"));
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
}
