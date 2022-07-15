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

package org.dataone.cn.index;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeoutException;

import org.apache.log4j.Logger;
import org.dataone.configuration.Settings;

import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.Consumer;
import com.rabbitmq.client.DefaultConsumer;
import com.rabbitmq.client.Envelope;

/**
 * Worker class to process index tasks and submit results to store.
 */
public class IndexWorker {
    
    //Those strings are the types of the index tasks.
    //The create is the index task type for the action when a new object was created. So the solr index will be generated.
    //delete is the index task type for the action when an object was deleted. So the solr index will be deleted
    //sysmeta is the index task type for the action when the system metadata of an existing object was updated. 
    public final static String CREATE_INDEXT_TYPE = "create";
    public final static String DELETE_INDEX_TYPE = "delete";
    public final static String SYSMETA_CHANGE_TYPE = "sysmeta"; //this handle for resource map only
    
    public final static int HIGHEST_PRIORITY = 4; // some special cases
    public final static int HIGH_PRIORITY = 3; //use for the operations such as create, update
    public final static int MEDIUM_PRIORITY = 2; //use for the operations such as updateSystem, delete, archive
    public final static int LOW_PRIORITY = 1; //use for the bulk operations such as reindexing the whole corpus 
    
    private final static String HEADER_ID = "id"; //The header name in the message to store the identifier
    private final static String HEADER_PATH = "path"; //The header name in the message to store the path of the object 
    private final static String HEADER_INDEX_TYPE = "index_type"; //The header name in the message to store the index type
    
    private final static String EXCHANGE_NAME = "dataone-index";
    private final static String INDEX_QUEUE_NAME = "index";
    private final static String INDEX_ROUTING_KEY = "index";
    
    // Default values for the RabbitMQ message broker server. The value of 'localhost' is valid for
    // a RabbitMQ server running on a 'bare metal' server, inside a VM, or within a Kubernetes
    // where Mmetacat and the RabbitMQ server are running in containers that belong
    // to the same Pod. These defaults will be used if the properties file cannot be read.
    private static String RabbitMQhost = Settings.getConfiguration().getString("index.rabbitmq.hostname", "localhost");
    private static int RabbitMQport = Settings.getConfiguration().getInt("index.rabbitmq.hostport", 5672);
    private static String RabbitMQusername = Settings.getConfiguration().getString("index.rabbitmq.username", "guest");
    private static String RabbitMQpassword = Settings.getConfiguration().getString("index.rabbitmq.password", "guest");
    private static int RabbitMQMaxPriority = Settings.getConfiguration().getInt("index.rabbitmq.max.priority");
    private static String dataRootDir = Settings.getConfiguration().getString("index.data.root.directory", "/var/metacat/");
    private static String docRootDir = Settings.getConfiguration().getString("index.document.root.directory", "/var/metacat/");
    private static Connection RabbitMQconnection = null;
    private static Channel RabbitMQchannel = null;
    private static Logger logger = Logger.getLogger(IndexWorker.class.getName());

    /**
     * Commandline main for the IndexWorker to be started.
     * @param args
     * @throws TimeoutException 
     * @throws IOException 
     */
    public static void main(String[] args) throws IOException, TimeoutException {

        System.out.println("Starting index worker...");
        IndexWorker worker = new IndexWorker();

        worker.handleIndexTask("123");
        System.out.println("Done.");
    }
    
    /**
     * Default constructor to initialize the RabbitMQ service
     * @throws IOException
     * @throws TimeoutException
     */
    public IndexWorker() throws IOException, TimeoutException {
        init();
    }
    /**
     * Initialize the RabbitMQ service
     * @throws IOException 
     * @throws TimeoutException 
     * @throws ServiceException
     */
    private void init() throws IOException, TimeoutException {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost(RabbitMQhost);
        factory.setPort(RabbitMQport);
        factory.setPassword(RabbitMQpassword);
        factory.setUsername(RabbitMQusername);
        // connection that will recover automatically
        factory.setAutomaticRecoveryEnabled(true);
        // attempt recovery every 10 seconds after a failure
        factory.setNetworkRecoveryInterval(10000);
        logger.debug("IndexWorker.init - Set RabbitMQ host to: " + RabbitMQhost);
        logger.debug("IndexWorker.init - Set RabbitMQ port to: " + RabbitMQport);

        // Setup the 'InProcess' queue with a routing key - messages consumed by this queue require that
        // this routine key be used. The routine key INDEX_ROUTING_KEY sends messages to the index worker,
        boolean durable = true;
        RabbitMQconnection = factory.newConnection();
        RabbitMQchannel = RabbitMQconnection .createChannel();
        RabbitMQchannel.exchangeDeclare(EXCHANGE_NAME, "direct", durable);

        boolean exclusive = false;
        boolean autoDelete = false;
        Map<String, Object> argus = new HashMap<String, Object>();
        argus.put("x-max-priority", RabbitMQMaxPriority);
        logger.debug("IndexWorker.init - Set RabbitMQ max priority to: " + RabbitMQMaxPriority);
        RabbitMQchannel.queueDeclare(INDEX_QUEUE_NAME, durable, exclusive, autoDelete, argus);
        RabbitMQchannel.queueBind(INDEX_QUEUE_NAME, EXCHANGE_NAME, INDEX_ROUTING_KEY);
        
        // Channel will only send one request for each worker at a time.
        RabbitMQchannel.basicQos(1);
        logger.info("IndexWorker.init - Connected to RabbitMQ queue " + INDEX_QUEUE_NAME);
        
        if(dataRootDir != null && !dataRootDir.endsWith("/")) {
            dataRootDir = dataRootDir + "/";
        }
        logger.debug("IndexWorker.init - The data root directory is " + dataRootDir);
        if (docRootDir != null && !docRootDir.endsWith("/")) {
            docRootDir = docRootDir + "/";
        }
        logger.debug("IndexWorker.init - The metadata root directory is " + docRootDir);
    }

    /**
     * Callback for processing a specific indexing task
     * @param message the message describing the task to be processed
     */
    public void handleIndexTask(String message) {
        System.out.println("Handling task: " + message);
        // TODO: Handle the index task
    }
    
    /**
     * Worker start to consume messages from the index queue  - calling SolrIndex to 
     * process index tasks and submit results to store.
     * @throws IOException
     */
    public void start() throws IOException {
        final Consumer consumer = new DefaultConsumer(RabbitMQchannel) {
            @Override
            public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body) 
                                       throws IOException {
                Map<String, Object> headers = properties.getHeaders();
                String identifier = (String)headers.get(HEADER_ID);
                String indexType = (String)headers.get(HEADER_INDEX_TYPE);
                String filePath = (String)headers.get(HEADER_PATH);
                int priority = properties.getPriority();
                logger.info("IndexWorker.consumer.handleDelivery - Received the index task from the index queue with the identifier: "+
                            identifier + " , the index type: " + indexType + ", the file path (null means not to have): " + filePath + 
                            ", the priotity: " + priority);
            }
         };
         logger.info("IndexWorker.start - Calling basicConsume");
         boolean autoAck = true;
         RabbitMQchannel.basicConsume(INDEX_QUEUE_NAME, autoAck, consumer);
    }
    
    /**
     * Stop the RabbitMQ connection
     * @throws TimeoutException 
     * @throws IOException 
     */
    public void stop() throws IOException, TimeoutException {
        RabbitMQchannel.close();
        RabbitMQconnection.close();
        logger.info("IndexWorker.stop - stop the index queue conection.");
    }
}
