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

package org.dataone.cn.indexer;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeoutException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathExpressionException;

import org.apache.commons.codec.EncoderException;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.log4j.Logger;
import org.apache.solr.client.solrj.SolrServerException;
import org.dataone.cn.indexer.annotation.OntologyModelService;
import org.dataone.cn.indexer.object.ObjectManager;
import org.dataone.configuration.Settings;
import org.dataone.exceptions.MarshallingException;
import org.dataone.indexer.queue.IndexQueueMessageParser;
import org.dataone.service.exceptions.InvalidRequest;
import org.dataone.service.exceptions.InvalidToken;
import org.dataone.service.exceptions.NotAuthorized;
import org.dataone.service.exceptions.NotFound;
import org.dataone.service.exceptions.NotImplemented;
import org.dataone.service.exceptions.ServiceFailure;
import org.dataone.service.exceptions.UnsupportedType;
import org.dataone.service.types.v1.Identifier;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.xml.sax.SAXException;

import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.Consumer;
import com.rabbitmq.client.DefaultConsumer;
import com.rabbitmq.client.Envelope;
import com.rabbitmq.client.LongString;




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
    
    private static final String springConfigFileURL = "/index-parser-context.xml";
    private static final String ENV_NAME_OF_PROPERTIES_FILE = "DATAONE_INDEXER_CONFIG";
    
    private static Logger logger = Logger.getLogger(IndexWorker.class);
    private static String defaultExternalPropertiesFile = "/etc/dataone/dataone-indexer.properties";
    
    protected static String propertyFilePath = null;
    protected boolean multipleThread = true;
    protected int nThreads = 1;
   
    private String RabbitMQhost = null;
    private int RabbitMQport = 0;
    private String RabbitMQusername = null;
    private String RabbitMQpassword = null;
    private int RabbitMQMaxPriority = 10;
    private Connection RabbitMQconnection = null;
    private Channel RabbitMQchannel = null;
    private ApplicationContext context = null;
    protected SolrIndex solrIndex = null;
    private String specifiedThreadNumberStr = null;
    private int specifiedThreadNumber = 0;
    private ExecutorService executor = null;
    
    
    /**
     * Commandline main for the IndexWorker to be started.
     * @param args
     * @throws TimeoutException 
     * @throws IOException 
     * @throws ServiceFailure 
     */
    public static void main(String[] args) throws IOException, TimeoutException, ServiceFailure {
        logger.info("IndexWorker.main - Starting index worker...");
        String propertyFile = null;
        loadExternalPropertiesFile(propertyFile);
        IndexWorker worker = new IndexWorker();
        worker.start();
    }
    
    /**
     * Load properties from an external file.  
     * DataONE-indexer will try to load the property file by this order
     * 1. try to read it from the user specified
     * 2. try to read it from an env variable - DATAONE_INDEXER_CONFIG.
     * 3  try to use the default path - /etc/dataone/dataone-indexer.properties
     * If all attempts fail, it will give up and use the one embedded in the jar file
     * @param propertyFile  the property file user specified
     */
    public static void loadExternalPropertiesFile(String propertyFile) {
        // try the users specified path
        if (propertyFile != null && !propertyFile.trim().equals("")) {
            propertyFilePath = propertyFile;
            logger.info("IndexWorker.loadExternalPropertiesFile - the configuration path specified by users is " + propertyFilePath);
            File defaultFile = new File (propertyFilePath);
            if (defaultFile.exists() && defaultFile.canRead()) {
                logger.info("IndexWorker.loadExternalPropertiesFile - the configuration path users specified is  " + 
                            propertyFilePath + ". The file exists and is readable. So it will be used.");
            } else {
                logger.info("IndexWorker.loadExternalPropertiesFile - the configuration path users specified is  " + 
                        propertyFilePath + ". But the file does NOT exist or is NOT readable. So it will NOT be used.");
                propertyFilePath = null;
            }
        } 
        
        //try the path from the env variable 
        if (propertyFilePath == null || propertyFilePath.trim().equals("")) {
            propertyFilePath = System.getenv(ENV_NAME_OF_PROPERTIES_FILE);
            logger.info("IndexWorker.loadExternalPropertiesFile - the configuration path from the env variable is " + propertyFilePath);
            if (propertyFilePath != null && !propertyFilePath.trim().equals("")) {
                File defaultFile = new File (propertyFilePath);
                if (defaultFile.exists() && defaultFile.canRead()) {
                    logger.info("IndexWorker.loadExternalPropertiesFile - the configuration path can be read from the env variable " + ENV_NAME_OF_PROPERTIES_FILE +
                               " and its value is " + propertyFilePath + ". The file exists and it will be used.");
                } else {
                    logger.info("IndexWorker.loadExternalPropertiesFile - the configuration path can be read from the env variable " + ENV_NAME_OF_PROPERTIES_FILE +
                            " and its value is " + propertyFilePath + ". But the file does NOT exist or is NOT readable. So it will NOT be used.");
                    propertyFilePath = null;
                }
            }
        }
        
        //The attempts to read the configuration file specified by users and from the env variable failed. We will try the default external path
        if (propertyFilePath == null || propertyFilePath.trim().equals("")) {
            File defaultFile = new File (defaultExternalPropertiesFile);
            if (defaultFile.exists() && defaultFile.canRead()) {
                logger.info("IndexWorker.loadExternalPropertiesFile - the configure path can't be read either by users specified or from the env variable " + ENV_NAME_OF_PROPERTIES_FILE +
                           ". However, the default external file " + defaultExternalPropertiesFile + " exists and it will be used.");
                propertyFilePath = defaultExternalPropertiesFile;
            }
        }
        if (propertyFilePath != null && !propertyFilePath.trim().equals("")) {
            try {
                //Settings.getConfiguration();
                Settings.augmentConfiguration(propertyFilePath);
                logger.info("IndexWorker.loadExternalPropertiesFile - loaded the properties from the file " + propertyFilePath);
            } catch (ConfigurationException e) {
               logger.error("IndexWorker.loadExternalPropertiesFile - can't load any properties from the file " + propertyFilePath + 
                            " since " + e.getMessage() + ". It will use the default properties in the jar file.");
            }
        } else {
            logger.info("IndexWorker.loadExternalPropertiesFile - can't load an external properties file from the env variable " +
                    ENV_NAME_OF_PROPERTIES_FILE + " or from the default path " + defaultExternalPropertiesFile + ". Dataone-indexer will use the properties file embedded in the jar file");
        }
    }
    
    /**
     * Load an additional property file to the worker when it is necessary.
     * The main reason to have this method is that metacat has two property files
     * - the site property file and metacat property file. We should use this
     * method to load the site property file after loading the metacat property file
     * @param propertyFile
     */
    public static void loadAdditionalPropertyFile (String propertyFile) {
        if (propertyFile != null && !propertyFile.trim().equals("")) {
            try {
                //Settings.getConfiguration();
                Settings.augmentConfiguration(propertyFile);
                logger.info("IndexWorker.loadAdditionalPropertyFile - loaded the properties from the file " + propertyFile);
            } catch (ConfigurationException e) {
               logger.error("IndexWorker.loadAdditionalPropertyFile - can't load any properties from the file " + propertyFile + 
                            " since " + e.getMessage() + ".");
            }
        } else {
            logger.info("IndexWorker.loadAdditionalPropertyFile - can't load an additional property file since its path is null or blank.");
        }
    }
    
    /**
     * Default constructor to initialize the RabbitMQ service
     * @throws IOException
     * @throws TimeoutException
     * @throws ServiceFailure 
     */
    public IndexWorker() throws IOException, TimeoutException, ServiceFailure {
        this(true);
    }
    
    /**
     * Constructor with/without initialization
     * @param initialize  if we need to initialize RabittMQ and et al
     * @throws IOException
     * @throws TimeoutException
     * @throws ServiceFailure
     */
    public IndexWorker(Boolean initialize) throws IOException, TimeoutException, ServiceFailure {
        if (initialize) {
            initExecutorService();//initialize the executor first
            initIndexQueue();
            initIndexParsers();
            ObjectManager.getInstance();
            OntologyModelService.getInstance();
        }
    }
    
    /**
     * Initialize the RabbitMQ service
     * @throws IOException 
     * @throws TimeoutException 
     * @throws ServiceException
     */
    private void initIndexQueue() throws IOException, TimeoutException {
        RabbitMQhost = Settings.getConfiguration().getString("index.rabbitmq.hostname", "localhost");
        RabbitMQport = Settings.getConfiguration().getInt("index.rabbitmq.hostport", 5672);
        RabbitMQusername = Settings.getConfiguration().getString("index.rabbitmq.username", "guest");
        RabbitMQpassword = Settings.getConfiguration().getString("index.rabbitmq.password", "guest");
        RabbitMQMaxPriority = Settings.getConfiguration().getInt("index.rabbitmq.max.priority");
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost(RabbitMQhost);
        factory.setPort(RabbitMQport);
        factory.setPassword(RabbitMQpassword);
        factory.setUsername(RabbitMQusername);
        // connection that will recover automatically
        factory.setAutomaticRecoveryEnabled(true);
        // attempt recovery every 10 seconds after a failure
        factory.setNetworkRecoveryInterval(10000);
        logger.debug("IndexWorker.initIndexQueue - Set RabbitMQ host to: " + RabbitMQhost);
        logger.debug("IndexWorker.initIndexQueue - Set RabbitMQ port to: " + RabbitMQport);

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
        logger.debug("IndexWorker.initIndexQueue - Set RabbitMQ max priority to: " + RabbitMQMaxPriority);
        RabbitMQchannel.queueDeclare(INDEX_QUEUE_NAME, durable, exclusive, autoDelete, argus);
        RabbitMQchannel.queueBind(INDEX_QUEUE_NAME, EXCHANGE_NAME, INDEX_ROUTING_KEY);
        
        logger.info("IndexWorker.initIndexQueue - the allowed unacknowledged message(s) number is " + nThreads);
        RabbitMQchannel.basicQos(nThreads);
        logger.debug("IndexWorker.initIndexQueue - Connected to the RabbitMQ queue with the name of " + INDEX_QUEUE_NAME);
    }
    
    /**
     * Initialize the solrIndex object which contains the index parsers.
     */
    protected void initIndexParsers() {
        if (context == null) {
            synchronized(IndexWorker.class) {
                if (context == null) {
                    context = new ClassPathXmlApplicationContext(springConfigFileURL);
                }
            }
        }
        solrIndex = (SolrIndex)context.getBean("solrIndex");
    }
    
    /**
     * Determine the size of the thread pool and initialize the executor service
     */
    protected void initExecutorService() {
        specifiedThreadNumberStr = Settings.getConfiguration().getString("index.thread.number", "0");
        try {
            specifiedThreadNumber = (new Integer(specifiedThreadNumberStr)).intValue();
        } catch (NumberFormatException e) {
            specifiedThreadNumber = 0;
            logger.warn("IndexWorker.initExecutorService - IndexWorker cannot parse the string " + specifiedThreadNumberStr +
                     " specified by property index.thread.number into a number since " + e.getLocalizedMessage() + 
                     ". The default value 0 will be used as the specified value");
        }
        int availableProcessors = Runtime.getRuntime().availableProcessors();
        availableProcessors = availableProcessors - 1;
        nThreads = Math.max(1, availableProcessors); //the default threads number
        if (specifiedThreadNumber > 0 && specifiedThreadNumber < nThreads) {
            nThreads = specifiedThreadNumber; //use the specified number in the property file
        }
        if (nThreads != 1) {
            logger.info("IndexWorker.initExecutorService - the size of index thread pool specified in the propery file is " + specifiedThreadNumber +
                    ". The size computed from the available processors is " + availableProcessors + 
                     ". Final computed thread pool size for index executor: " + nThreads);
            executor = Executors.newFixedThreadPool(nThreads);
            multipleThread = true;
        } else {
            logger.info("IndexWorker.initExecutorService - the size of index thread pool specified in the propery file is " + specifiedThreadNumber +
                    ". The size computed from the available processors is " + availableProcessors + 
                     ". Final computed thread pool size for index executor: " + nThreads + ". Since its value is 1, we do NOT need the executor service and use a single thread way.");
            multipleThread = false;
        }
        
    }

  
    
    /**
     * Worker starts to consume messages from the index queue  - calling SolrIndex to 
     * process index tasks and submit results to store.
     * @throws IOException
     */
    public void start() throws IOException {
        final Consumer consumer = new DefaultConsumer(RabbitMQchannel) {
            @Override
            public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body) 
                                       throws IOException {
                String identifier = null;
                try {
                    final IndexQueueMessageParser parser = new IndexQueueMessageParser();
                    parser.parse(properties, body);
                    final Envelope finalEnvelop = envelope;
                    if (multipleThread) {
                        logger.debug("IndexWorker.start.handleDelivery - using multiple threads to index identifier " + parser.getIdentifier().getValue());
                        Runnable runner = new Runnable() {
                            @Override
                            public void run() {
                                indexOjbect(parser, finalEnvelop.getDeliveryTag(), multipleThread);
                            }
                        };
                        // submit the task, and that's it
                        executor.submit(runner);
                    } else {
                        logger.debug("IndexWorker.start.handleDelivery - using single thread to index identifier " + parser.getIdentifier().getValue());
                        indexOjbect(parser, finalEnvelop.getDeliveryTag(), multipleThread);
                    }
                } catch (InvalidRequest e) {
                    logger.error("IndexWorker.start.handleDelivery - cannot index the task for identifier  " + 
                                 identifier + " since " + e.getMessage());
                    boolean requeue = false;
                    RabbitMQchannel.basicReject(envelope.getDeliveryTag(), requeue);
                }
            }
         };
         boolean autoAck = false;
         RabbitMQchannel.basicConsume(INDEX_QUEUE_NAME, autoAck, consumer);
         logger.info("IndexWorker.start - Calling basicConsume and waiting for the comming messages");
    }
    
    /**
     * Process the index task. This method is called by a single or multiple thread(s) determined by the configuration.
     @param paser  the parser parsed the index queue message and holds the index information
     * @param deliveryTag  the tag of the rabbitmq message
     * @param multipleThread  the task was handled by multiple thread or not (for the log information only)
     */
    private void indexOjbect(IndexQueueMessageParser parser, long deliveryTag, boolean multipleThread) {
        long start = System.currentTimeMillis();
        Identifier pid = parser.getIdentifier();
        String indexType = parser.getIndexType();
        int priority = parser.getPriority();
        String finalFilePath = parser.getObjectPath();
        try {
            long threadId = Thread.currentThread().getId();
            logger.info("IndexWorker.consumer.indexOjbect by multiple thread? " + multipleThread + ", with the thread id " + threadId + 
                    " - Received the index task from the index queue with the identifier: "+
                    pid.getValue() + " , the index type: " + indexType + ", the file path (null means not to have): " + finalFilePath + 
                    ", the priotity: " + priority);
            if (indexType.equals(CREATE_INDEXT_TYPE)) {
                boolean sysmetaOnly = false;
                solrIndex.update(pid, finalFilePath, sysmetaOnly);
            } else if (indexType.equals(SYSMETA_CHANGE_TYPE)) {
                boolean sysmetaOnly = true;
                solrIndex.update(pid, finalFilePath, sysmetaOnly);
            } else if (indexType.equals(DELETE_INDEX_TYPE)) {
                solrIndex.remove(pid);
            } else {
                throw new InvalidRequest("0000", "DataONE indexer does not know the index type: " + indexType + " in the index task");
            }
            try {
                boolean multiple = false;
                RabbitMQchannel.basicAck(deliveryTag, multiple);
            } catch (Exception e) {
                logger.error("IndexWorker.indexOjbect with the thread id " +  threadId +
                        " - Though the index worker Completed the index task from the index queue with the identifier: "+
                        pid.getValue() + " , the index type: " + indexType + ", sending acknowledgement back to rabbitmq failed since " 
                        + e.getMessage()  + ". So rabbitmq may resend the message again");
            }
            long end = System.currentTimeMillis();
            logger.info("IndexWorker.indexOjbect with the thread id " +  threadId +
                    " - Completed the index task from the index queue with the identifier: "+
                    pid.getValue() + " , the index type: " + indexType + ", the file path (null means not to have): " + finalFilePath + 
                    ", the priotity: " + priority + " and the time taking is " + (end-start) + " milliseconds");
            
        } catch (InvalidToken e) {
            logger.error("IndexWorker.indexOjbect - cannot index the task for identifier  " + 
                    pid.getValue() + " since " + e.getMessage());
            try {
                boolean requeue = false;
                RabbitMQchannel.basicReject(deliveryTag, requeue);
            } catch(IOException ee) {
                logger.error("IndexWorker.indexOjbect - cannot index the task for identifier  " + 
                        pid.getValue() + " and also the rejection acknowledgement cannot be sent back to rabbitmq since " + ee.getMessage());
            }
        } catch (NotAuthorized e) {
            logger.error("IndexWorker.indexOjbect - cannot index the task for identifier  " + 
                    pid.getValue() + " since " + e.getMessage());
            try {
                boolean requeue = false;
                RabbitMQchannel.basicReject(deliveryTag, requeue);
            } catch(IOException ee) {
                logger.error("IndexWorker.indexOjbect - cannot index the task for identifier  " + 
                        pid.getValue() + " and also the rejection acknowledgement cannot be sent back to rabbitmq since " + ee.getMessage());
            }
        } catch (NotImplemented e) {
            logger.error("IndexWorker.indexOjbect - cannot index the task for identifier  " + 
                    pid.getValue() + " since " + e.getMessage());
            try {
                boolean requeue = false;
                RabbitMQchannel.basicReject(deliveryTag, requeue);
            } catch(IOException ee) {
                logger.error("IndexWorker.indexOjbect - cannot index the task for identifier  " + 
                        pid.getValue() + " and also the rejection acknowledgement cannot be sent back to rabbitmq since " + ee.getMessage());
            }
        } catch (ServiceFailure e) {
            logger.error("IndexWorker.indexOjbect - cannot index the task for identifier  " + 
                    pid.getValue() + " since " + e.getMessage());
            try {
                boolean requeue = false;
                RabbitMQchannel.basicReject(deliveryTag, requeue);
            } catch(IOException ee) {
                logger.error("IndexWorker.indexOjbect - cannot index the task for identifier  " + 
                        pid.getValue() + " and also the rejection acknowledgement cannot be sent back to rabbitmq since " + ee.getMessage());
            }
        } catch (NotFound e) {
            logger.error("IndexWorker.indexOjbect - cannot index the task for identifier  " + 
                    pid.getValue() + " since " + e.getMessage());
            try {
                boolean requeue = false;
                RabbitMQchannel.basicReject(deliveryTag, requeue);
            } catch(IOException ee) {
                logger.error("IndexWorker.indexOjbect - cannot index the task for identifier  " + 
                        pid.getValue() + " and also the rejection acknowledgement cannot be sent back to rabbitmq since " + ee.getMessage());
            }
        } catch (XPathExpressionException e) {
            logger.error("IndexWorker.indexOjbect - cannot index the task for identifier  " + 
                    pid.getValue() + " since " + e.getMessage());
            try {
                boolean requeue = false;
                RabbitMQchannel.basicReject(deliveryTag, requeue);
            } catch(IOException ee) {
                logger.error("IndexWorker.indexOjbect - cannot index the task for identifier  " + 
                        pid.getValue() + " and also the rejection acknowledgement cannot be sent back to rabbitmq since " + ee.getMessage());
            }
        } catch (UnsupportedType e) {
            logger.error("IndexWorker.indexOjbect - cannot index the task for identifier  " + 
                    pid.getValue() + " since " + e.getMessage());
            try {
                boolean requeue = false;
                RabbitMQchannel.basicReject(deliveryTag, requeue);
            } catch(IOException ee) {
                logger.error("IndexWorker.indexOjbect - cannot index the task for identifier  " + 
                        pid.getValue() + " and also the rejection acknowledgement cannot be sent back to rabbitmq since " + ee.getMessage());
            }
        } catch (SAXException e) {
            logger.error("IndexWorker.indexOjbect - cannot index the task for identifier  " + 
                    pid.getValue() + " since " + e.getMessage());
            try {
                boolean requeue = false;
                RabbitMQchannel.basicReject(deliveryTag, requeue);
            } catch(IOException ee) {
                logger.error("IndexWorker.indexOjbect - cannot index the task for identifier  " + 
                        pid.getValue() + " and also the rejection acknowledgement cannot be sent back to rabbitmq since " + ee.getMessage());
            }
        } catch (ParserConfigurationException e) {
            logger.error("IndexWorker.indexOjbect - cannot index the task for identifier  " + 
                    pid.getValue() + " since " + e.getMessage());
            try {
                boolean requeue = false;
                RabbitMQchannel.basicReject(deliveryTag, requeue);
            } catch(IOException ee) {
                logger.error("IndexWorker.indexOjbect - cannot index the task for identifier  " + 
                        pid.getValue() + " and also the rejection acknowledgement cannot be sent back to rabbitmq since " + ee.getMessage());
            }
        } catch (SolrServerException e) {
            logger.error("IndexWorker.indexOjbect - cannot index the task for identifier  " + 
                    pid.getValue() + " since " + e.getMessage());
            try {
                boolean requeue = false;
                RabbitMQchannel.basicReject(deliveryTag, requeue);
            } catch(IOException ee) {
                logger.error("IndexWorker.indexOjbect - cannot index the task for identifier  " + 
                        pid.getValue() + " and also the rejection acknowledgement cannot be sent back to rabbitmq since " + ee.getMessage());
            }
        } catch (MarshallingException e) {
            logger.error("IndexWorker.indexOjbect - cannot index the task for identifier  " + 
                    pid.getValue() + " since " + e.getMessage());
            try {
                boolean requeue = false;
                RabbitMQchannel.basicReject(deliveryTag, requeue);
            } catch(IOException ee) {
                logger.error("IndexWorker.indexOjbect - cannot index the task for identifier  " + 
                        pid.getValue() + " and also the rejection acknowledgement cannot be sent back to rabbitmq since " + ee.getMessage());
            }
        } catch (EncoderException e) {
            logger.error("IndexWorker.indexOjbect - cannot index the task for identifier  " + 
                    pid.getValue() + " since " + e.getMessage());
            try {
                boolean requeue = false;
                RabbitMQchannel.basicReject(deliveryTag, requeue);
            } catch(IOException ee) {
                logger.error("IndexWorker.indexOjbect - cannot index the task for identifier  " + 
                        pid.getValue() + " and also the rejection acknowledgement cannot be sent back to rabbitmq since " + ee.getMessage());
            }
        } catch (InterruptedException e) {
            logger.error("IndexWorker.indexOjbect - cannot index the task for identifier  " + 
                    pid.getValue() + " since " + e.getMessage());
            try {
                boolean requeue = false;
                RabbitMQchannel.basicReject(deliveryTag, requeue);
            } catch(IOException ee) {
                logger.error("IndexWorker.indexOjbect - cannot index the task for identifier  " + 
                        pid.getValue() + " and also the rejection acknowledgement cannot be sent back to rabbitmq since " + ee.getMessage());
            }
        } catch (IOException e) {
            logger.error("IndexWorker.indexOjbect - cannot index the task for identifier  " + 
                    pid.getValue() + " since " + e.getMessage());
            try {
                boolean requeue = false;
                RabbitMQchannel.basicReject(deliveryTag, requeue);
            } catch(IOException ee) {
                logger.error("IndexWorker.indexOjbect - cannot index the task for identifier  " + 
                        pid.getValue() + " and also the rejection acknowledgement cannot be sent back to rabbitmq since " + ee.getMessage());
            }
        } catch (InvalidRequest e) {
            logger.error("IndexWorker.indexOjbect - cannot index the task for identifier  " + 
                    pid.getValue() + " since " + e.getMessage());
            try {
                boolean requeue = false;
                RabbitMQchannel.basicReject(deliveryTag, requeue);
            } catch(IOException ee) {
                logger.error("IndexWorker.indexOjbect - cannot index the task for identifier  " + 
                        pid.getValue() + " and also the rejection acknowledgement cannot be sent back to rabbitmq since " + ee.getMessage());
            }
        } catch (InstantiationException e) {
            logger.error("IndexWorker.indexOjbect - cannot index the task for identifier  " + 
                    pid.getValue() + " since " + e.getMessage());
            try {
                boolean requeue = false;
                RabbitMQchannel.basicReject(deliveryTag, requeue);
            } catch(IOException ee) {
                logger.error("IndexWorker.indexOjbect - cannot index the task for identifier  " + 
                        pid.getValue() + " and also the rejection acknowledgement cannot be sent back to rabbitmq since " + ee.getMessage());
            }
        } catch (IllegalAccessException e) {
            logger.error("IndexWorker.indexOjbect - cannot index the task for identifier  " + 
                    pid.getValue() + " since " + e.getMessage());
            try {
                boolean requeue = false;
                RabbitMQchannel.basicReject(deliveryTag, requeue);
            } catch(IOException ee) {
                logger.error("IndexWorker.indexOjbect - cannot index the task for identifier  " + 
                        pid.getValue() + " and also the rejection acknowledgement cannot be sent back to rabbitmq since " + ee.getMessage());
            }
        }
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
