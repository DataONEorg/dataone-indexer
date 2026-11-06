package org.dataone.indexer.queue;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

import java.io.FileInputStream;
import java.time.Instant;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.commons.io.IOUtils;
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
        assertNull(parser.getSystemMetadata());

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
        assertNull(parser.getSystemMetadata());

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
        assertNull(parser.getSystemMetadata());

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
        assertNull(parser.getSystemMetadata());

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
        assertNull(parser.getSystemMetadata());

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
        assertNull(parser.getSystemMetadata());
    }

    /**
     * Test the scenarios of the embedded system metadata in the message
     * @throws Exception
     */
    @Test
    public void testEmbeddedSystemMetadata() throws Exception {
        String filePath =
            "src/test/resources/org/dataone/cn/index/resources/d1_testdocs/data/data-system"
                + "-metadata.xml";
        String sysMetaStr = IOUtils.toString(new FileInputStream(filePath), "UTF-8");
        String id = "doi:10.5063/F1HX1B4Q";
        String indexType = "create";
        String docId = "foo.1.1";
        int priority = 1;
        LongString longId = LongStringHelper.asLongString(id);
        LongString longIndexType = LongStringHelper.asLongString(indexType);
        LongString longDocId = LongStringHelper.asLongString(docId);
        AMQP.BasicProperties properties = generateProperties(longId, longIndexType, priority,
                                                             longDocId);
        // Body is a non-json structure
        byte[] body = "hello".getBytes();
        IndexQueueMessageParser parser = new IndexQueueMessageParser();
        parser.parse(properties, body);
        assertEquals(id, parser.getIdentifier().getValue());
        assertEquals(indexType, parser.getIndexType());
        assertEquals(priority, parser.getPriority());
        assertEquals(docId, parser.getDocId());
        assertNull(parser.getSystemMetadata());

        // Body is a json structure without system metadata
        body = buildJsonBody("name", "value");
        parser.parse(properties, body);
        assertEquals(id, parser.getIdentifier().getValue());
        assertEquals(indexType, parser.getIndexType());
        assertEquals(priority, parser.getPriority());
        assertEquals(docId, parser.getDocId());
        assertNull(parser.getSystemMetadata());

        // Body is a json structure with non system metadata. The name is correct
        body = buildJsonBody(IndexQueueMessageParser.SYSMETA_TAG, "hello");
        parser.parse(properties, body);
        assertEquals(id, parser.getIdentifier().getValue());
        assertEquals(indexType, parser.getIndexType());
        assertEquals(priority, parser.getPriority());
        assertEquals(docId, parser.getDocId());
        assertNull(parser.getSystemMetadata());

        // Body is a json structure with system metadata. But the name is not sysmeta
        body = buildJsonBody("name", sysMetaStr);
        parser.parse(properties, body);
        assertEquals(id, parser.getIdentifier().getValue());
        assertEquals(indexType, parser.getIndexType());
        assertEquals(priority, parser.getPriority());
        assertEquals(docId, parser.getDocId());
        assertNull(parser.getSystemMetadata());

        // Body is a json structure with system metadata. The name is correct. But the id doesn't
        // match
        body = buildJsonBody(IndexQueueMessageParser.SYSMETA_TAG, sysMetaStr);
        parser.parse(properties, body);
        assertEquals(id, parser.getIdentifier().getValue());
        assertEquals(indexType, parser.getIndexType());
        assertEquals(priority, parser.getPriority());
        assertEquals(docId, parser.getDocId());
        assertNull(parser.getSystemMetadata());

        // Body is a json structure with system metadata. The name is correct and the id does match
        id = "urn:uuid:c30472f4-5b97-4a2b-b35d-e78eba19f77d";
        longId = LongStringHelper.asLongString(id);
        properties = generateProperties(longId, longIndexType, priority,
                                                             longDocId);
        body = buildJsonBody(IndexQueueMessageParser.SYSMETA_TAG, sysMetaStr);
        parser.parse(properties, body);
        assertEquals(id, parser.getIdentifier().getValue());
        assertEquals(indexType, parser.getIndexType());
        assertEquals(priority, parser.getPriority());
        assertEquals(docId, parser.getDocId());
        assertNotNull(parser.getSystemMetadata());
        assertEquals(id, parser.getSystemMetadata().getIdentifier().getValue());
        assertEquals("http://orcid.org/0000-0002-1209-5268",
                     parser.getSystemMetadata().getSubmitter().getValue());
        assertEquals(23, parser.getSystemMetadata().getSize().intValue());
        assertEquals("c5e0815b9d729df135968d535bd4a6de",
            parser.getSystemMetadata().getChecksum().getValue());
        assertEquals(Date.from(Instant.parse("2026-10-06T19:51:47.732+00:00")),
            parser.getSystemMetadata().getDateUploaded());
        assertEquals(Date.from(Instant.parse("2026-10-06T19:51:47.841+00:00")),
                     parser.getSystemMetadata().getDateSysMetadataModified());
        assertEquals("letter", parser.getSystemMetadata().getFileName());
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

    private byte[] buildJsonBody(String name, String value) throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();
        ObjectNode message = objectMapper.createObjectNode();
        message.put(name, value);
        return objectMapper.writeValueAsBytes(message);
    }

}
