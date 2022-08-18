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

package org.dataone.cn.index;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathExpressionException;

import org.apache.commons.codec.EncoderException;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.solr.SolrJettyTestBase;
import org.apache.solr.SolrTestCaseJ4.SuppressSSL;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.embedded.JettyConfig;
import org.apache.solr.client.solrj.embedded.JettySolrRunner;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.client.solrj.util.ClientUtils;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.params.ModifiableSolrParams;
import org.dataone.cn.indexer.SolrIndex;
import org.dataone.cn.indexer.parser.ISolrField;
import org.dataone.cn.indexer.solrhttp.SolrElementField;
import org.dataone.service.exceptions.NotFound;
import org.dataone.service.exceptions.NotImplemented;
import org.dataone.service.exceptions.ServiceFailure;
import org.dataone.service.exceptions.UnsupportedType;
import org.dataone.service.types.v1.Identifier;
import org.dataone.service.types.v2.SystemMetadata;
import org.dataone.service.util.DateTimeMarshaller;
import org.dataone.service.util.TypeMarshaller;
import org.junit.Assert;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.core.io.Resource;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

/**
 * Solr unit test framework is dependent on JUnit 4.7. Later versions of junit
 * will break the base test classes.
 * 
 * @author sroseboo
 * 
 */
@SuppressSSL
public abstract class DataONESolrJettyTestBase extends SolrJettyTestBase {

    protected ApplicationContext context;
    private SolrIndex solrIndexService;
    
    /**
     * Index the given object into solr
     * @param identifier  the identifier of the object which needs to be indexed
     * @param objectFile  the file path of the object which needs to be indexed
     * @throws Exception
     */
    protected void indexObjectToSolr(String identifier, Resource objectFile) throws Exception {
        boolean isSysmetaChangeOnly = false;
        String relativePath = objectFile.getFile().getPath();
        Identifier pid = new Identifier();
        pid.setValue(identifier);
        solrIndexService.update(pid, relativePath, isSysmetaChangeOnly);
    }
    
    /**
     * Delete the given identifier from the solr server
     * @param identifier
     * @throws XPathExpressionException
     * @throws ServiceFailure
     * @throws NotImplemented
     * @throws NotFound
     * @throws UnsupportedType
     * @throws IOException
     * @throws EncoderException
     * @throws SolrServerException
     * @throws ParserConfigurationException
     * @throws SAXException
     */
    protected void deleteSolrDoc(String identifier) throws XPathExpressionException, ServiceFailure, NotImplemented, 
                            NotFound, UnsupportedType, IOException, EncoderException, SolrServerException, 
                            ParserConfigurationException, SAXException {
        Identifier pid = new Identifier();
        pid.setValue(identifier);
        solrIndexService.remove(pid);
    }

    protected void addEmlToSolrIndex(Resource sysMetaFile) throws Exception {
        SolrIndex indexService = solrIndexService;
        SystemMetadata smd = TypeMarshaller.unmarshalTypeFromStream(SystemMetadata.class,
                sysMetaFile.getInputStream());
        // path to actual science metadata document
        String path = StringUtils.remove(sysMetaFile.getFile().getPath(), File.separator + "SystemMetadata");
        boolean isSysmetaChangeOnly = false;
        indexService.update(smd.getIdentifier(), path, isSysmetaChangeOnly);
       
    }

    protected void addSysAndSciMetaToSolrIndex(Resource sysMeta, Resource sciMeta) throws Exception {
        SolrIndex indexService = solrIndexService;
        SystemMetadata smd = TypeMarshaller.unmarshalTypeFromStream(SystemMetadata.class,
                sysMeta.getInputStream());
        String path = sciMeta.getFile().getAbsolutePath();
        boolean isSysmetaChangeOnly = false;
        indexService.update(smd.getIdentifier(), path, isSysmetaChangeOnly);
    }

    protected SolrDocument assertPresentInSolrIndex(String pid) throws SolrServerException,
            IOException {
        ModifiableSolrParams solrParams = new ModifiableSolrParams();
        solrParams.set("q", "id:" + ClientUtils.escapeQueryChars(pid));
        solrParams.set("fl", "*");
        QueryResponse qr = getSolrClient().query(solrParams);
        Assert.assertFalse(qr.getResults().isEmpty());
        SolrDocument result = qr.getResults().get(0);
        String id = (String) result.getFieldValue("id");
        Assert.assertEquals(pid, id);
        return result;
    }

    protected SolrDocumentList findByField(String field, String value) throws SolrServerException,
            IOException {
        return findByQueryString(field + ":" + value);
    }

    protected SolrDocumentList findByQueryString(String query) throws SolrServerException,
            IOException {
        ModifiableSolrParams solrParams = new ModifiableSolrParams();
        solrParams.set("q", query);
        QueryResponse qr = getSolrClient().query(solrParams);
        return qr.getResults();
    }

    public void sendSolrDeleteAll() throws SolrServerException, IOException {
        getSolrClient().deleteByQuery("*:*");
        getSolrClient().commit();
    }

    protected void assertNotPresentInSolrIndex(String pid) throws SolrServerException, IOException {
        ModifiableSolrParams solrParams = new ModifiableSolrParams();
        solrParams.set("q", "id:" + pid);
        QueryResponse qr = getSolrClient().query(solrParams);
        Assert.assertTrue(qr.getResults().isEmpty());
    }

    protected SolrDocumentList getAllSolrDocuments() throws SolrServerException, IOException {
        ModifiableSolrParams solrParams = new ModifiableSolrParams();
        solrParams.set("q", "*:*");
        QueryResponse qr = getSolrClient().query(solrParams);
        return qr.getResults();
    }

    protected void compareFields(SolrDocument solrResult, Document metadataDoc,
            ISolrField fieldToCompare, String identifier) throws Exception {
        List<SolrElementField> fields = fieldToCompare.getFields(metadataDoc, identifier);
        if (fields.isEmpty() == false) {
            SolrElementField docField = fields.get(0);
            Object solrValueObject = solrResult.getFieldValue(docField.getName());

            System.out.println("Comparing value for field " + docField.getName());
            if (solrValueObject == null) {
                if (!"text".equals(docField.getName())) {
                    System.out.println("Null result value for field name:  " + docField.getName()
                            + ", actual: " + docField.getValue());
                    Assert.assertTrue(docField.getValue() == null || "".equals(docField.getValue()));
                }
            } else if (solrValueObject instanceof String) {
                String solrValue = (String) solrValueObject;
                String docValue = docField.getValue();
                System.out.println("Doc Value:  " + docValue);
                System.out.println("Solr Value: " + solrValue);
                Assert.assertEquals(docField.getValue(), solrValue);
            } else if (solrValueObject instanceof Boolean) {
                Boolean solrValue = (Boolean) solrValueObject;
                Boolean docValue = Boolean.valueOf(docField.getValue());
                System.out.println("Doc Value:  " + docValue);
                System.out.println("Solr Value: " + solrValue);
                Assert.assertEquals(docValue, solrValue);
            } else if (solrValueObject instanceof Integer) {
                Integer solrValue = (Integer) solrValueObject;
                Integer docValue = Integer.valueOf(docField.getValue());
                System.out.println("Doc Value:  " + docValue);
                System.out.println("Solr Value: " + solrValue);
                Assert.assertEquals(docValue, solrValue);
            } else if (solrValueObject instanceof Long) {
                Long solrValue = (Long) solrValueObject;
                Long docValue = Long.valueOf(docField.getValue());
                System.out.println("Doc Value:  " + docValue);
                System.out.println("Solr Value: " + solrValue);
                Assert.assertEquals(docValue, solrValue);
            } else if (solrValueObject instanceof Float) {
                Float solrValue = (Float) solrValueObject;
                Float docValue = Float.valueOf(docField.getValue());
                System.out.println("Doc Value:  " + docValue);
                System.out.println("Solr Value: " + solrValue);
                Assert.assertEquals(docValue, solrValue);
            } else if (solrValueObject instanceof Date) {
                Date solrValue = (Date) solrValueObject;
                Date docValue = DateTimeMarshaller.deserializeDateToUTC(docField.getValue());
                System.out.println("Doc Value:  " + docValue);
                System.out.println("Solr Value: " + solrValue);
                Assert.assertEquals(docValue.getTime(), solrValue.getTime());
            } else if (solrValueObject instanceof ArrayList) {
                ArrayList solrValueArray = (ArrayList) solrValueObject;
                ArrayList documentValueArray = new ArrayList();
                for (SolrElementField sef : fields) {
                    documentValueArray.add(sef.getValue());
                }
                System.out.println("Doc Value:  " + documentValueArray);
                System.out.println("Solr Value: " + solrValueArray);
                Assert.assertTrue(CollectionUtils.isEqualCollection(documentValueArray,
                        solrValueArray));
            } else {
                Assert.assertTrue(
                        "Unknown solr value object type for field: " + docField.getName(), false);
            }
            System.out.println("");
        }
    }

    public void setUp() throws Exception {
        super.setUp();
        loadSpringContext();
        startJettyAndSolr();
    }

    public void tearDown() throws Exception {
        super.tearDown();
    }

    protected void loadSpringContext() {
        if (context == null) {
            context = new ClassPathXmlApplicationContext("org/dataone/cn/index/test-context.xml");
        }
        solrIndexService = (SolrIndex) context.getBean("solrIndex");
    }

    protected void startJettyAndSolr() throws Exception {
        if (jetty == null) {
            JettyConfig jconfig = JettyConfig.builder().setPort(8983).build();
            File f = new File(".");
            String localPath = f.getAbsolutePath();
            createJettyWithPort(localPath
                    + "/src/test/resources/org/dataone/cn/index/resources/solr5home", jconfig);
        }
    }

    // method is copied in from SolrJettyTestBase in order to set the port
    // number to match solr.properties (8983) for XPathDocumentParser to connect
    // to same solr server. If left unset, the port number is a random open
    // port.
    protected static JettySolrRunner createJettyWithPort(String solrHome, JettyConfig config)
            throws Exception {
        createJetty(solrHome, config);
        return jetty;
    }
    
    protected boolean compareFieldValue(String id, String fieldName, String expectedValue) throws SolrServerException, IOException {
        System.out.println("==================== start of compare");
        boolean equal = false;
        ModifiableSolrParams solrParams = new ModifiableSolrParams();
        solrParams.set("q", "id:" + ClientUtils.escapeQueryChars(id));
        solrParams.set("fl", "*");
        QueryResponse qr = getSolrClient().query(solrParams);
        SolrDocument result = qr.getResults().get(0);
        String value = (String)result.getFieldValue(fieldName);
        System.out.println("+++++++++++++++++++The value of the field "+ fieldName + " from Solr is " + value);
        System.out.println("The expected value of the field " + fieldName + " is " + expectedValue);
        return expectedValue.equals(value);
    }
}
