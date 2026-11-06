package org.dataone.cn.indexer.annotation;

import java.io.InputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;


import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.lang3.StringUtils;
import org.dataone.cn.index.BaseSolrFieldXPathTest;
import org.dataone.cn.indexer.IndexWorkerTest;
import org.dataone.cn.indexer.solrhttp.SolrDoc;
import org.dataone.cn.indexer.solrhttp.SolrElementField;
import org.dataone.configuration.Settings;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.carrotsearch.randomizedtesting.annotations.ThreadLeakScope;

@ThreadLeakScope(ThreadLeakScope.Scope.NONE)
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "../../index/test-context.xml" })
public class SolrFieldEmlAnnotationTest extends BaseSolrFieldXPathTest {
    static {
        try {
            Settings.augmentConfiguration(IndexWorkerTest.PORT_8985_PROPERTY_FILE_PATH);
        } catch (ConfigurationException e) {
            throw new RuntimeException(e);
        }
    }

    @Autowired
    private Resource eml220TestDocSciMeta;

    @Autowired
    private EmlAnnotationSubprocessor emlAnnotationSubprocessor;

    // what are we expecting from the annotation?
    private HashMap<String, String> annotationExpected = new HashMap<String, String>();

    @Before
    public void setUp() throws Exception {
        // annotations should include the superclass[es]
        annotationExpected.put("sem_annotation",
            "http://www.w3.org/2002/07/owl#FunctionalProperty" + "||" +
            "http://ecoinformatics.org/oboe/oboe.1.2/oboe-core.owl#MeasurementType" + "||" +
            "http://purl.dataone.org/odo/ARCRC_00000040" + "||" +
            "http://ecoinformatics.org/oboe/oboe.1.2/oboe-core.owl#hasUnit" + "||" +
            "http://www.w3.org/2000/01/rdf-schema#Class" + "||" +
            "http://purl.dataone.org/odo/ECSO_00000629" + "||" +
            "http://purl.dataone.org/odo/ARCRC_00000048" + "||" +
            "http://ecoinformatics.org/oboe/oboe.1.2/oboe-core.owl#containsMeasurementsOfType" + "||" +
            "http://www.w3.org/2002/07/owl#Class" + "||" +
            "http://purl.dataone.org/odo/ECSO_00000518" + "||" +
            "http://purl.dataone.org/odo/ECSO_00000516" + "||" +
            "http://purl.obolibrary.org/obo/UO_0000301" + "||" +
            "http://purl.dataone.org/odo/ECSO_00000512" + "||" +
            "http://purl.dataone.org/odo/ARCRC_00000500" + "||" +
            "http://purl.dataone.org/odo/ECSO_00001102" + "||" +
            "http://www.w3.org/1999/02/22-rdf-syntax-ns#Property" + "||" +
            "http://purl.dataone.org/odo/ECSO_00001243" + "||" +
            "http://www.w3.org/2002/07/owl#ObjectProperty" + "||" +
            "http://www.w3.org/2002/07/owl#NamedIndividual" + "||" +
            "http://www.w3.org/2000/01/rdf-schema#Resource");
    }

    protected boolean compareFields(HashMap<String, String> expected, InputStream document,
            EmlAnnotationSubprocessor subProcessor, String identifier) throws Exception {

        Map<String, SolrDoc> docs = new TreeMap<String, SolrDoc>();
        Map<String, SolrDoc> solrDocs = subProcessor.processDocument(identifier, docs, document);
        List<SolrElementField> fields = solrDocs.get(identifier).getFieldList();

        // make sure our expected fields have the expected values
        for (SolrElementField field : fields) {
            String name = field.getName();

            // Assert we expected this field
            Assert.assertTrue(annotationExpected.containsKey(name));

            // Check the values
            String value = field.getValue();
            List<String> expectedValues = Arrays.asList(StringUtils.split(expected.get(name), "||"));
            Assert.assertTrue(expectedValues.contains(value));
        }

        return true;
    }

    /**
     * Testing that the annotation is parsed correctly
     *
     * @throws Exception
     */
    @Test
    public void testAnnotationFields() throws Exception {
        compareFields(
            annotationExpected, eml220TestDocSciMeta.getInputStream(), emlAnnotationSubprocessor,
            "eml_annotation_example");
    }

}
