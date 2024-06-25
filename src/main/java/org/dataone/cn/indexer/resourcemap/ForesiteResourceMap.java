package org.dataone.cn.indexer.resourcemap;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.net.URISyntaxException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.io.input.ReaderInputStream;
import org.apache.log4j.Logger;
import org.dataone.cn.indexer.object.ObjectManager;
import org.dataone.cn.indexer.parser.utility.SeriesIdResolver;
import org.dataone.cn.indexer.solrhttp.SolrDoc;
import org.dataone.cn.indexer.solrhttp.SolrElementField;
import org.dataone.exceptions.MarshallingException;
import org.dataone.ore.ResourceMapFactory;
import org.dataone.service.exceptions.InvalidToken;
import org.dataone.service.exceptions.NotAuthorized;
import org.dataone.service.exceptions.NotFound;
import org.dataone.service.exceptions.NotImplemented;
import org.dataone.service.exceptions.ServiceFailure;
import org.dataone.service.types.v1.Identifier;
import org.dataone.service.types.v2.SystemMetadata;
import org.dspace.foresite.OREException;
import org.dspace.foresite.OREParserException;
import org.w3c.dom.Document;
import org.w3c.dom.bootstrap.DOMImplementationRegistry;
import org.w3c.dom.ls.DOMImplementationLS;
import org.w3c.dom.ls.LSOutput;
import org.w3c.dom.ls.LSSerializer;

public class ForesiteResourceMap implements ResourceMap {
    /* Class contants */
    private static final String RESOURCE_MAP_FORMAT = "http://www.openarchives.org/ore/terms";
    private static Logger logger = Logger.getLogger(ForesiteResourceMap.class.getName());

    /* Instance variables */
    private String identifier = null;
    private HashMap<String, ForesiteResourceEntry> resourceMap = null;

    private IndexVisibilityDelegate indexVisibilityDelegate = new IndexVisibilityDelegateImpl();

    public ForesiteResourceMap(String fileObjectPath) throws OREParserException {
        FileInputStream fileInputStream = null;
        try {
            fileInputStream = new FileInputStream(fileObjectPath);
            _init(fileInputStream);
        } catch (Exception e) {
            throw new OREParserException(e);
        } finally {
            try {
                if (fileInputStream != null) {
                    fileInputStream.close();
                }
            } catch (IOException e) {
                logger.error("error closing file input stream", e);
            }
        }
    }

    public ForesiteResourceMap(Document doc) throws OREParserException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        InputStream is = null;
        try {
            DOMImplementationLS domImpl = null;
            try {
                domImpl = (DOMImplementationLS) (DOMImplementationRegistry.newInstance()
                        .getDOMImplementation("LS"));
                LSOutput lsOutput = domImpl.createLSOutput();
                lsOutput.setEncoding("UTF-8");
                lsOutput.setByteStream(bos);
                LSSerializer lsSerializer = domImpl.createLSSerializer();
                lsSerializer.write(doc, lsOutput);
                is = new ReaderInputStream(new StringReader(bos.toString()));

                this._init(is);
            } catch (Exception e) {
                throw new OREParserException(e);
            }
        } finally {
            try {
                bos.close();
            } catch (IOException e) {
                throw new OREParserException(e);
            }
            if (is != null) {
                try {
                    is.close();
                } catch (IOException e) {
                    throw new OREParserException(e);
                }
            }
        }
    }

    /**
     * Constructor for testing, allows test class to override the index visibility delegate object.  
     * To avoid need for hazelcast during testing.
     * @param fileObjectPath
     * @param ivd
     * @throws OREException
     * @throws URISyntaxException
     * @throws OREParserException
     * @throws IOException
     */
    public ForesiteResourceMap(String fileObjectPath, IndexVisibilityDelegate ivd)
            throws OREParserException {
        if (ivd != null) {
            this.indexVisibilityDelegate = ivd;
        }
        FileInputStream fileInputStream = null;
        try {
            fileInputStream = new FileInputStream(fileObjectPath);
            _init(fileInputStream);
        } catch (Exception e) {
            throw new OREParserException(e);
        } finally {
            try {
                if (fileInputStream != null) {
                    fileInputStream.close();
                }
            } catch (IOException e) {
                logger.error("error closing file input stream", e);
            }
        }
    }

    private void _init(InputStream is) throws OREException, URISyntaxException,
            UnsupportedEncodingException, OREParserException {
        /* Creates the identifier map from the doc */
        Map<Identifier, Map<Identifier, List<Identifier>>> tmpResourceMap = null;

        try {
            tmpResourceMap = ResourceMapFactory.getInstance().parseResourceMap(is);

        } catch (Throwable e) {
            logger.error("Unable to parse ORE document:", e);
            throw new OREParserException(e);
        }

        /* Gets the top level identifier */
        Identifier identifier = tmpResourceMap.keySet().iterator().next();
        this.setIdentifier(identifier.getValue());

        /* Gets the to identifier map */
        Map<Identifier, List<Identifier>> identiferMap = tmpResourceMap.get(identifier);

        this.resourceMap = new HashMap<String, ForesiteResourceEntry>();

        for (Map.Entry<Identifier, List<Identifier>> entry : identiferMap.entrySet()) {
            ForesiteResourceEntry documentsResourceEntry = resourceMap.get(entry.getKey()
                    .getValue());

            if (documentsResourceEntry == null) {
                documentsResourceEntry = new ForesiteResourceEntry(entry.getKey().getValue(), this);
                resourceMap.put(entry.getKey().getValue(), documentsResourceEntry);
            }

            for (Identifier documentedByIdentifier : entry.getValue()) {

                Identifier pid = new Identifier();
                pid.setValue(documentedByIdentifier.getValue());
                if (indexVisibilityDelegate.isDocumentVisible(pid)) {
                    documentsResourceEntry.addDocuments(documentedByIdentifier.getValue());
                }

                ForesiteResourceEntry documentedByResourceEntry = resourceMap
                        .get(documentedByIdentifier.getValue());

                if (documentedByResourceEntry == null) {
                    documentedByResourceEntry = new ForesiteResourceEntry(
                            documentedByIdentifier.getValue(), this);

                    resourceMap.put(documentedByIdentifier.getValue(), documentedByResourceEntry);
                }

                pid = new Identifier();
                pid.setValue(entry.getKey().getValue());

                if (indexVisibilityDelegate.isDocumentVisible(pid)) {
                    documentedByResourceEntry.addDocumentedBy(entry.getKey().getValue());
                }
            }
        }
    }

    public static boolean representsResourceMap(String formatId) {
        return RESOURCE_MAP_FORMAT.equals(formatId);
    }

    private boolean isHeadVersion(Identifier pid, Identifier sid) {
        boolean isHead = true;
        if(pid != null && sid != null) {
            Identifier head = null;
            try {
               //if the passed sid actually is a pid, the method will return the pid.
               head = SeriesIdResolver.getPid(sid);
            } catch (Exception e) {
                isHead = true;
            }
            if(head != null ) {

                logger.info("||||||||||||||||||| the head version is " + head.getValue()
                            + " for sid " + sid.getValue());
                if(head.equals(pid)) {
                    logger.info("||||||||||||||||||| the pid " + pid.getValue()
                                + " is the head version for sid " + sid.getValue());
                    isHead=true;
                } else {
                    logger.info("||||||||||||||||||| the pid " + pid.getValue()
                                   + " is NOT the head version for sid " + sid.getValue());
                    isHead=false;
                }
            } else {
                logger.info("||||||||||||||||||| can't find the head version for sid "
                              + sid.getValue() + " and we think the given pid " + pid.getValue()
                              + " is the head version.");
            }
        }
        return isHead;
    }

    private SolrDoc _mergeMappedReference(ResourceEntry resourceEntry, SolrDoc mergeDocument)
                                                throws InvalidToken, NotAuthorized, NotImplemented,
                         NoSuchAlgorithmException, ServiceFailure, NotFound, InstantiationException,
                                        IllegalAccessException, IOException, MarshallingException {

        Identifier identifier = new Identifier();
        identifier.setValue(mergeDocument.getIdentifier());
        try {
            SystemMetadata sysMeta = (SystemMetadata) ObjectManager.getInstance()
                                                        .getSystemMetadata(identifier.getValue());
            if (sysMeta.getSeriesId() != null && sysMeta.getSeriesId().getValue() != null
                                       && !sysMeta.getSeriesId().getValue().trim().equals("")) {
                // skip this one
                if(!isHeadVersion(identifier, sysMeta.getSeriesId())) {
                    logger.info("The id " + identifier + " is not the head of the serial id "
                               + sysMeta.getSeriesId().getValue()
                               + " So, skip merge this one!!!!!!!!!!!!!!!!!!!!!!"
                               + mergeDocument.getIdentifier());
                    return mergeDocument;
                }
            }
        } catch (ClassCastException e) {
            logger.warn("The systemmetadata is a v1 object and we need to do nothing");
        }


        if (mergeDocument.hasField(SolrElementField.FIELD_ID) == false) {
            mergeDocument.addField(new SolrElementField(SolrElementField.FIELD_ID, resourceEntry
                    .getIdentifier()));
        }

        for (String documentedBy : resourceEntry.getDocumentedBy()) {
            if (mergeDocument
                    .hasFieldWithValue(SolrElementField.FIELD_ISDOCUMENTEDBY, documentedBy) == false) {
                mergeDocument.addField(new SolrElementField(SolrElementField.FIELD_ISDOCUMENTEDBY,
                        documentedBy));
            }
        }

        for (String documents : resourceEntry.getDocuments()) {
            if (mergeDocument.hasFieldWithValue(SolrElementField.FIELD_DOCUMENTS, documents) == false) {
                mergeDocument.addField(new SolrElementField(SolrElementField.FIELD_DOCUMENTS,
                        documents));
            }
        }

        for (String resourceMap : resourceEntry.getResourceMaps()) {
            if (mergeDocument.hasFieldWithValue(SolrElementField.FIELD_RESOURCEMAP, resourceMap) == false) {
                mergeDocument.addField(new SolrElementField(SolrElementField.FIELD_RESOURCEMAP,
                        resourceMap));
            }
        }

        mergeDocument.setMerged(true);

        return mergeDocument;
    }

    public Set<ResourceEntry> getMappedReferences() {
        /* Builds a set for references that are visible in solr doc index and
         * are not the resource map id */
        HashSet<ResourceEntry> resourceEntries = new HashSet<ResourceEntry>();

        for (ResourceEntry resourceEntry : this.resourceMap.values()) {
            Identifier pid = new Identifier();
            pid.setValue(resourceEntry.getIdentifier());
            // if the document does not have system metadata yet, cannot check visibility.  include in list of ids.
            // if document does exist, it must be visible in the index to be included.
            if (!indexVisibilityDelegate.documentExists(pid)
                    || indexVisibilityDelegate.isDocumentVisible(pid)) {
                if (resourceEntry.getIdentifier().equals(this.getIdentifier()) == false) {
                    resourceEntries.add(resourceEntry);
                }
            }
        }

        /* Return the set of resource entries */
        return resourceEntries;
    }

    @Override
    public Set<String> getContains() {
        Set<String> contains = new HashSet<String>();

        for (String id : this.resourceMap.keySet()) {
            contains.add(id);
        }

        for (ResourceEntry resourceEntry : this.resourceMap.values()) {
            contains.add(resourceEntry.getIdentifier());
        }

        return contains;
    }

    @Override
    public List<String> getAllDocumentIDs() {
        List<String> docIds = new LinkedList<String>();

        /* Adds the map identifier */
        docIds.add(this.getIdentifier());

        /* Adds the mapped references */
        for (ResourceEntry resourceEntry : getMappedReferences()) {
            docIds.add(resourceEntry.getIdentifier());
        }

        /* Return the document IDs */
        return docIds;
    }

    @Override
    public List<SolrDoc> mergeIndexedDocuments(List<SolrDoc> docs) {
        List<SolrDoc> mergedDocuments = new ArrayList<SolrDoc>();
        for (ResourceEntry resourceEntry : this.resourceMap.values()) {
            for (SolrDoc doc : docs) {

                logger.debug("in mergeIndexedDocuments of ForesiteResourceMap, the doc id is "
                          + doc.getIdentifier() + " in the thread "+Thread.currentThread().getId());
                logger.debug("in mergeIndexedDocuments of ForesiteResourceMap, the doc series id is "
                          + doc.getSeriesId() + " in the thread "+Thread.currentThread().getId());
                logger.debug("in mergeIndexedDocuments of ForesiteResourceMap, the resource entry id is "
                              + resourceEntry.getIdentifier() + " in the thread "
                              + Thread.currentThread().getId());
               
                if (doc.getIdentifier().equals(resourceEntry.getIdentifier())
                        || resourceEntry.getIdentifier().equals(doc.getSeriesId())) {
                    try {
                        mergedDocuments.add(_mergeMappedReference(resourceEntry, doc));
                    } catch (Exception e) {
                        logger.error("ForestieResourceMap.mergeIndexedDocuments - cannot merge the document since "
                                    + e.getMessage());
                    }
                    
                }
            }
        }
        return mergedDocuments;
    }

    @Override
    public String getIdentifier() {
        return this.identifier;
    }

    void setIdentifier(String identifier) {
        this.identifier = identifier;
    }
}