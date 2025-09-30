package org.dataone.cn.indexer;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.apache.commons.codec.EncoderException;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.solr.client.solrj.SolrServerException;
import org.dataone.cn.indexer.object.ObjectManagerFactory;
import org.dataone.cn.indexer.object.legacystore.LegacyStoreObjManager;
import org.dataone.cn.indexer.parser.BaseXPathDocumentSubprocessor;
import org.dataone.cn.indexer.parser.IDocumentDeleteSubprocessor;
import org.dataone.cn.indexer.parser.IDocumentSubprocessor;
import org.dataone.cn.indexer.parser.ISolrField;
import org.dataone.cn.indexer.parser.utility.RelationshipMergeUtility;
import org.dataone.cn.indexer.solrhttp.HTTPService;
import org.dataone.cn.indexer.solrhttp.SolrDoc;
import org.dataone.cn.indexer.solrhttp.SolrElementAdd;
import org.dataone.cn.indexer.solrhttp.SolrElementField;
import org.dataone.configuration.Settings;
import org.dataone.exceptions.MarshallingException;
import org.dataone.service.exceptions.InvalidRequest;
import org.dataone.service.exceptions.InvalidToken;
import org.dataone.service.exceptions.NotAuthorized;
import org.dataone.service.exceptions.NotFound;
import org.dataone.service.exceptions.NotImplemented;
import org.dataone.service.exceptions.ServiceFailure;
import org.dataone.service.exceptions.UnsupportedType;
import org.dataone.service.types.v1.Identifier;
import org.xml.sax.SAXException;



/**
 * A class does insert, update and remove indexes to a SOLR server
 * @author tao
 *
 */
public class SolrIndex {
            
    public static final String ID = "id";
    private static final String VERSION_CONFLICT = "version conflict";
    private static final int VERSION_CONFLICT_MAX_ATTEMPTS = Settings.getConfiguration().getInt(
        "index.solr.versionConflict.max.attempts", 25000);
    private static final int VERSION_CONFLICT_WAITING = Settings.getConfiguration().getInt(
        "index.solr.versionConflict.waiting.time", 10); //milliseconds
    private static final List<String> resourceMapFormatIdList = Settings.getConfiguration().getList(
        "index.resourcemap.namespace");
    private static List<IDocumentSubprocessor> subprocessors = null;
    private static List<IDocumentDeleteSubprocessor> deleteSubprocessors = null;
    private static List<String> copyFields = null;//list of solr copy fields
    
    private static HTTPService httpService = null;
    private String solrQueryUri = Settings.getConfiguration().getString("solr.query.uri");
    private String solrIndexUri = Settings.getConfiguration().getString("solr.index.uri");
    private XMLNamespaceConfig xmlNamespaceConfig = null;
    private static BaseXPathDocumentSubprocessor systemMetadataProcessor = null;
    private List<ISolrField> sysmetaSolrFields = null;
    private RelationshipMergeUtility relationshipMergeUtility = null;
    private static Log log = LogFactory.getLog(SolrIndex.class);


    /**
     * Constructor
     * @param xmlNamespaceConfig  a list of pairs of prefix and namespace
     * @param systemMetadataProcessor  the processor to parse system metadata
     * @param httpService  the HTTPService object associated with this class
     */
    public SolrIndex(
        XMLNamespaceConfig xmlNamespaceConfig,
        BaseXPathDocumentSubprocessor systemMetadataProcessor, HTTPService httpService) {
         this.xmlNamespaceConfig = xmlNamespaceConfig;
         this.systemMetadataProcessor = systemMetadataProcessor;
         this.httpService = httpService;
         init();
    }

    private void init() {
        sysmetaSolrFields = systemMetadataProcessor.getFieldList();
        copyFields = httpService.getSolrCopyFields();
        if (copyFields != null) {
            log.info("SolrIndex.init - the size of the copy fields from the solr schema is : "
                    + copyFields.size());
            for(String copyField : copyFields) {
                log.debug("SolrIndex.init - the copy field from the solr schema: " + copyField);
            }
        } else {
            log.warn("SolrIndex.init - the size of the copy fields from the solr schema is 0.");
        }
    }

    /**
     * Get the list of the Subprocessors in this index.
     * @return the list of the Subprocessors.
     */
    public List<IDocumentSubprocessor> getSubprocessors() {
        return subprocessors;
    }

    /**
     * Set the list of Subprocessors.
     * @param subprocessorList  the list will be set.
     */
    public void setSubprocessors(List<IDocumentSubprocessor> subprocessorList) {
        for (IDocumentSubprocessor subprocessor : subprocessorList) {
            if (subprocessor instanceof BaseXPathDocumentSubprocessor) {
                XPathFactory xpathFactory = XPathFactory.newInstance();
                XPath xpath = xpathFactory.newXPath();
                xpath.setNamespaceContext(xmlNamespaceConfig);
                ((BaseXPathDocumentSubprocessor)subprocessor).initExpression(xpath);
            }
        }
        this.subprocessors = subprocessorList;
    }

    public List<IDocumentDeleteSubprocessor> getDeleteSubprocessors() {
        return deleteSubprocessors;
    }

    public void setDeleteSubprocessors(
            List<IDocumentDeleteSubprocessor> deleteSubprocessors) {
        this.deleteSubprocessors = deleteSubprocessors;
    }

    /**
     * Generate the index for the given information
     * @param id  the id which will be indexed
     * @param isSysmetaChangeOnly  if this is a change on the system metadata only
     * @param docId  the docId (file name) of the object. This is only for LegacyObjManager
     * @return a map of solr doc with ids
     * @throws IOException
     * @throws XPathExpressionException
     * @throws EncoderException
     * @throws SolrServerException
     * @throws ClassNotFoundException
     * @throws InvocationTargetException
     * @throws NoSuchMethodException
     * @throws InstantiationException
     * @throws IllegalAccessException
     */
    private Map<String, SolrDoc> process(String id, boolean isSysmetaChangeOnly, String docId)
        throws IOException, XPathExpressionException, EncoderException, SolrServerException,
        ClassNotFoundException, InvocationTargetException, NoSuchMethodException,
        InstantiationException, IllegalAccessException {
        log.debug("SolrIndex.process - trying to generate the solr doc object for the pid "+id);
        long start = System.currentTimeMillis();
        Map<String, SolrDoc> docs = new HashMap<>();
        // Load the System Metadata document
        try (InputStream systemMetadataStream =
                 ObjectManagerFactory.getObjectManager().getSystemMetadataStream(id)){
            docs = systemMetadataProcessor.processDocument(id, docs, systemMetadataStream);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            throw new SolrServerException(e.getMessage());
        }
        long end = System.currentTimeMillis();
        // get the format id for this object
        String formatId = docs.get(id).getFirstFieldValue(SolrElementField.FIELD_OBJECTFORMAT);
        boolean skipOtherProcessor = false;
        log.debug("SolrIndex.process - the object format id for the pid "+id+" is "+formatId);
        if (resourceMapFormatIdList.contains(formatId) && isSysmetaChangeOnly) {
            //we need to make the solr doc exists (means the resource map was processed 
            SolrDoc existingResourceMapSolrDoc = httpService.getSolrDocumentById(solrQueryUri, id);
            if (existingResourceMapSolrDoc != null ) {
                log.info("SolrIndex.process - This is a systemmetadata-change-only event for the "
                        + "resource map " + id + ". So we only use the system metadata subprocessor");
                skipOtherProcessor = true;
            } else {
                log.info("SolrIndex.process - There is no solr doc for the resource map " + id
                        + ". Even though this is a systemmetadata-change-only event, we can NOT "
                        + "just reindex the systemmeta only.");
            }
        }
        log.debug("SolrIndex.process - the value of skipOtherProcessors is " + skipOtherProcessor
                   + " for the id " + id);
        //if the objectPath is null, we should skip the other processes
        if (!skipOtherProcessor) {
            // The default object id is the identifier of the object (the hashstore case)
            String objectID = id;
            if (ObjectManagerFactory.getObjectManager() instanceof LegacyStoreObjManager) {
                // In the LegacyStoreObjManager class, dataone-indexer uses the docid (which
                // always is the file name) to get the object
                objectID = docId;
            }
            log.debug("Start to use subprocessor list to process " + id);
            log.debug("The object id for " + id + " is " + objectID);
            // Determine if subprocessors are available for this ID
            if (subprocessors != null) {
                // for each subprocessor loaded from the spring config
                int index = 0;
                for (IDocumentSubprocessor subprocessor : subprocessors) {
                    log.debug(index + ". The subprocessor is " + subprocessor.getClass().getName());
                    index++;
                    // Does this subprocessor apply?
                    if (subprocessor.canProcess(formatId)) {
                        // if so, then extract the additional information from the
                        // document.
                        try (InputStream dataStream =
                                 ObjectManagerFactory.getObjectManager().getObject(objectID)) {
                            // docObject = the resource map document or science
                            // metadata document.
                            // note that resource map processing touches all objects
                            // referenced by the resource map.
                            start = System.currentTimeMillis();
                            docs = subprocessor.processDocument(id, docs, dataStream);
                            end = System.currentTimeMillis();
                            log.info("SolrIndex.process - the time for calling processDocument "
                                + "for the subprocessor " + subprocessor.getClass().getName()
                                +" for the pid " + id + " is " + (end-start) + "milliseconds.");
                            log.debug("SolrIndex.process - subprocessor "
                                     + subprocessor.getClass().getName()
                                     +" generated solr doc for id "+id);
                        } catch (Exception e) {
                            log.error(e.getMessage(), e);
                            throw new SolrServerException(e.getMessage());
                        }
                    }
                }
           }
        }
       // Merge the new-generated documents with the existing solr documents in the solr server
       for (SolrDoc mergeDoc : docs.values()) {
           if (!mergeDoc.isMerged()) {
               mergeRelationAttributesFromExistDocument(mergeDoc);
           }
       }
       return docs;
    }

    /**
     * Merge the given solr doc with the relationship attributes in existing solr documents.
     * This function replaces the resourcemap merge function
     * This method appears to re-set the data package field data into the
     * document about to be updated in the solr index. Since packaging
     * information is derived from the package document (resource map), this
     * information is not present when processing a document contained in a data
     * package. This method replaces those values from the existing solr index
     * record for the document being processed. -- sroseboo, 1-18-12
     *
     * @param indexDocument
     * @return
     * @throws IOException
     * @throws EncoderException
     * @throws XPathExpressionException
     */

    private SolrDoc mergeRelationAttributesFromExistDocument(SolrDoc indexDocument)
        throws IOException, EncoderException, XPathExpressionException {
        //Retrieve the existing solr document from the solr server for the id. If it doesn't exist,
        //null or empty solr doc will be returned.
        SolrDoc indexedDocument =
            httpService.getSolrDocumentById(solrQueryUri, indexDocument.getIdentifier());
        if (indexedDocument == null || indexedDocument.getFieldList().size() <= 0) {
            return indexDocument;
        } else {
            Vector<SolrElementField> mergeNeededFields = new Vector<>();
            for (SolrElementField field : indexedDocument.getFieldList()) {
                if ((field.getName().equals(SolrElementField.FIELD_ISDOCUMENTEDBY)
                        || field.getName().equals(SolrElementField.FIELD_DOCUMENTS) || field
                        .getName().equals(SolrElementField.FIELD_RESOURCEMAP))
                        && !indexDocument.hasFieldWithValue(field.getName(), field.getValue())) {
                    indexDocument.addField(field);
                } else if (!copyFields.contains(field.getName())
                        && !indexDocument.hasField(field.getName())
                        && !isSystemMetadataField(field.getName())) {
                    // we don't merge the system metadata field since they can be removed.
                    // we don't merge the copyFields as well
                    log.debug("SolrIndex.mergeWithIndexedDocument - put the merge-needed existing solr field "
                              + field.getName() + " with value " + field.getValue()
                              + " from the solr server to a vector. We will merge it later.");
                    //record this name since we can have multiple name/value for the same name.
                    //See https://projects.ecoinformatics.org/ecoinfo/issues/7168
                    mergeNeededFields.add(field);
                } else if (field.getName().equals(SolrElementField.FIELD_VERSION)) {
                    mergeNeededFields.add(field);
                    indexDocument.removeAllFields(field.getName());
                }
            }
            if(mergeNeededFields != null) {
                for(SolrElementField field: mergeNeededFields) {
                    log.debug("SolrIndex.mergeWithIndexedDocument - merge the existing solr field "
                              + field.getName() + " with value " + field.getValue()
                              +" from the solr server to the currently processing document of "
                              + indexDocument.getIdentifier());
                    indexDocument.addField(field);
                }
            }
            indexDocument.setMerged(true);
            return indexDocument;
        }
    }

    /*
     * If the given field name is a system metadata field.
     */
    private boolean isSystemMetadataField(String fieldName) {
        boolean is = false;
        if (fieldName != null && !fieldName.isBlank() && sysmetaSolrFields != null) {
            for(ISolrField field : sysmetaSolrFields) {
                if(field !=  null && field.getName() != null && field.getName().equals(fieldName)) {
                    log.debug("SolrIndex.isSystemMetadataField - the field name " + fieldName
                                + " matches one record of system metadata field list. It is a "
                                + "system metadata field.");
                    is = true;
                    break;
                }
            }
        }
        return is;
    }

    /**
     * Check the parameters of the insert or update methods.
     * @param pid  the pid which will be indexed
     * @throws InvalidRequest
     */
    private void checkParams(Identifier pid) throws InvalidRequest {
        if(pid == null || pid.getValue() == null || pid.getValue().isBlank()) {
            throw new InvalidRequest(
                "0000", "The identifier of the indexed document should not be null or blank.");
        }
    }

    /**
     * Insert the indexes for a document.
     * @param pid  the id of this document
     * @param isSysmetaChangeOnly  if this change is only for systemmetadata
     * @param docId  the docId (file name) of the object. This is only for LegacyObjManager
     * @throws IOException
     * @throws InvalidRequest
     * @throws XPathExpressionException
     * @throws SolrServerException
     * @throws EncoderException
     * @throws ClassNotFoundException
     * @throws InvocationTargetException
     * @throws NoSuchMethodException
     * @throws InstantiationException
     * @throws IllegalAccessException
     */
    private void insert(Identifier pid, boolean isSysmetaChangeOnly, String docId)
        throws IOException, InvalidRequest, XPathExpressionException, SolrServerException,
        EncoderException, ClassNotFoundException, InvocationTargetException, NoSuchMethodException,
        InstantiationException, IllegalAccessException {
        checkParams(pid);
        log.debug("SolrIndex.insert - trying to insert the solrDoc for object "+pid.getValue());
        long start = System.currentTimeMillis();
        Map<String, SolrDoc> docs = process(pid.getValue(), isSysmetaChangeOnly, docId);
        long end = System.currentTimeMillis();
        log.info("SolrIndex.insert - the subprocessor processing time of " + pid.getValue() + " is "
                 + (end-start) + " milliseconds.");
        //transform the Map to the SolrInputDocument which can be used by the solr server
        if(docs != null) {
            start = System.currentTimeMillis();
            Set<String> ids = docs.keySet();
            for(String id : ids) {
                if(id != null) {
                    SolrDoc doc = docs.get(id);
                    insertToIndex(doc);
                    log.debug("SolrIndex.insert - inserted the solr-doc object of pid " + id
                               + ", which relates to object " + pid.getValue()
                               + ", into the solr server.");
                }

            }
            end = System.currentTimeMillis();
            log.info("SolrIndex.insert - finished to insert the solrDoc to the solr server for "
                      + " object " + pid.getValue() + " and it took " + (end-start)
                      + " milliseconds.");
        } else {
            log.debug("SolrIndex.insert - the generated solrDoc is null. So we will not index the "
                      + "object "+pid.getValue());
        }
    }

    /*
     * Insert a SolrDoc to the solr server.
     */
    private void insertToIndex(SolrDoc doc) throws SolrServerException, IOException {
        Vector<SolrDoc> docs = new Vector<>();
        docs.add(doc);
        SolrElementAdd addCommand = new SolrElementAdd(docs);
        httpService.sendUpdate(solrIndexUri, addCommand, "UTF-8");
    }

    /**
     * Update the solr index. This method handles the three scenarios:
     * 1. Remove an existing doc - if the system metadata shows the value of the archive is true,
     *    remove the index for the previous version(s) and generate new index for the doc.
     * 2. Add a new doc - if the system metadata shows the value of the archive is false, generate
     *     the index for the doc.
     * @param pid  the identifier of object which will be indexed
     * @param isSysmetaChangeOnly  the flag indicating if the change is system metadata only
     * @param docId  the docId (file name) of the object. This is only for LegacyObjManager
     * @throws InvalidToken
     * @throws NotAuthorized
     * @throws NotImplemented
     * @throws ServiceFailure
     * @throws NotFound
     * @throws XPathExpressionException
     * @throws UnsupportedType
     * @throws SAXException
     * @throws ParserConfigurationException
     * @throws SolrServerException
     * @throws MarshallingException
     * @throws EncoderException
     * @throws InterruptedException
     * @throws IOException
     * @throws InvalidRequest
     * @throws InstantiationException
     * @throws IllegalAccessException
     * @throws ClassNotFoundException
     * @throws InvocationTargetException
     * @throws NoSuchMethodException
     */
    public void update(Identifier pid, boolean isSysmetaChangeOnly, String docId)
        throws InvalidToken, NotAuthorized, NotImplemented, ServiceFailure, NotFound,
        XPathExpressionException, UnsupportedType, SAXException, ParserConfigurationException,
        SolrServerException, MarshallingException, EncoderException, InterruptedException,
        IOException, InvalidRequest, InstantiationException, IllegalAccessException,
        ClassNotFoundException, InvocationTargetException, NoSuchMethodException {
        log.debug("SolrIndex.update - trying to update(insert or remove) solr index of object "
                    + pid.getValue());
        try {
            insert(pid, isSysmetaChangeOnly, docId);
        } catch (SolrServerException e) {
            if (e.getMessage().contains(VERSION_CONFLICT) && VERSION_CONFLICT_MAX_ATTEMPTS > 0) {
                log.info("SolrIndex.update - Indexer grabbed an older version (version conflict) "
                             + "of a solr doc when it processed the object " + pid.getValue()
                             + ". It will make " + VERSION_CONFLICT_MAX_ATTEMPTS + " attempts to "
                             + "fix the issues");
                for (int i=0; i<VERSION_CONFLICT_MAX_ATTEMPTS; i++) {
                    try {
                        insert(pid, isSysmetaChangeOnly, docId);
                        break;
                    } catch (SolrServerException ee) {
                        if (ee.getMessage().contains(VERSION_CONFLICT)) {
                            log.info("SolrIndex.update - Indexer grabbed an older version "
                                     + "(version conflict) of a solr doc when it processed object "
                                     + pid.getValue() + ". It will wait " + VERSION_CONFLICT_WAITING
                                     + " milliseconds and process it again in oder to get "
                                     + "the new solr doc copy. This is attempt number: " + (i+1)
                                     + " and the max attempt number is "
                                     + VERSION_CONFLICT_MAX_ATTEMPTS);
                            if (i >= (VERSION_CONFLICT_MAX_ATTEMPTS - 1)) {
                                log.error("SolrIndex.update - Indexer grabbed an older version of "
                                         + "a solr doc when it processed object " + pid.getValue()
                                         + ". However, Metacat already tried the max times - "
                                         + VERSION_CONFLICT_MAX_ATTEMPTS
                                         + " and still can't fix the issue.");
                                throw ee;
                            }
                        } else {
                            throw ee;
                        }
                    }
                    Thread.sleep(VERSION_CONFLICT_WAITING);
                }
            } else {
                throw e;
            }
        }
        log.info("SolrIndex.update - successfully inserted the solr index of the object "
                     + pid.getValue());
    }

    /*
     * Is the pid a resource map
     */
    private boolean isDataPackage(String formatId) {
        boolean isDataPackage = false;
        if(formatId != null) {
            isDataPackage = resourceMapFormatIdList.contains(formatId);
        }
        return isDataPackage;
    }

    private boolean isPartOfDataPackage(String pid) throws XPathExpressionException,
                                  IOException, EncoderException {
        SolrDoc dataPackageIndexDoc = httpService.getSolrDocumentById(solrQueryUri, pid);
        if (dataPackageIndexDoc != null) {
            String resourceMapId = dataPackageIndexDoc
                    .getFirstFieldValue(SolrElementField.FIELD_RESOURCEMAP);
            return StringUtils.isNotEmpty(resourceMapId);
        } else {
            return false;
        }
    }
    
    /**
     * Remove the solr index associated with specified pid
     * @param pid  the pid whose solr index will be removed
     * @throws EncoderException 
     * @throws IOException 
     * @throws XPathExpressionException 
     * @throws SAXException 
     * @throws ParserConfigurationException 
     * @throws SolrServerException 
     * @throws UnsupportedType 
     * @throws NotFound 
     * @throws NotImplemented 
     * @throws ServiceFailure 
     */
    public void remove(Identifier pid)
        throws XPathExpressionException, IOException, EncoderException, ServiceFailure,
        NotImplemented, NotFound, UnsupportedType, SolrServerException,
        ParserConfigurationException, SAXException {
        if(pid != null ) {
            log.debug(
                "SorIndex.remove - start to remove the solr index for the pid " + pid.getValue());
            SolrDoc indexDoc = httpService.getSolrDocumentById(solrQueryUri, pid.getValue());
            if (indexDoc != null) {
                log.debug("SorIndex.remove - in the branch which the solr doc was found for "
                           + pid.getValue());
                String formatId = indexDoc.getFirstFieldValue("formatId");
                log.debug("SorIndex.remove - the format id for the object " + pid.getValue()
                           + " is " + formatId);
                remove(pid.getValue(), formatId);
                log.info("SorIndex.remove - successfully removed the solr index for the pid "
                          + pid.getValue());
            }
        }
    }
    /**
     * Remove the indexed associated with specified pid.
     * @param pid  the pid which the indexes are associated with
     * @throws EncoderException
     * @throws IOException
     * @throws SolrServerException
     * @throws XPathExpressionException
     */
    private void remove(String pid, String formatId) throws XPathExpressionException,
        SolrServerException, IOException, EncoderException {
        if (isDataPackage(formatId)) {
            removeDataPackage(pid);
        } else if (isPartOfDataPackage(pid)) {
            removeFromDataPackage(pid);
        } else {
            deleteDocFromIndex(pid);
        }
    }

    /*
     * Remove the resource map from the solr index. It doesn't only remove the index for itself and
     *  also remove the relationship for the related metadata and data objects.
     */
    private void removeDataPackage(String pid) throws IOException, XPathExpressionException,
                                    SolrServerException, EncoderException  {
        deleteDocFromIndex(pid);
        for (int i=0; i<VERSION_CONFLICT_MAX_ATTEMPTS; i++) {
            try {
                List<SolrDoc> docsToUpdate = getUpdatedSolrDocsByRemovingResourceMap(pid);
                if (docsToUpdate != null && !docsToUpdate.isEmpty()) {
                    for(SolrDoc doc : docsToUpdate) {
                        insertToIndex(doc);
                    }
                }
                break;
            } catch (SolrServerException e) {
                if (e.getMessage().contains(VERSION_CONFLICT) && VERSION_CONFLICT_MAX_ATTEMPTS > 0) {
                    log.info("SolrIndex.removeDataPackage - Indexer grabbed an older version "
                             + "(version conflict) of the solr doc for object"
                             + ". It will try " + (VERSION_CONFLICT_MAX_ATTEMPTS - i )
                             + " to fix the issues");
                } else {
                    throw e;
                }
            }
        }
    }

    /*
     * Get the list of the solr doc which need to be updated because the removal of the resource map
     */
    private List<SolrDoc> getUpdatedSolrDocsByRemovingResourceMap(String resourceMapId)
            throws SolrServerException, IOException, XPathExpressionException, EncoderException {
        List<SolrDoc> updatedSolrDocs = null;
        if (resourceMapId != null && !resourceMapId.isBlank()) {
            List<SolrDoc> docsContainResourceMap = httpService
                                        .getDocumentsByResourceMap(solrQueryUri, resourceMapId);
            updatedSolrDocs = removeResourceMapRelationship(docsContainResourceMap,
                    resourceMapId);
        }
        return updatedSolrDocs;
    }

    /*
     * Get the list of the solr doc which need to be updated because the removal of the resource map
     */
    private List<SolrDoc> removeResourceMapRelationship(List<SolrDoc> docsContainResourceMap,
            String resourceMapId) {
        List<SolrDoc> totalUpdatedSolrDocs = new ArrayList<>();
        if (docsContainResourceMap != null && !docsContainResourceMap.isEmpty()) {
            for (SolrDoc doc : docsContainResourceMap) {
                List<SolrDoc> updatedSolrDocs = new ArrayList<>();
                List<String> resourceMapIdStrs = doc
                        .getAllFieldValues(SolrElementField.FIELD_RESOURCEMAP);
                List<String> dataIdStrs = doc
                        .getAllFieldValues(SolrElementField.FIELD_DOCUMENTS);
                List<String> metadataIdStrs = doc
                        .getAllFieldValues(SolrElementField.FIELD_ISDOCUMENTEDBY);
                if ((dataIdStrs == null || dataIdStrs.isEmpty())
                        && (metadataIdStrs == null || metadataIdStrs.isEmpty())) {
                    // only has resourceMap field, doesn't have either documentBy or documents fields.
                    // so we only remove the resource map field.
                    doc.removeFieldsWithValue(SolrElementField.FIELD_RESOURCEMAP, resourceMapId);
                    updatedSolrDocs.add(doc);
                } else if ((dataIdStrs != null && !dataIdStrs.isEmpty())
                        && (metadataIdStrs == null || metadataIdStrs.isEmpty())) {
                    //The solr doc is for a metadata object since the solr doc documents data files
                    updatedSolrDocs = removeAggregatedItems(resourceMapId, doc, resourceMapIdStrs,
                            dataIdStrs, SolrElementField.FIELD_DOCUMENTS);
                } else if ((dataIdStrs == null || dataIdStrs.isEmpty())
                        && (metadataIdStrs != null && !metadataIdStrs.isEmpty())) {
                    //The solr doc is for a data object since it documentedBy elements.
                    updatedSolrDocs = removeAggregatedItems(resourceMapId, doc, resourceMapIdStrs,
                            metadataIdStrs, SolrElementField.FIELD_ISDOCUMENTEDBY);
                } else if ((dataIdStrs != null && !dataIdStrs.isEmpty())
                        && (metadataIdStrs != null && !metadataIdStrs.isEmpty())){
                    // both metadata and data for one object
                    List<SolrDoc> solrDocsRemovedDocuments =
                        removeAggregatedItems(
                            resourceMapId, doc, resourceMapIdStrs, dataIdStrs,
                            SolrElementField.FIELD_DOCUMENTS);
                    List<SolrDoc> solrDocsRemovedDocumentBy =
                        removeAggregatedItems(
                            resourceMapId, doc, resourceMapIdStrs, metadataIdStrs,
                            SolrElementField.FIELD_ISDOCUMENTEDBY);
                    updatedSolrDocs =
                        mergeUpdatedSolrDocs(solrDocsRemovedDocumentBy, solrDocsRemovedDocuments);
                }
                //move them to the final result
                if(updatedSolrDocs != null) {
                    for(SolrDoc updatedDoc: updatedSolrDocs) {
                        totalUpdatedSolrDocs.add(updatedDoc);
                    }
                }
                
            }

        }
        return totalUpdatedSolrDocs;
    }

    /*
     * Process the list of ids of the documentBy/documents in a solr doc.
     */
    private List<SolrDoc> removeAggregatedItems(
        String targetResourceMapId, SolrDoc doc, List<String> resourceMapIdsInDoc,
        List<String> aggregatedItemsInDoc, String fieldNameRemoved) {
        List<SolrDoc> updatedSolrDocs = new ArrayList<>();
        if (doc != null && resourceMapIdsInDoc != null && aggregatedItemsInDoc != null
                && fieldNameRemoved != null) {
            if (resourceMapIdsInDoc.size() == 1) {
                //only has one resource map. remove the resource map. also remove the documentBy
                doc.removeFieldsWithValue(SolrElementField.FIELD_RESOURCEMAP, targetResourceMapId);
                doc.removeAllFields(fieldNameRemoved);
                updatedSolrDocs.add(doc);
            } else if (resourceMapIdsInDoc.size() > 1) {
                //we have multiple resource maps. We should match them.                     
                Map<String, String> ids =
                    matchResourceMapsAndItems(
                        doc.getIdentifier(), targetResourceMapId, resourceMapIdsInDoc,
                        aggregatedItemsInDoc, fieldNameRemoved);
                if (ids != null) {
                    for (String id : ids.keySet()) {
                        doc.removeFieldsWithValue(fieldNameRemoved, id);
                    }
                }
                doc.removeFieldsWithValue(SolrElementField.FIELD_RESOURCEMAP,
                        targetResourceMapId);
                updatedSolrDocs.add(doc);
            }
        }
        return updatedSolrDocs;
    }

    /*
     * Return a map of mapping aggregation id map the target resourceMapId.
     * This will look the aggregation information in another side - If the targetId
     * is a metadata object, we will look the data objects which it describes; If 
     * the targetId is a data object, we will look the metadata object which documents it.
     */
    private Map<String, String> matchResourceMapsAndItems(
        String targetId, String targetResourceMapId, List<String> originalResourceMaps,
        List<String> aggregatedItems, String fieldName) {
        Map<String, String> map = new HashMap<>();
        if (targetId != null && targetResourceMapId != null && aggregatedItems != null
                && fieldName != null) {
            String newFieldName = null;
            if (fieldName.equals(SolrElementField.FIELD_ISDOCUMENTEDBY)) {
                newFieldName = SolrElementField.FIELD_DOCUMENTS;
            } else if (fieldName.equals(SolrElementField.FIELD_DOCUMENTS)) {
                newFieldName = SolrElementField.FIELD_ISDOCUMENTEDBY;
            }
            if (newFieldName != null) {
                for (String item : aggregatedItems) {
                    SolrDoc doc = null;
                    try {
                        doc = httpService.getSolrDocumentById(solrQueryUri, item);
                        List<String> fieldValues = doc.getAllFieldValues(newFieldName);
                        List<String> resourceMapIds = doc
                                .getAllFieldValues(SolrElementField.FIELD_RESOURCEMAP);
                        if ((fieldValues != null && fieldValues.contains(targetId))
                                && (resourceMapIds != null && resourceMapIds
                                        .contains(targetResourceMapId))) {
                            // okay, we found the target aggregation item id and the resource map id
                            // in this solr doc. However, we need check if another resource map with
                            // different id but specify the same relationship. If we have the id(s),
                            // we should not remove the documents (or documentBy) element since
                            // we need to preserve the relationship for the remain resource map.
                            boolean hasDuplicateIds = false;
                            if(originalResourceMaps != null) {
                               for(String id :resourceMapIds) {
                                   if (originalResourceMaps.contains(id) && !id.equals(
                                       targetResourceMapId)) {
                                        hasDuplicateIds = true;
                                        break;
                                    }
                                }
                            }
                            if(!hasDuplicateIds) {
                                map.put(item, targetResourceMapId);
                            }

                        }
                    } catch (Exception e) {
                        log.warn("SolrIndex.matchResourceMapsAndItems - can't get the solrdoc for the id "
                                + item + " since " + e.getMessage());
                    }
                }
            }
        }
        return map;
    }


    /*
     * Merge two list of updated solr docs. removedDocumentBy has the correct information about
     * documentBy element. RemovedDocuments has the correct information about the documents element.
     * So we go through the two list and found the two docs having the same identifier.
     * Get the list of the documents value from the one in the removedDocuments (1).
     * Remove all values of documents from the one in the removedDocumentBy. 
     * Then copy the list of documents value from (1) to the one in the removedDocumentBy.
     */
    private List<SolrDoc> mergeUpdatedSolrDocs(
        List<SolrDoc> removedDocumentBy, List<SolrDoc> removedDocuments) {
        List<SolrDoc> mergedDocuments = new ArrayList<>();
        if(removedDocumentBy == null || removedDocumentBy.isEmpty()) {
            mergedDocuments = removedDocuments;
        } else if (removedDocuments == null || removedDocuments.isEmpty()) {
            mergedDocuments = removedDocumentBy;
        } else {
            int sizeOfDocBy = removedDocumentBy.size();
            int sizeOfDocs = removedDocuments.size();
            for(int i=sizeOfDocBy-1; i>= 0; i--) {
                SolrDoc docInRemovedDocBy = removedDocumentBy.get(i);
                for(int j= sizeOfDocs-1; j>=0; j--) {
                    SolrDoc docInRemovedDocs = removedDocuments.get(j);
                    if(docInRemovedDocBy.getIdentifier().equals(docInRemovedDocs.getIdentifier())) {
                        //find the same doc in both list. let's merge them.
                        //first get all the documents element from the docWithDocs
                        //(it has the correct information about the documents element)
                        List<String> idsInDocuments = docInRemovedDocs
                                            .getAllFieldValues(SolrElementField.FIELD_DOCUMENTS);
                        //clear out any documents element in docInRemovedDocBy
                        docInRemovedDocBy.removeAllFields(SolrElementField.FIELD_DOCUMENTS);
                        //add the Documents element from the docInRemovedDocs if it has any.
                        // The docInRemovedDocs has the correct information about the documentBy.
                        // Now it copied the correct information of the documents element.
                        // So docInRemovedDocs has both correct information about the documentBy
                        //and documents elements.
                        if(idsInDocuments != null) {
                            for(String id : idsInDocuments) {
                                if(id != null && !id.isBlank()) {
                                    docInRemovedDocBy.addField(
                                        new SolrElementField(SolrElementField.FIELD_DOCUMENTS, id));
                                }

                            }
                        }
                        //intersect the resource map ids.
                        List<String> resourceMapIdsInWithDocs = docInRemovedDocs
                                            .getAllFieldValues(SolrElementField.FIELD_RESOURCEMAP);
                        List<String> resourceMapIdsInWithDocBy = docInRemovedDocBy
                                            .getAllFieldValues(SolrElementField.FIELD_RESOURCEMAP);
                        docInRemovedDocBy.removeAllFields(SolrElementField.FIELD_RESOURCEMAP);
                        Collection resourceMapIds = CollectionUtils.union(resourceMapIdsInWithDocs,
                                                        resourceMapIdsInWithDocBy);
                        if(resourceMapIds != null) {
                            for(Object idObj : resourceMapIds) {
                                String id = (String)idObj;
                                docInRemovedDocBy.addField(new SolrElementField(
                                                        SolrElementField.FIELD_RESOURCEMAP, id));
                            }
                        }
                        //we don't need to do anything about the documentBy elements since the
                        //docInRemovedDocBy has the correct information.
                        mergedDocuments.add(docInRemovedDocBy);
                        //delete the two documents from the list
                        removedDocumentBy.remove(i);
                        removedDocuments.remove(j);
                        break;
                    }
                    
                }
            }
            // when we get there, if the two lists are empty, this will be a perfect merge.
            // However, if something are left. we just put them in.
            for(SolrDoc doc: removedDocumentBy) {
                mergedDocuments.add(doc);
            }
            for(SolrDoc doc: removedDocuments) {
                mergedDocuments.add(doc);
            }
        }
        return mergedDocuments;
    }

    /*
     * Remove a pid which is part of resource map.
     */
    private void removeFromDataPackage(String pid) throws XPathExpressionException, IOException,
                                                          EncoderException, SolrServerException {
        SolrDoc indexedDoc = httpService.getSolrDocumentById(solrQueryUri, pid);
        deleteDocFromIndex(pid);
        List<String> documents = indexedDoc.getAllFieldValues(SolrElementField.FIELD_DOCUMENTS);
        if (documents != null  && !documents.isEmpty()) {
            for (String documentsValue : documents) {
                for (int i=0; i<VERSION_CONFLICT_MAX_ATTEMPTS; i++) {
                    try {
                        SolrDoc solrDoc = httpService.getSolrDocumentById(solrQueryUri, documentsValue);
                        if (solrDoc != null) {
                            solrDoc.removeFieldsWithValue(SolrElementField.FIELD_ISDOCUMENTEDBY, pid);
                            insertToIndex(solrDoc);
                        }
                        break;
                    } catch (SolrServerException e) {
                        if (e.getMessage().contains(VERSION_CONFLICT) && VERSION_CONFLICT_MAX_ATTEMPTS > 0) {
                            log.info("SolrIndex.removeFromDataPackage - Indexer grabbed an older "
                                    + "version (version conflict) of the solr doc for object "
                                    + documentsValue + ". It will try "
                                    + (VERSION_CONFLICT_MAX_ATTEMPTS - i )+ " to fix the issues");
                        } else {
                            throw e;
                        }
                    }
                }
            }
        }

        List<String> documentedBy = indexedDoc.getAllFieldValues(SolrElementField.FIELD_ISDOCUMENTEDBY);
        if (documentedBy != null && !documentedBy.isEmpty()) {
            for (String documentedByValue : documentedBy) {
                for (int i=0; i<VERSION_CONFLICT_MAX_ATTEMPTS; i++) {
                    try {
                        SolrDoc solrDoc = httpService.getSolrDocumentById(solrQueryUri, documentedByValue);
                        if (solrDoc != null) {
                            solrDoc.removeFieldsWithValue(SolrElementField.FIELD_DOCUMENTS, pid);
                            insertToIndex(solrDoc);
                        }
                        break;
                    } catch (SolrServerException e) {
                        if (e.getMessage().contains(VERSION_CONFLICT) && VERSION_CONFLICT_MAX_ATTEMPTS > 0) {
                            log.info("SolrIndex.removeFromDataPackage - Indexer grabbed an older "
                                      + "version (version conflict) of the solr doc for object "
                                      + documentedByValue + ". It will try "
                                      + (VERSION_CONFLICT_MAX_ATTEMPTS - i )+ " to fix the issues");
                        } else {
                            throw e;
                        }
                    }
                }
            }
        }
    }


    private void deleteDocFromIndex(String pid) throws IOException {
        if (pid != null && !pid.isBlank()) {
            try {
                httpService.sendSolrDelete(pid, solrIndexUri);
            } catch (IOException e) {
                throw e;
            }

        }
    }

    /**
     * Set the http service
     * @param service  the http service will be used
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
     * Set the merge utility
     * @param relationshipMergeUtility  the merge utility will be set
     */
    public void setRelationshipMergeUtility(RelationshipMergeUtility relationshipMergeUtility) {
        this.relationshipMergeUtility = relationshipMergeUtility;
    }

    /**
     * Get the merge utility
     * @return the merge utility object
     */
    public RelationshipMergeUtility getRelationshipMergeUtility() {
        return this.relationshipMergeUtility;
    }

}
