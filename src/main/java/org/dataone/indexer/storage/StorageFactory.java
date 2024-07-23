package org.dataone.indexer.storage;

import java.io.IOException;

import org.dataone.configuration.Settings;

/**
 * The factory class to create a Storage instance
 */
public class StorageFactory {

    /**
     * Get the Storage implementation instance
     * @return  the Storage class instance
     * @throws IOException
     * @throws ServiceException
     */
    public static Storage getStorage() throws IOException, IllegalArgumentException{
        String className = Settings.getConfiguration().getString("storage.className");
        if (className != null && className.startsWith("org.dataone.hashstore")) {
            return HashStorage.getInstance(className);
        } else {
            throw new IllegalArgumentException("StorageFactory.getStorage - Unrecognized the "
                                             + " storage class " + className
                                             + ". So Indexer can't initialize the storage system.");
        }
    }
}
