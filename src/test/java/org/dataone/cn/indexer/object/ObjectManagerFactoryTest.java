package org.dataone.cn.indexer.object;

import org.dataone.cn.indexer.object.hashstore.HashStoreObjManager;
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
    @Rule
    public EnvironmentVariablesRule environmentVariablesClassName =
        new EnvironmentVariablesRule(envName, null);
    /**
     * Test to create a HashStoreObjManager
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
    }
}
