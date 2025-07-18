package org.dataone.cn.indexer.annotation;

import java.io.InputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.commons.lang3.StringUtils;
import org.dataone.cn.index.BaseSolrFieldXPathTest;
import org.dataone.cn.indexer.IndexWorkerTest;
import org.dataone.cn.indexer.convert.SolrDateConverter;
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
@ContextConfiguration(locations = { "../../index/test-context.xml", "test-context-annotator.xml" })
public class SolrFieldAnnotatorTest extends BaseSolrFieldXPathTest {

    @Autowired
    private Resource annotation1304;

    @Autowired
    private AnnotatorSubprocessor annotatorSubprocessor;

    private SolrDateConverter dateConverter = new SolrDateConverter();

    // what are we expecting from the annotation?
    private HashMap<String, String> annotationExpected = new HashMap<String, String>();

    @Before
    public void setUp() throws Exception {
        Settings.augmentConfiguration(IndexWorkerTest.PORT_8985_PROPERTY_FILE_PATH);
        // annotations should include the superclass[es]
        annotationExpected
            .put(AnnotatorSubprocessor.FIELD_ANNOTATION,
                    "http://ecoinformatics.org/oboe/oboe.1.0/oboe-characteristics.owl#Mass" +
                    "||" +
                    "http://ecoinformatics.org/oboe/oboe.1.0/oboe-core.owl#PhysicalCharacteristic" +
                    "||" +
                    "http://ecoinformatics.org/oboe/oboe.1.0/oboe-core.owl#Characteristic" +
                    "||" +
                    "http://www.w3.org/2000/01/rdf-schema#Resource");
        annotationExpected.put(AnnotatorSubprocessor.FIELD_COMMENT, "Original annotation content");

        // relationships
        annotationExpected.put(AnnotatorSubprocessor.FIELD_ANNOTATED_BY, "annotation.130.4");

        // system metadata
        annotationExpected.put(SolrElementField.FIELD_ID, "peggym.130.4");

        annotationExpected.put(
            "formatId", "http://docs.annotatorjs.org/en/v1.2.x/annotation-format.html");
        annotationExpected.put("formatType", "METADATA");
        annotationExpected.put("size", "855");
        annotationExpected.put("checksum", "df89097223d1afe36cad3d1e2bdda4fd");
        annotationExpected.put("checksumAlgorithm", "MD5");
        annotationExpected.put(
            "submitter",
            "CN=Benjamin Leinfelder A515,O=University of Chicago,C=US,DC=cilogon,DC=org");
        annotationExpected.put(
            "rightsHolder",
            "CN=Benjamin Leinfelder A515,O=University of Chicago,C=US,DC=cilogon,DC=org");
        annotationExpected.put("replicationAllowed", "true");
        annotationExpected.put("numberReplicas", "");
        annotationExpected.put("preferredReplicationMN", "");
        annotationExpected.put("blockedReplicationMN", "");
        annotationExpected.put("dateUploaded", dateConverter.convert("2014-12-03T23:29:20.262152"));
        annotationExpected.put("dateModified", dateConverter.convert("2014-12-03T23:29:20.262152"));
        annotationExpected.put("datasource", "urn:node:KNB");
        annotationExpected.put("authoritativeMN", "urn:node:KNB");
        annotationExpected.put("replicaMN", "");
        annotationExpected.put("replicaVerifiedDate", "");
        annotationExpected.put("readPermission", "public");
        annotationExpected.put("changePermission", "");
        annotationExpected.put("isPublic", "true");

    }

    protected boolean compareFields(HashMap<String, String> expected, InputStream annotation,
                                    AnnotatorSubprocessor subProcessor, String identifier,
                                    String referencedpid) throws Exception {

        Map<String, SolrDoc> docs = new TreeMap<String, SolrDoc>();
        Map<String, SolrDoc> solrDocs = subProcessor.processDocument(identifier, docs, annotation);
        List<SolrElementField> fields = solrDocs.get(referencedpid).getFieldList();
        
        // make sure our expected fields have the expected values
        for (SolrElementField docField : fields) {
            String name = docField.getName();
            String value = docField.getValue();
            
            String expectedValue = expected.get(name);
            if (expectedValue != null) {
                List<String> expectedValues = Arrays.asList(StringUtils.split(expectedValue , "||"));
                if (expectedValues != null && !expectedValues.isEmpty()) {
                        Assert.assertTrue(expectedValues.contains(value));
                }
            }
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
            annotationExpected, annotation1304.getInputStream(), annotatorSubprocessor,
            "annotation.130.4", "peggym.130.4");
    }

}
