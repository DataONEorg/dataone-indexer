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
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.dataone.service.types.v1.SystemMetadata;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;

/**
 * User: Porter Date: 7/26/11 Time: 4:04 PM
 * 
 * @see SolrElementAdd
 */
public class SolrDoc {
    public static final char[] ELEMENT_DOC_OPEN = "<doc>".toCharArray();
    public static final char[] ELEMENT_DOC_CLOSE = "</doc>".toCharArray();
	private static final String DYNAMIC_FIELD_SUFFIX = "_sm";
    private List<SolrElementField> fieldList = new ArrayList<SolrElementField>();

    // private Boolean resourceMap = null;
    private String identifier;
    private String seriesId;

    private boolean merged = false;

    public SolrDoc() {

    }

    public SolrDoc(List<SolrElementField> fieldList) {
        this.fieldList = fieldList;
    }

    public List<SolrElementField> getFieldList() {
        return fieldList;
    }

    public void setFieldList(List<SolrElementField> fieldList) {
        this.fieldList = fieldList;
    }

    public void serialize(OutputStream outputStream, String encoding) throws IOException {

        IOUtils.write(ELEMENT_DOC_OPEN, outputStream, encoding);

        for (SolrElementField field : getFieldList()) {
            field.serialize(outputStream, encoding);
        }
        IOUtils.write(ELEMENT_DOC_CLOSE, outputStream, encoding);

    }

    public boolean hasField(String fieldName) {
        for (SolrElementField field : fieldList) {
            if (field.getName().equals(fieldName)) {
                return true;
            }
        }
        return false;
    }

    public boolean hasFieldWithValue(String fieldName, String value) {
        for (SolrElementField field : fieldList) {
            if (field.getName() != null && field.getName().equals(fieldName)) {
                if (field.getValue().equals(value)) {
                    return true;
                }
            }
        }
        return false;
    }

    public SolrElementField getField(String fieldName) {
        SolrElementField searchField = null;
        for (SolrElementField field : fieldList) {
            if (field.getName().equals(fieldName)) {
                searchField = field;
                break;
            }
        }
        return searchField;
    }

    public void loadFromElement(Element docElement, List<String> validFields) {
        fieldList = new ArrayList<SolrElementField>();
        NodeList values = docElement.getChildNodes();
        for (int i = 0; i < values.getLength(); i++) {
            Node n = (Node) values.item(i);
            if (n instanceof Text) {
                continue;
            }
            Element elementField = (Element) n;
            String tagName = elementField.getTagName();
            String fieldName = elementField.getAttribute("name");
            if (validFields != null) {
            	// ensure valid field, or a dynamic field matching the pattern
            	// NOTE: there are many kinds of dynamic fields, only handling one for now.
            	if (!validFields.contains(fieldName) && !fieldName.endsWith(DYNAMIC_FIELD_SUFFIX)) {
            		continue;
            	}
            }
            if (tagName.equals("arr")) {
                NodeList arrayValues = elementField.getChildNodes();
                for (int j = 0; j < arrayValues.getLength(); j++) {
                    Node nv = arrayValues.item(j);
                    if (nv instanceof Text) {
                        continue;
                    }
                    Node arrayValue = (Node) nv;
                    String valueString = arrayValue.getTextContent();
                    fieldList.add(new SolrElementField(fieldName, valueString));
                }
            } else {
                // If not adding from an arr element, then be sure only to add
                // one
                // instance of the field.
                if (!hasField(fieldName)) {
                    String valueString = elementField.getTextContent();
                    fieldList.add(new SolrElementField(fieldName, valueString));
                }
            }
        }
    }

    public String getIdentifier() {
        if (identifier == null) {
            identifier = getFirstFieldValue(SolrElementField.FIELD_ID);
        }
        return identifier;
    }

    public String getSeriesId() {
        if (seriesId == null) {
            seriesId = getFirstFieldValue(SolrElementField.FIELD_SERIES_ID);
        }
        return seriesId;
    }

    public void addField(SolrElementField field) {
        getFieldList().add(field);
    }

    public void removeField(SolrElementField fieldToRemove) {
        if (fieldToRemove == null || fieldToRemove.getName() == null) {
            return;
        }
        SolrElementField temp = null;
        for (SolrElementField field : fieldList) {
            if (field.getName().equals(fieldToRemove.getName())) {
                temp = field;
                break;
            }
        }
        fieldList.remove(temp);
    }

    public void removeAllFields(String fieldName) {
        List<SolrElementField> fieldsToRemove = new ArrayList<SolrElementField>();
        for (SolrElementField field : fieldList) {
            if (field.getName().equals(fieldName)) {
                fieldsToRemove.add(field);
            }
        }
        fieldList.removeAll(fieldsToRemove);
    }

    public void removeFieldsWithValue(String name, String value) {
        List<SolrElementField> fieldsToRemove = new ArrayList<SolrElementField>();
        for (SolrElementField field : fieldList) {
            if (field.getName().equals(name) && field.getValue().equals(value)) {
                fieldsToRemove.add(field);
            }
        }
        fieldList.removeAll(fieldsToRemove);
    }

    public void removeOneFieldWithValue(String name, String value) {
        List<SolrElementField> fieldsToRemove = new ArrayList<SolrElementField>();
        for (SolrElementField field : fieldList) {
            if (field.getName().equals(name) && field.getValue().equals(value)) {
                fieldsToRemove.add(field);
                break;
            }
        }
        fieldList.removeAll(fieldsToRemove);
    }

    /**
     * 
     * @param fieldName
     * @return returns first value for given field name
     */
    public String getFirstFieldValue(String fieldName) {
        for (SolrElementField field : fieldList) {
            if (field.getName().equals(fieldName)) {
                return field.getValue();
            }
        }
        return null;

    }

    public List<String> getAllFieldValues(String fieldName) {
        List<String> allValues = new ArrayList<String>();
        for (SolrElementField field : fieldList) {
            if (field.getName().equals(fieldName)) {
                allValues.add(field.getValue());
            }
        }
        return allValues;
    }

    public void updateOrAddField(String fieldName, String fieldValue) {
        for (SolrElementField solrElementField : fieldList) {
            if (solrElementField.getName().equals(fieldName)) {
                solrElementField.setValue(fieldValue);
                return;
            }
        }
        SolrElementField field = new SolrElementField(fieldName, fieldValue);
        fieldList.add(field);
    }

    public boolean isMerged() {
        return merged;
    }

    public void setMerged(boolean merged) {
        this.merged = merged;
    }

    public static boolean visibleInIndex(SystemMetadata smd) {
        if (smd == null) {
            return false;
        }
        return !(smd.getArchived() != null && smd.getArchived().booleanValue());
    }
}
