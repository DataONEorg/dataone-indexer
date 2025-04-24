package org.dataone.cn.indexer.annotation;

import java.util.List;
import java.util.Set;
import java.util.Map;
import java.util.HashMap;
import java.util.HashSet;
import java.io.File;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;

import org.apache.commons.logging.Log;

import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.ontology.OntDocumentManager;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QueryException;
import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QueryExecutionFactory;
import com.hp.hpl.jena.query.QueryFactory;
import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.ResultSet;

import org.apache.commons.logging.LogFactory;
import org.dataone.cn.indexer.parser.ISolrDataField;
import org.dataone.cn.indexer.annotation.SparqlField;

public class OntologyModelService {
  private static Log log = LogFactory.getLog(OntologyModelService.class);
  private static String jarPrefix = "jar:file:";
  private static String jarAppendix = "!/";
  private static String filePrefix = "file:src/main/resources/";
  private static OntologyModelService instance = null;
  private static OntModel ontModel = null;
  public static final String FIELD_ANNOTATION = "sem_annotation";
  private static List<ISolrDataField> fieldList = new ArrayList<ISolrDataField>();
  private static List<String> ontologyList = new ArrayList<String>();
  private static Map<String, String> altEntryList = new HashMap<String, String>();
  private OntologyModelService() {}

  public static OntologyModelService getInstance() {
    if (instance == null) {
        synchronized(OntologyModelService.class) {
            if (instance == null) {
                instance = new OntologyModelService();
                // Populate the ontology model with registered ontologies and alternate
                // entries (local cache locations)
                instance.init();
            }
        }
    }
    log.info("OntologyModelService.getInstance - the instance is " + instance.toString());
    return instance;
  }

  private void init() {
    log.info(OntologyModelService.class.getName() + " init() called");

    if (ontModel != null) {
      return;
    }

    ontModel = ModelFactory.createOntologyModel();

    // Add any configured local copies of ontologies
    loadAltEntries();

    // Safely try to read the ontologies from disk
    for (String ontologyUri : ontologyList) {
      try {
        log.debug("Loading ontology " + ontologyUri);
        ontModel.read(ontologyUri);

      } catch (Exception e) {
        log.debug("Failed to read " + ontologyUri);
        e.printStackTrace();
      }
    }
  }

  protected Map<String, Set<String>> expandConcepts(String uri) {
    long start_method = System.currentTimeMillis();
    log.debug("expandConcepts " + uri);
    Map<String, Set<String>> conceptFields = new HashMap<String, Set<String>>();

    if (uri == null || uri.length() < 1) {
      log.debug("Expansion failed because uri " + uri + " was either null or non-zero length.");

      return conceptFields;
    }

    log.debug("About to run through fieldList which is size " + fieldList.size());

    try {
      for (ISolrDataField field : fieldList) {
        String q = null;

        if (!(field instanceof SparqlField)) {
          continue;
        }

        q = ((SparqlField) field).getQuery();
        q = q.replaceAll("\\$CONCEPT_URI", uri);

        log.debug("SPARQL Query" + q.toString());
        Query query = QueryFactory.create(q);
        QueryExecution qexec = QueryExecutionFactory.create(query, ontModel);
        ResultSet results = qexec.execSelect();
        
        // each field might have multiple solution values
        String name = field.getName();
        Set<String> values = new HashSet<String>();
        long start = System.currentTimeMillis();
        while (results.hasNext()) {
          QuerySolution solution = results.next();
          log.debug("Solution SPARQL result: " + solution.toString());
          long end = System.currentTimeMillis();
          log.info("OntologyMondelService.expandConcepts - the time to execute results.hasNext() " + (end - start) + "milliseconds.");
          if (!solution.contains(name)) {
            continue;
          }

          // Don't include blank nodes (anonymous)
          if (solution.get(field.getName()).isAnon()) {
            continue;
          }

          String value = solution.get(name).toString();
          log.debug("Adding value " + value);
          values.add(value);
          start = System.currentTimeMillis();
        }

        conceptFields.put(name, values);
      }
    } catch (QueryException ex) {
      log.error("OntologyModelService.expandConcepts(" + uri + ") encountered an exception while querying.");
    }
    long end = System.currentTimeMillis();
    log.info("OntologyModelService.expandConcept - the total time for the method is " + (end -start_method) + " milliseconds.");
    return conceptFields;
  }

  public List<ISolrDataField> getFieldList() {
    return fieldList;
  }

  public void setFieldList(List<ISolrDataField> flds) {
    fieldList = flds;
  }

  private List<String> getOntologyList() {
    return ontologyList;
  }

  public void setOntologyList(List<String> ontlist) {
    ontologyList = ontlist;
  }

  public Map<String, String> getAltEntryList() {
    return altEntryList;
  }

  public void setAltEntryList(Map<String, String> entryList) {
    altEntryList = entryList;
  }

  protected void loadAltEntries() {
    log.debug("OntologyModelService - Loading altEntries of size " + altEntryList.size());

    OntDocumentManager ontManager = ontModel.getDocumentManager();

    // Ignore reading imports. This prevents Jena from going onto the network
    // to grab imported OWL files in OWL files contained in the altEntries
    // list. I did this for speed and for security.
    ontManager.setProcessImports(false);
    
    boolean firstEntry = true;
    String prefix = null;
    for (Map.Entry<String, String> entry: altEntryList.entrySet()) {
        String path = entry.getValue();//this is the relative path
        if (firstEntry) {
            firstEntry = false;
            String jarFilePath = getJarFilePath();
            if (jarFilePath != null && !jarFilePath.trim().equals("")) {
                //if the file path doesn't exist, we will try the jar file path
                prefix = jarPrefix + jarFilePath + jarAppendix;
            } else {
                //the file exists and we will use the file path
                prefix = filePrefix;
            }
            log.info("OntologyModelService.loadAltEntries - the final prefix for ontology files is " + prefix);
        }
        ontManager.addAltEntry(entry.getKey(), prefix + path);
    }
  }
  
  /**
   * Get the current jar file path 
   * @return  the jar file path
   */
  protected String getJarFilePath() {
      String jarPath = "";
      URL classResource = OntologyModelService.class.getResource(OntologyModelService.class.getSimpleName() + ".class");
      if (classResource == null) {
          return jarPath;
      }
      String url = classResource.toString();
      log.info("OntologyModelService.getJarFilePrefix - the full path of the class is " + url);
      if (url.startsWith(jarPrefix)) {
          // extract 'file:......jarName.jar' part from the url string
          String path = url.replaceAll("^jar:(file:.*[.]jar)!/.*", "$1");
          try {
              jarPath =  Paths.get(new URL(path).toURI()).toString();
        } catch (MalformedURLException | URISyntaxException e) {
            jarPath = "";
        }
      }
      log.info("OntologyModelService.getJarFilePrefix - the full path of the jar file is " + jarPath);
      return jarPath;
  }
}
