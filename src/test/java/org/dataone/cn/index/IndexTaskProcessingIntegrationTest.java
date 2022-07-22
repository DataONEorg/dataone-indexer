/**
 * This work was created by participants in the DataONE project, and is
 * jointly copyrighted by participating institutions in DataONE. For 
 * more information on DataONE, see our web site at http://dataone.org.
 *
 *   Copyright ${year}
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
 * $Id$
 */

package org.dataone.cn.index;

import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;

import junit.framework.Assert;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.dataone.service.types.v2.SystemMetadata;
import org.dataone.service.util.TypeMarshaller;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.carrotsearch.randomizedtesting.annotations.ThreadLeakScope;

/**
 * This test loads the generator and processor daemons which will open their
 * application context configuration to load the processor/generator. This means
 * that the config found in the main project will be used to run this test.
 * (PostgreSQL) This test also connects to a Solr server for index processing.
 * 
 * @author sroseboo
 * 
 *         This test class is an integration test, not a unit test. It relies
 *         upon the index generator, processor and configuration of solr,
 *         postgres, hazelcast
 * 
 */
// TODO: CONVERT to DataONESolrJetty test to verify index changes

@ThreadLeakScope(ThreadLeakScope.Scope.NONE)
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "test-context.xml" })
public class IndexTaskProcessingIntegrationTest {

    private static Logger logger = Logger.getLogger(IndexTaskProcessingIntegrationTest.class
            .getName());

    @Autowired
    private Resource peggym1271Sys;
    @Autowired
    private Resource peggym1281Sys;
    @Autowired
    private Resource peggym1291Sys;
    @Autowired
    private Resource peggym1304Sys;
    @Autowired
    private Resource peggym1304SysArchived;

    @Autowired
    private Resource systemMetadataResource5;

    @BeforeClass
    public static void init() {
        //HazelcastClientFactoryTest.setUp();
    }

    @AfterClass
    public static void cleanup() throws Exception {
        //HazelcastClientFactoryTest.shutDown();
    }

    @Test
    public void emptyTest() {
    }

    // TODO: NEED test for add then update and verify changes test
    // @Test
    public void testDeleteArchivedFromIndex() throws Exception {

        // creating these deamon instance from class loader overrides spring
        // config for jpa repository so postgres is assumed/used.
        //IndexTaskGeneratorDaemon generatorDaemon = new IndexTaskGeneratorDaemon();
        //IndexTaskProcessorDaemon processorDaemon = new IndexTaskProcessorDaemon();

        //generatorDaemon.start();

        addSystemMetadata(peggym1304Sys);

        Thread.sleep(1000);

        //processorDaemon.start();
        Thread.sleep(3000);
        //processorDaemon.stop();

        addSystemMetadata(peggym1304SysArchived);
        Thread.sleep(1000);

        //processorDaemon.start();
        Thread.sleep(3000);
        //generatorDaemon.stop();
        //processorDaemon.stop();

        Assert.assertTrue(true);
    }

    // @Test
    public void testGenerateAndProcessIndexTasks() throws Exception {
        // creating these deamon instance from class loader overrides spring
        // config for jpa repository so postgres is assumed/used.
        //IndexTaskGeneratorDaemon generatorDaemon = new IndexTaskGeneratorDaemon();
        //IndexTaskProcessorDaemon processorDaemon = new IndexTaskProcessorDaemon();

        //generatorDaemon.start();

        addSystemMetadata(peggym1271Sys);
        addSystemMetadata(peggym1281Sys);
        addSystemMetadata(peggym1291Sys);
        addSystemMetadata(peggym1304Sys);
        addSystemMetadata(systemMetadataResource5);

        Thread.sleep(5000);

        // Starting processor daemon here to avoid waiting for scheduling
        // interval (2 minutes)
        //processorDaemon.start();

        // processing time
        Thread.sleep(10000);

        ///generatorDaemon.stop();
        //processorDaemon.stop();

        Assert.assertTrue(true);
    }

    private void addSystemMetadata(Resource systemMetadataResource) {
        SystemMetadata sysmeta = null;
        try {
            sysmeta = TypeMarshaller.unmarshalTypeFromStream(SystemMetadata.class,
                    systemMetadataResource.getInputStream());
        } catch (Exception ex) {
            logger.error(ex.getMessage(), ex);
            fail("Test SystemMetadata misconfiguration - Exception " + ex);
        }
        String path = null;
        try {
            path = StringUtils
                    .remove(systemMetadataResource.getFile().getPath(), File.separator + "SystemMetadata");
        } catch (IOException e) {
            logger.error(e.getMessage(), e);
        }
        //HazelcastClientFactory.getSystemMetadataMap().put(sysmeta.getIdentifier(), sysmeta);
        //HazelcastClientFactory.getSystemMetadataMap().put(sysmeta.getIdentifier(), sysmeta);
        //HazelcastClientFactory.getObjectPathMap().putAsync(sysmeta.getIdentifier(), path);
    }

}
