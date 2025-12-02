package org.dataone.cn.indexer.object;

import org.dataone.cn.indexer.object.hashstore.HashStoreObjManager;
import org.dataone.cn.indexer.object.legacystore.LegacyStoreObjManager;
import org.dataone.cn.indexer.object.legacystore.LegacyStoreObjManagerTest;
import org.junit.Rule;
import org.junit.Test;
import uk.org.webcompere.systemstubs.rules.EnvironmentVariablesRule;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * A Junit test class for ObjectManagerFactory
 * @author Tao
 */
public class ObjectManagerFactoryTest {

    private static final String envName = "DATAONE_INDEXER_OBJECT_MANAGER_CLASS_NAME";
    private static final String DATA_ROOT_DIR_ENV_NAME = "DATAONE_INDEXER_METACAT_DATA_ROOT_DIR";
    private static final String DOCUMENT_ROOT_DIR_ENV_NAME =
        "DATAONE_INDEXER_METACAT_DOCUMENT_ROOT_DIR";
    @Rule
    public EnvironmentVariablesRule environmentVariablesClassName =
        new EnvironmentVariablesRule(envName, null);
    /**
     * Test to create a HashStoreObjManager instance
     * @throws Exception
     */
    @Test
    public void testHashStoreObjManager() throws Exception {
        environmentVariablesClassName.set(envName, null);
        ObjectManagerFactory.resetManagerNull();
        ObjectManager manager = ObjectManagerFactory.getObjectManager();
        assertTrue(manager instanceof HashStoreObjManager);
    }

    /**
     * Test to create a LegacyStroeObjectManager instance
     * @throws Exception
     */
    @Test
    public void testLegacyStoreObjManager() throws Exception {
        environmentVariablesClassName.set(
            envName, "org.dataone.cn.indexer.object.legacystore.LegacyStoreObjManager");
        environmentVariablesClassName.set(DATA_ROOT_DIR_ENV_NAME,
                                          LegacyStoreObjManagerTest.DATA_DIR);
        environmentVariablesClassName.set(DOCUMENT_ROOT_DIR_ENV_NAME,
                                          LegacyStoreObjManagerTest.DOCUMENTS_DIR);
        ObjectManagerFactory.resetManagerNull();
        ObjectManager manager = ObjectManagerFactory.getObjectManager();
        assertTrue(manager instanceof LegacyStoreObjManager);
        environmentVariablesClassName.set(envName, null);
        environmentVariablesClassName.set(DATA_ROOT_DIR_ENV_NAME, null);
        environmentVariablesClassName.set(DOCUMENT_ROOT_DIR_ENV_NAME, null);
    }

    /**
     * Test the failure with a wrong class name
     * @throws Exception
     */
    @Test
    public void testWrongClassName() throws Exception {
        environmentVariablesClassName.set(envName, "foo.foo1.className");
        ObjectManagerFactory.resetManagerNull();
        try {
            ObjectManager manager = ObjectManagerFactory.getObjectManager();
            fail("Test shouldn't get here since the class doesn't exist with the given name.");
        } catch (Exception e) {
            assertTrue( e instanceof ClassNotFoundException);
        }
        environmentVariablesClassName.set(envName, null);
    }
}
