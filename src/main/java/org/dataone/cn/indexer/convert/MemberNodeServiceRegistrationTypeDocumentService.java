package org.dataone.cn.indexer.convert;

import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.log4j.Logger;
import org.dataone.cn.indexer.XmlDocumentUtility;
import org.dataone.cn.indexer.solrhttp.HTTPService;
import org.dataone.configuration.Settings;

import org.w3c.dom.Document;
import org.xml.sax.SAXException;

public class MemberNodeServiceRegistrationTypeDocumentService {

    private static Logger log = Logger
            .getLogger(MemberNodeServiceRegistrationTypeDocumentService.class.getName());

    private static final String SERVICE_DOC_LOCATION_URL = Settings.getConfiguration().getString(
            "dataone.mn.registration.serviceType.url");


    public MemberNodeServiceRegistrationTypeDocumentService() {
    }

    public Document getMemberNodeServiceRegistrationTypeDocument() {
        Document doc = null;
        InputStream stream = fetchServiceTypeDoc();
        if (stream != null) {
            try {
                doc = XmlDocumentUtility.generateXmlDocument(stream);
            } catch (SAXException e) {
                log.error("Unable to create w3c Document from input stream", e);
                e.printStackTrace();
            } finally {
                IOUtils.closeQuietly(stream);
            }
        }
        return doc;
    }

    private InputStream fetchServiceTypeDoc() {
        InputStream stream = null;
        HttpClient httpClient = HTTPService.getHttpClient();
        HttpGet get = new HttpGet(SERVICE_DOC_LOCATION_URL);
        HttpResponse response;
        try {
            response = httpClient.execute(get);
            HttpEntity entity = response.getEntity();
            stream = entity.getContent();
        } catch (ClientProtocolException e) {
            e.printStackTrace();
            log.error("Unable to fetch service type doc from: " + SERVICE_DOC_LOCATION_URL, e);
        } catch (IOException e) {
            e.printStackTrace();
            log.error("Unable to fetch service type doc from: " + SERVICE_DOC_LOCATION_URL, e);
        }
        return stream;
    }

    protected String getServiceTypeDocUrl() {
        return SERVICE_DOC_LOCATION_URL;
    }
}
