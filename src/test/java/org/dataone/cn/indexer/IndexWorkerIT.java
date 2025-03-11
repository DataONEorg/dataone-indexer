package org.dataone.cn.indexer;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import org.dataone.service.exceptions.ServiceFailure;
import org.junit.Test;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

import static org.junit.Assert.assertTrue;

/**
 * An Integration test for the IndexWorker class.
 * It requires that the rabbitmq server is running
 * @author Tao
 */
public class IndexWorkerIT {
    private static final int LIMIT = 100;

    /**
     * Test to restore the rabbitmq connection and channel when they are closed.
     * The restoration happens when the handleShutdownSignal is called.
     * @throws ServiceFailure
     * @throws IOException
     * @throws TimeoutException
     * @throws InterruptedException
     */
    @Test
    public void testRestoreRabbitMQConnectionAndChannel()
        throws ServiceFailure, IOException, TimeoutException, InterruptedException {
        IndexWorker worker = new IndexWorker();
        worker.start();
        Connection connection = worker.getRabbitMQconnection();
        connection.close();
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
            Thread.sleep(100);
            index++;
        }
        assertTrue(worker.getRabbitMQconnection().isOpen());
        assertTrue(worker.getRabbitMQchannel().isOpen());
    }
}
