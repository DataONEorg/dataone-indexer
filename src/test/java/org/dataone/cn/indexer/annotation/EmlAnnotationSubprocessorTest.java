package org.dataone.cn.indexer.annotation;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.carrotsearch.randomizedtesting.annotations.ThreadLeakScope;

import org.dataone.cn.indexer.solrhttp.SolrDoc;
import org.dataone.cn.indexer.solrhttp.SolrElementField;
import org.springframework.core.io.Resource;


@ThreadLeakScope(ThreadLeakScope.Scope.NONE)
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "../../index/test-context.xml" })

public class EmlAnnotationSubprocessorTest {
    private String FIELD_NAME = "sem_annotation";

    @Autowired
    private EmlAnnotationSubprocessor emlAnnotationSubprocessor;

    @Autowired
    private Resource eml220TestDocSciMeta;

    @Test
    public void canProcessEml220Docs() {
        assertNotNull(emlAnnotationSubprocessor);
        assertEquals(true, emlAnnotationSubprocessor.canProcess("https://eml.ecoinformatics.org/eml-2.2.0"));
    }

    @Test
    public void testConceptExpansion() {
        try {
            String identifier = "eml-annotation-example";
            Map<String, SolrDoc> docs = new HashMap<String, SolrDoc>();

            SolrDoc solrDoc = new SolrDoc();
            docs.put(identifier, solrDoc);

            Map<String, SolrDoc> solrDocs = emlAnnotationSubprocessor.processDocument(identifier, docs, eml220TestDocSciMeta.getInputStream());

            // Pull out the expanded concepts for asserting on
            List<String> expandedConcepts = new ArrayList<String>();

            for (SolrElementField field : solrDocs.get(identifier).getFieldList()) {
                expandedConcepts.add(field.getValue());
            }

            // Set up expected concepts and assert
            List<String> expectedConcepts = new ArrayList<String>();

            expectedConcepts.add("http://www.w3.org/2002/07/owl#FunctionalProperty");
            expectedConcepts.add("http://ecoinformatics.org/oboe/oboe.1.2/oboe-core.owl#MeasurementType");
            expectedConcepts.add("http://purl.dataone.org/odo/ARCRC_00000040");
            expectedConcepts.add("http://ecoinformatics.org/oboe/oboe.1.2/oboe-core.owl#hasUnit");
            expectedConcepts.add("http://www.w3.org/2000/01/rdf-schema#Class");
            expectedConcepts.add("http://purl.dataone.org/odo/ECSO_00000629");
            expectedConcepts.add("http://purl.dataone.org/odo/ARCRC_00000048");
            expectedConcepts.add("http://ecoinformatics.org/oboe/oboe.1.2/oboe-core.owl#containsMeasurementsOfType");
            expectedConcepts.add("http://www.w3.org/2002/07/owl#Class");
            expectedConcepts.add("http://purl.dataone.org/odo/ECSO_00000518");
            expectedConcepts.add("http://purl.dataone.org/odo/ECSO_00000516");
            expectedConcepts.add("http://purl.obolibrary.org/obo/UO_0000301");
            expectedConcepts.add("http://purl.dataone.org/odo/ECSO_00000512");
            expectedConcepts.add("http://purl.dataone.org/odo/ARCRC_00000500");
            expectedConcepts.add("http://purl.dataone.org/odo/ECSO_00001102");
            expectedConcepts.add("http://www.w3.org/1999/02/22-rdf-syntax-ns#Property");
            expectedConcepts.add("http://purl.dataone.org/odo/ECSO_00001243");
            expectedConcepts.add("http://www.w3.org/2002/07/owl#ObjectProperty");
            expectedConcepts.add("http://www.w3.org/2002/07/owl#NamedIndividual");
            expectedConcepts.add("http://www.w3.org/2000/01/rdf-schema#Resource");

            assertEquals(1, solrDocs.size());
            assertEquals(expectedConcepts, expandedConcepts);
        } catch (Exception e) {
            e.printStackTrace();
            fail(e.getMessage());
        }
    }
}
