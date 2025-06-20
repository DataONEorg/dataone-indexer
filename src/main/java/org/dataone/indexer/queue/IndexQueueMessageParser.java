package org.dataone.indexer.queue;

import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.dataone.service.exceptions.InvalidRequest;
import org.dataone.service.types.v1.Identifier;

import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.LongString;

/**
 * This class parses the messages coming from the index queue and 
 * store the information in its fields
 * @author tao
 *
 */
public class IndexQueueMessageParser {
    //The header name in the message to store the identifier
    private final static String HEADER_ID = "id";
    //The header name in the message to store the index type
    private final static String HEADER_INDEX_TYPE = "index_type";
    //The header name in the message to store the docid of the object
    private final static String HEADER_DOCID = "doc_id";
    private Identifier identifier = null;
    private String indexType = null;
    private int priority = 1;
    private String docId = null;

    private static Log logger = LogFactory.getLog(IndexQueueMessageParser.class);
    
    /**
     * Parse the message from the index queue and store the information
     * @param properties
     * @param body
     * @throws InvalidRequest
     */
    public void parse(AMQP.BasicProperties properties, byte[] body) throws InvalidRequest {
        if(properties == null) {
            throw new InvalidRequest("0000", "The properties, which contains the index task info, "
                                    + "cannot be null in the index queue message.");
        }
        Map<String, Object> headers = properties.getHeaders();
        if(headers == null) {
            throw new InvalidRequest("0000", "The header of the properties, which contains the "
                                + "index task info, cannot be null in the index queue message.");
        }
        Object pidObj = headers.get(HEADER_ID);
        if (pidObj == null) {
            throw new InvalidRequest(
                "0000", "The identifier cannot be null in the index queue message.");
        }
        String pid = ((LongString)pidObj).toString();
        if (pid == null || pid.isBlank()) {
            throw new InvalidRequest(
                "0000", "The identifier cannot be null or blank in the index queue message.");
        }
        logger.debug("IndexQueueMessageParser.parse - the identifier in the message is " + pid);
        identifier = new Identifier();
        identifier.setValue(pid);

        Object typeObj = headers.get(HEADER_INDEX_TYPE);
        if (typeObj == null) {
            throw new InvalidRequest(
                "0000", "The index type cannot be null in the index queue message for " + pid);
        }
        indexType = ((LongString)typeObj).toString();
        if (indexType == null || indexType.isBlank()) {
            throw new InvalidRequest(
                "0000",
                "The index type cannot be null or blank in the index queue message for " + pid);
        }
        logger.debug("The index type in the message is " + indexType + " for " + pid);
        Object docIdObject = headers.get(HEADER_DOCID);
        if (docIdObject != null) {
            docId = ((LongString)docIdObject).toString();
        }
        logger.debug(
            "The docId of the object which will be indexed in the message is " + docId +
                " for " + pid);

        try {
            priority = properties.getPriority();
        } catch (NullPointerException e) {
            logger.info(
                "IndexQueueMessageParser.parse - the priority is not set in the message and we "
                    + "will set it to 1.");
            priority = 1;
        }
        logger.debug(
            "IndexQueueMessageParser.parse - the priority in the message is " + priority + " for "
                + pid);
    }

    /**
     * Get the identifier after calling the parse method to parse the index queue message.
     * @return  the identifier. It shouldn't be null or blank
     */
    public Identifier getIdentifier() {
        return identifier;
    }

    /**
     * Get the type of the index task after calling the parse method to parse the index queue message.
     * @return  the type of the index task. It can be create, delete or sysmeta.
     */
    public String getIndexType() {
        return indexType;
    }

    /**
     * Get the priority of the index task after calling the parse method to parse the index queue message.
     * @return  the priority of the index task
     */
    public int getPriority() {
        return priority;
    }

    /**
     * Get the docId of the object, which will be indexed,
     * after calling the parse method to parse the index queue message.
     * @return  the docId of the object. DocId is an iternal id of Metacat, which is a file name in
     * the system. It can be null or blank, which means we don't have the object in the system.
     */
    public String getDocId() {
        return docId;
    }

}
