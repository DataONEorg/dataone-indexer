package org.dataone.indexer.queue;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

import java.util.HashMap;
import java.util.Map;

import org.dataone.service.exceptions.InvalidRequest;
import org.junit.Test;

import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.LongString;
import com.rabbitmq.client.impl.LongStringHelper;

/**
 * A junit test class for IndexQueueMessageParser
 * @author tao
 *
 */
public class IndexQueueMessageParserTest {
    //The header name in the message to store the identifier
    private final static String HEADER_ID = "id";
    //The header name in the message to store the index type
    private final static String HEADER_INDEX_TYPE = "index_type";
    private final static String HEADER_DOCID = "doc_id";

    /**
     * Test the invalid messages 
     * @throws Exception
     */
    @Test
    public void testInvalidRequest() throws Exception {
        LongString id = null;
        LongString index_type = LongStringHelper.asLongString("create");
        int priority = 1;
        LongString docId = LongStringHelper.asLongString("foo.1.1");
        AMQP.BasicProperties properties = generateProperties(id, index_type, priority, null);
        byte[] body = null;
        IndexQueueMessageParser parser = new IndexQueueMessageParser();
        try {
            parser.parse(properties, body);
            fail("Since the idenitifer is null, we shoulder get here");
        } catch (InvalidRequest e) {

        }

        id = LongStringHelper.asLongString(" ");
        index_type = LongStringHelper.asLongString("create");
        priority = 1;
        properties = generateProperties(id, index_type, priority, docId);
        try {
            parser.parse(properties, body);
            fail("Since the idenitifer is null, we shouldn't get here");
        } catch (InvalidRequest e) {

        }

        id = LongStringHelper.asLongString("foo");
        index_type = null;
        priority = 1;
        properties = generateProperties(id, index_type, priority, docId);
        try {
            parser.parse(properties, body);
            fail("Since the index type is null, we shouldn't get here");
        } catch (InvalidRequest e) {

        }

        id = LongStringHelper.asLongString("foo");
        index_type = LongStringHelper.asLongString("");
        priority = 1;
        properties = generateProperties(id, index_type, priority, null);
        try {
            parser.parse(properties, body);
            fail("Since the index type is null, we shouldn't get here");
        } catch (InvalidRequest e) {

        }
    }

    /**
     * Test valid messages
     * @throws Exception
     */
    @Test
    public void testParse() throws Exception {
        String id = "doi:10.5063/F1HX1B4Q";
        String indexType = "create";
        String docId = "foo.1.1";
        int priority = 1;
        LongString longId = LongStringHelper.asLongString(id);
        LongString longIndexType = LongStringHelper.asLongString(indexType);
        LongString longDocId = LongStringHelper.asLongString(docId);
        AMQP.BasicProperties properties = generateProperties(longId, longIndexType, priority,
                                                             longDocId);
        byte[] body = null;
        IndexQueueMessageParser parser = new IndexQueueMessageParser();
        parser.parse(properties, body);
        assertEquals(id, parser.getIdentifier().getValue());
        assertEquals(indexType, parser.getIndexType());
        assertEquals(priority, parser.getPriority());
        assertEquals(docId, parser.getDocId());

        id = "urn:uuid:45298965-f867-440c-841f-91d3abd729b7";
        indexType = "delete";
        priority = 2;
        docId = "foo.2.1";
        longId = LongStringHelper.asLongString(id);
        longIndexType = LongStringHelper.asLongString(indexType);
        longDocId = LongStringHelper.asLongString(docId);
        properties = generateProperties(longId, longIndexType, priority, longDocId);
        parser = new IndexQueueMessageParser();
        parser.parse(properties, body);
        assertEquals(id, parser.getIdentifier().getValue());
        assertEquals(indexType, parser.getIndexType());
        assertEquals(priority, parser.getPriority());
        assertEquals(docId, parser.getDocId());

        id = "urn:uuid:45298965-f867-440c-841f-000000";
        indexType = "create";
        priority = 1;
        longId = LongStringHelper.asLongString(id);
        longIndexType = LongStringHelper.asLongString(indexType);
        properties = generateProperties(longId, longIndexType, priority, null);
        parser = new IndexQueueMessageParser();
        parser.parse(properties, body);
        assertEquals(id, parser.getIdentifier().getValue());
        assertEquals(indexType, parser.getIndexType());
        assertEquals(priority, parser.getPriority());
        assertNull(parser.getDocId());

        id = "urn:uuid:45298965-f867-440c-841f-000000";
        indexType = "create";
        priority = 1;
        docId = "";
        longId = LongStringHelper.asLongString(id);
        longIndexType = LongStringHelper.asLongString(indexType);
        longDocId = LongStringHelper.asLongString(docId);
        properties = generateProperties(longId, longIndexType, priority, longDocId);
        parser = new IndexQueueMessageParser();
        parser.parse(properties, body);
        assertEquals(id, parser.getIdentifier().getValue());
        assertEquals(indexType, parser.getIndexType());
        assertEquals(priority, parser.getPriority());
        assertEquals(docId, parser.getDocId());

        id = "test-foo";
        indexType = "sysmeta";
        priority = 10;
        docId = "foo.3.1";
        longId = LongStringHelper.asLongString(id);
        longIndexType = LongStringHelper.asLongString(indexType);
        longDocId = LongStringHelper.asLongString(docId);
        properties = generateProperties(longId, longIndexType, priority, longDocId);
        parser = new IndexQueueMessageParser();
        parser.parse(properties, body);
        assertEquals(id, parser.getIdentifier().getValue());
        assertEquals(indexType, parser.getIndexType());
        assertEquals(priority, parser.getPriority());
        assertEquals(docId, parser.getDocId());

        id = "test-foo2";
        indexType = "sysmeta2";
        priority = 10;
        longId = LongStringHelper.asLongString(id);
        longIndexType = LongStringHelper.asLongString(indexType);
        properties = generateProperties(longId, longIndexType, priority, null);
        parser = new IndexQueueMessageParser();
        parser.parse(properties, body);
        assertEquals(id, parser.getIdentifier().getValue());
        assertEquals(indexType, parser.getIndexType());
        assertEquals(priority, parser.getPriority());
        assertNull(parser.getDocId());
    }

    /**
     * Generate the BasicProperties for the given values
     * @param id
     * @param indexType
     * @param priority
     * @param docId
     * @return
     */
    private AMQP.BasicProperties generateProperties(LongString id, LongString indexType,
                                                    int priority, LongString docId) {
        Map<String, Object> headers = new HashMap<String, Object>();
        headers.put(HEADER_ID, id);
        headers.put(HEADER_INDEX_TYPE, indexType);
        headers.put(HEADER_DOCID, docId);
        AMQP.BasicProperties basicProperties = new AMQP.BasicProperties.Builder()
                .contentType("text/plain")
                .deliveryMode(2) // set this message to persistent
                .priority(priority)
                .headers(headers)
                .build();
        return basicProperties;
    }

}
