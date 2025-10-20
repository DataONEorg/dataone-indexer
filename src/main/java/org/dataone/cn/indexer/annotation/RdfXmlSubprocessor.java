package org.dataone.cn.indexer.annotation;


import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;


import javax.xml.xpath.XPathExpressionException;

import org.apache.commons.codec.EncoderException;
import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.dataone.cn.indexer.parser.IDocumentSubprocessor;
import org.dataone.cn.indexer.parser.ISolrDataField;
import org.dataone.cn.indexer.parser.SubprocessorUtility;
import org.dataone.cn.indexer.solrhttp.DummySolrDoc;
import org.dataone.cn.indexer.solrhttp.HTTPService;
import org.dataone.cn.indexer.solrhttp.SolrDoc;
import org.dataone.cn.indexer.solrhttp.SolrElementField;
import org.dataone.configuration.Settings;
import org.dataone.indexer.performance.PerformanceLogger;

import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.query.Dataset;
import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QueryExecutionFactory;
import com.hp.hpl.jena.query.QueryFactory;
import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.rdf.model.ModelFactory;

/**
 * A solr index parser for an RDF/XML file.
 * The solr doc of the RDF/XML object only has the system metadata information.
 * The solr docs of the science metadata doc and data file have the annotation information.
 */
public class RdfXmlSubprocessor implements IDocumentSubprocessor {

    private static final String RESOLVE_URL = "https://cn.dataone.org/cn/v1/resolve/";

    private static Log log = LogFactory.getLog(RdfXmlSubprocessor.class);
    private static PerformanceLogger perfLog = PerformanceLogger.getInstance();
    /**
     * If xpath returns true execute the processDocument Method
     */
    private List<String> matchDocuments = null;

    private List<ISolrDataField> fieldList = new ArrayList<ISolrDataField>();

    private List<String> fieldsToMerge = new ArrayList<String>();

 
    private HTTPService httpService = null;

    private String solrQueryUri = Settings.getConfiguration().getString("solr.query.uri");

    private SubprocessorUtility processorUtility;

    /**
     * Returns true if subprocessor should be run against objects
     * 
     * @param formatId the document to be processed
     * @return true if this processor can parse the formatId
     */
    public boolean canProcess(String formatId) {
        return matchDocuments.contains(formatId);
    }

    public List<String> getMatchDocuments() {
        return matchDocuments;
    }

    public void setMatchDocuments(List<String> matchDocuments) {
        this.matchDocuments = matchDocuments;
    }

    public List<ISolrDataField> getFieldList() {
        return fieldList;
    }

    public void setFieldList(List<ISolrDataField> fieldList) {
        this.fieldList = fieldList;
    }

    @Override
    public Map<String, SolrDoc> processDocument(String identifier, Map<String, SolrDoc> docs,
            InputStream is) throws Exception {

        if (log.isTraceEnabled()) {
            log.trace("INCOMING DOCS to processDocument(): ");
            serializeDocuments(docs);
        }

        SolrDoc resourceMapDoc = docs.get(identifier);
        process(resourceMapDoc, is, docs);

        if (log.isTraceEnabled()) {
            log.trace("OUTGOING DOCS from processDocument(): ");
            serializeDocuments(docs);
        }

        return docs;
    }

    /**
     * Serialize documents to be indexed for debugging
     * 
     * @param docs
     * @throws IOException
     */
    private void serializeDocuments(Map<String, SolrDoc> docs) {
        StringBuilder documents = new StringBuilder();
        documents.append("<docs>");

        for (SolrDoc doc : docs.values()) {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            try {
                doc.serialize(baos, "UTF-8");

            } catch (IOException e) {
                log.trace("Couldn't serialize documents: " + e.getMessage());
            }
            
            try {
                documents.append(baos.toString());
            } finally {
                IOUtils.closeQuietly(baos);
            }
        }
        documents.append("</docs>");
        log.trace(documents.toString());
    }

    private void process(SolrDoc resourceMapDocument, InputStream is,
                                  Map<String, SolrDoc> docs) throws Exception {
        
        // get the triplestore dataset
        long start = System.currentTimeMillis();
        Dataset dataset = TripleStoreService.getInstance().getDataset();
        try {
            perfLog.log("RdfXmlSubprocess.process gets a dataset from triple store service ",
                        System.currentTimeMillis() - start);
            
            // read the annotation
            String resourceMapDocId = resourceMapDocument.getIdentifier();
            String name = resourceMapDocId;
    
            //Check if the identifier is a valid URI and if not, make it one by prepending "http://"
            URI nameURI;
            String scheme = null;
            try {
                nameURI = new URI(resourceMapDocId);
                scheme = nameURI.getScheme();
                
            } catch (URISyntaxException use) {
                // The identifier can't be parsed due to offending characters. It's not a URL
                
                name = RESOLVE_URL + resourceMapDocId;
            }
            
            // The had no scheme prefix. It's not a URL
            if ((scheme == null) || (scheme.isEmpty())) {
                name = RESOLVE_URL + resourceMapDocId;
                
            }
            
            long startOntModel = System.currentTimeMillis();
            boolean loaded = dataset.containsNamedModel(name);
            if (!loaded) {
                OntModel ontModel = ModelFactory.createOntologyModel();
                ontModel.read(is, name);
                dataset.addNamedModel(name, ontModel);
            }
            perfLog.log("RdfXmlSubprocess.process adds ont-model ",
                        System.currentTimeMillis() - startOntModel);

            //Track timing of this process
            long startField = System.currentTimeMillis();
            
            //Process each field listed in the fieldList in this subprocessor
            for (ISolrDataField field : this.fieldList) {
                long filed = System.currentTimeMillis();
                String q = null;
                
                //Process Sparql fields
                if (field instanceof SparqlField) {

                    //Get the Sparql query for this field
                    q = ((SparqlField) field).getQuery();

                    //Replace the graph name with the URI of this resource map
                    q = q.replaceAll("\\$GRAPH_NAME", name);
                    q = q.replaceAll("\\$DEFAULT_URI", RESOLVE_URL);


                    //Create a Query object
                    Query query = QueryFactory.create(q);
                    log.trace("Executing SPARQL query:\n" + query.toString());
                    
                    //Execute the Sparql query
                    QueryExecution qexec = QueryExecutionFactory.create(query, dataset);
                    
                    //Get the results of the query
                    ResultSet results = qexec.execSelect();
                    
                    //Iterate over each query result and process it
                    while (results.hasNext()) {

                        //Create a SolrDoc for this query result
                        SolrDoc solrDoc = null;
                        QuerySolution solution = results.next();
                        log.trace(solution.toString());
    
                        //Sparql queries can identify a SolrDoc by pid or seriesId. 
                        //If the Sparql query uses a pid, get the SolrDoc by pid
                        if (solution.contains("pid")) {
                            //Get the pid from the query result
                            String id = solution.getLiteral("pid").getString();

                            //Get the SolrDoc from the hash map, if it exists
                            solrDoc = docs.get(id);
                            
                            if (solrDoc == null) {
                                //If the id matches the document we are currently indexing,
                                // use that SolrDoc
                                if(id.equals(resourceMapDocId)) {
                                    solrDoc = resourceMapDocument;
                                }
                                //If the SolrDoc doesn't exist yet, create one
                                else {
                                    solrDoc = new DummySolrDoc(id, null);
                                }
                                //Add the SolrDoc to the hash map
                                docs.put(id, solrDoc);
                            }
                        }

                        else if (solution.contains("seriesId")) {
                            throw new RuntimeException("DataONE-Indexer shouldn't handle the sid "
                                                           + "queries");
                        }
    
                        //Get the index field name and value returned from the Sparql query
                        if (solution.contains(field.getName())) {
                            //Get the value for this field
                            String value = solution.get(field.getName()).toString();
                            
                            //Create an index field for this field name and value
                            SolrElementField f = new SolrElementField(field.getName(), value);
                            
                            //If this field isn't already populated with the same value, then add it
                            if (!solrDoc.hasFieldWithValue(f.getName(), f.getValue())) {
                                solrDoc.addField(f);
                            }
                        }
                    }
                }
                perfLog.log("RdfXmlSubprocess.process process the field " + field.getName(),
                            System.currentTimeMillis() - filed);
            }
            perfLog.log("RdfXmlSubprocess.process process the fields total ",
                        System.currentTimeMillis() - startField);
            // clean up the triple store
            perfLog.log("RdfXmlSubprocess.process() total take ", System.currentTimeMillis() - start);
        } finally {
            try {
                TripleStoreService.getInstance().destoryDataset(dataset);
            } catch (Exception e) {
                log.warn("A tdb directory can't be removed since "+e.getMessage(), e);
            }
        }
    }


    @Override
    public SolrDoc mergeWithIndexedDocument(SolrDoc indexDocument) throws IOException,
            EncoderException, XPathExpressionException {
        return processorUtility.mergeWithIndexedDocument(indexDocument, fieldsToMerge);
    }

    public List<String> getFieldsToMerge() {
        return fieldsToMerge;
    }

    public void setFieldsToMerge(List<String> fieldsToMerge) {
        this.fieldsToMerge = fieldsToMerge;
    }
    
    /**
     * Get the http service object
     * @return  the http service object 
     */
    public HTTPService getHttpService() {
        return httpService;
    }

    /**
     * Set the http service object for this class
     * @param httpService  the object will be set
     */
    public void setHttpService(HTTPService httpService) {
        this.httpService = httpService;
    }


    /**
     * Get the subprocessor utility
     * @return  the subprocessor utility object
     */
    public SubprocessorUtility getProcessorUtility() {
        return processorUtility;
    }

    /**
     * Set the subprocessor utility object for this class
     * @param processorUtility  the subprocessor utility object will be set
     */
    public void setProcessorUtility(SubprocessorUtility processorUtility) {
        this.processorUtility = processorUtility;
    }
}
