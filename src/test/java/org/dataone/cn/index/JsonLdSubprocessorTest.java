/**
 * This work was crfield name: eated" by participants in the DataONE project, and is
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
import java.io.FileInputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.client.solrj.util.ClientUtils;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.params.ModifiableSolrParams;
import org.dataone.cn.indexer.parser.JsonLdSubprocessor;
import org.dataone.cn.indexer.resourcemap.RdfXmlProcessorTest;
import org.dataone.service.types.v1.NodeReference;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.core.io.Resource;

import com.carrotsearch.randomizedtesting.annotations.ThreadLeakScope;
import com.github.jsonldjava.core.JsonLdProcessor;
import com.github.jsonldjava.utils.JsonUtils;

/**
 * Test the json-ld subprocessor
 * @author tao
 *
 */
@ThreadLeakScope(ThreadLeakScope.Scope.NONE)
public class JsonLdSubprocessorTest extends DataONESolrJettyTestBase {
    
    /* Log it */
    private static Log log = LogFactory.getLog(JsonLdSubprocessorTest.class);

    /* The schema.org object */
    private Resource schemaOrgDoc;
    private String schemaOrgDocPid = "bco-dmo.jsonld";
    private Resource schemaOrgDoc2;
    private String schemaOrgDoc2Pid = "doi_A10.5061_dryad.m8s2r36.jsonld";
    private Resource schemaOrgDocSOSO;
    private String schemaOrgDocSOSOPid = "ESIP-SOSO-v1.2.0-example-full.jsonld";
    private Resource schemaOrgTestWithoutVocab;
    private String schemaOrgTestWithoutVocabPid = "context-http-without-vocab.jsonld";
    private Resource schemaOrgTestDocHttpVocab;
    private String schemaOrgTestDocHttpVocabPid = "context-http-vocab.jsonld";
    private Resource schemaOrgTestDocHttpsVocab;
    private String schemaOrgTestDocHttpsVocabPid = "context-https-vocab.jsonld";
    private Resource schemaOrgTestDocHttp;
    private String schemaOrgTestDocHttpPid = "context-http.jsonld";
    private Resource schemaOrgTestDocHttps;
    private String schemaOrgTestDocHttpsPid = "context-https.jsonld";
    private Resource schemaOrgTestDocDryad1;
    private String schemaOrgTestDocDryad1Pid = "doi.org_10.5061_dryad.5qb78.jsonld";
    private Resource schemaOrgTestDocDryad2;
    private String schemaOrgTestDocDryad2Pid = "doi.org_10.5061_dryad.41sk145.jsonld";
    private Resource schemaOrgTesHakaiDeep;
    private String schemaOrgTesHakaiDeepPid = "hakai-deep-schema.jsonld";

    /* An instance of the RDF/XML Subprocessor */
    private JsonLdSubprocessor jsonLdSubprocessor;

    /* Store a map of expected Solr fields and their values for testing */
    private HashMap<String, String> expectedFields = new HashMap<String, String>();

    private static final int SLEEPTIME = 8000;
    private static final int SLEEP = 2000;
    private static final int TIMES = 10;
    

    /**
     * For each test, set up the Solr service and test data
     * 
     * @throws Exception
     */
    @Before
    public void setUp() throws Exception {
        // Start up the embedded Jetty server and Solr service
        super.setUp();
        schemaOrgDoc = (Resource) context.getBean("schemaOrgTestDoc");
        schemaOrgDoc2 = (Resource) context.getBean("schemaOrgTestDoc2");
        schemaOrgDocSOSO = (Resource) context.getBean("schemaOrgTestDocSOSO");
        schemaOrgTestWithoutVocab = (Resource) context.getBean("schemaOrgTestWithoutVocab");
        schemaOrgTestDocHttpVocab = (Resource) context.getBean("schemaOrgTestHttpVocab");
        schemaOrgTestDocHttpsVocab = (Resource) context.getBean("schemaOrgTestHttpsVocab");
        schemaOrgTestDocHttp = (Resource) context.getBean("schemaOrgTestHttp");
        schemaOrgTestDocHttps = (Resource) context.getBean("schemaOrgTestHttps");
        schemaOrgTestDocDryad1 = (Resource) context.getBean("schemaOrgTestDryad1");
        schemaOrgTestDocDryad2 = (Resource) context.getBean("schemaOrgTestDryad2");
        schemaOrgTesHakaiDeep = (Resource) context.getBean("schemaOrgTesHakaiDeep");

        // instantiate the subprocessor
        jsonLdSubprocessor = (JsonLdSubprocessor) context.getBean("jsonLdSubprocessor");
    }

    /**
     * For each test, clean up, bring down the Solr service
     */
    @After
    public void tearDown() throws Exception {
        super.tearDown();
    }

    /**
     * Test the end to end index processing a schema.org 'Dataset' document
     *
     * @throws Exception
     */
    //@Ignore
    @Test
    public void testInsertSchemaOrg() throws Exception {
        //index the object
        String id = schemaOrgDocPid; 
        indexObjectToSolr(id, schemaOrgDoc);
        Thread.sleep(SLEEPTIME);
        // now process the tasks
        //processor.processIndexTaskQueue();
        for (int i=0; i<TIMES; i++) {
            try {
                Thread.sleep(SLEEP);
                assertPresentInSolrIndex(id);
                break;
            } catch (Throwable e) {
                
            }
        }
        assertTrue(compareFieldValue(id, "title", "Neodymium isotopes, B/Ca and δ¹³C, and fresh sand volcanic glass count data from ODP Site 208-1267 and IODP Site 306-U1313 for MIS M2, MIS 100 and the Last Glacial-Holocene"));
        assertTrue(compareFieldValue(id, "abstract", "Marine Isotope Stage (MIS) M2, 3.3 Ma, is an isolated cold stage punctuating the benthic oxygen isotope (\u03b4\u00b9\u2078O)"));
        assertTrue(compareFieldValue(id, "label", "Neodymium isotopes"));
        assertTrue(compareFieldValue(id, "author", "Nicola Kirby"));
        assertTrue(compareFieldValue(id, "authorGivenName", "Nicola"));
        String[] authorLastName = {"Kirby", "Bailey", "Lang", "Brombacher", "Chalk", "Parker",
                "Crocker", "Taylor", "Milton", "Foster", "Raymo", "Kroon", "Bell", "Wilson"};
        assertTrue(compareFieldValue(id, "authorLastName", authorLastName));
        String[] investigator = {"Kirby", "Bailey", "Lang", "Brombacher", "Chalk", "Parker",
                "Crocker", "Taylor", "Milton", "Foster", "Raymo", "Kroon", "Bell", "Wilson"};
        assertTrue(compareFieldValue(id, "investigator", investigator));
        assertTrue(compareFieldValue(id, "awardNumber", new String [] {"http://www.nsf.gov/awardsearch/showAward.do?AwardNumber=1643466"}));
        assertTrue(compareFieldValue(id, "awardTitle", new String [] {"OPP-1643466"}));
        assertTrue(compareFieldValue(id, "pubDate", new String [] {"2020-12-09T00:00:00.000Z"}));
        String[] origins = {"Nicola Kirby", "Ian Bailey", "David C Lang", "A Brombacher", "Thomas B Chalk",
                "Rebecca L Parker", "Anya J Crocker", "Victoria E Taylor", "J Andy Milton", "Gavin L Foster", "Maureen E Raymo", "Dick Kroon", "David B Bell", "Paul A Wilson"};
        assertTrue(compareFieldValue(id, "origin", origins));
        assertTrue(compareFieldValue(id, "funderIdentifier", new String [] {"https://doi.org/10.13039/100000141"}));
        assertTrue(compareFieldValue(id, "funderName", new String [] {"NSF Division of Ocean Sciences"}));
        String[] parts = {"Sub dataset 01", "Sub dataset 02"};
        assertTrue(compareFieldValue(id, "hasPart", parts));
        String[] keywords = {"AMOC", "Atlantic circulation", "B/Ca", "Last Glacial", "MIS 100", "MIS M2", "Nd isotopes"};
        // "box": "-28.09816 -32.95731 41.000022722222 1.71098"
        // i.e. "south west north east" - lat, long of southwest corner ; lat, long of northeast corner
        assertTrue(compareFieldValue(id, "keywords", keywords));
        String [] coord = {"-28.09816"};
        assertTrue(compareFieldValue(id, "southBoundCoord", coord));
        coord[0] = "-32.95731";
        assertTrue(compareFieldValue(id, "westBoundCoord", coord));
        coord[0] = "41.000023";
        assertTrue(compareFieldValue(id, "northBoundCoord", coord));
        coord[0] = "1.71098";
        assertTrue(compareFieldValue(id, "eastBoundCoord", coord));
        assertTrue(compareFieldValue(id, "geohash_1", new String [] {"e"}));
        assertTrue(compareFieldValue(id, "geohash_2", new String [] {"e9"}));
        assertTrue(compareFieldValue(id, "geohash_3", new String [] {"e9h"}));
        assertTrue(compareFieldValue(id, "geohash_4", new String [] {"e9hu"}));
        assertTrue(compareFieldValue(id, "geohash_5", new String [] {"e9hus"}));
        assertTrue(compareFieldValue(id, "geohash_6", new String [] {"e9husq"}));
        assertTrue(compareFieldValue(id, "geohash_7", new String [] {"e9husqr"}));
        assertTrue(compareFieldValue(id, "geohash_8", new String [] {"e9husqre"}));
        assertTrue(compareFieldValue(id, "geohash_9", new String [] {"e9husqre3"}));
        assertTrue(compareFieldValue(id, "beginDate", new String [] {"2003-04-21T09:40:00.000Z"}));
        assertTrue(compareFieldValue(id, "endDate", new String [] {"2003-04-26T16:45:00.000Z"}));
        String[] parameters = {"unique record ID number", "Date (UTC) in ISO8601 format: YYYY-MM-DDThh:mmZ",
            "Date (local time zone of PST/PDT) in ISO8601; format: YYYY-MM-DDThh:mm", "Dissolved oxygen"};
        assertTrue(compareFieldValue(id, "parameter", parameters));
        assertTrue(compareFieldValue(id, "edition", "1"));
        String[] urls = {"https://doi.pangaea.de/10.1594/PANGAEA.925562",
                        "https://doi.pangaea.de/10.1594/PANGAEA.925562?format=zip"};
        assertTrue(compareFieldValue(id, "serviceEndpoint", urls));
        assertTrue(compareFieldLength(id, "text", 4269));
        String[] license = {"https://creativecommons.org/licenses/by/4.0/"};
        assertTrue(compareFieldValue(id, "licenseUrl", license));


    }

    /**
     * Test the end to end index processing a schema.org 'Dataset' document. This example
     * contains properties not present or in a different format than the first schema.org example.
     *
     * @throws Exception
     */
    @Test
    public void testInsertSchemaOrg2() throws Exception {
        String id = schemaOrgDoc2Pid;
        indexObjectToSolr(id, schemaOrgDoc2);
        Thread.sleep(SLEEPTIME);
        // now process the tasks
        //processor.processIndexTaskQueue();
        for (int i=0; i<TIMES; i++) {
            try {
                Thread.sleep(SLEEP);
                assertPresentInSolrIndex(id);
                break;
            } catch (Throwable e) {
                
            }
        }
        assertTrue(compareFieldValue(id, "title", new String [] {"Context-dependent costs and benefits of a heterospecific nesting association"}));
        assertTrue(compareFieldValue(id, "author", new String [] {"Rose J Swift"}));
        assertTrue(compareFieldValue(id, "abstract", new String [] {"The costs and benefits of interactions among species"}));
        assertTrue(compareFieldValue(id, "authorGivenName", new String [] {"Rose J"}));
        //assertTrue(compareFieldValue(id, "authorLastName", new String [] {"Swift"}));
        String[] origins = {"Rose J Swift", "Amanda D Rodewald", "Nathan R Senner"};
        assertTrue(compareFieldValue(id, "origin", origins));
        String[] keywords = {"Mew Gull", "Larus canus", "Limosa haemastica", "predation", "Hudsonian Godwit",
                "protective nesting association"};
        assertTrue(compareFieldValue(id, "keywords", keywords));
        assertTrue(compareFieldValue(id, "namedLocation", new String [] {"Beluga River", "Alaska"}));
        assertTrue(compareFieldValue(id, "beginDate", new String [] {"2018-03-05T15:54:47.000Z"}));
        assertTrue(compareFieldValue(id, "edition", new String [] {"1"}));
        String urls[] = {"http://datadryad.org/api/v2/datasets/doi%253A10.5061%252Fdryad.m8s2r36/download",
                         "http://datadryad.org/stash/dataset/doi%253A10.5061%252Fdryad.m8s2r36"};
        assertTrue(compareFieldValue(id, "serviceEndpoint", urls));
        assertTrue(compareFieldLength(id, "text", 691));
        String[] licenseUrl = {"https://creativecommons.org/publicdomain/zero/1.0/"};
        String[] licenseName = {"CC0 1.0 Universal (CC0 1.0) Public Domain Dedication"};
        assertTrue(compareFieldValue(id, "licenseUrl", licenseUrl));
        assertTrue(compareFieldValue(id, "licenseName", licenseName));
    }

    /**
     * Test the end to end index processing a schema.org 'Dataset' document. This example
     * contains properties from the ESIP Federation "Science on Schema.org" guidelines full example
     * document.
     *
     * @throws Exception
     */
    @Test
    public void testInsertSchemaOrgSOSO() throws Exception {
        String id = schemaOrgDocSOSOPid;
        indexObjectToSolr(id, schemaOrgDocSOSO);
        Thread.sleep(SLEEPTIME);
        // now process the tasks
        //processor.processIndexTaskQueue();
        for (int i=0; i<TIMES; i++) {
            try {
                Thread.sleep(SLEEP);
                assertPresentInSolrIndex(id);
                break;
            } catch (Throwable e) {
                
            }
        }
        assertTrue(compareFieldValue(id, "title", new String[] {"Larval krill studies - fluorescence and clearance from ARSV Laurence M. Gould LMG0106, LMG0205 in the Southern Ocean from 2001-2002 (SOGLOBEC project)"}));
        assertTrue(compareFieldValue(id, "abstract", new String[] {"Winter ecology of larval krill: quantifying their interaction with the pack ice habitat."}));
        String [] urls = {"https://www.example-data-repository.org/dataset/3300/data/larval-krill.tsv",
        "https://www.example-data-repository.org/dataset/3300"};
        assertTrue(compareFieldValue(id, "serviceEndpoint", urls));
        assertTrue(compareFieldValue(id, "prov_wasDerivedFrom", new String[] {"https://doi.org/10.xxxx/Dataset-1"}));
        assertTrue(compareFieldValue(id, "prov_generatedByExecution", new String[] {"https://example.org/executions/execution-42"}));
        assertTrue(compareFieldValue(id, "prov_generatedByProgram", new String[] {"https://somerepository.org/datasets/10.xxxx/Dataset-2.v2/process-script.R"}));
        assertTrue(compareFieldValue(id, "prov_instanceOfClass", new String[] {"http://purl.dataone.org/provone/2015/01/15/ontology#Data"}));
        assertTrue(compareFieldValue(id, "prov_hasDerivations", new String[] {"https://somerepository.org/datasets/10.xxxx/Dataset-101"}));
        assertTrue(compareFieldValue(id, "prov_usedByProgram", new String [] {"https://somerepository.org/datasets/10.xxxx/Dataset-101/process-script.R"}));
        assertTrue(compareFieldValue(id, "prov_usedByExecution", new String [] {"https://example.org/executions/execution-101"}));
        assertTrue(compareFieldValue(id, "abstract", new String [] {"Winter ecology of larval krill: quantifying their interaction with the pack ice habitat."}));
        String [] coord = {"-68.4817"};
        assertTrue(compareFieldValue(id, "southBoundCoord", coord));
        coord[0] = "-75.8183";
        assertTrue(compareFieldValue(id, "westBoundCoord", coord));
        coord[0] = "-65.08";
        assertTrue(compareFieldValue(id, "northBoundCoord", coord));
        coord[0] = "-68.5033";
        assertTrue(compareFieldValue(id, "eastBoundCoord", coord));
        assertTrue(compareFieldValue(id, "geohash_1", new String [] {"4"}));
        assertTrue(compareFieldValue(id, "geohash_2", new String [] {"4k"}));
        assertTrue(compareFieldValue(id, "geohash_3", new String [] {"4kh"}));
        assertTrue(compareFieldValue(id, "geohash_4", new String [] {"4khs"}));
        assertTrue(compareFieldValue(id, "geohash_5", new String [] {"4khsj"}));
        assertTrue(compareFieldValue(id, "geohash_6", new String [] {"4khsjf"}));
        assertTrue(compareFieldValue(id, "geohash_7", new String [] {"4khsjfy"}));
        assertTrue(compareFieldValue(id, "geohash_8", new String [] {"4khsjfyj"}));
        assertTrue(compareFieldValue(id, "geohash_9", new String [] {"4khsjfyj7"}));
        assertTrue(compareFieldLength(id, "text", 3681));
        String[] license = {"https://creativecommons.org/licenses/by/4.0/"};
        assertTrue(compareFieldValue(id, "licenseUrl", license));
    }

    /**
     * Test that the JsonLdSubprocessor can normalize several JSONLD @context variants, so that
     * indexing can be performed on the document, where the indexing queries only look for the
     * namespace http://schema.org.
     *
     * @throws Exception
     */
    @Test
    public void testInsertSchemaNormalization() throws Exception {
       

        ArrayList<Resource> resources = new ArrayList<>();
        resources.add(schemaOrgTestDocHttp);
        resources.add(schemaOrgTestDocHttps);
        resources.add(schemaOrgTestDocHttpVocab);
        resources.add(schemaOrgTestDocHttpsVocab);

        // Insert the schema.org file into the task queue
        ArrayList<String> ids = new ArrayList<>();
        ids.add(schemaOrgTestDocHttpPid);
        ids.add(schemaOrgTestDocHttpsPid);
        ids.add(schemaOrgTestDocHttpVocabPid);
        ids.add(schemaOrgTestDocHttpsVocabPid);
       
        int i = -1;
        String thisId;
        for (Resource res : resources) {
            i++;
            thisId = ids.get(i);
            log.info("processing doc with id: " + thisId);
            indexObjectToSolr(thisId, res);
            Thread.sleep(SLEEPTIME);
            // now process the tasks
            //processor.processIndexTaskQueue();
            for (int j=0; j<TIMES; j++) {
                try {
                    Thread.sleep(SLEEP);
                    assertPresentInSolrIndex(thisId);
                    break;
                } catch (Throwable e) {
                    
                }
            }
            assertTrue(compareFieldValue(thisId, "title", new String [] {"test of context normalization"}));
            assertTrue(compareFieldValue(thisId, "author", new String [] {"creator_03"}));
            String[] origins = {"creator_03", "creator_02", "creator_01"};
            assertTrue(compareFieldValue(thisId, "origin", origins));
            //assertTrue(compareFieldLength(thisId, "text", 140));
        }
    }

    /**
     * Test that the JsonLdSubprocessor can sucessfully index JSONLD Dataset description documents from Dryad.
     *
     * @throws Exception
     */
    @Test
    public void testInsertSchemaOrgDryad() throws Exception {
        ArrayList<Resource> resources = new ArrayList<>();
        resources.add(schemaOrgTestDocDryad1);
        resources.add(schemaOrgTestDocDryad2);

        // Insert the schema.org file into the task queue
        ArrayList<String> ids = new ArrayList<>();
        ids.add(schemaOrgTestDocDryad1Pid);
        ids.add(schemaOrgTestDocDryad2Pid);
        String thisId;

        int iDoc = 0;
        thisId = ids.get(iDoc);
        indexObjectToSolr(thisId, resources.get(iDoc));
        Thread.sleep(SLEEPTIME);
        // now process the tasks
        //processor.processIndexTaskQueue();
        for (int i=0; i<TIMES; i++) {
            try {
                Thread.sleep(SLEEP);
                assertPresentInSolrIndex(thisId);
                break;
            } catch (Throwable e) {
                
            }
        }
        assertTrue(compareFieldValue(thisId, "title", new String [] {"Mate choice and the operational sex ratio: an experimental test with robotic crabs"}));
        assertTrue(compareFieldValue(thisId, "abstract", new String [] {"The operational sex ratio (OSR) in robotic crabs)."}));
        assertTrue(compareFieldValue(thisId, "author", new String [] {"Catherine L. Hayes"}));
        String[] urls = {"http://datadryad.org/stash/dataset/doi%253A10.5061%252Fdryad.5qb78",
                         "http://datadryad.org/api/v2/datasets/doi%253A10.5061%252Fdryad.5qb78/download"};
        assertTrue(compareFieldValue(thisId, "serviceEndpoint", urls));

        iDoc++;
        thisId = ids.get(iDoc);
        indexObjectToSolr(thisId, resources.get(iDoc));
        Thread.sleep(SLEEPTIME);
        // now process the tasks
        //processor.processIndexTaskQueue();
        for (int i=0; i<TIMES; i++) {
            try {
                Thread.sleep(SLEEP);
                assertPresentInSolrIndex(thisId);
                break;
            } catch (Throwable e) {
                
            }
        }
        assertTrue(compareFieldValue(thisId, "title", new String [] {"Flow of CO2 from soil may not correspond with CO2 concentration in soil"}));
        assertTrue(compareFieldValue(thisId, "abstract", new String [] {"Soil CO2 concentration was investigated in the northwest of the Czechia."}));
        assertTrue(compareFieldValue(thisId, "author", new String [] {"Jan Frouz"}));
        urls = new String[]{"http://datadryad.org/stash/dataset/doi%253A10.5061%252Fdryad.41sk145",
                "http://datadryad.org/api/v2/datasets/doi%253A10.5061%252Fdryad.41sk145/download"};
        assertTrue(compareFieldValue(thisId, "serviceEndpoint", urls));
        assertTrue(compareFieldLength(thisId, "text", 2501));
        String[] licenseUrl = {"https://creativecommons.org/publicdomain/zero/1.0/"};
        String[] licenseName = {"CC0 1.0 Universal (CC0 1.0) Public Domain Dedication"};
        assertTrue(compareFieldValue(thisId, "licenseUrl", licenseUrl));
        assertTrue(compareFieldValue(thisId, "licenseName", licenseName));
    }

    /**
     * Compare the string length of a result with a known correct value.
     * <p>
     *     Some Solr fields (e.g. text) are derived by concatenating multiple source fields together into a single value. Because of the
     *     RDF serialization and retrieval by SPARQL, there is no guarentee that the resulting string will be the same as any previous
     *     result. Therefore, the only way to check that the value could be the same is to compare the resulting string length, which sould
     *     always be the same, regardless of the order of component strings that comprise it. This isn't a perfect test, as it doesn't
     *     definitively prove the string is correct, just that it could be correct.
     * </p>
     *
     * @throws Exception
     */
    protected boolean compareFieldLength(String id, String fieldName, int expectedLength) throws SolrServerException, IOException {
        boolean equal = true;
        ModifiableSolrParams solrParams = new ModifiableSolrParams();
        solrParams.set("q", "id:" + ClientUtils.escapeQueryChars(id));
        solrParams.set("fl", "*");
        QueryResponse qr = getSolrClient().query(solrParams);
        SolrDocument result = qr.getResults().get(0);
        String testResult = (String) result.getFirstValue(fieldName);
        int fieldLength = testResult.length();

        System.out.println("++++++++++++++++ the string length of solr result for the string field " + fieldName + " is " + fieldLength);
        System.out.println("++++++++++++++++ the expected string length for the field " + fieldName + " is " + expectedLength);

        return (fieldLength == expectedLength);
    }
    
    @Test
    public void testIsHttps() throws Exception {
        File file = schemaOrgTestWithoutVocab.getFile();
        Object object = JsonUtils.fromInputStream(new FileInputStream(file), "UTF-8");
        List list = JsonLdProcessor.expand(object);
        assertTrue(!(jsonLdSubprocessor.isHttps(list)));
        file = schemaOrgDoc.getFile();
         object = JsonUtils.fromInputStream(new FileInputStream(file), "UTF-8");
         list = JsonLdProcessor.expand(object);
         assertTrue(jsonLdSubprocessor.isHttps(list));
    }
    
    @Test
    public void testHakaiDeep() throws Exception {
        String id = schemaOrgTesHakaiDeepPid;
        indexObjectToSolr(id, schemaOrgTesHakaiDeep);

        Thread.sleep(2*SLEEPTIME);
        // now process the tasks
        //processor.processIndexTaskQueue();
        for (int i=0; i<TIMES; i++) {
            try {
                Thread.sleep(SLEEP);
                assertPresentInSolrIndex(id);
                break;
            } catch (Throwable e) {
                
            }
        }
        assertTrue(compareFieldValue(id, "title", "Salmon Blood Analyses from the 2020 Gulf of Alaska International Year of the Salmon Expedition"));
        assertTrue(compareFieldValue(id, "abstract", "Salmon blood analyses from salmon collected in the Northeast Pacific Ocean. These data were collected as part of the International Year of the Salmon (IYS) Gulf of Alaska High Seas Expedition conducted in March and April 2020, to further improve the understanding of factors impacting salmon early marine winter survival. Blood was collected for assessment of IGF-1 (growth), stress indices (cortisol, glucose, lactate), ionoregulation (osmolality, ions)."));
        assertTrue(compareFieldValue(id, "author", "Chrys Neville"));
        assertTrue(compareFieldValue(id, "authorGivenName", "Chrys"));
        String[] authorLastName = {"Neville"};
        assertTrue(compareFieldValue(id, "authorLastName", authorLastName));
        assertTrue(compareFieldValue(id, "pubDate", new String [] {"2022-04-27T23:32:25.621Z"}));
        String[] origins = {"Chrys Neville"};
        assertTrue(compareFieldValue(id, "origin", origins));
        String[] keywords = {"blood-samples", "annee-internationale-du-saumon", "growth", "stress", "igf-1", "xml", "ionoregulation", 
                            "international-year-of-the-salmon", "other", "autre", "oceans"};
        assertTrue(compareFieldValue(id, "keywords", keywords));
        String [] coord = {"46.37"};
        assertTrue(compareFieldValue(id, "southBoundCoord", coord));
        coord[0] = "-147.525";
        assertTrue(compareFieldValue(id, "westBoundCoord", coord));
        coord[0] = "54.5617";
        assertTrue(compareFieldValue(id, "northBoundCoord", coord));
        coord[0] = "-125.4467";
        assertTrue(compareFieldValue(id, "eastBoundCoord", coord));
        assertTrue(compareFieldValue(id, "geohash_1", new String [] {"b"}));
        assertTrue(compareFieldValue(id, "geohash_2", new String [] {"bb"}));
        assertTrue(compareFieldValue(id, "geohash_3", new String [] {"bby"}));
        assertTrue(compareFieldValue(id, "geohash_4", new String [] {"bbyz"}));
        assertTrue(compareFieldValue(id, "geohash_5", new String [] {"bbyzn"}));
        assertTrue(compareFieldValue(id, "geohash_6", new String [] {"bbyzn5"}));
        assertTrue(compareFieldValue(id, "geohash_7", new String [] {"bbyzn5n"}));
        assertTrue(compareFieldValue(id, "geohash_8", new String [] {"bbyzn5n0"}));
        assertTrue(compareFieldValue(id, "geohash_9", new String [] {"bbyzn5n0c"}));
        assertTrue(compareFieldValue(id, "beginDate", new String [] {"2020-03-12T00:00:00.000Z"}));
        assertTrue(compareFieldValue(id, "endDate", new String [] {"2020-04-05T00:00:00.000Z"}));
        assertTrue(compareFieldValue(id, "edition", "1"));
        String[] urls = {"https://iys.hakai.org/dataset/ca-cioos_02784c0c-72f7-4887-b1d0-d1fb6dacbcb4",
                        "https://international-year-of-the-salmon.github.io/about/data-unavailable.html"};
        assertTrue(compareFieldValue(id, "serviceEndpoint", urls));
        String[] license = {"https://creativecommons.org/licenses/by/4.0/"};
        assertTrue(compareFieldValue(id, "licenseUrl", license));
    }
    
}
