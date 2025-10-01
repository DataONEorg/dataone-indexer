package org.dataone.cn.indexer.parser.utility;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.dataone.cn.indexer.solrhttp.DummySolrDoc;
import org.dataone.cn.indexer.solrhttp.SolrDoc;
import org.dataone.cn.indexer.solrhttp.SolrElementField;

import java.util.List;

/**
 * A class to merge relationship solr fields from one solr doc to another.
 */
public class RelationshipMergeUtility {
    private static Log log = LogFactory.getLog(RelationshipMergeUtility.class);
    // It will be initialized by Spring
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
     * Merge the relationship fields from a dummy doc to a real doc. Also, it has the mechanism
     * to check the duplicated ones.
     * @param dummy  the placeholder which contains all relationship fields
     * @param realDoc  the existing doc from a solr server
     * @return a new solr which combines the real doc fields and the relationship fields from the
     * dummy doc. It will not be null.
     */
    public SolrDoc mergeRelationships(DummySolrDoc dummy, SolrDoc realDoc) {
        SolrDoc newDoc = new SolrDoc();
        if (realDoc == null) {
            if (dummy != null) {
                //Just assign the field list from dummy to the new solr doc
                newDoc.setFieldList(dummy.getFieldList());
            } else {
                // Dummy is null as well. Just return a new empty solr doc
            }
        } else {
            // The real doc is not null
            List<String> ids = realDoc.getAllFieldValues(SolrElementField.FIELD_ID);
            if (ids.size() != 1) {
                throw new IllegalArgumentException(
                    "RelationshipMergeUtilityThe.mergeRelationships - real solr document should "
                        + "have and have only one id.");
            }
            String id = ids.get(0);
            if (id == null || id.isBlank()) {
                throw new IllegalArgumentException(
                    "RelationshipMergeUtilityThe.mergeRelationships - the id of the real solr "
                        + "document should not be null or blank.");
            }
            if (dummy == null) {
                //Since the dummy is null, nothing needs merge. Just assign the real doc's field
                // list to the new solr doc
                newDoc.setFieldList(realDoc.getFieldList());
            } else {
                //Check if the realDoc matches the dummyId if the realDoc has an id
                List<String> dummyIds = dummy.getAllFieldValues(SolrElementField.FIELD_ID);
                if (dummyIds.size() == 1 && !id.equals(dummyIds.get(0))) {
                    throw new IllegalArgumentException(
                        "RelationshipMergeUtilityThe.mergeRelationships - dummy solr document should"
                            + " have the same id as the one in the real doc.");
                } else if (dummyIds.size() != 1) {
                    throw new IllegalArgumentException(
                        "RelationshipMergeUtilityThe.mergeRelationships - the dummy solr document "
                            + "should have only one id.");
                }
                // Assign the real doc's field list to the new doc. Now the newDoc preserves all
                // fields from the realDoc
                newDoc.setFieldList(realDoc.getFieldList());
                // Add all relationship fields in the dummy doc to the new doc if the new doc
                // doesn't have it yet.
                for (SolrElementField field : dummy.getFieldList()) {
                    if (relationshipFieldsToMerge.contains(field.getName())
                        && !newDoc.hasFieldWithValue(field.getName(), field.getValue())) {
                        log.debug("Merge " + field.getName() + " with value " + field.getValue());
                        newDoc.addField(field);
                    }
                }
            }
        }
        return newDoc;
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
