package org.dataone.cn.indexer.parser.utility;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.dataone.cn.indexer.solrhttp.SolrDoc;
import org.dataone.cn.indexer.solrhttp.SolrElementField;

import java.util.List;
import java.util.Map;

/**
 * A class to merge relationship solr fields from one solr doc to another.
 */
public class RelationshipMergeUtility {
    private static Log log = LogFactory.getLog(RelationshipMergeUtility.class);
    private List<String> relationshipFieldsToMerge = null;


    /**
     * Method the relationship fields and version from the "from" to the "to". The version of
     * the "to" will be overwritten.
     * @param from  the source solr doc
     * @param to  the destination solr doc
     */
    public void merge(SolrDoc from, SolrDoc to) {
        if (to == null) {
            throw new IllegalArgumentException("The solr doc receiving relationship fields should"
                                                   + " not be null");
        }
        if (from != null && from.getFieldList() != null) {
            for (SolrElementField field : from.getFieldList()) {
                if (field.getName() != null && relationshipFieldsToMerge.contains(field.getName())
                    && field.getValue() != null && !field.getValue().isBlank()
                    && !to.hasFieldWithValue(field.getName(), field.getValue())) {
                    log.debug("Merge the relationship field " + field.getName() + " with value "
                                  + field.getValue() + " to the destination.");
                    to.addField(new SolrElementField(field.getName(), field.getValue()));
                } else if (field.getName() != null && field.getName()
                    .equals(SolrElementField.FIELD_VERSION)) {
                    log.debug("Merge the relationship field " + field.getName() + " with value "
                                  + field.getValue() + " to the destination.");
                    to.removeAllFields(field.getName());
                    to.addField(field);
                }
            }
            to.setMerged(true);
        }
    }

    /**
     * This method is for Spring to populate the class variable
     * @return  the relationshipFieldsToMerge
     */
    public List<String> getRelationshipFieldsToMerge() {
        return relationshipFieldsToMerge;
    }

    /**
     * This method is for Spring to populate the class variable
     * @param relationshipFieldsToMerge  the list will be set
     */
    public void setRelationshipFieldsToMerge(List<String> relationshipFieldsToMerge) {
        this.relationshipFieldsToMerge = relationshipFieldsToMerge;
    }
}
