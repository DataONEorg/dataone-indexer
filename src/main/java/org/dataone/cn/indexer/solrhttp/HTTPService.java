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
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.message.BasicNameValuePair;
import org.apache.log4j.Logger;
import org.apache.solr.client.solrj.SolrServerException;
import org.dataone.configuration.Settings;
import org.dataone.service.exceptions.NotFound;
import org.dataone.service.exceptions.UnsupportedType;
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
    private static final String ARCHIVED_FIELD = "archived";
    private static final String ARCHIVED_SHOWING_VALUE = "-archived:*fake";

    final static String PARAM_START = "start";
    final static String PARAM_ROWS = "rows";
    final static String PARAM_INDENT = "indent";
    final static String VALUE_INDENT_ON = "on";
    final static String PARAM_QUERY = "q";
    final static String PARAM_RETURN = "fl";
    final static String VALUE_WILDCARD = "*";
    final static String WT = "wt";

    private static final String MAX_ROWS = "5000";
    private List<String> copyDestinationFields = null;

    private static Logger log = Logger.getLogger(HTTPService.class.getName());
    private static HttpClient httpClient;

    private String SOLR_SCHEMA_PATH = Settings.getConfiguration().getString("solr.schema.path");
    private List<String> validSolrFieldNames = new ArrayList<String>();

    static {
        RequestConfig requestConfig = RequestConfig.custom().setConnectTimeout(5 * 1000).build();
        httpClient = HttpClientBuilder.create().setDefaultRequestConfig(requestConfig).build();
    }

    public HTTPService() throws IOException, ParserConfigurationException, SAXException {
        loadSolrSchemaFields();
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

    public void sendUpdate(String uri, SolrElementAdd data, String encoding)
        throws IOException, SolrServerException {
        this.sendUpdate(uri, data, encoding, XML_CONTENT_TYPE);
    }


    public void sendUpdate(String uri, SolrElementAdd data, String encoding, String contentType)
            throws IOException, SolrServerException {
        InputStream inputStreamResponse = null;
        HttpPost post = null;
        HttpResponse response = null;
        try {
            post = new HttpPost(uri);
            post.setHeader("Content-Type", contentType);
            post.setEntity(new OutputStreamHttpEntity(data, encoding));
            response = getHttpClient().execute(post);
            HttpEntity responseEntity = response.getEntity();
            log.info("HTTPService.sendUpdate - after get the http response entity.");
            inputStreamResponse = responseEntity.getContent();
            if (response.getStatusLine().getStatusCode() != 200) {
                ByteArrayOutputStream baosResponse = new ByteArrayOutputStream();
                org.apache.commons.io.IOUtils.copy(inputStreamResponse, baosResponse);
                String error = new String(baosResponse.toByteArray());
                log.error(error);
                post.abort();
                throw new SolrServerException("unable to update solr, non 200 response code." + error);
            }
            post.abort();
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
                ByteArrayOutputStream baosResponse = new ByteArrayOutputStream();
                org.apache.commons.io.IOUtils.copy(inputStreamResponse, baosResponse);
                String error = new String(baosResponse.toByteArray());
                log.error(error);
                post.abort();
                throw new IOException("unable to update solr, non 200 response code." + error);
            }
            post.abort();
        } catch (Exception ex) {
            throw new IOException(ex.getMessage());
        } finally {
            IOUtils.closeQuietly(inputStreamResponse);
        }
    }

    public void sendSolrDelete(String pid, String solrUpdateUri) throws IOException {
        // generate request to solr server to remove index record for task.pid
        OutputStream outputStream = new ByteArrayOutputStream();
        try {
            IOUtils.write("<?xml version=\"1.1\" encoding=\"utf-8\"?>\n", outputStream,
                    CHAR_ENCODING);
            String escapedId = StringEscapeUtils.escapeXml(pid);
            IOUtils.write("<delete><id>" + escapedId + "</id></delete>", outputStream, CHAR_ENCODING);
            sendPost(solrUpdateUri, outputStream.toString());
        } catch (IOException e) {
            //e.printStackTrace();
            throw e;
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
        List<SolrDoc> docs = getDocumentsByField(uir, ids, SolrElementField.FIELD_ID, false);
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
        UnsupportedType, NotFound, SolrServerException, ParserConfigurationException, IOException,
        SAXException, XPathExpressionException {
        //Contruct a query to search for the most recent SolrDoc with the given seriesId
        StringBuilder query = new StringBuilder();
        query.append(SolrElementField.FIELD_SERIES_ID + ":" + escapeQueryChars(seriesId)
                         + (" AND -obsoletedBy:*"));
        log.debug("HTTPService.getDocumentBeySeriesId - the uir is " + uir);
        log.debug("HTTPService.getDocumentBeySeriesId - the query is " + query.toString());
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
    
    /**
     * Get a single solr doc for a given id
     * @param uir  the query url
     * @param id  the id to identify the solr doc
     * @return  the solr doc associated with the given id. Return null if nothing was found.
     * @throws XPathExpressionException
     * @throws IOException
     * @throws EncoderException
     */
    public SolrDoc getSolrDocumentById(String uir, String id) throws XPathExpressionException, 
                                                                IOException, EncoderException {
        int targetIndex = 0;
        SolrDoc doc = null;
        List<SolrDoc> list = getDocumentById(uir, id);
        if(list != null && !list.isEmpty()) {
            doc = list.get(targetIndex);
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

    private List<SolrDoc> doRequest(String uir, StringBuilder sb, String rows) throws IOException,
            ClientProtocolException, XPathExpressionException {
        List<NameValuePair> params = new ArrayList<NameValuePair>();
        params.add(new BasicNameValuePair(PARAM_QUERY, sb.toString()));
        params.add(new BasicNameValuePair(PARAM_START, "0"));
        params.add(new BasicNameValuePair(PARAM_ROWS, rows));
        params.add(new BasicNameValuePair(PARAM_INDENT, VALUE_INDENT_ON));
        params.add(new BasicNameValuePair(PARAM_RETURN, VALUE_WILDCARD));
        params.add(new BasicNameValuePair(WT, "xml"));
        //make sure archived objects being included
        params.add(new BasicNameValuePair(ARCHIVED_FIELD, ARCHIVED_SHOWING_VALUE));
        String paramString = URLEncodedUtils.format(params, "UTF-8");

        String requestURI = uir + "?" + paramString;
        log.debug("HTTPService.doRequest - REQUEST URI: " + requestURI);
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

    private void loadSolrSchemaFields() throws IOException, ParserConfigurationException, SAXException {
        if (SOLR_SCHEMA_PATH != null && validSolrFieldNames.isEmpty()) {
            Document doc = loadSolrSchemaDocument();
            NodeList nList = doc.getElementsByTagName("copyField");
            copyDestinationFields = new ArrayList<String>();
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
            //fields.remove("_version_");
        }
    }

    private Document loadSolrSchemaDocument()
        throws IOException, ParserConfigurationException, SAXException {
        Document doc = null;
        InputStream fis = null;
        if (SOLR_SCHEMA_PATH.startsWith("http://") || SOLR_SCHEMA_PATH.startsWith("https://")) {
            log.info("HTTPService.loadSolrSchemaDocument - will load the schema file from "
                         + SOLR_SCHEMA_PATH + " by http client");
            HttpGet commandGet = new HttpGet(SOLR_SCHEMA_PATH);
            HttpResponse response;
            try {
                response = getHttpClient().execute(commandGet);
                HttpEntity entity = response.getEntity();
                fis = entity.getContent();
            } catch (IOException e) {
                log.error("HTTPService.loadSolrSchemaDocument - can't get the schema doc from "
                              + SOLR_SCHEMA_PATH + " since " + e.getMessage());
                throw e;
            }
        } else {
            log.info("HTTPService.loadSolrSchemaDocument - will load the schema file from "
                         + SOLR_SCHEMA_PATH + " by http client");
            File schemaFile = new File(SOLR_SCHEMA_PATH);
            if (schemaFile != null) {
                try {
                    fis = new FileInputStream(schemaFile);
                } catch (FileNotFoundException e) {
                    log.error("HTTPService.loadSolrSchemaDocument - can't get the schema doc from "
                                  + SOLR_SCHEMA_PATH + " since " + e.getMessage());
                    throw e;
                }
            }
        }
        if (fis != null) {
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = null;
            try {
                dBuilder = dbFactory.newDocumentBuilder();
                doc = dBuilder.parse(fis);
            } catch (ParserConfigurationException e) {
                log.error("HTTPService.loadSolrSchemaDocument - can't parse the schema doc from "
                              + SOLR_SCHEMA_PATH + " since " + e.getMessage());
                throw e;
            } catch (SAXException e) {
                log.error("HTTPService.loadSolrSchemaDocument - can't parse the schema doc from "
                              + SOLR_SCHEMA_PATH + " since " + e.getMessage());
                throw e;
            } catch (IOException e) {
                log.error("HTTPService.loadSolrSchemaDocument - can't parse the schema doc from "
                              + SOLR_SCHEMA_PATH + " since " + e.getMessage());
                throw e;
            } finally {
                try {
                    if (fis != null) {
                        fis.close();
                    }
                } catch (IOException e) {
                    log.warn(
                        "HTTPService.loadSolrSchemaDocument - can't close the input stream from "
                            + SOLR_SCHEMA_PATH + " since " + e.getMessage());
                }
            }
        }
        return doc;
    }

    public static HttpClient getHttpClient() {
        return httpClient;
    }

    /**
     * Get the copy fields after parsing the solr schema
     * @return
     */
    public List<String> getSolrCopyFields() {
        return copyDestinationFields;
    }
}
