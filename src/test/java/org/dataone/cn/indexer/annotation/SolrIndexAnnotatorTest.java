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

/**
 * Solr unit test framework is dependent on JUnit 4.7. Later versions of junit
 * will break the base test classes.
 * 
 * @author sroseboo
 * 
 */
@ThreadLeakScope(ThreadLeakScope.Scope.NONE)
public class SolrIndexAnnotatorTest extends DataONESolrJettyTestBase {

    protected BaseXPathDocumentSubprocessor systemMetadata200Subprocessor;

    @Test
    public void testSystemMetadataEml210AndAnnotation() throws Exception {
        // peggym.130.4 system metadata document for eml2.1.0 science metadata
        // document
        String pid = "peggym.130.4";
        Resource systemMetadataResource = (Resource) context.getBean("peggym1304Sys");
        Resource scienceMetadataResource = (Resource) context.getBean("peggym1304Sci");

        // add peggym.130.4 to solr index, using XPathDocumentParser (used by
        // index-task-processor)
        indexObjectToSolr(pid, scienceMetadataResource);

        // retrieve solrDocument for peggym130.4 from solr server by pid
        SolrDocument result = assertPresentInSolrIndex(pid);

        // test science metadata fields in eml210 config match actual fields in
        // solr index document
        ScienceMetadataDocumentSubprocessor eml210 = (ScienceMetadataDocumentSubprocessor) context
                .getBean("eml210Subprocessor");

        
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

        //test the annotator fields
        Resource annotationSysMeta = (Resource) context.getBean("annotation1304Sys");
        Resource annotationResource = (Resource) context.getBean("annotation1304");
        assertNotNull("Annotation resource not found", annotationResource);

        //add the annotation
        indexObjectToSolr("annotation.130.4", annotationResource);

        result = assertPresentInSolrIndex(pid);
        for (String field : result.getFieldNames()) {
            System.out.println("FIELD NAME=" + field + ", VALUE=" + result.getFieldValue(field));
        }

        Collection<Object> annotationValues = result.getFieldValues("sem_annotation");
        //List<String> annotationValues = solrDoc.getAllFieldValues("sem_annotation");

        if (annotationValues != null && !annotationValues.isEmpty()) {
            for (Object annotationValue : annotationValues) {
                System.out.println("annotationValue: " + annotationValue);
            }
            assertFalse("sem_annotation should have multiple values", annotationValues.isEmpty());
        } else {
            fail("NO VALUES FOUND FOR sem_annotation!");
        }

        // check the fields in the science metadata again to make sure we did not overwrite them
        for (ISolrField field : eml210.getFieldList()) {
            compareFields(result, scienceMetadataDoc, field, pid);
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
}
