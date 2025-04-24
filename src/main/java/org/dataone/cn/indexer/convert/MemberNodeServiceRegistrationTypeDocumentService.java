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
package org.dataone.cn.indexer.convert;

import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.LogFactory;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.commons.logging.Log;
import org.dataone.cn.indexer.XmlDocumentUtility;
import org.dataone.configuration.Settings;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

public class MemberNodeServiceRegistrationTypeDocumentService {

    private static Log log = LogFactory
            .getLog(MemberNodeServiceRegistrationTypeDocumentService.class.getName());

    private static final String SERVICE_DOC_LOCATION_URL = Settings.getConfiguration().getString(
            "dataone.mn.registration.serviceType.url");

    private HttpComponentsClientHttpRequestFactory httpRequestFactory;

    public HttpComponentsClientHttpRequestFactory getHttpRequestFactory() {
        return httpRequestFactory;
    }

    public void setHttpRequestFactory(
            HttpComponentsClientHttpRequestFactory httpRequestFactory) {
        this.httpRequestFactory = httpRequestFactory;
    }

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
        HttpClient httpClient = httpRequestFactory.getHttpClient();
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

    protected HttpComponentsClientHttpRequestFactory getHttpClientFactory() {
        return httpRequestFactory;
    }
}
