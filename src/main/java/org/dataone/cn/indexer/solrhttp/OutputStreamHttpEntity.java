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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.apache.commons.logging.LogFactory;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.message.BasicHeader;
import org.apache.commons.logging.Log;

/**
  * User: Porter
 * Date: 8/1/11
 * Time: 12:04 PM
 */
/**Serializes SolrElementAdd command and child documents to HTTP stream
 *
 */

public class OutputStreamHttpEntity implements HttpEntity {

    private static Log log = LogFactory.getLog(OutputStreamHttpEntity.class);

    private Header contentType;

    private SolrElementAdd add = null;
    private String encoding = "UTF-8";

    public OutputStreamHttpEntity(SolrElementAdd add, String encoding) {
        this.add = add;
        this.encoding = encoding;
    }

    public boolean isRepeatable() {
        return false;
    }

    public boolean isChunked() {
        return false;
    }

    /**Content length is unknown always returns -1
     *
     * @return -1
     */
    public long getContentLength() {
        return -1;
    }

    /**Default ContentType is UTF-8
     * @return
     */
    public Header getContentType() {
        return contentType;
    }

    /**Content-Type: text/xml; charset=<ContentType>
     *
     * @return
     */
    public Header getContentEncoding() {
        return new BasicHeader("Content-Type", "text/xml; charset=" + encoding + "");
    }

    public InputStream getContent() throws IOException, IllegalStateException {
        throw new Error("MethodNotImplemented");
    }

    public void writeTo(OutputStream outputStream) throws IOException {
        log.info("Creating HTTP Output Stream for " + add.getDocList().size() + " docs: ");
        add.serialize(outputStream, encoding);
        outputStream.flush();
        outputStream.close();
        log.info("OutputStreamHttpEntity.writeTo - after put the the solr doc into the http output stream.");
    }

    public boolean isStreaming() {
        return false;
    }

    public void consumeContent() throws IOException {
    }

    public void setContentType(Header contentType) {
        this.contentType = contentType;
    }

}
