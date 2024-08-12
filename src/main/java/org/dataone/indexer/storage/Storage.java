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
public class Storage {

    private static Log log = LogFactory.getLog(Storage.class);
    private static Storage instance;
    private static HashStore hashStore;
    static {
        try {
            instance = new Storage();
        } catch (IOException e) {
            log.error(
                "Dataone-indexer cannot initialize the Storage class since " + e.getMessage());
        }
    }

    /**
     * Private constructor
     * @throws IOException
     * @throws HashStoreFactoryException
     */
    private Storage() throws HashStoreFactoryException, IOException {
        String className = Settings.getConfiguration().getString("storage.className");
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
     * @return the instance of the class
     * @throws IOException
     */
    public static Storage getInstance() throws IOException {
        return instance;
    }

    /**
     * Returns an InputStream to an object from HashStore using a given persistent identifier.
     *
     * @param pid Authority-based identifier
     * @return Object InputStream
     * @throws IllegalArgumentException When pid is null or empty
     * @throws FileNotFoundException    When requested pid has no associated object
     * @throws IOException              I/O error when creating InputStream to object
     * @throws NoSuchAlgorithmException When algorithm used to calculate object address is not
     *                                  supported
     */
    public InputStream retrieveObject(String pid)
            throws IllegalArgumentException, FileNotFoundException, IOException,
            NoSuchAlgorithmException {
        return hashStore.retrieveObject(pid);
    }

    /**
     * Returns an InputStream to the system metadata content of a given pid
     *
     * @param pid      Authority-based identifier
     * @return Metadata InputStream
     * @throws IllegalArgumentException When pid/formatId is null or empty
     * @throws FileNotFoundException    When requested pid+formatId has no associated object
     * @throws IOException              I/O error when creating InputStream to metadata
     * @throws NoSuchAlgorithmException When algorithm used to calculate metadata address is not
     *                                  supported
     */
    public InputStream retrieveSystemMetadata(String pid)
            throws IllegalArgumentException, FileNotFoundException, IOException,
            NoSuchAlgorithmException {
        return hashStore.retrieveMetadata(pid);
    }

    /**
     * Store the input stream object into hash store. This method is only for the test classes.
     * @param object  the input stream of the object
     * @param pid  the identifier of the object which will be stored
     * @throws NoSuchAlgorithmException
     * @throws IOException
     * @throws RuntimeException
     * @throws InterruptedException
     */
    public void storeObject(InputStream object, String pid) throws NoSuchAlgorithmException,
                                               IOException,RuntimeException, InterruptedException {
        hashStore.storeObject(object, pid, null, null, null, -1);
    }

    /**
     * Store the system metadata into hash store. This method is only for the test classes.
     * @param metadata  the input stream of the system metadata
     * @param pid  the identifier of the system metadata
     * @throws IOException
     * @throws IllegalArgumentException
     * @throws FileNotFoundException
     * @throws InterruptedException
     * @throws NoSuchAlgorithmException
     */
    public void storeMetadata(InputStream metadata, String pid) throws IOException,
                                                IllegalArgumentException, FileNotFoundException,
                                                InterruptedException, NoSuchAlgorithmException {
        hashStore.storeMetadata(metadata, pid);
    }

}
