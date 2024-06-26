package org.dataone.indexer.storage;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.security.NoSuchAlgorithmException;
import java.util.Properties;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.dataone.configuration.Settings;
import org.dataone.hashstore.HashStore;
import org.dataone.hashstore.HashStoreFactory;
import org.dataone.hashstore.exceptions.HashStoreFactoryException;

/**
 * The HashFileStore implementation of the Storage interface 
 */
public class HashStorage implements Storage {

    private static Log logMetacat = LogFactory.getLog(HashStorage.class);
    private static HashStorage hashStorage;
    private HashStore hashStore;

    /**
     * Private constructor
     * @param className the name of the implementation class
     * @throws IOException
     * @throws HashStoreFactoryException
     */
    private HashStorage(String className) throws HashStoreFactoryException, IOException {
        String rootPath = Settings.getConfiguration().getString("storage.hashstore.rootDirectory");
        if (rootPath == null) {
            throw new HashStoreFactoryException("HashStorage.constructor - The HashStore root path "
                        + " is null or blank from the property of storage.hashstore.rootDirectory");
        }
        String directoryDepth = Settings.getConfiguration()
                                               .getString("storage.hashstore.directory.depth", "3");
        String directoryNameWidth = Settings.getConfiguration()
                                               .getString("storage.hashstore.directory.width", "2");
        String fileNameAlgorithm = Settings.getConfiguration()
                                       .getString("storage.hashstore.fileNameAlgorithm", "SHA-256");
        String defaultNamespace = Settings.getConfiguration()
                                         .getString("storage.hashstore.defaultNamespace",
                                        "https://ns.dataone.org/service/types/v2.0#SystemMetadata");
        Properties storeProperties = new Properties();
        storeProperties.setProperty("storePath", rootPath);
        storeProperties.setProperty("storeDepth", directoryDepth);
        storeProperties.setProperty("storeWidth", directoryNameWidth);
        storeProperties.setProperty("storeAlgorithm", fileNameAlgorithm);
        storeProperties.setProperty("storeMetadataNamespace", defaultNamespace);
        hashStore = HashStoreFactory.getHashStore(className, storeProperties);
    }

    /**
     * Get the instance of the class through the singleton pattern
     * @param className the name of the implementation class
     * @return the instance of the class
     * @throws IOException
     */
    public static HashStorage getInstance(String className) throws IOException {
        if(hashStorage == null) {
            synchronized(HashStorage.class) {
                if (hashStorage == null) {
                    hashStorage = new HashStorage(className);
                }
             }
        }
        return hashStorage;
    }

    @Override
    public InputStream retrieveObject(String pid)
            throws IllegalArgumentException, FileNotFoundException, IOException,
            NoSuchAlgorithmException {
        return hashStore.retrieveObject(pid);
    }

    @Override
    public InputStream retrieveSystemMetadata(String pid)
            throws IllegalArgumentException, FileNotFoundException, IOException,
            NoSuchAlgorithmException {
        return hashStore.retrieveMetadata(pid);
    }

    @Override
    public void storeObject(InputStream object, String pid) throws NoSuchAlgorithmException,
                                               IOException,RuntimeException, InterruptedException {
        hashStore.storeObject(object, pid, null, null, null, -1);
    }

}
