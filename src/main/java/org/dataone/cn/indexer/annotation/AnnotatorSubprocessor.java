package org.dataone.cn.indexer.annotation;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.xpath.XPathExpressionException;

import net.minidev.json.JSONArray;
import net.minidev.json.JSONObject;
import net.minidev.json.JSONValue;

import org.apache.commons.codec.EncoderException;
import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.dataone.cn.indexer.parser.IDocumentSubprocessor;
import org.dataone.cn.indexer.parser.ISolrDataField;
import org.dataone.cn.indexer.parser.SubprocessorUtility;
import org.dataone.cn.indexer.solrhttp.HTTPService;
import org.dataone.cn.indexer.solrhttp.SolrDoc;
import org.dataone.cn.indexer.solrhttp.SolrElementField;
import org.dataone.configuration.Settings;
import org.dataone.indexer.performance.PerformanceLogger;

/**
 * The intent of this subprocessor is to fetch annotations about the given 
 * documents as housed in an external store and add them to the solr index.
 * As of November 2014, this is still experimental for the semantic goals of 
 * the project.
 * @author leinfelder
 *
 */
public class AnnotatorSubprocessor implements IDocumentSubprocessor {

    public static final String FIELD_ANNOTATION = "sem_annotation";
    public static final String FIELD_ANNOTATES = "sem_annotates";
    public static final String FIELD_ANNOTATED_BY = "sem_annotated_by";
    public static final String FIELD_COMMENT = "sem_comment";

    private static Log log = LogFactory.getLog(AnnotatorSubprocessor.class.getName());

    private SubprocessorUtility processorUtility;

    private HTTPService httpService = null;

    private String solrQueryUri = Settings.getConfiguration().getString("solr.query.uri");

    private PerformanceLogger perfLog = PerformanceLogger.getInstance();
    
    private List<String> matchDocuments = null;

    private List<String> fieldsToMerge = new ArrayList<String>();

    private List<ISolrDataField> fieldList = new ArrayList<ISolrDataField>();

    public List<String> getMatchDocuments() {
        return matchDocuments;
    }

    public void setMatchDocuments(List<String> matchDocuments) {
        this.matchDocuments = matchDocuments;
    }

    public List<String> getFieldsToMerge() {
        return fieldsToMerge;
    }

    public void setFieldsToMerge(List<String> fieldsToMerge) {
        this.fieldsToMerge = fieldsToMerge;
    }

    public List<ISolrDataField> getFieldList() {
        return fieldList;
    }

    public void setFieldList(List<ISolrDataField> fieldList) {
        this.fieldList = fieldList;
    }
    
    /**
     * Set the http service
     * @param service
     */
    public void setHttpService(HTTPService service) {
        this.httpService = service;
    }

    /**
     * Get the http service
     * @return  the http service
     */
    public HTTPService getHttpService() {
        return httpService;
    }
    
    /**
     * Get the subprocessor utility object
     * @return  the subprocessor utility
     */
    public SubprocessorUtility getProcessorUtility() {
        return processorUtility;
    }

    /**
     * Set the subprocessor utility
     * @param processorUtility  the subprocessor utility object will be set 
     */
    public void setProcessorUtility(SubprocessorUtility processorUtility) {
        this.processorUtility = processorUtility;
    }

    /**
     * Returns true if subprocessor should be run against object
     * 
     * @param formatId the the document to be processed
     * @return true if this processor can parse the formatId
     */
    public boolean canProcess(String formatId) {
        return matchDocuments.contains(formatId);
    }

    @Override
    public Map<String, SolrDoc> processDocument(String annotationId, Map<String, SolrDoc> docs,
            InputStream is) throws Exception {

        // check for annotations, and add them if found
        long parseAnnotationStart = System.currentTimeMillis();
        SolrDoc annotations = parseAnnotation(is);
        perfLog.log("AnnotatorSubprocessor.processDocument() parseAnnotation() ", System.currentTimeMillis() - parseAnnotationStart);
        
        if (annotations != null) {
            String referencedPid = annotations.getIdentifier();
            SolrDoc referencedDoc = docs.get(referencedPid);

            // make sure we have a reference for the document we annotating
            if (referencedDoc == null) {
                try {
                    referencedDoc = httpService.retrieveDocumentFromSolrServer(referencedPid,
                            solrQueryUri);
                } catch (Exception e) {
                    log.warn("Unable to retrieve solr document: " + referencedPid
                                 + ". Exception attempting to communicate with solr server."
                                 + e.getMessage());
                }

                if (referencedDoc == null) {
                     log.warn("DID NOT LOCATE REFERENCED DOC: " + referencedPid);
                    referencedDoc = new SolrDoc();
                    referencedDoc.addField(new SolrElementField(SolrElementField.FIELD_ID, referencedPid));
                }
                docs.put(referencedPid, referencedDoc);
            }

            // make sure we say we annotate the object
            SolrDoc annotationDoc = docs.get(annotationId);
            if (annotationDoc != null) {
                annotationDoc.addField(new SolrElementField(FIELD_ANNOTATES, referencedPid));
            }

            // add the annotations to the referenced document
            Iterator<SolrElementField> annotationIter = annotations.getFieldList().iterator();
            while (annotationIter.hasNext()) {
                SolrElementField annotation = annotationIter.next();
                if (!fieldsToMerge.contains(annotation.getName())) {
                    log.debug("SKIPPING field (not in fieldsToMerge): " + annotation.getName());
                    continue;
                }
                referencedDoc.addField(annotation);
                log.debug("ADDING annotation to " + referencedPid + ": " + annotation.getName()
                        + "=" + annotation.getValue());
            }
        } else {
            log.warn("Annotations were not found when parsing: " + annotationId);
        }
        // return the collection that we have augmented
        return docs;
    }

    /**
     * Parse the annotation for fields
     * @see "http://docs.annotatorjs.org/en/latest/storage.html"
     * @param the stream of the [JSON] annotation
     * @return
     */
    protected SolrDoc parseAnnotation(InputStream is) {

        try {
            String results = null;
            try {
                results = IOUtils.toString(is, "UTF-8");
            } finally {
                IOUtils.closeQuietly(is);
            }
            log.debug("RESULTS: " + results);
            JSONObject annotation = (JSONObject) JSONValue.parse(results);

            SolrDoc annotations = new SolrDoc();

            // use catch-all annotation field for the tags
            String tagKey = FIELD_ANNOTATION;

            // make sure we know which pid we are talking about
            String pidValue = annotation.get("pid").toString();
            if (!annotations.hasFieldWithValue(SolrElementField.FIELD_ID, pidValue)) {
                annotations.addField(new SolrElementField(SolrElementField.FIELD_ID, pidValue));
            }

            // and which object is doing the annotating
            String idValue = annotation.get("id").toString();
            if (!annotations.hasFieldWithValue(FIELD_ANNOTATED_BY, idValue)) {
                annotations.addField(new SolrElementField(FIELD_ANNOTATED_BY, idValue));
            }

            // do not index rejected annotations (clear them out)
            Object reject = annotation.get("reject");
            if (reject != null && Boolean.parseBoolean(reject.toString())) {

                // include empty index values to force removal
                if (!annotations.hasFieldWithValue(tagKey, "")) {
                    annotations.addField(new SolrElementField(tagKey, ""));
                }
                if (!annotations.hasFieldWithValue(FIELD_COMMENT, "")) {
                    annotations.addField(new SolrElementField(FIELD_COMMENT, ""));
                }

            } else {

                // index the (semantic) tags
                // if the annotation told us the target index field, then use it
                Object field = annotation.get("field");
                if (field != null) {
                    tagKey = field.toString();
                }

                Object obj = annotation.get("tags");
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
                Object commentObj = annotation.get("text");
                if (commentObj != null) {
                    String value = commentObj.toString();
                    if (value != null && value.length() > 0) {
                        if (!annotations.hasFieldWithValue(FIELD_COMMENT, value)) {
                            annotations.addField(new SolrElementField(FIELD_COMMENT, value));
                        }
                    }
                }

                // expand the tags, adding expanded concepts to the existing fields
                for (String tag : annotations.getAllFieldValues(tagKey)) {
                    try {
                        // get the expanded tags
                        Map<String, Set<String>> expandedConcepts = OntologyModelService.getInstance().expandConcepts(tag);

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

            }

            // return them
            return annotations;

        } catch (Exception e) {
            log.error("Problem parsing annotation: " + e.getMessage(), e);
        }

        return null;
    }

    /**
     * Merge updates with existing solr documents
     * 
     * @param indexDocument
     * @return
     * @throws IOException
     * @throws EncoderException
     * @throws XPathExpressionException
     */
    public SolrDoc mergeWithIndexedDocument(SolrDoc indexDocument) throws IOException,
            EncoderException, XPathExpressionException {

        return processorUtility.mergeWithIndexedDocument(indexDocument, fieldsToMerge);

    }
}
