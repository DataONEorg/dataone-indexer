package org.dataone.cn.indexer.annotation;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileAttribute;
import java.util.Hashtable;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
import org.dataone.configuration.Settings;

import com.hp.hpl.jena.query.Dataset;
import com.hp.hpl.jena.tdb.TDBFactory;

public class TripleStoreService {
    private static Logger log = Logger.getLogger(TripleStoreService.class);
	private static TripleStoreService instance;
	private static Hashtable<Dataset, File> dataset_location_map = new Hashtable<Dataset, File>();
	
	private TripleStoreService() {}
	
	public static TripleStoreService getInstance() {
		if (instance == null) {
			instance = new TripleStoreService();
		}
		return instance;
	}
	
	public Dataset getDataset() throws IOException {
		String directory = Settings.getConfiguration().getString("index.tdb.directory", "./tdb");
		log.debug("TripleStoreService.getDataset - the parent directory of the triple store location ================= is "+directory);
    	// for testing, delete the triplestore each time
    	File dir = new File(directory);
    	if (!dir.exists()) {
        	dir.mkdirs();
    	}
    	String prefix ="tdb";
    	Path store = Files.createTempDirectory(dir.toPath(), prefix, new FileAttribute<?>[] {});
    	FileUtils.forceDeleteOnExit(store.toFile());
    	log.info("TripleStoreService.getDataset - the store directory  ================= is "+store.toString());
		Dataset dataset = TDBFactory.createDataset(store.toString());
		dataset_location_map.put(dataset, store.toFile());
		return dataset;
	}
	
	/**
	 * Destroy the given dataset (delete the backup store)
	 * @param dataset
	 */
	public void destoryDataset(Dataset dataset) throws IOException {
	    if(dataset != null) {
	        TDBFactory.release(dataset);
	        File file = dataset_location_map.remove(dataset);
	        if(file != null) {
	            FileUtils.deleteDirectory(file);	           
	            log.debug("The direcotry was deleted "+file.getAbsolutePath());
	        }
	        log.debug("The size of hashmap is "+dataset_location_map.size());
	    }
	}
}
