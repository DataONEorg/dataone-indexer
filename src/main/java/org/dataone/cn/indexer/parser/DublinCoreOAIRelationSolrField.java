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
package org.dataone.cn.indexer.parser;

import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.dataone.cn.indexer.solrhttp.SolrElementField;
import org.w3c.dom.Document;


/**
 * This class represents the parser to handle the "relation" element on the DublinCoreOAI document
 * @author tao
 *
 */
public class DublinCoreOAIRelationSolrField extends SolrField implements ISolrField {
    private static Log logger = LogFactory.getLog(DublinCoreOAIRelationSolrField.class);
    private static final String SERVICE_ENDPOINT = "serviceEndpoint";
    private static final String SERVICE_COUPLING = "serviceCoupling";
    private static final String COUPLING_VALUE = "mixed";
    private static final String SERVICE_DESCRIPTION = "serviceDescription";
    private static final String DESCRIPTION_VALUE = "Landing page for resource access";
    private static final String SERVICE_TITLE = "serviceTitle";
    private static final String TITLE_VALUE = "Resource Landing Page";
    private static final String SERVICE_TYPE = "serviceType";
    private static final String TYPE_VALUE = "HTTP";

    /*
     * Default constructor
     */
    public DublinCoreOAIRelationSolrField() {
        
    }
    
    /**
     * Constructor
     * @param xpath the xpath to get the value of the service_endpoint
     */
    public DublinCoreOAIRelationSolrField(String xpath) {
        this.name = SERVICE_ENDPOINT;
        this.xpath = xpath;
        this.multivalue = true;
    }
    
    /**
     * Returns one or more elements of a single SOLR record.
     */
    @Override
    public List<SolrElementField> getFields(Document doc, String identifier) throws Exception {
        List<SolrElementField> result =  super.getFields(doc, identifier);
        if(result != null && !result.isEmpty()) {
            result.add(new SolrElementField(SERVICE_COUPLING, COUPLING_VALUE));
            result.add(new SolrElementField(SERVICE_DESCRIPTION, DESCRIPTION_VALUE));
            result.add(new SolrElementField(SERVICE_TITLE, TITLE_VALUE));
            result.add(new SolrElementField(SERVICE_TYPE, TYPE_VALUE));
        }
        return result;
    }

}
