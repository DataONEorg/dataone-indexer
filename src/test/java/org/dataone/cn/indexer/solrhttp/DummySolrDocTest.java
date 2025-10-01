package org.dataone.cn.indexer.solrhttp;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.Test;

/**
 * Test the DummySolrDoc class
 */
public class DummySolrDocTest {
    private static final String INDICATION_FIELD_NAME = "abstract";
    private static final String INDICATION_FIELD_VALUE = "A placeholding document";

    /**
     * Test the constructor
     * @throws Exception
     */
    @Test
    public void testDummySolrDocConstructorAndRemoveArtificialFields() throws Exception {
        String pid = "pid";
        String sid = "sid";
        String pid1 = "pid1";
        String resourceMapId = "foo.1";
        String publicUser = "public";
        String user1 = "user1";
        String user2 = "user2";
        DummySolrDoc doc;
        SolrDoc accessDoc = new SolrDoc();
        accessDoc.addField(new SolrElementField(SolrElementField.FIELD_READPERMISSION, publicUser));
        accessDoc.addField(new SolrElementField(SolrElementField.FIELD_WRITEPERMISSION, user1));
        accessDoc.addField(new SolrElementField(SolrElementField.FIELD_WRITEPERMISSION, user2));

        try {
            doc = new DummySolrDoc(null, accessDoc);
            fail("Test cannot reach here since the id is null");
        } catch (RuntimeException e) {
            assertTrue(e.getMessage().contains(SolrElementField.FIELD_ID));
        }

        try {
            doc = new DummySolrDoc("", accessDoc);
            fail("Test cannot reach here since the id is blank");
        } catch (RuntimeException e) {
            assertTrue(e.getMessage().contains(SolrElementField.FIELD_ID));
        }

        doc = new DummySolrDoc(pid1, null);
        assertEquals(3, doc.getFieldList().size());
        assertEquals("-1", doc.getField(SolrElementField.FIELD_VERSION).getValue());
        assertEquals(pid1, doc.getField(SolrElementField.FIELD_ID).getValue());
        assertEquals(INDICATION_FIELD_VALUE, doc.getField(INDICATION_FIELD_NAME).getValue());
        doc.removeArtificialFields();
        assertEquals(1, doc.getFieldList().size());
        assertEquals(pid1, doc.getField(SolrElementField.FIELD_ID).getValue());

        doc = new DummySolrDoc(pid, accessDoc);
        assertEquals("-1", doc.getField(SolrElementField.FIELD_VERSION).getValue());
        assertEquals(pid, doc.getField(SolrElementField.FIELD_ID).getValue());
        assertNull(doc.getField(SolrElementField.FIELD_SERIES_ID));
        assertNull(doc.getField(SolrElementField.FIELD_SIZE));
        assertEquals(publicUser,
                     doc.getAllFieldValues(SolrElementField.FIELD_READPERMISSION).get(0));
        assertEquals(user1, doc.getAllFieldValues(SolrElementField.FIELD_WRITEPERMISSION).get(0));
        assertEquals(user2, doc.getAllFieldValues(SolrElementField.FIELD_WRITEPERMISSION).get(1));
        assertNull(doc.getField(SolrElementField.FIELD_CHANGEPERMISSION));
        doc.removeArtificialFields();
        assertEquals(1, doc.getFieldList().size());
        assertEquals(pid, doc.getField(SolrElementField.FIELD_ID).getValue());

        doc = new DummySolrDoc(sid, accessDoc);
        doc.addField(new SolrElementField(SolrElementField.FIELD_RESOURCEMAP, resourceMapId));
        assertEquals("-1", doc.getField(SolrElementField.FIELD_VERSION).getValue());
        assertEquals(sid, doc.getField(SolrElementField.FIELD_ID).getValue());
        assertNull(doc.getField(SolrElementField.FIELD_SIZE));
        assertEquals(publicUser,
                     doc.getAllFieldValues(SolrElementField.FIELD_READPERMISSION).get(0));
        assertEquals(user1, doc.getAllFieldValues(SolrElementField.FIELD_WRITEPERMISSION).get(0));
        assertEquals(user2, doc.getAllFieldValues(SolrElementField.FIELD_WRITEPERMISSION).get(1));
        assertNull(doc.getField(SolrElementField.FIELD_CHANGEPERMISSION));
        doc.removeArtificialFields();
        assertEquals(2, doc.getFieldList().size());
        assertEquals(sid, doc.getField(SolrElementField.FIELD_ID).getValue());
        // Since the resource map field is not the initial one, so it can be kept.
        assertEquals(resourceMapId, doc.getField(SolrElementField.FIELD_RESOURCEMAP).getValue());
    }

    /**
     * Test the getIndicationFieldName
     * @throws Exception
     */
    @Test
    public void testGetIndicationFieldName() throws Exception {
        assertEquals(INDICATION_FIELD_NAME, DummySolrDoc.getIndicationFieldName());
    }

    /**
     * Test the getIndicationFieldName
     * @throws Exception
     */
    @Test
    public void testGetIndicationFieldValue() throws Exception {
        assertEquals(INDICATION_FIELD_VALUE, DummySolrDoc.getIndicationFieldValue());
    }
}
