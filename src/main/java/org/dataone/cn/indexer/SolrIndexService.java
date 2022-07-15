/**
 * This work was created by participants in the DataONE project, and is
 * jointly copyrighted by participating institutions in DataONE. For 
 * more information on DataONE, see our web site at http://dataone.org.
 *
 *   Copyright ${year}
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and 
 * limitations under the License.
 * 
 * $Id$
 */

package org.dataone.cn.indexer;

import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathExpressionException;

import org.apache.commons.codec.EncoderException;
import org.apache.commons.io.output.ByteArrayOutputStream;
import org.apache.log4j.Logger;
import org.dataone.cn.hazelcast.HazelcastClientFactory;
import org.dataone.cn.index.task.IndexTask;
import org.dataone.cn.index.util.PerformanceLogger;
import org.dataone.cn.indexer.parser.IDocumentDeleteSubprocessor;
import org.dataone.cn.indexer.parser.IDocumentSubprocessor;
import org.dataone.cn.indexer.solrhttp.HTTPService;
import org.dataone.cn.indexer.solrhttp.SolrDoc;
import org.dataone.cn.indexer.solrhttp.SolrElementAdd;
import org.dataone.cn.indexer.solrhttp.SolrElementField;
import org.dataone.service.types.v1.Identifier;
import org.dataone.service.types.v2.SystemMetadata;
import org.dataone.service.util.TypeMarshaller;
import org.springframework.beans.factory.annotation.Autowired;
import org.xml.sax.SAXException;

/**
 * Top level document processing class.  
 * 
 * Contains collection of document sub-processors which are used to mine search
 * index data from document objects.  Each sub-processor is configured via spring
 * to collect data from different types of documents (by formatId).
 * 
 * There should only be one instance of XPathDocumentParser in place at a time
 * since it performs updates on the SOLR index and transactions on SOLR are at
 * the server level - so if multiple threads write and commit then things could
 * get messy.
 * 
 */

public class SolrIndexService {

    private static Logger log = Logger.getLogger(SolrIndexService.class);
    private static final String OUTPUT_ENCODING = "UTF-8";
    private List<IDocumentSubprocessor> subprocessors = null;
    private List<IDocumentDeleteSubprocessor> deleteSubprocessors = null;
    private IDocumentSubprocessor systemMetadataProcessor = null;

    @Autowired
    private HTTPService httpService = null;

    @Autowired
    private String solrIndexUri = null;

    @Autowired
    private String solrQueryUri = null;

    private PerformanceLogger perfLog = PerformanceLogger.getInstance();
    
    public SolrIndexService() {
    }

    public void removeFromIndex(String identifier) throws Exception {

        Map<String, SolrDoc> docs = new HashMap<String, SolrDoc>();

        for (IDocumentDeleteSubprocessor deleteSubprocessor : getDeleteSubprocessors()) {
            docs.putAll(deleteSubprocessor.processDocForDelete(identifier, docs));
        }
        List<SolrDoc> docsToUpdate = new ArrayList<SolrDoc>();
        List<String> idsToIndex = new ArrayList<String>();
        for (String idToUpdate : docs.keySet()) {
            if (docs.get(idToUpdate) != null) {
                docsToUpdate.add(docs.get(idToUpdate));
            } else {
                idsToIndex.add(idToUpdate);
            }
        }

        SolrElementAdd addCommand = getAddCommand(docsToUpdate);
        sendCommand(addCommand);

        deleteDocFromIndex(identifier);

        for (String idToIndex : idsToIndex) {
            Identifier pid = new Identifier();
            pid.setValue(idToIndex);
            SystemMetadata sysMeta = HazelcastClientFactory.getSystemMetadataMap().get(pid);
            if (SolrDoc.visibleInIndex(sysMeta)) {
                String objectPath = HazelcastClientFactory.getObjectPathMap().get(pid);
                ByteArrayOutputStream os = new ByteArrayOutputStream();
                TypeMarshaller.marshalTypeToOutputStream(sysMeta, os);
                insertIntoIndex(idToIndex, new ByteArrayInputStream(os.toByteArray()), objectPath);
            }
        }
    }

    public void removeFromIndex(List<IndexTask> tasks) throws Exception {
    
        for (IndexTask indexTask : tasks) {
            String identifier = indexTask.getPid();
            try {
                removeFromIndex(identifier);
            } catch (Exception e) {
                log.error("Unable to remove from index: " + identifier, e);
            }
        }
        
        /*
         * TODO test:
        
        SolrElementAdd addCommand = new SolrElementAdd();
        List<String> identifiersToDelete = new ArrayList<String>();
        List<String> idsToIndex = new ArrayList<String>();
        List<IndexTask> tasksToIndex = new ArrayList<IndexTask>();
        
        for (IndexTask indexTask : tasks) {
            Map<String, SolrDoc> docs = new HashMap<String, SolrDoc>();
            String identifier = indexTask.getPid();
            
            for (IDocumentDeleteSubprocessor deleteSubprocessor : getDeleteSubprocessors()) {
                docs.putAll(deleteSubprocessor.processDocForDelete(identifier, docs));
            }
            List<SolrDoc> docsToUpdate = new ArrayList<SolrDoc>();
            
            for (String idToUpdate : docs.keySet()) {
                if (docs.get(idToUpdate) != null) {
                    docsToUpdate.add(docs.get(idToUpdate));
                } else {
                    idsToIndex.add(idToUpdate);
                }
            }

            SolrElementAdd newAddCommand = getAddCommand(docsToUpdate);
            addCommand = mergeAddCommands(addCommand, newAddCommand);
            
            identifiersToDelete.add(identifier);

            for (String idToIndex : idsToIndex) {
                Identifier pid = new Identifier();
                pid.setValue(idToIndex);
                SystemMetadata sysMeta = HazelcastClientFactory.getSystemMetadataMap().get(pid);
                if (SolrDoc.visibleInIndex(sysMeta)) {
                    String objectPath = HazelcastClientFactory.getObjectPathMap().get(pid);
                    IndexTask task = new IndexTask(sysMeta, objectPath);
                    tasksToIndex.add(task);
                }
            }
        }
        
        sendCommand(addCommand);
        httpService.sendSolrDeletes(identifiersToDelete);
        insertIntoIndex(tasksToIndex);
        */
    }
    
    /**
     * Given a PID, system metadata input stream, and an optional document
     * path, populate the set of SOLR fields for the document. 
     * 
     * @param id
     * @param systemMetaDataStream
     * @param objectPath
     * @return
     * @throws IOException
     * @throws SAXException
     * @throws ParserConfigurationException
     * @throws XPathExpressionException
     * @throws EncoderException
     */
    public SolrElementAdd processObject(String id, InputStream systemMetaDataStream,
            String objectPath) throws IOException, SAXException, ParserConfigurationException,
            XPathExpressionException, EncoderException, Exception {

        long processObjStart = System.currentTimeMillis();
        
        Map<String, SolrDoc> docs = new HashMap<String, SolrDoc>();
        try {
            long sysmetaProcStart = System.currentTimeMillis();
            docs = systemMetadataProcessor.processDocument(id, docs, systemMetaDataStream);
            perfLog.log(systemMetadataProcessor.getClass().getSimpleName() + ".processDocument() processing sysmeta for id "+id, System.currentTimeMillis() - sysmetaProcStart);
        } catch (Exception e) {
            log.error("Error parsing system metadata for id: " + id + e.getMessage(), e);
        }

        if (objectPath != null) {
            String formatId = docs.get(id).getFirstFieldValue(SolrElementField.FIELD_OBJECTFORMAT);
            int i=1;
            for (IDocumentSubprocessor subprocessor : getSubprocessors()) {
                if (subprocessor.canProcess(formatId)) {
                    try {
                        // note that resource map processing touches all objects
                        // referenced by the resource map.
                        long startFechingFile = System.currentTimeMillis();
                        FileInputStream objectStream = new FileInputStream(objectPath);
                        perfLog.log("Loop "+i+". SolrIndexService.processObject() fetch file for id "+id, System.currentTimeMillis() - startFechingFile);
                        if (!objectStream.getFD().valid()) {
                            log.error("Could not load OBJECT file for ID,Path=" + id + ", "
                                    + objectPath);
                        } else {
                            long scimetaProcStart = System.currentTimeMillis();
                            docs = subprocessor.processDocument(id, docs, objectStream);
                            perfLog.log(String.format(
                                    "Loop %d. SolrIndexService.processObject() " 
                                            + "%s.processDocument() total subprocessor processing time for id %s with format: %s",
                                            i, 
                                            subprocessor.getClass().getSimpleName(),
                                            id,
                                            formatId),
                                            System.currentTimeMillis() - scimetaProcStart);
                        }
                    } catch (Exception e) {
                        log.warn(String.format("The subprocessor %s can't process the id %s since %s. " +
                                "However, the index still can be achieved without this part of information provided by the processor.",
                                subprocessor.getClass().getName(),
                                id,
                                e.getMessage()),
                                e);
                    } 
                }
                i++;
            }
        } else {
            log.warn("The optional objectPath for pid " + id + " is null, so skipping processing with content subprocessors");
        }

        long mergeProcStart = System.currentTimeMillis();
        Map<String, SolrDoc> mergedDocs = new HashMap<String, SolrDoc>();
        int index =1;
        for (SolrDoc mergeDoc : docs.values()) {
            int innerIndex =1;
            for (IDocumentSubprocessor subprocessor : getSubprocessors()) {
                long before = System.currentTimeMillis();
                mergeDoc = subprocessor.mergeWithIndexedDocument(mergeDoc);
                perfLog.log("Outer loop "+index+", inner loop"+innerIndex+" SolrIndexService.processObject() merging docs for id "+id, System.currentTimeMillis() - before);
                innerIndex++;
            }
            mergedDocs.put(mergeDoc.getIdentifier(), mergeDoc);
            index++;
        }
        perfLog.log("Total - SolrIndexService.processObject() merging docs for id "+id, System.currentTimeMillis() - mergeProcStart);
        
        SolrElementAdd addCommand = getAddCommand(new ArrayList<SolrDoc>(mergedDocs.values()));
        if (log.isTraceEnabled()) {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            addCommand.serialize(baos, OUTPUT_ENCODING);
            log.trace(baos.toString());
        }

        perfLog.log("SolrIndexService.processObject() total processing time for id " + id, System.currentTimeMillis() - processObjStart);
        return addCommand;
    }

    /**
     * Given a PID, system metadata input stream, and an optional document
     * path, populate the set of SOLR fields for the document and update the
     * index. Note that if the document is a resource map, then records that it
     * references will be updated as well.
     * 
     * @param id
     * @param systemMetaDataStream
     * @param objectPath
     * @return
     * @throws IOException
     * @throws SAXException
     * @throws ParserConfigurationException
     * @throws XPathExpressionException
     * @throws EncoderException
     */
    public void insertIntoIndex(String id, InputStream systemMetaDataStream, String objectPath)
            throws IOException, SAXException, ParserConfigurationException,
            XPathExpressionException, EncoderException, Exception {

        // get the add command for solr
        SolrElementAdd addCommand = processObject(id, systemMetaDataStream, objectPath);

        // send it
        long solrAddStart = System.currentTimeMillis();
        sendCommand(addCommand);
        perfLog.log("SolrIndexService.sendCommand(SolrElementAdd) adding docs into Solr index for id "+id, System.currentTimeMillis() - solrAddStart);
    }

    public void insertIntoIndex(List<IndexTask> tasks) 
            throws IOException, SAXException, ParserConfigurationException,
            XPathExpressionException, EncoderException, Exception {
     
        SolrElementAdd batchAddCommand = new SolrElementAdd();
        
        for (IndexTask task : tasks) {
            InputStream smdStream = new ByteArrayInputStream(task.getSysMetadata().getBytes());
            
            // get the add command for solr
            SolrElementAdd addCommand = processObject(task.getPid(), smdStream, task.getObjectPath());
            batchAddCommand = mergeAddCommands(batchAddCommand, addCommand);
        }
        // send batch add command
        long solrAddStart = System.currentTimeMillis();
        sendCommand(batchAddCommand);
        perfLog.log("SolrIndexService.sendCommand(SolrElementAdd) batch adding (" + tasks.size() + ") docs into Solr index", System.currentTimeMillis() - solrAddStart);
    }
    
    private SolrElementAdd mergeAddCommands(SolrElementAdd original, SolrElementAdd addition) {
        if (original.getDocList() == null)
            original.setDocList(new ArrayList<SolrDoc>());
        if (addition.getDocList() == null)
            addition.setDocList(new ArrayList<SolrDoc>());
        
        original.getDocList().addAll(addition.getDocList());
        
        return original;
    }
    
    private void sendCommand(SolrElementAdd addCommand) throws IOException {
        HTTPService service = getHttpService();
        service.sendUpdate(getSolrindexUri(), addCommand, OUTPUT_ENCODING);
    }

    private SolrElementAdd getAddCommand(List<SolrDoc> docs) {
        return new SolrElementAdd(docs);
    }

    private void deleteDocFromIndex(String identifier) {
        httpService.sendSolrDelete(identifier);
    }

    public String getSolrindexUri() {
        return solrIndexUri;
    }

    public void setSolrIndexUri(String solrindexUri) {
        this.solrIndexUri = solrindexUri;
    }

    public void setHttpService(HTTPService service) {
        this.httpService = service;
    }

    public HTTPService getHttpService() {
        return httpService;
    }

    public String getSolrQueryUri() {
        return solrQueryUri;
    }

    public void setSolrQueryUri(String solrQueryUri) {
        this.solrQueryUri = solrQueryUri;
    }

    public List<IDocumentSubprocessor> getSubprocessors() {
        if (this.subprocessors == null) {
            this.subprocessors = new ArrayList<IDocumentSubprocessor>();
        }
        return subprocessors;
    }

    public List<IDocumentDeleteSubprocessor> getDeleteSubprocessors() {
        if (this.deleteSubprocessors == null) {
            this.deleteSubprocessors = new ArrayList<IDocumentDeleteSubprocessor>();
        }
        return deleteSubprocessors;
    }

    public void setSubprocessors(List<IDocumentSubprocessor> subprocessorList) {
        this.subprocessors = subprocessorList;
    }

    public void setDeleteSubprocessors(List<IDocumentDeleteSubprocessor> deleteSubprocessorList) {
        this.deleteSubprocessors = deleteSubprocessorList;
    }

    public IDocumentSubprocessor getSystemMetadataProcessor() {
        return systemMetadataProcessor;
    }

    public void setSystemMetadataProcessor(IDocumentSubprocessor systemMetadataProcessor) {
        this.systemMetadataProcessor = systemMetadataProcessor;
    }

}
