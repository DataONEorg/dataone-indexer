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

import java.io.CharArrayWriter;
import java.io.IOException;
import java.io.OutputStream;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * User: Porter Date: 7/25/11 Time: 4:14 PM Contains constants FILED_XXXX for
 * DataOne system metdata solr index fields.
 * 
 * @see SolrElementAdd
 */
public class SolrElementField {

    private static Log log = LogFactory.getLog(SolrElementField.class);

    public static final String FIELD_ID = "id";
    public static final String FIELD_SERIES_ID = "seriesId";
    public static final String FIELD_OBJECTFORMAT = "formatId";
    public static final String FIELD_OBJECTFORMATTYPE = "formatType";
    public static final String FIELD_SIZE = "size";
    public static final String FIELD_CHECKSUM = "checksum";
    public static final String FIELD_CHECKSUMALGORITHM = "checksumAlgorithm";
    public static final String FIELD_SUBMITTER = "submitter";
    public static final String FIELD_RIGHTSHOLDER = "rightsHolder";
    public static final String FIELD_REP_ALLOWED = "replicationAllowed";
    public static final String FIELD_N_REPLICAS = "numberReplicas";
    public static final String FIELD_PREF_REP_MN = "preferredRelicationNM";
    public static final String FIELD_BLOCKED_REP_MN = "blockedReplicationMN";
    public static final String FIELD_OBSOLETES = "obsoletes";
    public static final String FIELD_OBSOLETED_BY = "obsoletedBy";
    public static final String FIELD_DATEUPLOADED = "dateUploaded";
    public static final String FIELD_DATEMODIFIED = "dateModified";
    public static final String FIELD_ORIGIN_MN = "datasource";
    public static final String FIELD_AUTH_MN = "authoritativeMN";
    public static final String FIELD_REPLICA_MN = "replicaMN";
    public static final String FIELD_RESOURCEMAP = "resourceMap";
    public static final String FIELD_DOCUMENTS = "documents";
    public static final String FIELD_ISDOCUMENTEDBY = "isDocumentedBy";
    public static final String FIELD_READPERMISSION = "readPermission";
    public static final String FIELD_WRITEPERMISSION = "writePermission";
    public static final String FIELD_EXECUTEPERMISSION = "executePermission";
    public static final String FIELD_CHANGEPERMISSION = "changePermission";
    public static final String FIELD_ISPUBLIC = "isPublic";
    public static final String FIELD_DECADE = "decade";
    public static final String FIELD_BEGIN_DATE = "beginDate";
    public static final String FIELD_END_DATE = "endDate";
    public static final String FIELD_VERSION = "_version_";

    public static final char[] ELEMENT_FIELD_OPEN = "<field ".toCharArray();
    public static final char[] ELEMENT_FIELD_CLOSE = "</field>".toCharArray();
    public static final String ATTRIBUTE_NAME = "name";
    public static final String NEGATIVE_ONE = "-1";

    private String name = null;
    private String value = null;
    private boolean escapeXML = true;

    public SolrElementField() {
    }

    public SolrElementField(String name, String value) {
        this.name = name;
        this.value = value;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public void serialize(OutputStream outputStream, String encoding) throws IOException {
        if (value == null || value.equals("")) {
            return;
        }

        IOUtils.write(ELEMENT_FIELD_OPEN, outputStream, encoding);

        java.io.CharArrayWriter cw = new CharArrayWriter();

        cw.append(ATTRIBUTE_NAME);
        cw.append("=\"");
        cw.append(name);
        cw.append("\">");
        IOUtils.write(cw.toCharArray(), outputStream, encoding);

        char[] toWrite = StringEscapeUtils.escapeXml11(value).toCharArray();
        IOUtils.write(toWrite, outputStream, encoding);

        IOUtils.write(ELEMENT_FIELD_CLOSE, outputStream, encoding);

        outputStream.flush();

        if (log.isDebugEnabled()) {
            log.debug("SolrElementField serializing field: " + name + " with value: " + value);
        }
    }
}
