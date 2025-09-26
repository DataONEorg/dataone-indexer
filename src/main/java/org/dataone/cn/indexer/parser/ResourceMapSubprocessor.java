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
        SolrDoc resourceMapDoc = docs.get(identifier);
        Identifier id = new Identifier();
        id.setValue(identifier);
        
        //Get the path to the resource map file
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document resourceMap = builder.parse(is);
        List<SolrDoc> processedDocs = processResourceMap(resourceMapDoc, resourceMap);
        Map<String, SolrDoc> processedDocsMap = new HashMap<String, SolrDoc>();
        for (SolrDoc processedDoc : processedDocs) {
            processedDocsMap.put(processedDoc.getIdentifier(), processedDoc);
        }
        return processedDocsMap;
    }

    private List<SolrDoc> processResourceMap(SolrDoc indexDocument, Document resourcMap)
        throws XPathExpressionException, IOException, EncoderException, OREParserException {
        ResourceMap resourceMap = ResourceMapFactory.buildResourceMap(resourcMap);
        //this list includes the resourceMap id itself.
        List<String> documentIds = resourceMap.getAllDocumentIDs();
        List<SolrDoc> updateDocuments =
            getSolrDocs(resourceMap.getIdentifier(), documentIds, indexDocument);
        List<SolrDoc> mergedDocuments = resourceMap.mergeIndexedDocuments(updateDocuments);
        mergedDocuments.add(indexDocument);
        return mergedDocuments;
    }

    private List<SolrDoc> getSolrDocs(String resourceMapId, List<String> ids,
                                      SolrDoc resourceMapSolrDoc)
        throws IOException, XPathExpressionException, EncoderException {
        List<SolrDoc> list = new ArrayList<SolrDoc>();
        if(ids != null) {
            for(String id : ids) {
                SolrDoc doc = httpService.getSolrDocumentById(solrQueryUri, id);
                if(doc != null) {
                    list.add(doc);
                } else if ( !id.equals(resourceMapId)) {
                    // generate a dummy solr doc which only has the id and put it into the list.
                    doc = generateDummySolrDoc(id, resourceMapSolrDoc);
                    list.add(doc);
                }
            }
        }
        return list;
    }

    /**
     * Generate a dummy solr doc for the given id. The _version_ of the dummy doc is -1.
     * It has the same access rules as the docHoldsPermission solr doc. It also has an "abstract"
     * field with the value of "A placeholding document".
     * @param id  the identifier of the dummy solr doc
     * @param docHoldsPermission  it holds the access rules the dummy solr doc will have
     * @return the dommy solr doc
     */
    public static SolrDoc generateDummySolrDoc(String id, SolrDoc docHoldsPermission) {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("The id used to generate a dummy solr doc can't be"
                                                   + " null or blank");
        }
        SolrDoc doc = new SolrDoc();
        SolrElementField idField = new SolrElementField(SolrElementField.FIELD_ID, id);
        doc.addField(idField);
        // Set the version to -1. This makes sure that the solr doc only can be created
        // if the solr server doesn't have the id. Otherwise, it throws a version
        // conflict exception.
        doc.addField(new SolrElementField(SolrElementField.FIELD_VERSION,
                                          SolrElementField.NEGATIVE_ONE));
        doc.addField(new SolrElementField("abstract", "A placeholding document"));
        if (docHoldsPermission != null) {
            // Copy the access rules from the resource map solr doc to the new solr doc
            copyFieldAllValue(SolrElementField.FIELD_READPERMISSION,
                              docHoldsPermission, doc);
            copyFieldAllValue(SolrElementField.FIELD_WRITEPERMISSION,
                              docHoldsPermission, doc);
            copyFieldAllValue(SolrElementField.FIELD_CHANGEPERMISSION,
                              docHoldsPermission, doc);
            copyFieldAllValue(SolrElementField.FIELD_RIGHTSHOLDER,
                              docHoldsPermission, doc);
        }
        return doc;
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
