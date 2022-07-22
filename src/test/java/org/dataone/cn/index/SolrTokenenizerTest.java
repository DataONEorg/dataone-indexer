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

import org.apache.solr.common.SolrDocumentList;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.springframework.core.io.Resource;

import com.carrotsearch.randomizedtesting.annotations.ThreadLeakScope;

@ThreadLeakScope(ThreadLeakScope.Scope.NONE)
public class SolrTokenenizerTest extends DataONESolrJettyTestBase {

    private Resource peggym1271Sys;
    private Resource peggym1281Sys;
    private Resource peggym1291Sys;
    private Resource peggym1304Sys;
    private Resource tao129301Sys;

    @BeforeClass
    public static void init() {
        //HazelcastClientFactoryTest.setUp();
    }

    @Test
    public void testTokenizingPeriod() throws Exception {
        String pid = "peggym.130.4";
        sendSolrDeleteAll();
        addAllToSolr();
        assertPresentInSolrIndex(pid);
        SolrDocumentList sdl = null;
        sdl = findByField("text", "frank");
        Assert.assertEquals(1, sdl.size());
    }

    /**
     * EML specific test to ensure '||' characters do not appear in origin field when
     * eml references are used to indicate people resources.
     * 
     * @throws Exception
     */
    @Test
    public void testTokenizingPipe() throws Exception {
        String pid = "tao.12930.1";
        sendSolrDeleteAll();
        addAllToSolr();
        assertPresentInSolrIndex(pid);
        SolrDocumentList sdl = null;
        sdl = findByField("origin", "tao||");
        Assert.assertEquals(0, sdl.size());
        sdl = findByField("origin", "tao");
        Assert.assertEquals(1, sdl.size());
    }

    @Test
    public void testTokenizingComma() throws Exception {
        String pid = "peggym.130.4";
        sendSolrDeleteAll();
        addAllToSolr();
        assertPresentInSolrIndex(pid);
        SolrDocumentList sdl = null;
        sdl = findByField("text", "fred");
        Assert.assertEquals(1, sdl.size());
    }

    @Test
    public void testTokenizingParentheses() throws Exception {
        String pid = "peggym.130.4";
        sendSolrDeleteAll();
        addAllToSolr();
        assertPresentInSolrIndex(pid);
        SolrDocumentList sdl = null;
        sdl = findByField("text", "parenthized");
        Assert.assertEquals(1, sdl.size());
        sdl = findByField("text", "(parenthized)");
        Assert.assertEquals(1, sdl.size());
    }

    @Test
    public void testQuotations() throws Exception {
        String pid = "peggym.130.4";
        sendSolrDeleteAll();
        addAllToSolr();
        assertPresentInSolrIndex(pid);
        SolrDocumentList sdl = null;
        sdl = findByField("text", "double");
        Assert.assertEquals(1, sdl.size());
        sdl = findByField("text", "single");
        Assert.assertEquals(1, sdl.size());
    }

    @Test
    public void testTokenizingContractionPreserved() throws Exception {
        String pid = "peggym.130.4";
        sendSolrDeleteAll();
        addAllToSolr();
        assertPresentInSolrIndex(pid);
        SolrDocumentList sdl = null;
        sdl = findByField("text", "can't"); // exact match
        Assert.assertEquals(1, sdl.size());
        sdl = findByField("text", "cant"); // slop match
        Assert.assertEquals(1, sdl.size());
    }

    @Test
    public void testTokenizingCaseSensitive() throws Exception {
        String pid = "peggym.130.4";
        sendSolrDeleteAll();
        addAllToSolr();
        assertPresentInSolrIndex(pid);
        SolrDocumentList sdl = null;
        sdl = findByField("text", "upper");
        Assert.assertEquals(1, sdl.size());
        sdl = findByField("text", "UPPER");
        Assert.assertEquals(1, sdl.size());
        sdl = findByField("text", "LOWER");
        Assert.assertEquals(1, sdl.size());
        sdl = findByField("text", "lower");
        Assert.assertEquals(1, sdl.size());
    }

    @Test
    public void testWildcardSerach() throws Exception {
        String pid = "peggym.130.4";
        sendSolrDeleteAll();
        addAllToSolr();
        assertPresentInSolrIndex(pid);
        SolrDocumentList sdl = null;
        sdl = findByField("text", "fran*");
        Assert.assertEquals(1, sdl.size());
        sdl = findByField("text", "*ank");
        Assert.assertEquals(1, sdl.size());
        sdl = findByField("text", "fram*");
        Assert.assertEquals(0, sdl.size());
        sdl = findByField("text", "*ang");
        Assert.assertEquals(0, sdl.size());
    }

    @Test
    public void testTokenizingHyphen() throws Exception {
        String pid = "peggym.130.4";
        sendSolrDeleteAll();
        addAllToSolr();
        assertPresentInSolrIndex(pid);
        SolrDocumentList sdl = null;
        sdl = findByField("text", "TT-12");
        Assert.assertEquals(1, sdl.size());
        sdl = findByField("text", "TT12");
        Assert.assertEquals(1, sdl.size()); // not the same, should not return

        sdl = findByField("text", "long-term"); // exact match
        Assert.assertEquals(1, sdl.size());
        sdl = findByField("text", "longterm"); // slop match
        Assert.assertEquals(1, sdl.size());
        sdl = findByField("text", "term"); // word part match
        Assert.assertEquals(1, sdl.size());

        sdl = findByField("text", "12-34");
        Assert.assertEquals(1, sdl.size());
        sdl = findByField("text", "1234");
        Assert.assertEquals(1, sdl.size()); // not the same, should not match
    }

    private void addAllToSolr() throws Exception {
        peggym1271Sys = (Resource) context.getBean("peggym1271Sys");
        peggym1281Sys = (Resource) context.getBean("peggym1281Sys");
        peggym1291Sys = (Resource) context.getBean("peggym1291Sys");
        peggym1304Sys = (Resource) context.getBean("peggym1304Sys");
        tao129301Sys = (Resource) context.getBean("tao129301Sys");
        addEmlToSolrIndex(peggym1271Sys);
        addEmlToSolrIndex(peggym1281Sys);
        addEmlToSolrIndex(peggym1291Sys);
        addEmlToSolrIndex(peggym1304Sys);
        addEmlToSolrIndex(tao129301Sys);
    }
}
