package org.dataone.cn.indexer;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.FileTime;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.locks.ReentrantLock;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathExpressionException;

import org.apache.commons.codec.EncoderException;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.solr.client.solrj.SolrServerException;
import org.dataone.cn.indexer.annotation.OntologyModelService;
import org.dataone.cn.indexer.object.ObjectManagerFactory;
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

    private static Log logger = LogFactory.getLog(IndexWorker.class);
    private static String defaultExternalPropertiesFile = "/etc/dataone/dataone-indexer.properties";

    protected static String propertyFilePath = null;
    protected boolean multipleThread = true;
    protected int nThreads = 1;

    private Connection rabbitMQconnection = null;
    private Channel rabbitMQchannel = null;
    private ApplicationContext context = null;
    protected SolrIndex solrIndex = null;
    private ExecutorService executor = null;
    private ConnectionFactory factory = null;

    private final ReentrantLock connectionLock = new ReentrantLock();
    private boolean isK8s = false;
    private Consumer consumer;
    protected static int readinessInitialDelaySec = 2;
    protected static int readinessPeriodSec = 10;
    /**
     * Commandline main for the IndexWorker to be started.
     *
     * @param args (not used -- command line args)
     */
    public static void main(String[] args) {
        logger.info("IndexWorker.main - Starting index worker...");
        String propertyFile = null;
        if (args != null && args.length == 1) {
            // The args should be a property file which the dataone-indexer will use
            propertyFile = args[0];
            logger.debug("The external property file specified in the argument is " + propertyFile);
        }
        loadExternalPropertiesFile(propertyFile);
        try {
            IndexWorker worker = new IndexWorker();
            worker.start();
            worker.startReadinessProbe();
            worker.startLivenessProbe();
        } catch (Exception e) {
            logger.fatal("IndexWorker.main() exiting due to fatal error: " + e.getMessage(), e);
            System.exit(1);
        }
    }

    /**
     * Load properties from an external file.
     * DataONE-indexer will try to load the property file by this order
     * 1. try to read it from the user specified
     * 2. try to read it from an env variable - DATAONE_INDEXER_CONFIG.
     * 3  try to use the default path - /etc/dataone/dataone-indexer.properties
     * If all attempts fail, it will give up and use the one embedded in the jar file
     * @param propertyFile the property file user specified
     */
    public static void loadExternalPropertiesFile(String propertyFile) {
        // try the users specified path
        if (propertyFile != null && !propertyFile.trim().isEmpty()) {
            propertyFilePath = propertyFile;
            logger.info("IndexWorker.loadExternalPropertiesFile - the configuration path specified by users is " + propertyFilePath);
            File defaultFile = new File(propertyFilePath);
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
        if (propertyFilePath == null || propertyFilePath.trim().isEmpty()) {
            propertyFilePath = System.getenv(ENV_NAME_OF_PROPERTIES_FILE);
            logger.info("IndexWorker.loadExternalPropertiesFile - the configuration path from the env variable is " + propertyFilePath);
            if (propertyFilePath != null && !propertyFilePath.trim().equals("")) {
                File defaultFile = new File(propertyFilePath);
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

        //The attempts to read the configuration file specified by users and from the env
        // variable failed. We will try the default external path
        if (propertyFilePath == null || propertyFilePath.trim().isEmpty()) {
            File defaultFile = new File(defaultExternalPropertiesFile);
            if (defaultFile.exists() && defaultFile.canRead()) {
                logger.info("IndexWorker.loadExternalPropertiesFile - the configure path can't be read either by users specified or from the env variable " + ENV_NAME_OF_PROPERTIES_FILE +
                           ". However, the default external file " + defaultExternalPropertiesFile + " exists and it will be used.");
                propertyFilePath = defaultExternalPropertiesFile;
            }
        }
        if (propertyFilePath != null && !propertyFilePath.trim().isEmpty()) {
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
    public static void loadAdditionalPropertyFile(String propertyFile) {
        if (propertyFile != null && !propertyFile.trim().isEmpty()) {
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
     *
     * @throws IOException
     * @throws TimeoutException
     * @throws ServiceFailure
     */
    public IndexWorker()
        throws IOException, TimeoutException, ServiceFailure, ClassNotFoundException,
        InvocationTargetException, NoSuchMethodException, InstantiationException,
        IllegalAccessException {
        this(true);
    }

    /**
     * Constructor with/without initialization
     *
     * @param initialize if we need to initialize RabbitMQ et al
     * @throws IOException
     * @throws TimeoutException
     * @throws ServiceFailure
     */
    public IndexWorker(Boolean initialize)
        throws IOException, TimeoutException, ClassNotFoundException, InvocationTargetException,
        NoSuchMethodException, InstantiationException, IllegalAccessException {
        String value = System.getenv("KUBERNETES_SERVICE_HOST");
        // Java doc says: the string value of the variable, or null if the variable is not defined
        // in the system environment
        if (value != null) {
            isK8s = true;
            logger.info("The index worker is in the k8s environment.");
        }
        if (initialize) {
            initExecutorService();//initialize the executor first
            initIndexQueue();
            initIndexParsers();
            ObjectManagerFactory.getObjectManager();
            OntologyModelService.getInstance();
        }
    }

    /**
     * Initialize the RabbitMQ service
     *
     * @throws IOException
     * @throws TimeoutException
     */
    private void initIndexQueue() throws IOException, TimeoutException {
        String rabbitMQhost =
            Settings.getConfiguration().getString("index.rabbitmq.hostname", "localhost");
        int rabbitMQport = Settings.getConfiguration().getInt("index.rabbitmq.hostport", 5672);
        String rabbitMQusername =
            Settings.getConfiguration().getString("index.rabbitmq.username", "guest");
        String rabbitMQpassword =
            Settings.getConfiguration().getString("index.rabbitmq.password", "guest");
        factory = new ConnectionFactory();
        factory.setHost(rabbitMQhost);
        factory.setPort(rabbitMQport);
        factory.setPassword(rabbitMQpassword);
        factory.setUsername(rabbitMQusername);
        // connection that will recover automatically
        factory.setAutomaticRecoveryEnabled(true);
        // attempt recovery every 10 seconds after a failure
        factory.setNetworkRecoveryInterval(10000);
        logger.debug("Set RabbitMQ host to: " + rabbitMQhost);
        logger.debug("Set RabbitMQ port to: " + rabbitMQport);
        generateConnectionAndChannel();
    }

    private void generateConnectionAndChannel() throws IOException, TimeoutException {
        int rabbitMQMaxPriority = Settings.getConfiguration().getInt("index.rabbitmq.max.priority");
        boolean durable = true;
        rabbitMQconnection = factory.newConnection();
        rabbitMQchannel = rabbitMQconnection.createChannel();
        rabbitMQchannel.exchangeDeclare(EXCHANGE_NAME, "direct", durable);
        boolean exclusive = false;
        boolean autoDelete = false;
        Map<String, Object> args = new HashMap<>();
        args.put("x-max-priority", rabbitMQMaxPriority);
        logger.debug("Set RabbitMQ max priority to: " + rabbitMQMaxPriority);
        rabbitMQchannel.queueDeclare(INDEX_QUEUE_NAME, durable, exclusive, autoDelete, args);
        rabbitMQchannel.queueBind(INDEX_QUEUE_NAME, EXCHANGE_NAME, INDEX_ROUTING_KEY);
        logger.info("The allowed unacknowledged message(s) number is " + nThreads);
        rabbitMQchannel.basicQos(nThreads);
        logger.debug("Connected to the RabbitMQ queue with the name of " + INDEX_QUEUE_NAME);
    }

    /**
     * Initialize the solrIndex object which contains the index parsers.
     */
    protected void initIndexParsers() {
        if (context == null) {
            synchronized (IndexWorker.class) {
                if (context == null) {
                    context = new ClassPathXmlApplicationContext(springConfigFileURL);
                }
            }
        }
        solrIndex = (SolrIndex) context.getBean("solrIndex");
    }

    /**
     * Determine the size of the thread pool and initialize the executor service
     */
    protected void initExecutorService() {
        String specifiedThreadNumberStr
            = Settings.getConfiguration().getString("index.thread.number", "0");
        int specifiedThreadNumber;
        try {
            specifiedThreadNumber = Integer.parseInt(specifiedThreadNumberStr);
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
        consumer = new DefaultConsumer(rabbitMQchannel) {
            @Override
            public void handleDelivery(
                String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body)
                throws IOException {

                logger.debug("Received message with delivery tag: " + envelope.getDeliveryTag());

                // Send acknowledgment back to RabbitMQ before processing index.
                // This is a temporary solution for the RabbitMQ timeout issue.
                // Set multiple false
                rabbitMQchannel.basicAck(envelope.getDeliveryTag(), false);

                final IndexQueueMessageParser parser = new IndexQueueMessageParser();
                try {
                    parser.parse(properties, body);
                    if (multipleThread) {
                        logger.debug(
                            "using multiple threads to index identifier " + parser.getIdentifier()
                                .getValue());
                        Runnable runner = new Runnable() {
                            @Override
                            public void run() {
                                indexObject(parser, multipleThread);
                            }
                        };
                        // submit the task, and that's it
                        executor.submit(runner);
                    } else {
                        logger.debug(
                            "using single thread to index identifier " + parser.getIdentifier()
                                .getValue());
                        indexObject(parser, multipleThread);
                    }
                } catch (InvalidRequest e) {
                    logger.error(
                        "cannot index the task for identifier " + parser.getIdentifier().getValue()
                            + " since " + e.getMessage());
                    boolean requeue = false;
                    rabbitMQchannel.basicReject(envelope.getDeliveryTag(), requeue);
                }
            }
        };

        // Set autoAck = false
        rabbitMQchannel.basicConsume(INDEX_QUEUE_NAME, false, consumer);

        logger.info("IndexWorker.start - Calling basicConsume and waiting for the coming messages");
    }

    private void recreateConnection() throws IOException {
        connectionLock.lock();
        try {
            if (rabbitMQchannel != null && rabbitMQchannel.isOpen()) {
                try {
                    rabbitMQchannel.close();
                    logger.debug("After closing the RabbitMQ channel.");
                } catch (Exception e) {
                    logger.warn("The rabbitmq channel can't be closed since " + e.getMessage());
                }
            }
            if (rabbitMQconnection != null && rabbitMQconnection.isOpen()) {
                try {
                    rabbitMQconnection.close();
                    logger.debug("After closing the RabbitMQ connection.");
                } catch (Exception e) {
                    logger.warn("The rabbitmq connection can't be closed since " + e.getMessage());
                }
            }
            try {
                generateConnectionAndChannel();
            } catch (TimeoutException | IOException e) {
                throw new IOException("Exception trying to re-initialize connection and "
                                          + "channel: " + e.getMessage(), e);
            }
            // Tell RabbitMQ this worker is ready for tasks
            if (consumer == null) {
                throw new RuntimeException(
                    "The consumer object is null and hasn't been initialized. IndexWorker.start "
                        + "should be called first.");
            }
            rabbitMQchannel.basicConsume(INDEX_QUEUE_NAME, false, consumer);
            logger.debug("RabbitMQ connection and channel successfully re-created");
        } finally {
            connectionLock.unlock();
            logger.debug("The connection lock was released");
        }
    }

    /**
     * Process the index task. This method is called by a single or multiple thread(s) determined by the configuration.
     * @param parser  the parser parsed the index queue message and holds the index information
     * @param multipleThread  the task was handled by multiple thread or not (for the log information only)
     */
    private void indexObject(IndexQueueMessageParser parser, boolean multipleThread) {
        long start = System.currentTimeMillis();
        Identifier pid = parser.getIdentifier();
        String indexType = parser.getIndexType();
        int priority = parser.getPriority();
        String docId = parser.getDocId();// It can be null.
        try {
            long threadId = Thread.currentThread().getId();
            logger.info("IndexWorker.consumer.indexObject by multiple thread? " + multipleThread
                            + ", with the thread id " + threadId
                    + " - Received the index task from the index queue with the identifier: "
                            + pid.getValue() + " , the index type: " + indexType
                            + ", the priority: " + priority + ", the docId(can be null): " + docId);
            switch (indexType) {
                case CREATE_INDEXT_TYPE -> {
                    boolean sysmetaOnly = false;
                    solrIndex.update(pid, sysmetaOnly, docId);
                }
                case SYSMETA_CHANGE_TYPE -> {
                    boolean sysmetaOnly = true;
                    solrIndex.update(pid, sysmetaOnly, docId);
                }
                case DELETE_INDEX_TYPE -> solrIndex.remove(pid);
                default -> throw new InvalidRequest(
                    "0000", "DataONE indexer does not know the index type: " + indexType
                    + " in the index task");
            }

            long end = System.currentTimeMillis();
            logger.info("IndexWorker.indexOjbect with the thread id " + threadId
                    + " - Completed the index task from the index queue with the identifier: "
                            + pid.getValue() + " , the index type: " + indexType
                            + ", the priority: " + priority + " and the time taking is "
                            + (end - start) + " milliseconds");

        } catch (InvalidToken | NotAuthorized | NotImplemented | NotFound | InvalidRequest |
                 ServiceFailure | XPathExpressionException | UnsupportedType | SAXException |
                 ParserConfigurationException | SolrServerException | MarshallingException |
                 EncoderException | InterruptedException | IOException | InstantiationException |
                 IllegalAccessException | ClassNotFoundException | InvocationTargetException |
                 NoSuchMethodException e) {
            logger.error("Cannot index the task for identifier " + pid.getValue()
                             + " since " + e.getMessage(), e);
        }
    }

    private void startLivenessProbe() {
        if (!isK8s) {
            logger.debug("This is a non-k8s deployment and LivenessProbe do nothing.");
            return;
        }
        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
        Path path = Paths.get("./livenessprobe");
        Runnable task = () -> {
            try {
                Files.setLastModifiedTime(path, FileTime.fromMillis(System.currentTimeMillis()));
            } catch (IOException e) {
                logger.error("IndexWorker.startLivenessProbe - failed to update file: " + path, e);
            }
        };
        scheduler.scheduleAtFixedRate(task, 0, 10, TimeUnit.SECONDS);
        logger.info("IndexWorker.startLivenessProbe - livenessProbe started");
    }

    /**
     * Start the timer task to check if the index-worker is ready
     */
    public void startReadinessProbe() {
        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
        Path path = Paths.get("./readinessprobe");
        Runnable task = () -> {
            try {
                if (isK8s && !Files.exists(path)) {
                    Files.createFile(path);
                }
                if (rabbitMQconnection != null && rabbitMQchannel != null
                    && rabbitMQconnection.isOpen() && rabbitMQchannel.isOpen()) {
                    if (isK8s) {
                        Files.setLastModifiedTime(
                            path, FileTime.fromMillis(System.currentTimeMillis()));
                    }
                    logger.debug("The RabbitMQ connection and channel are healthy.");
                } else {
                    logger.error("The RabbitMQ connection or channel were closed. DataONE-indexer "
                                     + "has a mechanism to restore them. However, if this error "
                                     + "message shows up repeatedly and there is no network outage,"
                                     + " intervention may be required (e.g. checking "
                                     + "configuration)");
                    try {
                        recreateConnection();
                    } catch (IOException | RuntimeException e) {
                        logger.error("DataONE-indexer cannot recreate the RabbitMQ "
                                         + "connections/channels since " + e.getMessage());
                    }
                }
            } catch (IOException e) {
                logger.error("Failed to update file: " + path, e);
            }
        };
        scheduler.scheduleAtFixedRate(
            task, readinessInitialDelaySec, readinessPeriodSec, TimeUnit.SECONDS);
        logger.info("ReadinessProb started");
    }

    /**
     * Get the Rabbitmq connection
     * @return the Rabbitmq connection
     */
    protected Connection getRabbitMQconnection() {
        return rabbitMQconnection;
    }

    /**
     * Get the Rabbitmq channel
     * @return the Rabbitmq channel
     */
    protected Channel getRabbitMQchannel() {
        return rabbitMQchannel;
    }
}
