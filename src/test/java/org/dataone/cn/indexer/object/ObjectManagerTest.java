package org.dataone.cn.indexer.object;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;


import org.dataone.cn.indexer.IndexWorker;
import org.dataone.configuration.Settings;
import org.junit.Before;
import org.junit.Test;

/**
 * A junit test class for the ObjecManager class.
 * @author tao
 *
 */
public class ObjectManagerTest {

    @Before
    public void setUp() throws Exception {
        String propertyFilePath =
            "./src/main/resources/org/dataone/configuration/index-processor.properties";
        Settings.augmentConfiguration(propertyFilePath);
    }
    /**
     * Test the isCN method
     * @throws Exception
     */
    @Test
    public void testIsCN() throws Exception {
        String url = "https://knb.ecoinformatics.org/knb/d1/mn";
        assertFalse(ObjectManager.isCN(url));
        url = "https://cn-orc-1.dataone.org/cn";
        assertTrue(ObjectManager.isCN(url));
    }

    /**
     * Test the getSystemMetadataByAPI based the settings from properties
     */
    @Test
    public void testRefreshD1NodeFromProperties() throws Exception {
        ObjectManager.refreshD1Node();
        assertEquals("https://valley.duckdns.org/metacat/d1/mn/v2",
                     ObjectManager.getD1Node().getNodeBaseServiceUrl());
    }

}
