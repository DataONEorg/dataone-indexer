package org.dataone.cn.indexer;

import static org.junit.Assert.assertTrue;

import java.io.File;

import org.dataone.configuration.Settings;
import org.junit.Test;


/**
 * A junit test class for methods without third party software running
 * @author tao
 *
 */
public class IndexWorkerTest {

    /**
     * Test the initIndexParsers methdo
     * @throws Exception
     */
    @Test
    public void testInitIndexParsers() throws Exception {
        String propertyFilePath =
            "./src/test/resources/org/dataone/configuration/index-processor-2.properties";
        Settings.augmentConfiguration(propertyFilePath);
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
        String propertyFilePath2 = "./src/test/resources/org/dataone/configuration/index-processor-2.properties";
        IndexWorker.loadAdditionalPropertyFile(propertyFilePath2);
        assertTrue(IndexWorker.propertyFilePath.equals(propertyFilePath));
        assertTrue(Settings.getConfiguration().getString("dataone.mn.baseURL").
                equals("https://valley.duckdns.org/metacat/d1/mn"));
        assertTrue(Settings.getConfiguration().
                getString("index.data.root.directory").equals("/objects"));
        assertTrue(Settings.getConfiguration().
                getString("index.document.root.directory").equals("/objects"));
        assertTrue(Settings.getConfiguration().getString("cn.router.hostname2").equals("cn.dataone.org"));
    }

}
