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

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathExpressionException;

import org.apache.commons.codec.EncoderException;
import org.dataone.cn.indexer.solrhttp.SolrDoc;
import org.xml.sax.SAXException;

/**
 * 
 * Subprocessors test xml expression with return type of boolean.  
 * canProcess function returns true subprocessor is run 
 * and can add, remove, or manipulate existing fields in document to be indexed 
 * and documents associated to the indexed
 * document.
 *
 * Date: 9/22/11
 * Time: 1:33 PM
 */
public interface IDocumentSubprocessor {

    /**Determines if subprocessor should be run on document
     * @param formatId for the object being tested
     * @return returns true if subprocessor should be run on document
     */
    public boolean canProcess(String formatId);

    /**
     * Method allows for manipulation of indexed fields that should be added to solr index
     *
     * @param identifier id of original document in Map docs
     * @param docs Documents indexed by identifiers
     * @param is original document stream
     * @return Updated solr index documents
     */
    public Map<String, SolrDoc> processDocument(String identifier, Map<String, SolrDoc> docs,
            InputStream is) throws Exception;

    public SolrDoc mergeWithIndexedDocument(SolrDoc indexDocument)
        throws IOException, EncoderException, XPathExpressionException,
        ParserConfigurationException, SAXException;
}