package org.dataone.indexer.queue;

import static org.junit.Assert.assertTrue;
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
    private final static String HEADER_ID = "id"; //The header name in the message to store the identifier
    private final static String HEADER_PATH = "path"; //The header name in the message to store the path of the object 
    private final static String HEADER_INDEX_TYPE = "index_type"; //The header name in the message to store the index type
    
    /**
     * Test the invalid messages 
     * @throws Exception
     */
    @Test
    public void testInvalidRequest() throws Exception {
        LongString id = null;
        LongString index_type = LongStringHelper.asLongString("create");
        int priority = 1;
        LongString filePath = LongStringHelper.asLongString("foo");
        AMQP.BasicProperties properties = generateProperties(id, index_type, priority, filePath);
        byte[] body = null;
        IndexQueueMessageParser parser = new IndexQueueMessageParser();
        try {
            parser.parse(properties, body);
            fail("Since the idenitifer is null, we shoulder get here");
        } catch (InvalidRequest e) {
            //we should be here
        }
        
        id = LongStringHelper.asLongString(" ");
        index_type = LongStringHelper.asLongString("create");
        priority = 1;
        filePath = LongStringHelper.asLongString("foo");
        properties = generateProperties(id, index_type, priority, filePath);
        try {
            parser.parse(properties, body);
            fail("Since the idenitifer is null, we shouldn't get here");
        } catch (InvalidRequest e) {
            //we should be here
        }
        
        id = LongStringHelper.asLongString("foo");
        index_type = null;
        priority = 1;
        filePath = LongStringHelper.asLongString("foo");
        properties = generateProperties(id, index_type, priority, filePath);
        try {
            parser.parse(properties, body);
            fail("Since the index type is null, we shouldn't get here");
        } catch (InvalidRequest e) {
            //we should be here
        }
        
        id = LongStringHelper.asLongString("foo");
        index_type = LongStringHelper.asLongString("");
        priority = 1;
        filePath = LongStringHelper.asLongString("foo");
        properties = generateProperties(id, index_type, priority, filePath);
        try {
            parser.parse(properties, body);
            fail("Since the index type is null, we shouldn't get here");
        } catch (InvalidRequest e) {
            //we should be here
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
        int priority = 1;
        String filePath = "/var/metacat/12dfad";
        LongString longId = LongStringHelper.asLongString(id);
        LongString longIndexType = LongStringHelper.asLongString(indexType);
        LongString longFilePath = LongStringHelper.asLongString(filePath);
        AMQP.BasicProperties properties = generateProperties(longId, longIndexType, priority, longFilePath);
        byte[] body = null;
        IndexQueueMessageParser parser = new IndexQueueMessageParser();
        parser.parse(properties, body);
        assertTrue(parser.getIdentifier().getValue().equals(id));
        assertTrue(parser.getIndexType().equals(indexType));
        assertTrue(parser.getPriority() == priority);
        assertTrue(parser.getObjectPath().equals(filePath));
        
        id = "urn:uuid:45298965-f867-440c-841f-91d3abd729b7";
        indexType = "delete";
        priority = 2;
        filePath = "/var/metacat/12d-fad";
        longId = LongStringHelper.asLongString(id);
        longIndexType = LongStringHelper.asLongString(indexType);
        longFilePath = LongStringHelper.asLongString(filePath);
        properties = generateProperties(longId, longIndexType, priority, longFilePath);
        parser = new IndexQueueMessageParser();
        parser.parse(properties, body);
        assertTrue(parser.getIdentifier().getValue().equals(id));
        assertTrue(parser.getIndexType().equals(indexType));
        assertTrue(parser.getPriority() == priority);
        assertTrue(parser.getObjectPath().equals(filePath));

        id = "test-foo";
        indexType = "sysmeta";
        priority = 10;
        filePath = "c:\\foo\\abc";
        longId = LongStringHelper.asLongString(id);
        longIndexType = LongStringHelper.asLongString(indexType);
        longFilePath = LongStringHelper.asLongString(filePath);
        properties = generateProperties(longId, longIndexType, priority, longFilePath);
        parser = new IndexQueueMessageParser();
        parser.parse(properties, body);
        assertTrue(parser.getIdentifier().getValue().equals(id));
        assertTrue(parser.getIndexType().equals(indexType));
        assertTrue(parser.getPriority() == priority);
        assertTrue(parser.getObjectPath().equals(filePath));
    }
    
    /**
     * Generate the BasicProperties for the given values
     * @param id
     * @param index_type
     * @param priority
     * @param filePath
     * @return
     */
    private AMQP.BasicProperties generateProperties(LongString id, LongString index_type, int priority, LongString filePath) {
        Map<String, Object> headers = new HashMap<String, Object>();
        headers.put(HEADER_ID, id);
        headers.put(HEADER_INDEX_TYPE, index_type);
        if (filePath != null) {
            headers.put(HEADER_PATH, filePath);
        }
        AMQP.BasicProperties basicProperties = new AMQP.BasicProperties.Builder()
                .contentType("text/plain")
                .deliveryMode(2) // set this message to persistent
                .priority(priority)
                .headers(headers)
                .build();
        return basicProperties;
    }

}
