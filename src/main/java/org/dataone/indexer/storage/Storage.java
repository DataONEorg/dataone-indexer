package org.dataone.indexer.storage;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.security.NoSuchAlgorithmException;


/**
 * The Storage represents the interface to access the objects and system metadata
 */
public interface Storage {
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
    public InputStream retrieveObject(String pid) throws IllegalArgumentException,
            FileNotFoundException, IOException, NoSuchAlgorithmException;

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
    public InputStream retrieveSystemMetadata(String pid) throws IllegalArgumentException,
                            FileNotFoundException, IOException, NoSuchAlgorithmException;

}
