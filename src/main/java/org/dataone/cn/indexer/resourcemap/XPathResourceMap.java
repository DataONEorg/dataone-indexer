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

package org.dataone.cn.indexer.resourcemap;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.xml.namespace.NamespaceContext;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.dataone.cn.indexer.XMLNamespace;
import org.dataone.cn.indexer.XMLNamespaceConfig;
import org.dataone.cn.indexer.XmlDocumentUtility;
import org.dataone.cn.indexer.solrhttp.SolrDoc;
import org.dataone.cn.indexer.solrhttp.SolrElementField;
import org.dataone.service.types.v1.Identifier;
import org.dspace.foresite.OREParserException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Created by IntelliJ IDEA.
 * User: Porter
 * Date: 8/26/11
 * Time: 12:22 PM
 */

/**
 * ResourceMap parses resource map and generate index information
 * 
 */
public class XPathResourceMap implements ResourceMap {
    private Set<ResourceEntry> mappedReferences;
    private Set<String> contains = null;

    private Document doc = null;

    private String identifier = null;

    private XPathFactory factory = null;

    private IndexVisibilityDelegate indexVisibilityDelegate = new IndexVisibilityDelegateImpl();

    public static String XPATH_RESOURCE_MAP_IDENTIFIER = "/rdf:RDF/rdf:Description/rdf:type[@rdf:resource='http://www.openarchives.org/ore/terms/ResourceMap']/parent::*/dcterms:identifier/text()";
    static private NamespaceContext nameSpaceContext;

    public static final String NS_DCTERMS = "http://purl.org/dc/terms/";
    public static final String NS_CITO = "http://purl.org/spar/cito/";
    public static final String NS_RDF = "http://www.w3.org/1999/02/22-rdf-syntax-ns#";
    public static final String ATTRIBUTE_ABOUT = "about";
    public static final String ATTRIBUTE_RESOURCE = "resource";

    public static final String TAG_DOCUMENTS = "documents";
    public static final String TAG_IDENTIFIER = "identifier";
    public static final String TAG_IS_DOCUMENTED_BY = "isDocumentedBy";
    private HashMap<String, String> descriptionURIToIdentifierMap = null;

    public XPathResourceMap(Document doc) throws OREParserException {
        init(doc);
    }

    public XPathResourceMap(Document doc, IndexVisibilityDelegate ivd) throws OREParserException {
        if (ivd != null) {
            this.indexVisibilityDelegate = ivd;
        }
        init(doc);
    }

    public XPathResourceMap(String objectFilePath) throws OREParserException {
        try {
            Document doc = XmlDocumentUtility.loadDocument(objectFilePath);
            init(doc);
        } catch (Exception e) {
            throw new OREParserException(e);
        }
    }

    public XPathResourceMap(String filePath, IndexVisibilityDelegate ivd) throws OREParserException {
        if (ivd != null) {
            this.indexVisibilityDelegate = ivd;
        }
        try {
            Document doc = XmlDocumentUtility.loadDocument(filePath);
            init(doc);
        } catch (Exception e) {
            throw new OREParserException(e);
        }
    }

    private void init(Document doc) throws OREParserException {
        this.doc = doc;

        try {
            setIdentifier(parseIdentifier(doc));
        } catch (XPathExpressionException e) {
            throw new OREParserException(e);
        }
        mappedReferences = getMappedReferences();
        contains = new HashSet<String>();
        contains.addAll(descriptionURIToIdentifierMap.values());
    }

    private String parseIdentifier(Document doc) throws XPathExpressionException {
        factory = XPathFactory.newInstance();
        XPath xPath = factory.newXPath();
        xPath.setNamespaceContext(getNameSpaceContext());
        XPathExpression resourceMapIdentifierXpath = xPath.compile(XPATH_RESOURCE_MAP_IDENTIFIER);
        String resourceMapIdentifier = (String) resourceMapIdentifierXpath.evaluate(doc,
                XPathConstants.STRING);
        return resourceMapIdentifier;
    }

    /* (non-Javadoc)
     * @see org.dataone.cn.indexer.resourcemap.ResourceMap#getMappedReferences()
     */
    @Override
    public Set<ResourceEntry> getMappedReferences() {
        if (mappedReferences == null || mappedReferences.isEmpty()) {
            XPathFactory factory = XPathFactory.newInstance();
            XPath xp = factory.newXPath();
            xp.setNamespaceContext(getNameSpaceContext());
            Set<ResourceEntry> resourceEntries = new HashSet<ResourceEntry>();
            try {

                xp.setNamespaceContext(getNameSpaceContext());
                XPathExpression identifierDescriptionsExpression = xp
                        .compile("/rdf:RDF/rdf:Description[dcterms:identifier]");
                NodeList descriptions = (NodeList) identifierDescriptionsExpression.evaluate(doc,
                        XPathConstants.NODESET);
                parseResourceIdentifierMap(descriptions);

                for (int i = 0; i < descriptions.getLength(); i++) {
                    Element descriptionElement = (Element) descriptions.item(i);
                    XPathResourceEntry resourceEntry = new XPathResourceEntry(descriptionElement,
                            this, this.indexVisibilityDelegate);
                    Identifier pid = new Identifier();
                    pid.setValue(resourceEntry.getIdentifier());
                    // if the document does not have system metadata yet, cannot check visibility.  include in list of ids.
                    // if document does exist, it must be visible in the index to be included.
                    if (!indexVisibilityDelegate.documentExists(pid)
                            || indexVisibilityDelegate.isDocumentVisible(pid)) {
                        if (resourceEntry.getIdentifier().equals(this.getIdentifier()) == false) {
                            resourceEntries.add(resourceEntry);
                        }
                    }
                }

            } catch (XPathExpressionException e) {
                e.printStackTrace();
            }
            mappedReferences = resourceEntries;
        }

        return mappedReferences;
    }

    private void parseResourceIdentifierMap(NodeList descriptions) {
        descriptionURIToIdentifierMap = new HashMap<String, String>();
        for (int i = 0; i < descriptions.getLength(); i++) {
            Element descriptionElement = (Element) descriptions.item(i);
            String uri = descriptionElement.getAttributeNS(NS_RDF, ATTRIBUTE_ABOUT);
            Node identifierElement = (Element) descriptionElement.getElementsByTagNameNS(
                    NS_DCTERMS, TAG_IDENTIFIER).item(0);
            String identifierText = identifierElement.getTextContent();

            if (!identifierText.equals(getIdentifier())) {
                descriptionURIToIdentifierMap.put(uri, identifierText);
            }
        }
    }

    /* (non-Javadoc)
     * @see org.dataone.cn.indexer.resourcemap.ResourceMap#getContains()
     */
    @Override
    public Set<String> getContains() {
        return contains;
    }

    /* (non-Javadoc)
     * @see org.dataone.cn.indexer.resourcemap.ResourceMap#setContains(java.util.Set)
     */
    void setContains(Set<String> contains) {
        this.contains = contains;
    }

    /* (non-Javadoc)
     * @see org.dataone.cn.indexer.resourcemap.ResourceMap#setMappedReferences(java.util.Set)
     */
    void setMappedReferences(Set<ResourceEntry> mappedReferences) {
        this.mappedReferences = mappedReferences;
    }

    public static NamespaceContext getNameSpaceContext() {
        if (nameSpaceContext == null) {
            List<XMLNamespace> namespaces = new ArrayList<XMLNamespace>();
            namespaces.add(new XMLNamespace("cito", "http://purl.org/spar/cito/"));
            namespaces.add(new XMLNamespace("dc", "http://purl.org/dc/elements/1.1/"));
            namespaces.add(new XMLNamespace("dcterms", "http://purl.org/dc/terms/"));
            namespaces.add(new XMLNamespace("foaf", "http://xmlns.com/foaf/0.1/"));
            namespaces.add(new XMLNamespace("ore", "http://www.openarchives.org/ore/terms/"));
            namespaces.add(new XMLNamespace("rdf", "http://www.w3.org/1999/02/22-rdf-syntax-ns#"));
            namespaces.add(new XMLNamespace("rdfs1", "http://www.w3.org/2001/01/rdf-schema#"));
            nameSpaceContext = new XMLNamespaceConfig(namespaces);
        }
        return nameSpaceContext;
    }

    /* (non-Javadoc)
     * @see org.dataone.cn.indexer.resourcemap.ResourceMap#getAllDocumentIDs()
     */
    @Override
    public List<String> getAllDocumentIDs() {
        List<String> ids = new ArrayList<String>();
        ids.add(getIdentifier());
        for (ResourceEntry resourceEntry : getMappedReferences()) {
            ids.add(resourceEntry.getIdentifier());
        }
        return ids;
    }

    /* (non-Javadoc)
     * @see org.dataone.cn.indexer.resourcemap.ResourceMap#mergeIndexedDocuments(java.util.List)
     */
    @Override
    public List<SolrDoc> mergeIndexedDocuments(List<SolrDoc> docs) {

        List<SolrDoc> mergedDocuments = new ArrayList<SolrDoc>();
        for (ResourceEntry mappedReference : mappedReferences) {
            SolrDoc mergeDocument = null;
            for (SolrDoc doc : docs) {
                if (doc.getIdentifier().equals(mappedReference.getIdentifier())) {
                    mergeDocument = doc;
                    break;
                }
            }
            if (mergeDocument != null) {
                mergedDocuments.add(mergeMappedReference(mappedReference, mergeDocument));
            }
        }

        return mergedDocuments;
    }

    private SolrDoc mergeMappedReference(ResourceEntry resourceEntry, SolrDoc mergeDocument) {
        if (!mergeDocument.hasField(SolrElementField.FIELD_ID)) {
            mergeDocument.addField(new SolrElementField(SolrElementField.FIELD_ID, resourceEntry
                    .getIdentifier()));
        }

        for (String documentedBy : resourceEntry.getDocumentedBy()) {
            if (!mergeDocument.hasFieldWithValue(SolrElementField.FIELD_ISDOCUMENTEDBY,
                    documentedBy)) {
                mergeDocument.addField(new SolrElementField(SolrElementField.FIELD_ISDOCUMENTEDBY,
                        documentedBy));
            }
        }
        for (String documents : resourceEntry.getDocuments()) {
            if (!mergeDocument.hasFieldWithValue(SolrElementField.FIELD_DOCUMENTS, documents)) {
                mergeDocument.addField(new SolrElementField(SolrElementField.FIELD_DOCUMENTS,
                        documents));
            }
        }
        for (String resourceMap : resourceEntry.getResourceMaps()) {
            if (!mergeDocument.hasFieldWithValue(SolrElementField.FIELD_RESOURCEMAP, resourceMap)) {
                mergeDocument.addField(new SolrElementField(SolrElementField.FIELD_RESOURCEMAP,
                        resourceMap));
            }
        }
        mergeDocument.setMerged(true);

        return mergeDocument;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("ResourceMap: ").append(getIdentifier()).append("\n");

        for (String contain : contains) {
            sb.append("\tContains: ").append(contain).append("\n");
        }
        for (ResourceEntry mappedReference : mappedReferences) {

            sb.append(mappedReference.toString());
        }

        return sb.toString();
    }

    /* (non-Javadoc)
     * @see org.dataone.cn.indexer.resourcemap.ResourceMap#getIdentifierFromResource(java.lang.String)
     */
    String getIdentifierFromResource(String resourceURI) {
        String mappedIdentifier = descriptionURIToIdentifierMap.get(resourceURI);
        return mappedIdentifier;
    }

    /* (non-Javadoc)
     * @see org.dataone.cn.indexer.resourcemap.ResourceMap#getIdentifier()
     */
    @Override
    public String getIdentifier() {
        return identifier;
    }

    /* (non-Javadoc)
     * @see org.dataone.cn.indexer.resourcemap.ResourceMap#setIdentifier(java.lang.String)
     */
    void setIdentifier(String identifier) {
        this.identifier = identifier;
    }

    private static final String RESOURCE_MAP_FORMAT = "http://www.openarchives.org/ore/terms";

    public static boolean representsResourceMap(String formatId) {
        return RESOURCE_MAP_FORMAT.equals(formatId);
    }
}
