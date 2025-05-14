package org.dataone.cn.indexer.object;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.lang.reflect.InvocationTargetException;

/**
 * Class to create a concrete ObjectManager Object
 * @author Tao
 */
public class ObjectManagerFactory {
    private static volatile ObjectManager manager = null;
    private static String className = "org.dataone.cn.indexer.object.hashstore.HashStoreObjManager";
    private static Log logger = LogFactory.getLog(ObjectManagerFactory.class);
    private static final String OBJECT_MANAGER_CLASSNAME_ENV =
        "DATAONE_INDEXER_OBJECT_MANAGER_CLASSNAME";

    /**
     * Create a Concrete ObjectManager object by the single pattern.
     * First, Indexer will check if the env variable of DATAONE_INDEXER_OBJECT_MANAGER_CLASSNAME
     * is defined. If it is defined, indexer will use it; otherwise it uses the default one -
     * org.dataone.cn.indexer.object.hashstore.HashStoreObjManager
     * @return an ObjectManager object
     * @throws ClassNotFoundException
     * @throws NoSuchMethodException
     * @throws InvocationTargetException
     * @throws InstantiationException
     * @throws IllegalAccessException
     */
    public static ObjectManager getObjectManager()
        throws ClassNotFoundException, NoSuchMethodException, InvocationTargetException,
        InstantiationException, IllegalAccessException {
        String classNameFromEnv = System.getenv(OBJECT_MANAGER_CLASSNAME_ENV);
        if (classNameFromEnv != null && !classNameFromEnv.isBlank()) {
            logger.debug("The ObjectManager class name form env variable "
                            + OBJECT_MANAGER_CLASSNAME_ENV + " is " + classNameFromEnv);
            className = classNameFromEnv;
        }
        if (manager == null) {
            synchronized (ObjectManagerFactory.class) {
                if (manager == null) {
                    logger.info("The final ObjectManager class name form env variable is "
                                     + classNameFromEnv);
                    Class managerClass = Class.forName(className);
                    manager = (ObjectManager) managerClass.getDeclaredConstructor().newInstance();
                }
            }
        }
        return manager;
    }
}
