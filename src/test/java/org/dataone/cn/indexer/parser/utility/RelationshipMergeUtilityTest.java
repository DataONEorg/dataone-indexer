package org.dataone.cn.indexer.parser.utility;

import org.dataone.cn.index.DataONESolrJettyTestBase;
import org.dataone.cn.indexer.solrhttp.SolrDoc;
import org.dataone.cn.indexer.solrhttp.SolrElementField;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.carrotsearch.randomizedtesting.annotations.ThreadLeakScope;

@ThreadLeakScope(ThreadLeakScope.Scope.NONE)

/**
 * Test the RelationshipMergeUtility class
 */
public class RelationshipMergeUtilityTest extends DataONESolrJettyTestBase {

    private RelationshipMergeUtility mergeUtility;

    @Before
    public void setUp() throws Exception {
        // Start up the embedded Jetty server and Solr service
        super.setUp();
        mergeUtility = getRelationshipMergeUtility();
    }

    @After
    public void tearDown() throws Exception {
        super.tearDown();

    }

    @Test
    public void testMerge() throws Exception {
        String id = "foo.1.1";
        String documentsId1 = "documents.1.1";
        String documentsId2 = "documents.2.1";
        String size = "222";
        String testUser = "test";
        String documentedById1 = "documentedBy.1.1";
        String documentedById2 = "documentedBy.2.1";
        String resourceMapId1 = "resourceMap.1.1";
        String resourceMapId2 = "resourceMap.2.1";
        String resourceMapId3 = "resourceMap.3.1";
        SolrDoc from = new SolrDoc();
        from.addField(new SolrElementField(SolrElementField.FIELD_ID, id));
        from.addField(new SolrElementField(SolrElementField.FIELD_SIZE, "111"));
        from.addField(new SolrElementField(SolrElementField.FIELD_CHECKSUM, "foo"));
        from.addField(new SolrElementField(SolrElementField.FIELD_READPERMISSION, "public"));
        from.addField(new SolrElementField(SolrElementField.FIELD_READPERMISSION, "public2"));
        from.addField(new SolrElementField(SolrElementField.FIELD_DOCUMENTS, documentsId1));
        from.addField(new SolrElementField(SolrElementField.FIELD_DOCUMENTS, documentsId2));
        from.addField(new SolrElementField(SolrElementField.FIELD_RESOURCEMAP, resourceMapId1));
        from.addField(new SolrElementField(SolrElementField.FIELD_RESOURCEMAP, resourceMapId2));
        SolrDoc to = new SolrDoc();
        to.addField(new SolrElementField(SolrElementField.FIELD_ID, id));
        to.addField(new SolrElementField(SolrElementField.FIELD_SIZE, size));
        to.addField(new SolrElementField(SolrElementField.FIELD_CHANGEPERMISSION, testUser));
        to.addField(new SolrElementField(SolrElementField.FIELD_ISDOCUMENTEDBY, documentedById1));
        to.addField(new SolrElementField(SolrElementField.FIELD_ISDOCUMENTEDBY, documentedById2));
        to.addField(new SolrElementField(SolrElementField.FIELD_RESOURCEMAP, resourceMapId1));
        to.addField(new SolrElementField(SolrElementField.FIELD_RESOURCEMAP, resourceMapId3));
        mergeUtility.merge(from, to);
        assertEquals(9, from.getFieldList().size());
        assertEquals(10, to.getFieldList().size());
        assertEquals(id, to.getAllFieldValues(SolrElementField.FIELD_ID).get(0));
        assertEquals(1, to.getAllFieldValues(SolrElementField.FIELD_ID).size());
        assertEquals(size, to.getAllFieldValues(SolrElementField.FIELD_SIZE).get(0));
        assertEquals(1, to.getAllFieldValues(SolrElementField.FIELD_SIZE).size());
        assertNull(to.getField(SolrElementField.FIELD_CHECKSUM));
        assertNull(to.getField(SolrElementField.FIELD_READPERMISSION));
        assertEquals(
            testUser, to.getAllFieldValues(SolrElementField.FIELD_CHANGEPERMISSION).get(0));
        assertEquals(1, to.getAllFieldValues(SolrElementField.FIELD_CHANGEPERMISSION).size());
        assertEquals(documentedById1,
                     to.getAllFieldValues(SolrElementField.FIELD_ISDOCUMENTEDBY).get(0));
        assertEquals(documentedById2,
                     to.getAllFieldValues(SolrElementField.FIELD_ISDOCUMENTEDBY).get(1));
        assertEquals(2, to.getAllFieldValues(SolrElementField.FIELD_ISDOCUMENTEDBY).size());
        assertEquals(documentsId1,
                     to.getAllFieldValues(SolrElementField.FIELD_DOCUMENTS).get(0));
        assertEquals(documentsId2,
                     to.getAllFieldValues(SolrElementField.FIELD_DOCUMENTS).get(1));
        assertEquals(2, to.getAllFieldValues(SolrElementField.FIELD_DOCUMENTS).size());
        assertEquals(resourceMapId1,
                     to.getAllFieldValues(SolrElementField.FIELD_RESOURCEMAP).get(0));
        assertEquals(resourceMapId3,
                     to.getAllFieldValues(SolrElementField.FIELD_RESOURCEMAP).get(1));
        assertEquals(resourceMapId2,
                     to.getAllFieldValues(SolrElementField.FIELD_RESOURCEMAP).get(2));
        assertEquals(3, to.getAllFieldValues(SolrElementField.FIELD_RESOURCEMAP).size());
    }
}
