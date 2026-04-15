package org.dataone.cn.indexer;

import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;

import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Consumer;
import com.rabbitmq.client.Envelope;
import com.rabbitmq.client.impl.LongStringHelper;
import org.dataone.configuration.Settings;
import org.junit.Test;


/**
 * A junit test class for methods without third party software running
 * @author tao
 *
 */
public class IndexWorkerTest {
    public static final String PORT_8985_PROPERTY_FILE_PATH =
        "./src/test/resources/org/dataone/configuration/index-processor-port-8985.properties";

    /**
     * Test the initIndexParsers methdo
     * @throws Exception
     */
    @Test
    public void testInitIndexParsers() throws Exception {
        Settings.augmentConfiguration(PORT_8985_PROPERTY_FILE_PATH);
        boolean initialize = false;
        IndexWorker worker = new IndexWorker(initialize);
        worker.initIndexParsers();
        assertTrue(worker.solrIndex != null);
        // Reset the path null
        IndexWorker.propertyFilePath = null;
        //Reset the property of cn.rounter.hostname2 null
        Settings.getConfiguration().setProperty("cn.router.hostname2", null);
    }
    
    /**
     * Test the initExecutorServicemethod
     * @throws Exception
     */
    @Test
    public void testInitExecutorService() throws Exception {
        //first to try the property of index.thread.number without value (the default setting in the test)
        int availableProcessors = Runtime.getRuntime().availableProcessors();
        System.out.println("availableProcessors: " + availableProcessors);
        availableProcessors = availableProcessors - 1;
        int finalThreads = Math.max(1, availableProcessors);
        boolean initialize = false;
        IndexWorker worker = new IndexWorker(initialize);
        worker.initExecutorService();
        System.out.println("worker.nThreads(default): " + worker.nThreads);
        assertTrue(worker.nThreads == finalThreads);
        if (finalThreads > 1) {
            assertTrue(worker.multipleThread);
        } else {
            assertTrue(!worker.multipleThread);
        }
        String propertyName = "index.thread.number";
        String numberStr = "5";
        int number = Integer.parseInt(numberStr);
        // only test setting multiple threads if enough processors are available
        if (finalThreads > number) { 
            Settings.getConfiguration().setProperty(propertyName, numberStr);
            worker.initExecutorService();
            System.out.println("worker.nThreads(" + numberStr + "): " + worker.nThreads);
            assertTrue(worker.nThreads == number);
            assertTrue(worker.multipleThread);
        }
        numberStr = "1";
        number = Integer.parseInt(numberStr);
        Settings.getConfiguration().setProperty(propertyName, numberStr);
        worker.initExecutorService();
        System.out.println("worker.nThreads(1): " + worker.nThreads);
        assertTrue(worker.nThreads == number);
        assertTrue(!worker.multipleThread);
        Settings.getConfiguration().clearProperty(propertyName);
    }
    
    /**
     * Test the loadExternalPropertiesFile method
     * @throws Exception
     */
    @Test
    public void testLoadExternalPropertiesFile() throws Exception {
        boolean readFromEnv = false;
        IndexWorker.loadExternalPropertiesFile(null);
        String propertyFilePath = System.getenv("DATAONE_INDEXER_CONFIG");
        if (propertyFilePath != null && !propertyFilePath.trim().equals("")) {
            File defaultFile = new File (propertyFilePath);
            if (defaultFile.exists() && defaultFile.canRead()) {
                //The one from the env variable
                System.out.println("read from the env variable ----- " + propertyFilePath);
                assertTrue(IndexWorker.propertyFilePath.equals(propertyFilePath));
                readFromEnv = true;
            } 
        }
        
        if(!readFromEnv) {
            propertyFilePath = "/etc/dataone/dataone-indexer.properties";
            File defaultFile = new File (propertyFilePath);
            if (defaultFile.exists() && defaultFile.canRead()) {
                //The one from the default the location
                System.out.println("read from the default location  /etc/dataone/dataone-indexer.properties");
                assertTrue(IndexWorker.propertyFilePath.equals(propertyFilePath));
            } else {
                //read from jar file
                System.out.println("read from the property file embedded in the jar file " + IndexWorker.propertyFilePath);
                assertTrue(IndexWorker.propertyFilePath == null);
            }
        }
    }
    
    /**
     * Test the loadExternalPropertiesFile method
     * @throws Exception
     */
    @Test
    public void testLoadUserSpecifiedExternalPropertiesFile() throws Exception {
        String propertyFilePath = "./src/main/resources/org/dataone/configuration/index-processor.properties";
        IndexWorker.loadExternalPropertiesFile(propertyFilePath);
        File defaultFile = new File (propertyFilePath);
        if (defaultFile.exists() && defaultFile.canRead()) {
            //The one from the user specified location
            assertTrue(IndexWorker.propertyFilePath.equals(propertyFilePath));
        } else {
            //read from jar file
            System.out.println("read from the property file embedded in the jar file " + IndexWorker.propertyFilePath);
            assertTrue(IndexWorker.propertyFilePath == null);
        }
       
    }
    
    /**
     * Test the loadAdditionalPropertiesFile method
     * @throws Exception
     */
    @Test
    public void testLoadAdditionalPropertiesFile() throws Exception {
        String propertyFilePath = "./src/main/resources/org/dataone/configuration/index-processor.properties";
        IndexWorker.loadExternalPropertiesFile(propertyFilePath);
        File defaultFile = new File (propertyFilePath);
        //The one from the user specified location
        assertTrue(IndexWorker.propertyFilePath.equals(propertyFilePath));
        assertTrue(Settings.getConfiguration().getString("dataone.mn.baseURL").
                equals("https://valley.duckdns.org/metacat/d1/mn"));
        assertTrue(Settings.getConfiguration().
                getString("index.data.root.directory").equals("/var/metacat/data"));
        assertTrue(Settings.getConfiguration().
                getString("index.document.root.directory").equals("/var/metacat/documents"));
        assertTrue(Settings.getConfiguration().getString("cn.router.hostname2") == null);
        //load another file, it will overwrite the properties which have different values
        String propertyFilePath2 = PORT_8985_PROPERTY_FILE_PATH;
        IndexWorker.loadAdditionalPropertyFile(propertyFilePath2);
        assertTrue(IndexWorker.propertyFilePath.equals(propertyFilePath));
        assertTrue(Settings.getConfiguration().getString("dataone.mn.baseURL").
                equals("https://valley.duckdns.org/metacat/d1/mn"));
        assertTrue(Settings.getConfiguration().
                getString("index.data.root.directory").equals("./target"));
        assertTrue(Settings.getConfiguration().
                getString("index.document.root.directory").equals("./target"));
        assertTrue(Settings.getConfiguration().getString("cn.router.hostname2").equals("cn.dataone.org"));
    }

    /**
     * This method tests the Consumer.handleDelivery method. It covers this scenario:
     * There are no threads available when the worker submits a job to the executor. The
     * worker will try multiple times until the max waiting time reaches.
     * @throws Exception
     */
    @Test
    public void testHandleDeliveryWithMaxWaitingTime() throws Exception {
        IndexWorker.setMaxSubmitTimeMin(0.05F); // Shorten the testing time.
        IndexWorker worker = new IndexWorker(false);
        // Make ALL submit(...) calls throw RejectedExecutionException
        ExecutorService executor = mock(ExecutorService.class);
        when(executor.submit(any(Runnable.class)))
            .thenThrow(new RejectedExecutionException("Rejected"));
        worker.setExecutor(executor);
        // Make channel.basicNack do nothing
        Channel rabbitMQchannel = mock(Channel.class);
        doNothing().when(rabbitMQchannel)
            .basicNack(anyLong(), anyBoolean(), anyBoolean());
        worker.setRabbitMQchannel(rabbitMQchannel);
        worker.start();
        Envelope envelope = mock(Envelope.class);
        when(envelope.getDeliveryTag()).thenReturn(100L);
        Consumer consumer = worker.getRabbitMQConsumer();
        long start = System.currentTimeMillis();
        consumer.handleDelivery("consumerTag", envelope, generateRabbitMQProperties(), null);
        long end = System.currentTimeMillis();
        // Make sure the submit/wait process last more than the MAX_SUBMIT_TIME_MIN
        assertTrue((end - start) > IndexWorker.MAX_SUBMIT_TIME_MIN * 60 * 1000);
        // verify the rabbitMQchannel.basicNack was called exactly once
        verify(rabbitMQchannel, times(1))
            .basicNack(anyLong(), anyBoolean(), anyBoolean());
    }

    /**
     * This method tests the Consumer.handleDelivery method. It covers the scenario that executor
     * .submit succeeds.
     * @throws Exception
     */
    @Test
    public void testHandleDelivery() throws Exception {
        IndexWorker worker = new IndexWorker(false);
        // Make ALL submit(...) calls do nothing
        Future future = mock(Future.class);
        ExecutorService executor = mock(ExecutorService.class);
        when(executor.submit(any(Runnable.class))).thenReturn(future);
        worker.setExecutor(executor);
        // Make channel.basicAckdo nothing
        Channel rabbitMQchannel = mock(Channel.class);
        doNothing().when(rabbitMQchannel).basicAck(anyLong(), anyBoolean());
        worker.setRabbitMQchannel(rabbitMQchannel);
        worker.start();
        Envelope envelope = mock(Envelope.class);
        when(envelope.getDeliveryTag()).thenReturn(100L);
        Consumer consumer = worker.getRabbitMQConsumer();
        consumer.handleDelivery("consumerTag", envelope, generateRabbitMQProperties(), null);
        // verify the rabbitMQchannel.basicAck was called exactly once
        verify(rabbitMQchannel, times(1)).basicAck(anyLong(), anyBoolean());
    }

    /**
     * A utility method to generate a RabbitMQ property
     * @return a RabbitMQ basic property
     */
    private AMQP.BasicProperties generateRabbitMQProperties() {
        Map<String, Object> headers = new HashMap<>();
        headers.put("id", LongStringHelper.asLongString("foo.1.1"));
        headers.put("index_type", LongStringHelper.asLongString("create"));
        AMQP.BasicProperties basicProperties =
            new AMQP.BasicProperties.Builder()
                .contentType("text/plain")
                .deliveryMode(2) // set this message to persistent
                .priority(3)
                .headers(headers)
                .build();
        return basicProperties;
    }

}
