package org.dataone.cn.indexer.parser.utility;

import org.dataone.cn.index.DataONESolrJettyTestBase;
import org.dataone.cn.indexer.solrhttp.DummySolrDoc;
import org.dataone.cn.indexer.solrhttp.SolrDoc;
import org.dataone.cn.indexer.solrhttp.SolrElementField;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.carrotsearch.randomizedtesting.annotations.ThreadLeakScope;

@ThreadLeakScope(ThreadLeakScope.Scope.NONE)

/**
 * Test the RelationshipMergeUtility class
 */ public class RelationshipMergeUtilityTest extends DataONESolrJettyTestBase {

    private RelationshipMergeUtility mergeUtility;
    private String id = "foo.1.1";
    private String id2 = "foo.2.1";
    private String documentsId1 = "documents.1.1";
    private String documentsId2 = "documents.2.1";
    private String size = "222";
    private String size1 = "111";
    private String checksum = "checksum";
    private String publicUser = "public";
    private String publicUser1 = "public1";
    private String testUser = "test";
    private String documentedById1 = "documentedBy.1.1";
    private String documentedById2 = "documentedBy.2.1";
    private String resourceMapId1 = "resourceMap.1.1";
    private String resourceMapId2 = "resourceMap.2.1";
    private String resourceMapId3 = "resourceMap.3.1";
    private String resourceMapId4 = "resourceMap.4.1";
    private String version = "500";
    private String title = "tile";
    private String titleStr = "this is a tile";
    private String abstractStr = "this is a test";

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
        from.addField(new SolrElementField(SolrElementField.FIELD_VERSION, version));
        SolrDoc to = new SolrDoc();
        to.addField(new SolrElementField(SolrElementField.FIELD_ID, id));
        to.addField(new SolrElementField(SolrElementField.FIELD_SIZE, size));
        to.addField(new SolrElementField(SolrElementField.FIELD_CHANGEPERMISSION, testUser));
        to.addField(new SolrElementField(SolrElementField.FIELD_ISDOCUMENTEDBY, documentedById1));
        to.addField(new SolrElementField(SolrElementField.FIELD_ISDOCUMENTEDBY, documentedById2));
        to.addField(new SolrElementField(SolrElementField.FIELD_RESOURCEMAP, resourceMapId1));
        to.addField(new SolrElementField(SolrElementField.FIELD_RESOURCEMAP, resourceMapId3));
        to.addField(new SolrElementField(SolrElementField.FIELD_VERSION, "-1"));
        to.addField(new SolrElementField(SolrElementField.FIELD_VERSION, "1"));
        mergeUtility.merge(from, to);
        assertEquals(10, from.getFieldList().size());
        assertEquals(11, to.getFieldList().size());
        assertEquals(id, to.getAllFieldValues(SolrElementField.FIELD_ID).get(0));
        assertEquals(1, to.getAllFieldValues(SolrElementField.FIELD_ID).size());
        assertEquals(size, to.getAllFieldValues(SolrElementField.FIELD_SIZE).get(0));
        assertEquals(1, to.getAllFieldValues(SolrElementField.FIELD_SIZE).size());
        assertNull(to.getField(SolrElementField.FIELD_CHECKSUM));
        assertNull(to.getField(SolrElementField.FIELD_READPERMISSION));
        assertEquals(
            testUser, to.getAllFieldValues(SolrElementField.FIELD_CHANGEPERMISSION).get(0));
        assertEquals(1, to.getAllFieldValues(SolrElementField.FIELD_CHANGEPERMISSION).size());
        assertEquals(
            documentedById1,
            to.getAllFieldValues(SolrElementField.FIELD_ISDOCUMENTEDBY).get(0));
        assertEquals(
            documentedById2,
            to.getAllFieldValues(SolrElementField.FIELD_ISDOCUMENTEDBY).get(1));
        assertEquals(2, to.getAllFieldValues(SolrElementField.FIELD_ISDOCUMENTEDBY).size());
        assertEquals(documentsId1, to.getAllFieldValues(SolrElementField.FIELD_DOCUMENTS).get(0));
        assertEquals(documentsId2, to.getAllFieldValues(SolrElementField.FIELD_DOCUMENTS).get(1));
        assertEquals(2, to.getAllFieldValues(SolrElementField.FIELD_DOCUMENTS).size());
        assertEquals(
            resourceMapId1,
            to.getAllFieldValues(SolrElementField.FIELD_RESOURCEMAP).get(0));
        assertEquals(
            resourceMapId3,
            to.getAllFieldValues(SolrElementField.FIELD_RESOURCEMAP).get(1));
        assertEquals(
            resourceMapId2,
            to.getAllFieldValues(SolrElementField.FIELD_RESOURCEMAP).get(2));
        assertEquals(3, to.getAllFieldValues(SolrElementField.FIELD_RESOURCEMAP).size());
        assertEquals(version, to.getAllFieldValues(SolrElementField.FIELD_VERSION).get(0));
        assertEquals(1, to.getAllFieldValues(SolrElementField.FIELD_VERSION).size());
    }

    /**
     * Test the mergeRelationship method
     *
     * @throws Exception
     */
    @Test
    public void testMergeRelationship() throws Exception {
        // Both are null
        DummySolrDoc dummyDoc = null;
        SolrDoc doc = null;
        SolrDoc newDoc = mergeUtility.mergeRelationships(dummyDoc, doc);
        assertEquals(0, newDoc.getFieldList().size());
        // doc is null, but dummy is not
        dummyDoc = new DummySolrDoc(id, null);
        newDoc = mergeUtility.mergeRelationships(dummyDoc, doc);
        assertEquals(4, newDoc.getFieldList().size());
        assertEquals(id, newDoc.getFirstFieldValue(SolrElementField.FIELD_ID));
        assertEquals("-1", newDoc.getFirstFieldValue(SolrElementField.FIELD_VERSION));
        // dummy is null, but doc is not
        dummyDoc = null;
        doc = new SolrDoc();
        doc.addField(new SolrElementField(title, titleStr));
        doc.addField(new SolrElementField(DummySolrDoc.getIndicationFieldName(), abstractStr));
        doc.addField(new SolrElementField(SolrElementField.FIELD_ID, id2));
        doc.addField(new SolrElementField(SolrElementField.FIELD_SIZE, size1));
        doc.addField(new SolrElementField(SolrElementField.FIELD_CHECKSUM, checksum));
        doc.addField(new SolrElementField(SolrElementField.FIELD_READPERMISSION, publicUser));
        doc.addField(new SolrElementField(SolrElementField.FIELD_READPERMISSION, publicUser1));
        doc.addField(new SolrElementField(SolrElementField.FIELD_DOCUMENTS, documentsId1));
        doc.addField(new SolrElementField(SolrElementField.FIELD_DOCUMENTS, documentsId2));
        doc.addField(new SolrElementField(SolrElementField.FIELD_RESOURCEMAP, resourceMapId1));
        doc.addField(new SolrElementField(SolrElementField.FIELD_RESOURCEMAP, resourceMapId2));
        doc.addField(new SolrElementField(SolrElementField.FIELD_VERSION, version));
        newDoc = mergeUtility.mergeRelationships(dummyDoc, doc);
        assertEquals(12, newDoc.getFieldList().size());
        assertEquals(id2, newDoc.getFirstFieldValue(SolrElementField.FIELD_ID));
        assertEquals(size1, newDoc.getFirstFieldValue(SolrElementField.FIELD_SIZE));
        assertEquals(checksum, newDoc.getFirstFieldValue(SolrElementField.FIELD_CHECKSUM));
        assertEquals(publicUser,
            newDoc.getAllFieldValues(SolrElementField.FIELD_READPERMISSION).get(0));
        assertEquals(publicUser1,
            newDoc.getAllFieldValues(SolrElementField.FIELD_READPERMISSION).get(1));
        assertEquals(documentsId1,
            newDoc.getAllFieldValues(SolrElementField.FIELD_DOCUMENTS).get(0));
        assertEquals(documentsId2,
            newDoc.getAllFieldValues(SolrElementField.FIELD_DOCUMENTS).get(1));
        assertEquals(resourceMapId1,
            newDoc.getAllFieldValues(SolrElementField.FIELD_RESOURCEMAP).get(0));
        assertEquals(resourceMapId2,
            newDoc.getAllFieldValues(SolrElementField.FIELD_RESOURCEMAP).get(1));
        assertEquals(version, newDoc.getFirstFieldValue(SolrElementField.FIELD_VERSION));
        // both not null
        dummyDoc = new DummySolrDoc(id, null);
        try {
            mergeUtility.mergeRelationships(dummyDoc, doc);
            fail("Test shouldn't get there since the ids don't match");
        } catch (RuntimeException e) {
            assertTrue(e instanceof IllegalArgumentException);
        }
        dummyDoc = new DummySolrDoc(id2, null);
        dummyDoc.addField(new SolrElementField(SolrElementField.FIELD_ID, id));
        try {
            mergeUtility.mergeRelationships(dummyDoc, doc);
            fail("Test shouldn't get there since the dummy doc has two ids.");
        } catch (RuntimeException e) {
            assertTrue(e instanceof IllegalArgumentException);
        }
        // Do the merge job
        dummyDoc = new DummySolrDoc(id2, null);
        dummyDoc.addField(new SolrElementField(SolrElementField.FIELD_CHANGEPERMISSION, testUser));
        dummyDoc.addField(new SolrElementField(SolrElementField.FIELD_DOCUMENTS, documentsId1));
        dummyDoc.addField(new SolrElementField(SolrElementField.FIELD_DOCUMENTS, documentsId2));
        dummyDoc.addField(
            new SolrElementField(SolrElementField.FIELD_ISDOCUMENTEDBY, documentedById1));
        dummyDoc.addField(
            new SolrElementField(SolrElementField.FIELD_ISDOCUMENTEDBY, documentedById2));
        dummyDoc.addField(new SolrElementField(SolrElementField.FIELD_RESOURCEMAP, resourceMapId1));
        dummyDoc.addField(new SolrElementField(SolrElementField.FIELD_RESOURCEMAP, resourceMapId3));
        dummyDoc.addField(new SolrElementField(SolrElementField.FIELD_RESOURCEMAP, resourceMapId4));
        newDoc = mergeUtility.mergeRelationships(dummyDoc, doc);
        assertEquals(16, newDoc.getFieldList().size());
        assertEquals(titleStr, newDoc.getFirstFieldValue(title));
        assertEquals(abstractStr, newDoc.getFirstFieldValue(DummySolrDoc.getIndicationFieldName()));
        assertEquals(id2, newDoc.getFirstFieldValue(SolrElementField.FIELD_ID));
        assertEquals(size1, newDoc.getFirstFieldValue(SolrElementField.FIELD_SIZE));
        assertEquals(checksum, newDoc.getFirstFieldValue(SolrElementField.FIELD_CHECKSUM));
        assertEquals(publicUser,
            newDoc.getAllFieldValues(SolrElementField.FIELD_READPERMISSION).get(0));
        assertEquals(publicUser1,
            newDoc.getAllFieldValues(SolrElementField.FIELD_READPERMISSION).get(1));
        // The changePermission from dummy shouldn't be merged
        assertNull(newDoc.getField(SolrElementField.FIELD_CHANGEPERMISSION));
        assertEquals(documentsId1,
            newDoc.getAllFieldValues(SolrElementField.FIELD_DOCUMENTS).get(0));
        assertEquals(documentsId2,
            newDoc.getAllFieldValues(SolrElementField.FIELD_DOCUMENTS).get(1));
        assertEquals(documentedById1,
            newDoc.getAllFieldValues(SolrElementField.FIELD_ISDOCUMENTEDBY).get(0));
        assertEquals(documentedById2,
            newDoc.getAllFieldValues(SolrElementField.FIELD_ISDOCUMENTEDBY).get(1));
        assertEquals(resourceMapId1,
            newDoc.getAllFieldValues(SolrElementField.FIELD_RESOURCEMAP).get(0));
        assertEquals(resourceMapId2,
            newDoc.getAllFieldValues(SolrElementField.FIELD_RESOURCEMAP).get(1));
        assertEquals(resourceMapId3,
            newDoc.getAllFieldValues(SolrElementField.FIELD_RESOURCEMAP).get(2));
        assertEquals(resourceMapId4,
            newDoc.getAllFieldValues(SolrElementField.FIELD_RESOURCEMAP).get(3));
        assertEquals(version, newDoc.getFirstFieldValue(SolrElementField.FIELD_VERSION));
    }
}
