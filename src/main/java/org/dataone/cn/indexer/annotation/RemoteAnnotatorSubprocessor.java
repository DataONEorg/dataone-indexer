package org.dataone.cn.indexer.annotation;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeSet;

import javax.xml.xpath.XPathExpressionException;

import net.minidev.json.JSONArray;
import net.minidev.json.JSONObject;
import net.minidev.json.JSONValue;

import org.apache.commons.codec.EncoderException;
import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClientBuilder;
import org.dataone.cn.indexer.parser.IDocumentSubprocessor;
import org.dataone.cn.indexer.parser.ISolrDataField;
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
import com.hp.hpl.jena.tdb.TDBFactory;

/**
 * The intent of this subprocessor is to fetch annotations about the given 
 * documents as housed in an external store and add them to the solr index.
 * As of November 2014, this is still experimental for the semantic goals of 
 * the project.
 * @author leinfelder
 *
 */
public class RemoteAnnotatorSubprocessor implements IDocumentSubprocessor {

    private PerformanceLogger perfLog = PerformanceLogger.getInstance();
    
    private static Log log = LogFactory.getLog(RemoteAnnotatorSubprocessor.class);

    private List<ISolrDataField> fieldList = new ArrayList<ISolrDataField>();
    private HttpClientBuilder httpClientBuilder = HttpClientBuilder.create();

    public List<ISolrDataField> getFieldList() {
        return fieldList;
    }

    public void setFieldList(List<ISolrDataField> fieldList) {
        this.fieldList = fieldList;
    }

    @Override
    public boolean canProcess(String formatId) {
        // process any document, looking for annotations about said document
        return true;
    }

    @Override
    public Map<String, SolrDoc> processDocument(String identifier, Map<String, SolrDoc> docs,
            InputStream is) throws Exception {

        // for each document, check if there are any annotations
        Iterator<Entry<String, SolrDoc>> entries = docs.entrySet().iterator();
        while (entries.hasNext()) {
            Entry<String, SolrDoc> entry = entries.next();
            String pid = entry.getKey();
            SolrDoc solrDoc = entry.getValue();

            // check for annotations, and add them if found
            long lookUpAnnotationsStart = System.currentTimeMillis();
            SolrDoc annotations = lookUpAnnotations(pid);
            perfLog.log("RemoteAnnotatorSubprocessor.lookUpAnnotations()", System.currentTimeMillis() - lookUpAnnotationsStart);
            if (annotations != null) {
                Iterator<SolrElementField> annotationIter = annotations.getFieldList().iterator();
                // each field can have multiple values
                while (annotationIter.hasNext()) {
                    SolrElementField annotation = annotationIter.next();
                    solrDoc.addField(annotation);
                }
            }
        }

        // return the collection that we have augmented
        return docs;
    }

    /**
     * Look up annotations from annotator service
     * @see "http://docs.annotatorjs.org/en/latest/storage.html"
     * @param pid the identifier to fetch annotations about
     * @return
     */
    private SolrDoc lookUpAnnotations(String pid) {

        String annotatorUrl = null;
        String consumerKey = null;
        try {

            annotatorUrl = Settings.getConfiguration().getString("annotator.store.url");
            consumerKey = Settings.getConfiguration().getString("annotator.consumerKey");

            // skip if not configured to query the annotator-store
            if (annotatorUrl == null || annotatorUrl.length() == 0) {
                return null;
            }

            // TODO: query for matching PID only - wasting time iterating over full list
            //String urlParameters = "pid=" + URLEncoder.encode(pid, "UTF-8");
            String urlParameters = "consumer=" + consumerKey;

            String url = annotatorUrl + "?" + urlParameters;
            HttpClient client = httpClientBuilder.build();
            HttpGet method = new HttpGet(url);
            method.addHeader("Accept", "application/json");
            HttpResponse response = client.execute(method);
            InputStream is = response.getEntity().getContent();

            String results = null; 
            try {
                results = IOUtils.toString(is, "UTF-8");
            } finally {
                IOUtils.closeQuietly(is);
            }
            log.debug("RESULTS: " + results);
            JSONObject jo = (JSONObject) JSONValue.parse(results);

            JSONArray rows = (JSONArray) jo.get("rows");
            int count = rows.size();
            SolrDoc annotations = new SolrDoc();

            // use catch-all annotation field for the tags
            String tagKey = "sem_annotation";

            // track the comments here
            String commentKey = "sem_comment";

            for (int i = 0; i < count; i++) {
                JSONObject row = (JSONObject) rows.get(i);

                // skip this row if it is not about this pid
                // FIXME: Bug in annotator-store prevents effective search by pid
                String pidValue = row.get("pid").toString();
                if (!pidValue.equals(pid)) {
                    continue;
                }

                // index the (semantic) tags
                // if the annotation told us the target index field, then use it
                Object field = row.get("field");
                if (field != null) {
                    tagKey = field.toString();
                }

                // do not index rejected annotations
                Object reject = row.get("reject");
                if (reject != null && Boolean.parseBoolean(reject.toString())) {
                    // include empty index values to force removal
                    if (!annotations.hasFieldWithValue(tagKey, "")) {
                        annotations.addField(new SolrElementField(tagKey, ""));
                    }
                    if (!annotations.hasFieldWithValue(commentKey, "")) {
                        annotations.addField(new SolrElementField(commentKey, ""));
                    }
                    continue;
                }

                Object obj = row.get("tags");
                if (obj instanceof JSONArray) {
                    JSONArray tags = (JSONArray) obj;
                    for (Object tag : tags) {
                        String value = tag.toString();
                        if (!annotations.hasFieldWithValue(tagKey, value)) {
                            annotations.addField(new SolrElementField(tagKey, value));
                        }
                    }
                } else {
                    String value = obj.toString();
                    if (!annotations.hasFieldWithValue(tagKey, value)) {
                        annotations.addField(new SolrElementField(tagKey, value));
                    }
                }

                // index the comments
                Object commentObj = row.get("text");
                if (commentObj != null) {
                    String value = commentObj.toString();
                    if (value != null && value.length() > 0) {
                        if (!annotations.hasFieldWithValue(commentKey, value)) {
                            annotations.addField(new SolrElementField(commentKey, value));
                        }
                    }
                }
            }

            // expand the tags, adding expanded concepts to the existing fields
            for (String tag : annotations.getAllFieldValues(tagKey)) {
                try {
                    // get the expanded tags
                    Map<String, Set<String>> expandedConcepts = this.expandConcepts(tag);
                    for (Map.Entry<String, Set<String>> entry : expandedConcepts.entrySet()) {
                        for (String value : entry.getValue()) {
                            String name = entry.getKey();
                            if (!annotations.hasFieldWithValue(name, value)) {
                                annotations.addField(new SolrElementField(name, value));
                            }
                        }
                    }
                } catch (Exception e) {
                    log.error("Problem exapnding concept: " + tag, e);
                }
            }

            // return them
            return annotations;

        } catch (Exception e) {
            log.error("Could not lookup annotation using: " + annotatorUrl, e);
        }

        return null;
    }

    protected Map<String, Set<String>> expandConcepts(String uri) throws Exception {

        // return structure allows multi-valued fields
        Map<String, Set<String>> conceptFields = new HashMap<String, Set<String>>();

        if (uri == null || uri.length() < 1) {
            return conceptFields;
        }

        // get the triples tore dataset
        Dataset dataset = TripleStoreService.getInstance().getDataset();
        try {
            
            // load the ontology
            boolean loaded = dataset.containsNamedModel(uri);
            if (!loaded) {
                OntModel ontModel = ModelFactory.createOntologyModel();
                //InputStream sourceStream = new URI(uri).toURL().openStream();
                // TODO: look up physical source from bioportal
                ontModel.read(uri);
                dataset.addNamedModel(uri, ontModel);
            }
    
            // process each field query
            for (ISolrDataField field : fieldList) {
                String q = null;
                if (field instanceof SparqlField) {
                    q = ((SparqlField) field).getQuery();
                    q = q.replaceAll("\\$CONCEPT_URI", uri);
                    q = q.replaceAll("\\$GRAPH_NAME", uri);
                    Query query = QueryFactory.create(q);
                    QueryExecution qexec = QueryExecutionFactory.create(query, dataset);
                    ResultSet results = qexec.execSelect();
    
                    // each field might have multiple solution values
                    String name = field.getName();
                    Set<String> values = new TreeSet<String>();
    
                    while (results.hasNext()) {
    
                        QuerySolution solution = results.next();
                        log.debug(solution.toString());
    
                        // the value[s] for that field
                        if (solution.contains(name)) {
                            String value = solution.get(field.getName()).toString();
                            values.add(value);
                        }
                    }
                    conceptFields.put(name, values);
    
                }
            }
        } finally {
            try {
                TripleStoreService.getInstance().destoryDataset(dataset);
            } catch (Exception e) {
                log.warn("A tdb directory can't be removed since "+e.getMessage(), e);
            }
        }
        // clean up the triple store
        //TDBFactory.release(dataset);

        return conceptFields;

    }

    public SolrDoc mergeWithIndexedDocument(SolrDoc indexDocument) throws IOException,
            EncoderException, XPathExpressionException {
        // TODO Auto-generated method stub
        return indexDocument;
    }

}
