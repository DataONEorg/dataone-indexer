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
    
    private static int waitingTime = Settings.getConfiguration().getInt("index.resourcemap.waitingComponent.time", 600);
    private static int maxAttempts = Settings.getConfiguration().getInt("index.resourcemap.waitingComponent.max.attempts", 15);


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
                    throws XPathExpressionException, IOException, SAXException,
        ParserConfigurationException, EncoderException, SolrServerException,
        NotImplemented, NotFound, UnsupportedType, OREParserException, InterruptedException {
        ResourceMap resourceMap = ResourceMapFactory.buildResourceMap(resourcMap);
        //this list includes the resourceMap id itself.
        List<String> documentIds = resourceMap.getAllDocumentIDs();
        List<SolrDoc> updateDocuments = getSolrDocs(resourceMap.getIdentifier(), documentIds);
        List<SolrDoc> mergedDocuments = resourceMap.mergeIndexedDocuments(updateDocuments);
        /*if(mergedDocuments != null) {
            for(SolrDoc doc : mergedDocuments) {
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                doc.serialize(out, "UTF-8");
                String result = new String(out.toByteArray(), "UTF-8");
                System.out.println("after updated document===========================");
                System.out.println(result);
            }
        }*/
        mergedDocuments.add(indexDocument);
        return mergedDocuments;
    }

    private List<SolrDoc> getSolrDocs(String resourceMapId, List<String> ids)
        throws SolrServerException, IOException, ParserConfigurationException, SAXException,
        XPathExpressionException, NotImplemented, NotFound, UnsupportedType, InterruptedException,
        EncoderException {
        List<SolrDoc> list = new ArrayList<SolrDoc>();
        if(ids != null) {
            for(String id : ids) {
                SolrDoc doc = httpService.getSolrDocumentById(solrQueryUri, id);
                if(doc != null) {
                    list.add(doc);
                } else if ( !id.equals(resourceMapId)) {
                    for (int i=0; i<maxAttempts; i++) {
                        Thread.sleep(waitingTime);
                        doc = httpService.getSolrDocumentById(solrQueryUri, id);
                        logger.info("ResourceMapSubprocessor.getSolrDocs - the " + (i + 1)
                                        + " time to wait " + waitingTime
                                        + " to get the solr doc for " + id);
                        if (doc != null) {
                            break;
                        }
                    }
                    if (doc != null) {
                        list.add(doc);
                    } else {
                        throw new SolrServerException(
                            "Solr index doesn't have the information " + "about the id " + id
                                + " which is a " + "component in the resource map " + resourceMapId
                                + ". Metacat-Index can't process the resource map prior to its "
                                + "components.");
                    }
                }
            }
        }
        return list;
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
