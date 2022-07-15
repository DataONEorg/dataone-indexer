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

package org.dataone.cn.indexer.solrhttp;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.apache.commons.codec.EncoderException;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.output.ByteArrayOutputStream;
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.message.BasicNameValuePair;
import org.apache.log4j.Logger;
import org.apache.solr.client.solrj.SolrServerException;
import org.dataone.service.exceptions.NotFound;
import org.dataone.service.exceptions.UnsupportedType;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;


/**
 * User: Porter Date: 7/26/11 Time: 11:37 AM
 * <p/>
 * HTTP Services based on Apache httpcomponents. This class to handles various
 * solr functions including adding documents to index.
 */

public class HTTPService {

    private static final String CHAR_ENCODING = "UTF-8";
    private static final String XML_CONTENT_TYPE = "text/xml";

    final static String PARAM_START = "start";
    final static String PARAM_ROWS = "rows";
    final static String PARAM_INDENT = "indent";
    final static String VALUE_INDENT_ON = "on";
    final static String VALUE_INDENT_OFF = "off";
    final static String PARAM_QUERY = "q";
    final static String PARAM_RETURN = "fl";
    final static String VALUE_WILDCARD = "*";

    private static final String MAX_ROWS = "5000";

    private static Logger log = Logger.getLogger(HTTPService.class.getName());
    private HttpComponentsClientHttpRequestFactory httpRequestFactory;

    private String SOLR_SCHEMA_PATH;
    private String solrIndexUri;
    private List<String> validSolrFieldNames = new ArrayList<String>();

    public HTTPService(HttpComponentsClientHttpRequestFactory requestFactory) {
        httpRequestFactory = requestFactory;
    }

    /**
     * Posts document data to Solr indexer.
     * 
     * @param uri
     *            Solr index url example:
     *            http://localhost:8080/solr/update?commit=true
     * @param data
     *            documents to index
     * @param encoding
     *            use "UTF-8"
     * @throws IOException
     */

    public void sendUpdate(String uri, SolrElementAdd data, String encoding) throws IOException {
        this.sendUpdate(uri, data, encoding, XML_CONTENT_TYPE);
    }

    public void sendUpdate(String uri, SolrElementAdd data) throws IOException {
        sendUpdate(uri, data, CHAR_ENCODING, XML_CONTENT_TYPE);
    }

    public void sendUpdate(String uri, SolrElementAdd data, String encoding, String contentType)
            throws IOException {
        InputStream inputStreamResponse = null;
        HttpPost post = null;
        HttpResponse response = null;
        try {
            post = new HttpPost(uri);
            post.setHeader("Content-Type", contentType);
            post.setEntity(new OutputStreamHttpEntity(data, encoding));
            response = getHttpClient().execute(post);
            HttpEntity responseEntity = response.getEntity();
            inputStreamResponse = responseEntity.getContent();
            if (response.getStatusLine().getStatusCode() != 200) {
                writeError(null, data, inputStreamResponse, uri);
                post.abort();
                throw new IOException("unable to update solr, non 200 response code.");
            }
            post.abort();
        } catch (Exception ex) {
            writeError(ex, data, inputStreamResponse, uri);
            throw new IOException(ex);
        } finally {
            IOUtils.closeQuietly(inputStreamResponse);
        }
    }

    private void sendPost(String uri, String data) throws IOException {
        sendPost(uri, data, CHAR_ENCODING, XML_CONTENT_TYPE);
    }

    private void sendPost(String uri, String data, String encoding, String contentType)
            throws IOException {
        InputStream inputStreamResponse = null;
        HttpPost post = null;
        HttpResponse response = null;
        try {
            post = new HttpPost(uri);
            post.setHeader("Content-Type", contentType);
            ByteArrayEntity entity = new ByteArrayEntity(data.getBytes());
            entity.setContentEncoding(encoding);
            post.setEntity(entity);
            response = getHttpClient().execute(post);
            HttpEntity responseEntity = response.getEntity();
            inputStreamResponse = responseEntity.getContent();
            if (response.getStatusLine().getStatusCode() != 200) {
                writeError(null, data, inputStreamResponse, uri);
            }
            post.abort();
        } catch (Exception ex) {
            writeError(ex, data, inputStreamResponse, uri);
        }
    }

    public void sendSolrDelete(String pid) {
        // generate request to solr server to remove index record for task.pid
        OutputStream outputStream = new ByteArrayOutputStream();
        try {
            IOUtils.write("<?xml version=\"1.1\" encoding=\"utf-8\"?>\n", outputStream,
                    CHAR_ENCODING);
            String escapedId = StringEscapeUtils.escapeXml(pid);
            IOUtils.write("<delete><id>" + escapedId + "</id></delete>", outputStream, CHAR_ENCODING);
            sendPost(getSolrIndexUri(), outputStream.toString());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void sendSolrDeletes(List<String> pids) {
        // generate request to solr server to remove index record for task.pid
        OutputStream outputStream = new ByteArrayOutputStream();
        try {
            IOUtils.write("<?xml version=\"1.1\" encoding=\"utf-8\"?>\n", outputStream,
                    CHAR_ENCODING);
            IOUtils.write("<update>", outputStream, CHAR_ENCODING);
            for (String pid : pids) {
                String escapedId = StringEscapeUtils.escapeXml(pid);
                IOUtils.write("<delete><id>" + escapedId + "</id></delete>", outputStream, CHAR_ENCODING);  
            }
            IOUtils.write("</update>", outputStream, CHAR_ENCODING);
            sendPost(getSolrIndexUri(), outputStream.toString());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    /**
     * Borrowed from
     * http://www.docjar.com/html/api/org/apache/solr/client/solrj/
     * util/ClientUtils.java.html
     * 
     * @param s
     * @return
     */
    public static String escapeQueryChars(String s) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            // These characters are part of the query syntax and must be escaped
            if (c == '\\' || c == '+' || c == '-' || c == '!' || c == '(' || c == ')' || c == ':'
                    || c == '^' || c == '[' || c == ']' || c == '\"' || c == '{' || c == '}'
                    || c == '~' || c == '*' || c == '?' || c == '|' || c == '&' || c == ';'
                    || Character.isWhitespace(c)) {
                sb.append('\\');
            }
            sb.append(c);
        }
        return sb.toString();
    }

    private void writeError(Exception ex, SolrElementAdd data, InputStream inputStreamResonse,
            String uri) throws IOException {

        try {
            if (ex != null) {
                log.error("Unable to write to stream", ex);
            }

            log.error("URL: " + uri);
            log.error("Post: ");
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            data.serialize(baos, "UTF-8");
            log.error(new String(baos.toByteArray(), "UTF-8"));
            log.error("\n\n\nResponse: \n");
            ByteArrayOutputStream baosResponse = new ByteArrayOutputStream();
            org.apache.commons.io.IOUtils.copy(inputStreamResonse, baosResponse);
            log.error(new String(baosResponse.toByteArray()));
            inputStreamResonse.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void writeError(Exception ex, String data, InputStream inputStreamResonse, String uri)
            throws IOException {

        try {
            if (ex != null) {
                log.error("Unable to write to stream", ex);
            }

            log.error("URL: " + uri);
            log.error("Post: ");
            log.error(data);
            log.error("\n\n\nResponse: \n");
            ByteArrayOutputStream baosResponse = new ByteArrayOutputStream();
            org.apache.commons.io.IOUtils.copy(inputStreamResonse, baosResponse);
            log.error(new String(baosResponse.toByteArray()));
            inputStreamResonse.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ?q=id%3Ac6a8c20f-3503-4ded-b395-98fcb0fdd78c+OR+f5aaac58-dee1-4254-8cc4-95c5626ab037+OR+f3229cfb-2c53-4aa0-8437-057c2a52f502&version=2.2

    /**
     * Return the SOLR records for the specified PIDs
     * 
     * @param uir
     * @param ids
     * @return
     * @throws IOException
     * @throws XPathExpressionException
     * @throws EncoderException
     */
    public List<SolrDoc> getDocumentsById(String uir, List<String> ids) throws IOException,
            XPathExpressionException, EncoderException {
        List<SolrDoc> docs = getDocumentsByField(uir, ids, SolrElementField.FIELD_SERIES_ID, false);
        docs.addAll(getDocumentsByField(uir, ids, SolrElementField.FIELD_ID, false));
        return docs;
    }
    
    /**
     * Gets a single solr document that is at the top of the version chain for the given seriesId
     * @param seriesId - the target object's seriesId
     * @return the SolrDoc
     * @throws MalformedURLException
     * @throws UnsupportedType
     * @throws NotFound
     * @throws SolrServerException
     * @throws ParserConfigurationException
     * @throws IOException
     * @throws SAXException
     * @throws XPathExpressionException 
     */
    public SolrDoc getDocumentBySeriesId(String seriesId, String uir) throws MalformedURLException, 
                UnsupportedType, NotFound, SolrServerException, ParserConfigurationException, IOException, SAXException, XPathExpressionException {
        //Contruct a query to search for the most recent SolrDoc with the given seriesId
        StringBuilder query = new StringBuilder();
        //query.append("q=" + SolrElementField.FIELD_SERIES_ID + ":\"" + escapeQueryChars(seriesId) + "\" AND -obsoletedBy:*"); 
        query.append(SolrElementField.FIELD_SERIES_ID + ":" + escapeQueryChars(seriesId) + (" AND -obsoletedBy:*")); 
        log.info("HTTPService.getDocumentBeySeriesId - the uir is " + uir);
        log.info("HTTPService.getDocumentBeySeriesId - the query is " + query.toString());
        //Get the SolrDoc by querying for it
        List<SolrDoc> list = new ArrayList<SolrDoc>();
        list.addAll(doRequest(uir, query, MAX_ROWS));
                
        //If query results were found, get the first one (only one result should be found anyway)
        SolrDoc doc = null;
        if(list != null && !list.isEmpty()) {
            doc = list.get(0);
        }
        
        return doc;
    }

    public List<SolrDoc> getDocumentById(String uir, String id) throws IOException,
            XPathExpressionException, EncoderException {
        return getDocumentsByField(uir, Collections.singletonList(id), SolrElementField.FIELD_ID,
                false);
    }

    public List<SolrDoc> getDocumentsByResourceMap(String uir, String resourceMapId)
            throws IOException, XPathExpressionException, EncoderException {
        return getDocumentsByField(uir, Collections.singletonList(resourceMapId),
                SolrElementField.FIELD_RESOURCEMAP, true);
    }

    public List<SolrDoc> getDocumentsByField(String uir, List<String> fieldValues,
            String queryField, boolean maxRows) throws IOException, XPathExpressionException,
            EncoderException {

        if (fieldValues == null || fieldValues.size() <= 0) {
            return null;
        }

        loadSolrSchemaFields();

        List<SolrDoc> docs = new ArrayList<SolrDoc>();

        int rows = 0;
        String rowString = "";
        StringBuilder sb = new StringBuilder();
        for (String id : fieldValues) {
            if (sb.length() > 0) {
                sb.append(" OR ");
            }
            sb.append(queryField + ":").append(escapeQueryChars(id));
            rows++;
            if (sb.length() > 5000) {
                if (maxRows) {
                    rowString = MAX_ROWS;
                } else {
                    rowString = Integer.toString(rows);
                }
                docs.addAll(doRequest(uir, sb, rowString));
                rows = 0;
                sb = new StringBuilder();
            }
        }
        if (sb.length() > 0) {
            if (maxRows) {
                rowString = MAX_ROWS;
            } else {
                rowString = Integer.toString(rows);
            }
            docs.addAll(doRequest(uir, sb, rowString));
        }
        return docs;
    }

    public List<SolrDoc> getDocumentsByResourceMapFieldAndDocumentsField(String uir,
            String resourceMapId, String documentsId) throws IOException, XPathExpressionException,
            EncoderException {
        return getDocumentsByTwoFields(uir, SolrElementField.FIELD_RESOURCEMAP, resourceMapId,
                SolrElementField.FIELD_DOCUMENTS, documentsId);
    }

    public List<SolrDoc> getDocumentsByResourceMapFieldAndIsDocumentedByField(String uir,
            String resourceMapId, String isDocumentedById) throws IOException,
            XPathExpressionException, EncoderException {
        return getDocumentsByTwoFields(uir, SolrElementField.FIELD_RESOURCEMAP, resourceMapId,
                SolrElementField.FIELD_ISDOCUMENTEDBY, isDocumentedById);
    }

    private List<SolrDoc> getDocumentsByTwoFields(String uir, String field1, String field1Value,
            String field2, String field2Value) throws IOException, XPathExpressionException,
            EncoderException {
        loadSolrSchemaFields();
        List<SolrDoc> docs = new ArrayList<SolrDoc>();
        StringBuilder sb = new StringBuilder();
        sb.append(field1 + ":").append(escapeQueryChars(field1Value));
        sb.append(" AND ");
        sb.append(field2 + ":").append(escapeQueryChars(field2Value));
        docs.addAll(doRequest(uir, sb, MAX_ROWS));
        return docs;
    }

    private List<SolrDoc> doRequest(String uir, StringBuilder sb, String rows) throws IOException,
            ClientProtocolException, XPathExpressionException {
        List<NameValuePair> params = new ArrayList<NameValuePair>();
        params.add(new BasicNameValuePair(PARAM_QUERY, sb.toString()));
        params.add(new BasicNameValuePair(PARAM_START, "0"));
        params.add(new BasicNameValuePair(PARAM_ROWS, rows));
        params.add(new BasicNameValuePair(PARAM_INDENT, VALUE_INDENT_ON));
        params.add(new BasicNameValuePair(PARAM_RETURN, VALUE_WILDCARD));
        String paramString = URLEncodedUtils.format(params, "UTF-8");

        String requestURI = uir + "?" + paramString;
        log.info("REQUEST URI= " + requestURI);
        HttpGet commandGet = new HttpGet(requestURI);

        HttpResponse response = getHttpClient().execute(commandGet);

        HttpEntity entity = response.getEntity();
        InputStream content = entity.getContent();
        Document document = null;
        try {
            document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(content);
        } catch (SAXException e) {
            log.error(e);
        } catch (ParserConfigurationException e) {
            log.error(e);
        }
        commandGet.abort();
        List<SolrDoc> docs = parseResults(document);
        return docs;
    }

    public SolrDoc retrieveDocumentFromSolrServer(String id, String solrQueryUri)
            throws XPathExpressionException, IOException, EncoderException {
        List<String> ids = new ArrayList<String>();
        ids.add(id);
        List<SolrDoc> indexedDocuments = getDocumentsById(solrQueryUri, ids);
        if (indexedDocuments.size() > 0) {
            return indexedDocuments.get(0);
        } else {
            return null;
        }
    }

    private List<SolrDoc> parseResults(Document document) throws XPathExpressionException {

        NodeList nodeList = (NodeList) XPathFactory.newInstance().newXPath()
                .evaluate("/response/result/doc", document, XPathConstants.NODESET);
        List<SolrDoc> docList = new ArrayList<SolrDoc>();
        for (int i = 0; i < nodeList.getLength(); i++) {
            Element docElement = (Element) nodeList.item(i);
            docList.add(parseDoc(docElement));

        }

        return docList;
    }

    private SolrDoc parseDoc(Element docElement) {
        SolrDoc doc = new SolrDoc();
        doc.loadFromElement(docElement, validSolrFieldNames);
        return doc;
    }

    public void setSolrSchemaPath(String path) {
        SOLR_SCHEMA_PATH = path;
    }

    private void loadSolrSchemaFields() {
        if (SOLR_SCHEMA_PATH != null && validSolrFieldNames.isEmpty()) {
            Document doc = loadSolrSchemaDocument();
            NodeList nList = doc.getElementsByTagName("copyField");
            List<String> copyDestinationFields = new ArrayList<String>();
            for (int i = 0; i < nList.getLength(); i++) {
                Node node = nList.item(i);
                String destinationField = node.getAttributes().getNamedItem("dest").getNodeValue();
                copyDestinationFields.add(destinationField);
            }
            nList = doc.getElementsByTagName("field");
            List<String> fields = new ArrayList<String>();
            for (int i = 0; i < nList.getLength(); i++) {
                Node node = nList.item(i);
                String fieldName = node.getAttributes().getNamedItem("name").getNodeValue();
                fields.add(fieldName);
            }
            fields.removeAll(copyDestinationFields);
            validSolrFieldNames = fields;
            fields.remove("_version_");
        }
    }

    private Document loadSolrSchemaDocument() {

        Document doc = null;
        File schemaFile = new File(SOLR_SCHEMA_PATH);
        if (schemaFile != null) {
            FileInputStream fis = null;
            try {
                fis = new FileInputStream(schemaFile);
            } catch (FileNotFoundException e) {
                log.error(e.getMessage(), e);
            }
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = null;
            try {
                dBuilder = dbFactory.newDocumentBuilder();
            } catch (ParserConfigurationException e) {
                log.error(e.getMessage(), e);
            }
            try {
                doc = dBuilder.parse(fis);
            } catch (SAXException e) {
                log.error(e.getMessage(), e);
            } catch (IOException e) {
                log.error(e.getMessage(), e);
            }
            try {
                if (fis != null) {
                    fis.close();
                }
            } catch (IOException e) {
                log.error(e.getMessage(), e);
            }
        }
        return doc;
    }

    public void setSolrIndexUri(String uri) {
        this.solrIndexUri = uri;
    }

    public String getSolrIndexUri() {
        return this.solrIndexUri;
    }

    public HttpClient getHttpClient() {
        return httpRequestFactory.getHttpClient();
    }
}
