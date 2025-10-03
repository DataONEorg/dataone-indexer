package org.dataone.cn.indexer.parser;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathExpressionException;

import org.apache.commons.codec.EncoderException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.solr.client.solrj.SolrServerException;
import org.dataone.cn.indexer.resourcemap.ResourceMap;
import org.dataone.cn.indexer.resourcemap.ResourceMapFactory;
import org.dataone.cn.indexer.solrhttp.DummySolrDoc;
import org.dataone.cn.indexer.solrhttp.HTTPService;
import org.dataone.cn.indexer.solrhttp.SolrDoc;
import org.dataone.cn.indexer.solrhttp.SolrElementField;
import org.dataone.configuration.Settings;
import org.dataone.service.exceptions.NotFound;
import org.dataone.service.exceptions.NotImplemented;
import org.dataone.service.exceptions.ServiceFailure;
import org.dataone.service.exceptions.UnsupportedType;
import org.dataone.service.types.v1.Identifier;
import org.dspace.foresite.OREParserException;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;


/**
 * Resource Map Document processor.  Operates on ORE/RDF objects.  Maps 
 * 'documents', 'documentedBy', and 'aggregates' relationships.
 * 
 * Uses org.dataone.cn.indexer.resourcemap.ResourceMap to update individual
 * SolrDoc objects with values for 'documents', 'documentedBy', and 'resourceMap'
 * (aggregates) fields.
 * 
 * Updates entries for related documents in index. For document relational
 * information refer to
 * http://purl.dataone.org/architecture/design/SearchMetadata.html#id4
 * 
 * Date: 9/26/11
 * Time: 3:51 PM
 */
public class ResourceMapSubprocessor implements IDocumentSubprocessor {

    private static Log logger = LogFactory.getLog(ResourceMapSubprocessor.class.getName());

    private HTTPService httpService = null;

    private String solrQueryUri = Settings.getConfiguration().getString("solr.query.uri");

    private SubprocessorUtility processorUtility;



    private List<String> matchDocuments = null;
    private List<String> fieldsToMerge = new ArrayList<String>();

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


    @Override
    public Map<String, SolrDoc> processDocument(String identifier, Map<String, SolrDoc> docs,
    InputStream is) throws IOException, EncoderException, SAXException,
        XPathExpressionException, ParserConfigurationException, SolrServerException, 
        NotImplemented, NotFound, UnsupportedType, OREParserException, ServiceFailure,
        InterruptedException{
        Identifier id = new Identifier();
        id.setValue(identifier);
        //Get the path to the resource map file
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document resourceMap = builder.parse(is);
        // Process the resource map to get the result - a list of solr docs containing
        // relationship. However, those solr docs haven't been merged (got information) from the
        // existing solr docs on the solr server.
        List<SolrDoc> processedDocs = processResourceMap(resourceMap, docs);
        Map<String, SolrDoc> processedDocsMap = new HashMap<>();
        for (SolrDoc processedDoc : processedDocs) {
            processedDocsMap.put(processedDoc.getIdentifier(), processedDoc);
        }
        return processedDocsMap;
    }

    /**
     * Process the resource map object - parse the resource map object and add the relationship
     * fields to the solr docs.
     * @param resourceMapDOMDoc  the representation of the resourcemap itself in the DOM format
     * @param docs  the map of the solr docs inherited from the previous subprocessors
     * @return a list of solr docs containing the relationship fields
     * @throws OREParserException
     */
    private List<SolrDoc> processResourceMap(Document resourceMapDOMDoc,
                                             Map<String, SolrDoc> docs) throws OREParserException {
        ResourceMap resourceMap = ResourceMapFactory.buildResourceMap(resourceMapDOMDoc);
        //this list includes the resourceMap id itself.
        List<String> documentIds = resourceMap.getAllDocumentIDs();
        // Get a list of solr docs for the above list of ids (but, it excludes the resource map
        // id itself. The solr document in the list either inherits from the solr doc from the
        // previous subprocessor or a new empty dummy solr doc. They are the baseline for
        // resource map to process.
        List<SolrDoc> updateDocuments =
            getSolrDocs(resourceMap.getIdentifier(), documentIds, docs);
        // The resource map object merges the relationship fields into the baseline solr docs
        List<SolrDoc> mergedDocuments = resourceMap.mergeIndexedDocuments(updateDocuments);
        // Add the resource map solr doc back to the map
        mergedDocuments.add(docs.get(resourceMap.getIdentifier()));
        return mergedDocuments;
    }

    /**
     * Create a list of the solr documents for the given id list in a resource map (it doesn't
     * have the resource map itself). First, it looks up the solr doc from the map (docs) to see
     * if it exists. If it exists, just use that one; otherwise generate a dummy solr doc. We
     * don't merge the document from the solr server here.
     * @param resourceMapId  the id of the resource map id
     * @param ids  the list of ids in the resource map
     * @param docs  the map of the solr docs inherited from the previous subprocessors
     * @return a list of the solr documents for the given id list in a resource map (it doesn't
     * have the resource map itself)
     */
    private List<SolrDoc> getSolrDocs(String resourceMapId, List<String> ids,
                                      Map<String, SolrDoc> docs) {
        List<SolrDoc> list = new ArrayList<>();
        if(ids != null) {
            for(String id : ids) {
                SolrDoc doc = docs.get(id);
                if(doc != null) {
                    list.add(doc);
                } else if ( !id.equals(resourceMapId)) {
                    // generate a dummy solr doc which only has the id and put it into the list.
                    doc = new DummySolrDoc(id, docs.get(resourceMapId));
                    list.add(doc);
                }
            }
        }
        return list;
    }

    /**
     * Copy all values of the given field name in the source solr doc to the destination solr doc
     * @param fieldName  the given field name
     * @param source  the source solr doc
     * @param dest  the destination solr doc
     */
    protected static void copyFieldAllValue(String fieldName, SolrDoc source, SolrDoc dest) {
        if (fieldName != null && !fieldName.isBlank()) {
            List<String> values = source.getAllFieldValues(fieldName);
            logger.debug("The all values of the field of " + fieldName + " in the source solr doc"
                             + " is " + values);
            if (values != null && !values.isEmpty()) {
                for (String value : values) {
                    dest.addField(new SolrElementField(fieldName, value));
                }
            }
        }
    }

    public List<String> getMatchDocuments() {
        return matchDocuments;
    }

    public void setMatchDocuments(List<String> matchDocuments) {
        this.matchDocuments = matchDocuments;
    }

    public boolean canProcess(String formatId) {
        return matchDocuments.contains(formatId);
    }

    public List<String> getFieldsToMerge() {
        return fieldsToMerge;
    }

    public void setFieldsToMerge(List<String> fieldsToMerge) {
        this.fieldsToMerge = fieldsToMerge;
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
}
