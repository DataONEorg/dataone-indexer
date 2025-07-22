package org.dataone.cn.indexer.object.legacystore;

import org.apache.commons.io.IOUtils;
import org.dataone.cn.indexer.object.ObjectManager;
import org.dataone.cn.indexer.object.ObjectManagerTest;
import org.dataone.configuration.Settings;
import org.dataone.service.exceptions.NotFound;
import org.dataone.service.exceptions.ServiceFailure;
import org.dataone.service.types.v1.SystemMetadata;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import uk.org.webcompere.systemstubs.rules.EnvironmentVariablesRule;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.InputStream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * A junit test class for LegacyStoreObjManager
 * @author Tao
 */
public class LegacyStoreObjManagerTest {
    private static final String DATA_ROOT_DIR_ENV_NAME = "DATAONE_INDEXER_METACAT_DATA_ROOT_DIR";
    private static final String DOCUMENT_ROOT_DIR_ENV_NAME =
        "DATAONE_INDEXER_METACAT_DOCUMENT_ROOT_DIR";

    private static final String DATA_ROOT_DIR_PROPERTY_NAME = "index.data.root.directory";
    private static final String DOCUMENT_ROOT_DIR_PROPERTY_NAME = "index.document.root.directory";

    public static final String DATA_DIR = "src/test/resources/org/dataone/configuration/";
    public static final String DOCUMENTS_DIR = "src/test/resources/";

    @Rule
    public EnvironmentVariablesRule environmentVariables =
        new EnvironmentVariablesRule(DATA_ROOT_DIR_ENV_NAME, null);

    @Before
    public void setUp() throws Exception {
        String propertyFilePath =
            "./src/main/resources/org/dataone/configuration/index-processor.properties";
        Settings.augmentConfiguration(propertyFilePath);
        environmentVariables.set(DATA_ROOT_DIR_ENV_NAME, DATA_DIR);
        environmentVariables.set(DOCUMENT_ROOT_DIR_ENV_NAME, DOCUMENTS_DIR);
    }

    @After
    public void tearDown() throws Exception {
        environmentVariables.set(DATA_ROOT_DIR_ENV_NAME, null);
        environmentVariables.set(DOCUMENT_ROOT_DIR_ENV_NAME, null);
    }

    /**
     * Test the constructor from the properties
     */
    @Test
    public void testConstructorFromProperties() throws Exception {
        environmentVariables.set(DATA_ROOT_DIR_ENV_NAME, null);
        environmentVariables.set(DOCUMENT_ROOT_DIR_ENV_NAME, null);
        String dataDir = "/var/metacat/data/";
        String documentDir = "/var/metacat/documents/";
        LegacyStoreObjManager manager;
        if ((new File(dataDir)).exists() && (new File(documentDir)).exists()) {
            manager = new LegacyStoreObjManager();
            assertEquals(documentDir, manager.getDocumentRootDir());
            assertEquals(dataDir, manager.getDataRootDir());
        } else {
            try {
                manager = new LegacyStoreObjManager();
                fail(
                    "Test shouldn't get here since the previous statement should throw an "
                        + "exception");
            } catch (Exception e) {
                assertTrue( e instanceof ServiceFailure);
            }
        }
        Settings.getConfiguration().setProperty(DATA_ROOT_DIR_PROPERTY_NAME, DATA_DIR);
        Settings.getConfiguration().setProperty(DOCUMENT_ROOT_DIR_PROPERTY_NAME, DOCUMENTS_DIR);
        manager = new LegacyStoreObjManager();
        assertEquals(DOCUMENTS_DIR, manager.getDocumentRootDir());
        assertEquals(DATA_DIR, manager.getDataRootDir());
        Settings.getConfiguration().setProperty(DATA_ROOT_DIR_PROPERTY_NAME, null);
        try {
            manager = new LegacyStoreObjManager();
            fail("Test shouldn't get here since the previous statement should throw an exception");
        } catch (Exception e) {
            assertTrue( e instanceof ServiceFailure);
        }
        Settings.getConfiguration().setProperty(DATA_ROOT_DIR_PROPERTY_NAME, DATA_DIR);
        manager = new LegacyStoreObjManager();
        assertEquals(DOCUMENTS_DIR, manager.getDocumentRootDir());
        assertEquals(DATA_DIR, manager.getDataRootDir());
        Settings.getConfiguration().setProperty(DOCUMENT_ROOT_DIR_PROPERTY_NAME, null);
        try {
            manager = new LegacyStoreObjManager();
            fail("Test shouldn't get here since the previous statement should throw an exception");
        } catch (Exception e) {
            assertTrue( e instanceof ServiceFailure);
        }
    }

    /**
     * Test the constructor based on environment variables
     */
    @Test
    public void testConstructor() throws Exception {
        Settings.getConfiguration().setProperty(DOCUMENT_ROOT_DIR_PROPERTY_NAME, null);
        Settings.getConfiguration().setProperty(DATA_ROOT_DIR_PROPERTY_NAME, null);
        LegacyStoreObjManager manager = new LegacyStoreObjManager();
        assertEquals(DOCUMENTS_DIR, manager.getDocumentRootDir());
        assertEquals(DATA_DIR, manager.getDataRootDir());
        environmentVariables.set(DATA_ROOT_DIR_ENV_NAME, null);
        try {
            manager = new LegacyStoreObjManager();
            fail("Test shouldn't get here since the previous statement should throw an exception");
        } catch (Exception e) {
            assertTrue( e instanceof ServiceFailure);
        }
        environmentVariables.set(DATA_ROOT_DIR_ENV_NAME, DATA_DIR);
        manager = new LegacyStoreObjManager();
        assertEquals(DOCUMENTS_DIR, manager.getDocumentRootDir());
        assertEquals(DATA_DIR, manager.getDataRootDir());
        environmentVariables.set(DOCUMENT_ROOT_DIR_ENV_NAME, null);
        try {
            manager = new LegacyStoreObjManager();
            fail("Test shouldn't get here since the previous statement should throw an exception");
        } catch (Exception e) {
            assertTrue( e instanceof ServiceFailure);
        }
    }

    /**
     * Test the getObject method
     * @throws Exception
     */
    @Test
    public void testGetObject() throws Exception {
        LegacyStoreObjManager manager = new LegacyStoreObjManager();
        assertEquals(DOCUMENTS_DIR, manager.getDocumentRootDir());
        assertEquals(DATA_DIR, manager.getDataRootDir());
        InputStream inputData = manager.getObject("config.xml");
        assertNotNull(inputData);
        InputStream inputDocument = manager.getObject("commons-logging.properties");
        assertNotNull(inputDocument);
        try {
            InputStream input = manager.getObject("foo");
            fail("Test shouldn't get here since the foo file doesn't exist");
        } catch (Exception e) {
            assertTrue(e instanceof FileNotFoundException);
        }
    }

    /**
     * Test the getSystemMetacat and getSystemMetadataStream methods
     * @throws Exception
     */
    @Test
    public void testGetSystemMetadata() throws Exception {
        String id = "doi:10.18739/A21J9795R";
        String url = "https://cn.dataone.org/cn";
        environmentVariables.set(ObjectManagerTest.NODE_BASE_URL_ENV_NAME, url);
        ObjectManager.refreshD1Node();
        LegacyStoreObjManager manager = new LegacyStoreObjManager();
        SystemMetadata systemMetadata = manager.getSystemMetadata(id);
        assertEquals(id, systemMetadata.getIdentifier().getValue());
        InputStream inputStream = manager.getSystemMetadataStream(id);
        String sysStr = IOUtils.toString(inputStream, "UTF-8");
        assertTrue(sysStr.contains("checksum"));
        assertTrue(sysStr.contains("rightsHolder"));
        assertTrue(sysStr.contains("authoritativeMemberNode"));
        try {
            systemMetadata = manager.getSystemMetadata("fake-id-foo231");
            fail("Test should get here since the object doesn't exist");
        } catch (Exception e) {
            assertTrue(e instanceof NotFound);
        }

    }
}
