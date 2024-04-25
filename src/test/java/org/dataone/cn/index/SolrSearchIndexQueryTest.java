package org.dataone.cn.index;

import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.common.SolrDocumentList;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.core.io.Resource;

import com.carrotsearch.randomizedtesting.annotations.ThreadLeakScope;

@ThreadLeakScope(ThreadLeakScope.Scope.NONE)
public class SolrSearchIndexQueryTest extends DataONESolrJettyTestBase {

    private Resource peggym1304Sci;

    @Test
    public void testQueryForIdInFullTextField() throws Exception {
        sendSolrDeleteAll();
        loadTestResource();
        SolrDocumentList sdl = findByField("text", "peggym.130.4");
        Assert.assertEquals(1, sdl.size());
    }

    @Test
    public void testQueryForWordInAbstractInFullTextField() throws Exception {
        sendSolrDeleteAll();
        loadTestResource();
        SolrDocumentList sdl = findByField("text", "frank");
        Assert.assertEquals(1, sdl.size());
    }

    private void loadTestResource() throws Exception, SolrServerException {
        String pid = "peggym.130.4";
        peggym1304Sci = (Resource) context.getBean("peggym1304Sci");
        indexObjectToSolr(pid, peggym1304Sci);
        assertPresentInSolrIndex(pid);
    }
}
