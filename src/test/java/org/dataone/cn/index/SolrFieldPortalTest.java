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

import java.util.HashMap;

import org.dataone.cn.indexer.XmlDocumentUtility;
import org.dataone.cn.indexer.convert.SolrDateConverter;
import org.dataone.cn.indexer.parser.ScienceMetadataDocumentSubprocessor;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.w3c.dom.Document;

import com.carrotsearch.randomizedtesting.annotations.ThreadLeakScope;

@ThreadLeakScope(ThreadLeakScope.Scope.NONE)
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "test-context.xml" })
public class SolrFieldPortalTest extends BaseSolrFieldXPathTest {


    @Autowired
    private Resource portalTestDocSysMeta;

    @Autowired
    private Resource portalTestDoc;

    @Autowired
    private ScienceMetadataDocumentSubprocessor portalsSubprocessor;

    
    private SolrDateConverter dateConverter = new SolrDateConverter();

    // solr/rule field name (from spring context fields) mapped to actual value
    // from xml documents (system and science metadata docs)
    private HashMap<String, String> portalExpected = new HashMap<String, String>();
 
    
    @Before
    public void setUp() throws Exception {
        setUpPortal();
    }
    
 

    public void setUpPortal() throws Exception {
        portalExpected.put("title", "Lauren test 10");
        portalExpected.put("label", "laurentest10");
        portalExpected.put("collectionQuery", "((text:*ocean*) OR isPartOf:urn\\:uuid\\:27ae3627-be62-4963-859a-8c96d940cadc) AND (-obsoletedBy:* AND formatType:METADATA)");
        portalExpected.put("logo", "urn:uuid:4adc5fac-79d5-46d1-b5d9-425a5a90b39a");
        portalExpected.put("investigator", "");
        portalExpected.put("funderName", "");
        portalExpected.put("funderIdentifier", "");
        portalExpected.put("awardNumber", "");
        portalExpected.put("awardTitle", "");
        portalExpected.put("abstract", "");
        
        // system metadata
        portalExpected.put("id", "urn:uuid:b210adf0-f08a-4cae-aa86-5b64605e9297");
        portalExpected.put("seriesId", "urn:uuid:27ae3627-be62-4963-859a-8c96d940cadc");
        portalExpected.put("fileName", "laurentest10.xml");
        portalExpected.put("mediaType", "");
        portalExpected.put("mediaTypeProperty", "");
        portalExpected.put("formatId", "https://purl.dataone.org/portals-1.0.0");
        portalExpected.put("formatType", "METADATA");
        portalExpected.put("size", "7906");
        portalExpected.put("checksum", "1bd96c1864aa2c51c37480b9a965ffbb");
        portalExpected.put("checksumAlgorithm", "MD5");
        portalExpected.put("submitter", "http://orcid.org/0000-0003-2192-431X");
        portalExpected.put("rightsHolder", "http://orcid.org/0000-0003-2192-431X");
        portalExpected.put("replicationAllowed", "false");
        portalExpected.put("numberReplicas", "");
        portalExpected.put("preferredReplicationMN", "");
        portalExpected.put("blockedReplicationMN", "");
        portalExpected.put("obsoletes", "urn:uuid:9174cac2-363c-4f18-ba23-a5bcd8db407d");
        portalExpected.put("obsoletedBy", "");
        portalExpected.put("archived", "false");
        portalExpected.put("dateUploaded", dateConverter.convert("2019-10-11T14:26:05.59Z"));
        portalExpected.put("dateModified", dateConverter.convert("2019-10-11T14:26:06.066Z"));
        portalExpected.put("datasource", "urn:node:mnTestKNB");
        portalExpected.put("authoritativeMN", "urn:node:mnTestKNB");
        portalExpected.put("replicaMN", "");
        portalExpected.put("replicationStatus", "");
        portalExpected.put("replicaVerifiedDate", "");
        portalExpected.put("readPermission", "public");
        portalExpected.put("writePermission", "");
        portalExpected.put("changePermission", "");
        portalExpected.put("isPublic", "true");
        portalExpected.put("dataUrl", "https://cn.dataone.org/cn/v2/resolve/urn%3Auuid%3Ab210adf0-f08a-4cae-aa86-5b64605e9297");

        //portalExpected.put("isService", "false");
        //portalExpected.put("serviceTitle", "");
        //portalExpected.put("serviceDescription", "");
        //portalExpected.put("serviceEndpoint", "");
    }

   

    @Test
    public void testPortalFields() throws Exception {
        testXPathParsing(portalsSubprocessor, portalTestDocSysMeta, portalTestDoc, portalExpected, "urn:uuid:b210adf0-f08a-4cae-aa86-5b64605e9297");
    }

  
}
