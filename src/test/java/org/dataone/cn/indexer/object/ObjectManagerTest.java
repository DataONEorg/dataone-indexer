package org.dataone.cn.indexer.object;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.dataone.configuration.Settings;
import org.dataone.service.types.v2.SystemMetadata;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import uk.org.webcompere.systemstubs.rules.EnvironmentVariablesRule;

/**
 * A junit test class for the ObjectManager class.
 * @author tao
 *
 */
public class ObjectManagerTest {
    public static final String NODE_BASE_URL_ENV_NAME = "DATAONE_INDEXER_NODE_BASE_URL";
    private static final String TOKEN_ENV_NAME = "DATAONE_INDEXER_AUTH_TOKEN";

    @Rule
    public EnvironmentVariablesRule environmentVariables =
        new EnvironmentVariablesRule(TOKEN_ENV_NAME, null);

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
     * Test the refreshD1Node method based the settings from properties
     */
    @Test
    public void testRefreshD1NodeFromProperties() throws Exception {
        ObjectManager.refreshD1Node();
        assertEquals("https://valley.duckdns.org/metacat/d1/mn/v2",
                     ObjectManager.getD1Node().getNodeBaseServiceUrl());
    }

    /**
     * Test the getSystemMetadataByAPI method base the env values.
     * @throws Exception
     */
    @Test
    public void testGetSystemMetadataByAPI() throws Exception {
        String url = "https://knb.ecoinformatics.org/knb/d1/mn";
        String token = "fake_token";
        environmentVariables.set(NODE_BASE_URL_ENV_NAME, url);
        environmentVariables.set(TOKEN_ENV_NAME, token);
        ObjectManager.refreshD1Node();
        assertEquals(url + "/v2", ObjectManager.getD1Node().getNodeBaseServiceUrl());
        assertEquals(token, ObjectManager.getDataONEauthToken());
        String id = "doi:10.5063/F1N0150S";
        SystemMetadata sys = ObjectManager.getSystemMetadataByAPI(id);
        assertNotNull(sys);
        assertEquals(id, sys.getIdentifier().getValue());
        environmentVariables.set(NODE_BASE_URL_ENV_NAME, null);
        environmentVariables.set(TOKEN_ENV_NAME, null);
    }

}
