package org.dataone.cn.indexer;

import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.Consumer;
import com.rabbitmq.client.Envelope;
import com.rabbitmq.client.impl.LongStringHelper;
import org.dataone.service.exceptions.ServiceFailure;
import org.junit.Rule;
import org.junit.Test;
import uk.org.webcompere.systemstubs.rules.EnvironmentVariablesRule;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeoutException;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyBoolean;
import static org.mockito.Mockito.anyLong;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


/**
 * An Integration test for the IndexWorker class.
 * It requires that the rabbitmq server is running
 * @author Tao
 */
public class IndexWorkerIT {
    private static final int LIMIT = 10;
    @Rule
    public EnvironmentVariablesRule environmentVariablesRule =
        new EnvironmentVariablesRule("METACAT_OSTI_TOKEN", null);

    /**
     * Test to restore the rabbitmq connection and channel when they are closed.
     * The restoration happens when the handleShutdownSignal is called.
     * @throws ServiceFailure
     * @throws IOException
     * @throws TimeoutException
     * @throws InterruptedException
     */
    @Test
    public void testRestoreRabbitMQConnectionAndChannel() throws Exception {
        IndexWorker worker = new IndexWorker();
        worker.start();
        Connection connection = worker.getRabbitMQconnection();
        connection.close();
        IndexWorker.readinessInitialDelaySec = 0;
        IndexWorker.readinessPeriodSec = 1;
        worker.startReadinessProbe();
        int index = 0;
        while (!worker.getRabbitMQchannel().isOpen() && index < LIMIT) {
            Thread.sleep(100);
            index++;
        }
        assertTrue(worker.getRabbitMQconnection().isOpen());
        assertTrue(worker.getRabbitMQchannel().isOpen());
        Channel channel = worker.getRabbitMQchannel();
        channel.close();
        index = 0;
        while (!worker.getRabbitMQchannel().isOpen() && index < LIMIT) {
            Thread.sleep(300);
            index++;
        }
        assertTrue(worker.getRabbitMQconnection().isOpen());
        assertTrue(worker.getRabbitMQchannel().isOpen());
    }

    /**
     * Test the StartRabbitMQConnectionProb method in the k8s environment
     * @throws Exception
     */
    @Test
    public void testStartRabbitMQConnectionProbK8s() throws Exception {
        environmentVariablesRule.set("KUBERNETES_SERVICE_HOST", "localhost");
        IndexWorker worker = new IndexWorker();
        worker.start();
        Path path = Paths.get("readinessprobe");
        if (Files.exists(path)) {
            Files.delete(path);
        }
        worker.startReadinessProbe();
        int index = 0;
        path = Paths.get("readinessprobe");
        while (!Files.exists(path) && index < LIMIT) {
            Thread.sleep(200);
            index++;
        }
        assertTrue(Files.exists(path));
        if (Files.exists(path)) {
            Files.delete(path);
        }
    }

    /**
     * Test the StartRabbitMQConnectionProb method in the non-k8s environment
     * @throws Exception
     */
    @Test
    public void testStartRabbitMQConnectionProb() throws Exception {
        IndexWorker worker = new IndexWorker();
        worker.start();
        Path path = Paths.get("readinessprobe");
        if (Files.exists(path)) {
            Files.delete(path);
        }
        worker.startReadinessProbe();
        int index = 0;
        path = Paths.get("readinessprobe");
        while (!Files.exists(path) && index < LIMIT) {
            Thread.sleep(200);
            index++;
        }
        assertFalse(Files.exists(path));
    }

    /**
     * This method tests the scenario that there are no threads available when the worker submits a
     * job to the executor. The worker will try multiple times until the max waiting time reaches.
     * @throws Exception
     */
    @Test
    public void testMaxWaitTimeForSubmittingJobs() throws Exception {
        IndexWorker.setMaxSubmitTimeMin(0.05F); // Shorten the testing time.
        IndexWorker worker = new IndexWorker();
        worker.start();
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
        Envelope envelope = mock(Envelope.class);
        when(envelope.getDeliveryTag()).thenReturn(100L);
        Consumer consumer = worker.getRabbitMQConsumer();
        long start = System.currentTimeMillis();
        consumer.handleDelivery("consumerTag", envelope, basicProperties, null);
        long end = System.currentTimeMillis();
        // Make sure the submit/wait process last more than the MAX_SUBMIT_TIME_MIN
        assertTrue((end - start) > IndexWorker.MAX_SUBMIT_TIME_MIN * 60 * 1000);
        // verify the rabbitMQchannel.basicNack was called exactly once
        verify(rabbitMQchannel, times(1))
            .basicNack(anyLong(), anyBoolean(), anyBoolean());
    }
}
