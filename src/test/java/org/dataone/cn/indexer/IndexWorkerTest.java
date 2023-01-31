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
        boolean initialize = false;
        IndexWorker worker = new IndexWorker(initialize);
        worker.initIndexParsers();
        assertTrue(worker.solrIndex != null);
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
        int number = (new Integer(numberStr)).intValue();
        // only test setting multiple threads if enough processors are available
        if (finalThreads > number) { 
            Settings.getConfiguration().setProperty(propertyName, numberStr);
            worker.initExecutorService();
            System.out.println("worker.nThreads(" + numberStr + "): " + worker.nThreads);
            assertTrue(worker.nThreads == number);
            assertTrue(worker.multipleThread);
        }
        numberStr = "1";
        number = (new Integer(numberStr)).intValue();
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

}
