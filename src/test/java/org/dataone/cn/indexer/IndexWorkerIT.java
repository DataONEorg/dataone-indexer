package org.dataone.cn.indexer;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import org.dataone.service.exceptions.ServiceFailure;
import org.junit.Rule;
import org.junit.Test;
import uk.org.webcompere.systemstubs.rules.EnvironmentVariablesRule;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.TimeoutException;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

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
}
