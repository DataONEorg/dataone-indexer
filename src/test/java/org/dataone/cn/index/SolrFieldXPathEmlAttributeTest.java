/**
 * This work was created by participants in the DataONE project, and is
 * jointly copyrighted by participating institutions in DataONE. For 
 * more information on DataONE, see our web site at http://dataone.org.
 *
 *   Copyright 2021
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
import java.util.HashMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.dataone.cn.hazelcast.HazelcastClientFactory;
//import org.dataone.cn.index.processor.IndexTaskProcessor;
import org.dataone.cn.indexer.parser.JsonLdSubprocessor;
import org.dataone.cn.indexer.parser.ScienceMetadataDocumentSubprocessor;
import org.dataone.cn.indexer.resourcemap.RdfXmlProcessorTest;
import org.dataone.service.types.v1.Identifier;
import org.dataone.service.types.v1.NodeReference;
import org.dataone.service.types.v2.SystemMetadata;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.core.io.Resource;

import com.carrotsearch.randomizedtesting.annotations.ThreadLeakScope;

/**
 * A junit test to test the xpath of attributes of EML for dataTables or otherEntities
 * @author tao
 *
 */
@ThreadLeakScope(ThreadLeakScope.Scope.NONE)
public class SolrFieldXPathEmlAttributeTest extends JsonLdSubprocessorTest {
    
    /* Log it */
    private static Log log = LogFactory.getLog( SolrFieldXPathEmlAttributeTest.class);

    /* The EML objects */
    private Resource emlWithDataTable;
    private Resource emlWithOtherEntity;

    /* An instance of the EML220 Subprocessor */
    private ScienceMetadataDocumentSubprocessor eml220Subprocessor;
    
    /* Store a map of expected Solr fields and their values for testing */
    private HashMap<String, String> expectedFields = new HashMap<String, String>();

    private static final int SLEEPTIME = 500;
    private static final int TIMES = 30;
    
    /**
     * For each test, set up the Solr service and test data
     * 
     * @throws Exception
     */
    @Before
    public void setUp() throws Exception {
        // Start up the embedded Jetty server and Solr service
        super.setUp();
        emlWithDataTable = (Resource) context.getBean("emlWithDataTableTestDoc");
        emlWithOtherEntity = (Resource) context.getBean("emlWithOtherEntityTestDoc");
        eml220Subprocessor = (ScienceMetadataDocumentSubprocessor) context.getBean("eml220Subprocessor");
    }

    /**
     * For each test, clean up, bring down the Solr service
     */
    @After
    public void tearDown() throws Exception {
        super.tearDown();
    }
    
    /**
     * Test the end to end index processing an EML document with a data table
     *
     * @throws Exception
     */
    //@Ignore
    @Test
    public void testInsertEmlWithDataTable() throws Exception {
        /* variables used to populate system metadata for each resource */
        File object = null;
        String formatId = null;

        NodeReference nodeid = new NodeReference();
        nodeid.setValue("urn:node:mnTestXXXX");

        String userDN = "uid=tester,o=testers,dc=dataone,dc=org";

        // Insert the schema.org file into the task queue
        String id = "urn:uuid:4ad48407-8044-4f4a-9596-18e9cb221656";
        formatId = "https://eml.ecoinformatics.org/eml-2.2.0";
        insertResource(id, formatId, emlWithDataTable, nodeid, userDN);
        Identifier identifier = new Identifier();
        SystemMetadata sysmeta = HazelcastClientFactory.getSystemMetadataMap().get(identifier);
        int count = 0;
        while (sysmeta == null && count < TIMES) {
            count ++;
            Thread.sleep(SLEEPTIME);
            sysmeta = HazelcastClientFactory.getSystemMetadataMap().get(identifier);
        }
        
        // now process the tasks
        //processor.processIndexTaskQueue();
        count = 0;
        while (count < TIMES) {
            try {
                assertTrue(compareFieldValue(id, "title", "Chum salmon escapement on Bonanza River in Norton Sound, Alaska"));
                break;
            } catch (Exception e) {
                count ++;
                Thread.sleep(SLEEPTIME);
            }
        }
        assertPresentInSolrIndex(id);
        assertTrue(compareFieldValue(id, "title", "Chum salmon escapement on Bonanza River in Norton Sound, Alaska"));
        String[] projects = {"Chum salmon escapement on Bonanza River in Norton Sound, Alaska"};
        assertTrue(compareFieldValue(id, "project", projects));
        String[] attributeNames = {"Species","Sp code","Sample Date","GumCard #","Fish #","External Sex","METF Length", "Comments", 
                                    "ageFresh", "ageSalt", "ageErrorID"};
        assertTrue(compareFieldValue(id, "attributeName", attributeNames));
        String[] attributeDescriptions = {"Species of salmon sampled","ADF&G species codes","Date that a salmon was collected",
                                        "Scale gum card identifier","Fish identifier/number",
                                        "Code which represents the sex of a sampled salmon. Sex was determined using external features.",
                                        "Mid-eye to fork of tail fish length measurement","comments about sample",
                                        "Freshwater age of fish in years","Saltwater age of fish in years","Source of error in age estimate"};
        assertTrue(compareFieldValue(id, "attributeDescription", attributeDescriptions));
        String[] attributeUnits = {"millimeter","dimensionless","dimensionless"};
        assertTrue(compareFieldValue(id, "attributeUnit", attributeUnits));
        String[] attributes = {"Species  Species of salmon sampled","Sp code  ADF&G species codes","Sample Date  Date that a salmon was collected",
                                "GumCard #  Scale gum card identifier","Fish #  Fish identifier/number",
                                "External Sex  Code which represents the sex of a sampled salmon. Sex was determined using external features.",
                                "METF Length  Mid-eye to fork of tail fish length measurement millimeter","Comments  comments about sample",
                                "ageFresh  Freshwater age of fish in years dimensionless","ageSalt  Saltwater age of fish in years dimensionless",
                                "ageErrorID  Source of error in age estimate"};
        assertTrue(compareFieldValue(id, "attribute", attributes));
    } 
    
    /**
     * Test the end to end index processing an EML document with other entities
     *
     * @throws Exception
     */
    @Test
    public void testInsertEmlWithOtherEntity() throws Exception {
        File object = null;
        String formatId = null;

        NodeReference nodeid = new NodeReference();
        nodeid.setValue("urn:node:mnTestXXXX");

        String userDN = "uid=tester,o=testers,dc=dataone,dc=org";

        // Insert the schema.org file into the task queue
        String id = "urn:uuid:4ad48407-8044-4f4a-9596-18e9cb221658";
        formatId = "https://eml.ecoinformatics.org/eml-2.2.0";
        insertResource(id, formatId, emlWithOtherEntity, nodeid, userDN);
        Identifier identifier = new Identifier();
        SystemMetadata sysmeta = HazelcastClientFactory.getSystemMetadataMap().get(identifier);
        int count = 0;
        while (sysmeta == null && count < TIMES) {
            count ++;
            Thread.sleep(SLEEPTIME);
            sysmeta = HazelcastClientFactory.getSystemMetadataMap().get(identifier);
        }
        
        // now process the tasks
        //processor.processIndexTaskQueue();
        count = 0;
        while (count < TIMES) {
            try {
                assertTrue(compareFieldValue(id, "title", "Chum salmon escapement on Bonanza River in Norton Sound, Alaska"));
                break;
            } catch (Exception e) {
                count ++;
                Thread.sleep(SLEEPTIME);
            }
        }
        assertPresentInSolrIndex(id);
        assertTrue(compareFieldValue(id, "title", "Chum salmon escapement on Bonanza River in Norton Sound, Alaska"));
        String[] projects = {"Chum salmon escapement on Bonanza River in Norton Sound, Alaska"};
        assertTrue(compareFieldValue(id, "project", projects));
        String[] attributeNames = {"Species","Sp code","Sample Date","GumCard #","Fish #","External Sex","METF Length", "Comments", 
                                    "ageFresh", "ageSalt", "ageErrorID"};
        assertTrue(compareFieldValue(id, "attributeName", attributeNames));
        String[] attributeDescriptions = {"Species of salmon sampled","ADF&G species codes","Date that a salmon was collected",
                                        "Scale gum card identifier","Fish identifier/number",
                                        "Code which represents the sex of a sampled salmon. Sex was determined using external features.",
                                        "Mid-eye to fork of tail fish length measurement","comments about sample",
                                        "Freshwater age of fish in years","Saltwater age of fish in years","Source of error in age estimate"};
        assertTrue(compareFieldValue(id, "attributeDescription", attributeDescriptions));
        String[] attributeUnits = {"millimeter","dimensionless", "dimensionless"};
        assertTrue(compareFieldValue(id, "attributeUnit", attributeUnits));
        String[] attributes = {"Species  Species of salmon sampled","Sp code  ADF&G species codes","Sample Date  Date that a salmon was collected",
                                "GumCard #  Scale gum card identifier","Fish #  Fish identifier/number",
                                "External Sex  Code which represents the sex of a sampled salmon. Sex was determined using external features.",
                                "METF Length  Mid-eye to fork of tail fish length measurement millimeter","Comments  comments about sample",
                                "ageFresh  Freshwater age of fish in years dimensionless","ageSalt  Saltwater age of fish in years dimensionless",
                                "ageErrorID  Source of error in age estimate"};
        assertTrue(compareFieldValue(id, "attribute", attributes));
    }
    
    /**
     * Overwrite a super class to do nothing.
     */
    @Test
    public void testInsertSchemaNormalization() throws Exception {
        
    }

}
