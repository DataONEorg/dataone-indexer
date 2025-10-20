package org.dataone.cn.indexer.solrhttp;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * Test the DummySolrDoc class
 */
public class DummySolrDocTest {
    private static final String INDICATION_FIELD_NAME = "title";
    private static final String INDICATION_FIELD_VALUE =
        "dataone-indexer-placeholder-title-please-ignore";

    /**
     * Test the constructor
     * @throws Exception
     */
    @Test
    public void testDummySolrDocConstructor() throws Exception {
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

        RuntimeException e = assertThrows(RuntimeException.class,
                                                  () -> new DummySolrDoc(null, accessDoc));
        assertTrue(e.getMessage().contains(SolrElementField.FIELD_ID));

        e = assertThrows(RuntimeException.class,
                         () -> new DummySolrDoc("", accessDoc));
        assertTrue(e.getMessage().contains(SolrElementField.FIELD_ID));

        doc = new DummySolrDoc(pid1, null);
        assertEquals(3, doc.getFieldList().size());
        assertEquals("-1", doc.getField(SolrElementField.FIELD_VERSION).getValue());
        assertEquals(pid1, doc.getField(SolrElementField.FIELD_ID).getValue());
        assertEquals(INDICATION_FIELD_VALUE, doc.getField(INDICATION_FIELD_NAME).getValue());


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
