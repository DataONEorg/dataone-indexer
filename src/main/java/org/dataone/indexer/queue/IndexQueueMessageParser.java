/**
 * This work was created by participants in the DataONE project, and is
 * jointly copyrighted by participating institutions in DataONE. For 
 * more information on DataONE, see our web site at http://dataone.org.
 *
 *   Copyright 2022
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and 
 * limitations under the License.
 * 
 */
package org.dataone.indexer.queue;

import java.util.Map;

import org.apache.log4j.Logger;
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
    private final static String HEADER_ID = "id"; //The header name in the message to store the identifier
    private final static String HEADER_PATH = "path"; //The header name in the message to store the path of the object 
    private final static String HEADER_INDEX_TYPE = "index_type"; //The header name in the message to store the index type
    
    private Identifier identifier = null;
    private String objectPath = null;
    private String indexType = null;
    private int priority = 1;
    
    private static Logger logger = Logger.getLogger(IndexQueueMessageParser.class);
    
    /**
     * Parse the message from the index queue and store the information
     * @param properties
     * @param body
     * @throws InvalidRequest
     */
    public void parse(AMQP.BasicProperties properties, byte[] body) throws InvalidRequest {
        Map<String, Object> headers = properties.getHeaders();
        String pid = ((LongString)headers.get(HEADER_ID)).toString();
        if (pid == null || pid.trim().equals("")) {
        throw new InvalidRequest("0000", "The identifier cannot be null or blank in the index queue message.");
        }
        logger.debug("IndexQueueMessageParser.parse - the identifier in the message is " + pid);
        identifier.setValue(pid);
        
        indexType = ((LongString)headers.get(HEADER_INDEX_TYPE)).toString();
        if (indexType == null || indexType.trim().equals("")) {
        throw new InvalidRequest("0000", "The index type cannot be null or blank in the index queue message.");
        }
        logger.debug("IndexQueueMessageParser.parse - the index type in the message is " + indexType);
 
        Object pathObject = headers.get(HEADER_PATH);
        if (pathObject != null) {
            objectPath = ((LongString)pathObject).toString();
        }
        logger.debug("IndexQueueMessageParser.parse - the file path of the object which be indexed in the message is " + objectPath);
        
        priority = properties.getPriority();
        logger.debug("IndexQueueMessageParser.parse - the priority in the message is " + priority);
    }

    /**
     * Get the identifier after calling the parse method to parse the index queue message.
     * @return  the identifier. It shouldn't be null or blank
     */
    public Identifier getIdentifier() {
        return identifier;
    }

    /**
     * Get the file path of the object, which will be indexed, 
     * after calling the parse method to parse the index queue message.
     * @return  the file path of the object. It can be null or blank, which 
     * means we don't have the object in the system.
     */
    public String getObjectPath() {
        return objectPath;
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

}
