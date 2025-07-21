package org.dataone.cn.indexer.annotation;

import static org.junit.Assert.*;

import org.dataone.cn.indexer.IndexWorkerTest;
import org.dataone.configuration.Settings;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.carrotsearch.randomizedtesting.annotations.ThreadLeakScope;

@ThreadLeakScope(ThreadLeakScope.Scope.NONE)
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "../../index/test-context.xml", "test-context-annotator.xml" })

/**
 * This test is basically empty at the moment. Most of the functionality of the
 * AnnotatorSubprocessor is handled by the OntologyModelService but it would be
 * good to test the remaining, untested methods.
 */
public class AnnotatorSubprocessorTest {

    @Autowired
    private AnnotatorSubprocessor annotatorSubprocessor;

    /**
     * Test the canProcess method
     * @throws Exception
     */
    @Test
    public void testCanProcess() throws Exception {
        Settings.augmentConfiguration(IndexWorkerTest.PORT_8985_PROPERTY_FILE_PATH);
        assertTrue(annotatorSubprocessor.canProcess("http://docs.annotatorjs.org/en/v1.2"
                   + ".x/annotation-format.html"));
    }
}
