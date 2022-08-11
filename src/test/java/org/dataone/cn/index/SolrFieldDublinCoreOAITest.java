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

import org.dataone.cn.indexer.convert.SolrDateConverter;
import org.dataone.cn.indexer.parser.ScienceMetadataDocumentSubprocessor;
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
@ContextConfiguration(locations = { "test-context.xml" })
public class SolrFieldDublinCoreOAITest extends BaseSolrFieldXPathTest {

    @Autowired
    private Resource dc_oai_SysMeta;

    @Autowired
    private Resource dc_oai_SciMeta;

    @Autowired
    private ScienceMetadataDocumentSubprocessor dublinCoreOAISubprocessor;
    
    private HashMap<String, String> dcxExpected = new HashMap<String, String>();
    private SolrDateConverter dateConverter = new SolrDateConverter();

    @Before
    public void setUp() throws Exception {
        // science metadata
        dcxExpected
                .put("keywords",
                        "Ecology#White tailed deer#chestnut oak");
        dcxExpected.put("title", "Chestnut Oak (Quercus Prinus) Response to Browsing by White Tailed Deer: Implications for Carbon and Nitrogen Allocation");
        dcxExpected.put("author", "figshare admin Cary Institute (1246804)");
        dcxExpected.put("authorSurName", "figshare admin Cary Institute (1246804)");
        dcxExpected.put("authorSurNameSort", "figshare admin Cary Institute (1246804)");
        dcxExpected.put("investigator", "John Smith#Mary Alex#figshare admin Cary Institute (1246804)#Jen Nieves (1725067)");
        dcxExpected.put("contactOrganization", "figshare admin Cary Institute (1246804)#Jen Nieves (1725067)");
        dcxExpected.put("origin", "figshare admin Cary Institute (1246804)#Jen Nieves (1725067)");
        dcxExpected.put("originator", "e-journal#e-journal-2");
        //dcxExpected.put("beginDate", "");
        //dcxExpected.put("endDate", "");
        dcxExpected.put("pubDate", "2018-04-19T19:54:04.000Z");
        //dcxExpected.put("site", "");
        //dcxExpected.put("serviceCoupling", "mixed");
        //dcxExpected.put("serviceDescription", "Landing page for resource access");
        dcxExpected.put("serviceEndpoint", "https://figsh.com/articles/Chestnut_Oak_Quercus_Prinus_Response_to_Browsing_by_White_Tailed_Deer_Implications_for_Carbon_and_Nitrogen_Allocation/5853507");
        //dcxExpected.put("serviceTitle", "Resource Landing Page");
        //dcxExpected.put("serviceType", "HTTP");
        dcxExpected.put("abstract", "Fileset contains data file, including dataset metadata, as well as R scripts.<br>");
        //dcxExpected.put("fileID", "https://figsh.com/articles/Chestnut_Oak_Quercus_Prinus_Response_to_Browsing_by_White_Tailed_Deer_Implications_for_Carbon_and_Nitrogen_Allocation/5853507");
        dcxExpected
                .put("text",
                        "Chestnut Oak (Quercus Prinus) Response to Browsing by White Tailed Deer: Implications for Carbon and Nitrogen Allocation  figshare admin Cary Institute (1246804)  Jen Nieves (1725067)  Ecology  White tailed deer  chestnut oak  Fileset contains data file, including dataset metadata, as well as R scripts.<br>  e-journal  e-journal-2  John Smith  Mary Alex  2018-04-19T19:54:04Z  2017-05-19T19:54:04Z  Dataset  Fileset  10.5072/fk2.stagefigshare.5853507.v1  https://figsh.com/articles/Chestnut_Oak_Quercus_Prinus_Response_to_Browsing_by_White_Tailed_Deer_Implications_for_Carbon_and_Nitrogen_Allocation/5853507  CC BY oai_dc.1.1.xml");

        // system metadata
        dcxExpected.put("id", "oai_dc.1.1.xml");
        dcxExpected.put("seriesId", "");
        dcxExpected.put("fileName", "");
        dcxExpected.put("mediaType", "");
        dcxExpected.put("mediaTypeProperty", "");
        dcxExpected.put("formatId", "http://www.openarchives.org/OAI/2.0/oai_dc/");
        //dcxExpected.put("formatType", "METADATA");
        dcxExpected.put("formatType", "METADATA");
        dcxExpected.put("size", "1567");
        dcxExpected.put("checksum", "f79af9b5feaed3ed6b8583fd5c3cf9bd");
        dcxExpected.put("checksumAlgorithm", "MD5");
        dcxExpected.put("submitter", "CN=urn:node:mnTestMPC,DC=dataone,DC=org");
        dcxExpected.put("rightsHolder",
                "CN=Judy Kallestad A13391,O=University of Minnesota,C=US,DC=cilogon,DC=org");
        dcxExpected.put("replicationAllowed", "");
        dcxExpected.put("numberReplicas", "");
        dcxExpected.put("archived", "false");
        dcxExpected.put("preferredReplicationMN", "");
        dcxExpected.put("blockedReplicationMN", "");
        dcxExpected.put("obsoletes", "");
        dcxExpected.put("obsoletedBy", "");
        dcxExpected.put("dateUploaded", dateConverter.convert("2014-08-28T20:55:19.003582"));
        dcxExpected.put("dateModified", dateConverter.convert("2014-08-28T20:55:19.034555Z"));
        dcxExpected.put("datasource", "urn:node:mnTestMPC");
        dcxExpected.put("authoritativeMN", "urn:node:mnTestMPC");
        dcxExpected.put("replicaMN", "urn:node:CN");
        dcxExpected.put("replicationStatus", "completed");
        dcxExpected.put("replicaVerifiedDate", dateConverter.convert("2014-08-29T20:55:19.003582"));
        dcxExpected.put("readPermission", "public");
        dcxExpected.put("writePermission", "");
        dcxExpected.put("changePermission", "");
        dcxExpected.put("isPublic", "true");
        dcxExpected.put("dataUrl", "https://" + hostname
                + "/cn/v2/resolve/oai_dc.1.1.xml");
        
       
    }

    @Test
    public void testDublinCoreExtendedFieldParsing() throws Exception {
        testXPathParsing(dublinCoreOAISubprocessor, dc_oai_SysMeta, dc_oai_SciMeta,
                dcxExpected, "oai_dc.1.1.xml");
    }
    
  
}
