package org.dataone.cn.index;

import org.apache.solr.common.SolrDocumentList;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.core.io.Resource;

public class SolrTokenenizerTest extends DataONESolrJettyTestBase {

    private Resource peggym1271Sys;
    private Resource peggym1281Sys;
    private Resource peggym1291Sys;
    private Resource peggym1304Sys;

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
    public void testTokenizingHyphen() throws Exception {
        String pid = "peggym.130.4";
        sendSolrDeleteAll();
        addAllToSolr();
        assertPresentInSolrIndex(pid);
        SolrDocumentList sdl = null;
        sdl = findByField("text", "TT-12");
        Assert.assertEquals(1, sdl.size());
        sdl = findByField("text", "TT12");
        Assert.assertEquals(0, sdl.size()); // not the same, should not return

        sdl = findByField("text", "long-term"); // exact match
        Assert.assertEquals(1, sdl.size());
        sdl = findByField("text", "longterm"); // slop match
        Assert.assertEquals(1, sdl.size());
        sdl = findByField("text", "term"); // word part match
        Assert.assertEquals(1, sdl.size());

        sdl = findByField("text", "12-34");
        Assert.assertEquals(1, sdl.size());
        sdl = findByField("text", "1234");
        Assert.assertEquals(0, sdl.size()); // not the same, should not match
    }

    private void addAllToSolr() throws Exception {
        peggym1271Sys = (Resource) context.getBean("peggym1271Sys");
        peggym1281Sys = (Resource) context.getBean("peggym1281Sys");
        peggym1291Sys = (Resource) context.getBean("peggym1291Sys");
        peggym1304Sys = (Resource) context.getBean("peggym1304Sys");
        addToSolrIndex(peggym1271Sys);
        addToSolrIndex(peggym1281Sys);
        addToSolrIndex(peggym1291Sys);
        addToSolrIndex(peggym1304Sys);
    }
}