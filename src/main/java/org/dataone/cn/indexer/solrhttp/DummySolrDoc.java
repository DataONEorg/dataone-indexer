package org.dataone.cn.indexer.solrhttp;

import java.util.List;

/**
 * This solr doc is a placeholder for a pid whose the object doesn't exist.
 * This document is used for a component in a resource map object, which doesn't exist in the
 * solr server.
 */
public class DummySolrDoc extends SolrDoc {
    private static final String INDICATION_FIELD = "isPlaceHolder_b";
    private static final String INDICATION_VALUE = "true";

    /**
     * Constructor
     * @param pid  the id of the document
     * @param docHoldsPermission  a doc containing the access rules that will be copied to this
     *                            dummy doc
     */
    public DummySolrDoc(String pid, SolrDoc docHoldsPermission) {
        super();
        if (pid == null || pid.isBlank()) {
            throw new IllegalArgumentException("The id used to generate a dummy solr doc can't be"
                                                   + " null or blank");
        }
        SolrElementField idField = new SolrElementField(SolrElementField.FIELD_ID, pid);
        addField(idField);
        // Set the version to -1. This makes sure that the solr doc only can be created
        // if the solr server doesn't have the id. Otherwise, it throws a version
        // conflict exception.
        // the version field
        addField(
            new SolrElementField(SolrElementField.FIELD_VERSION, SolrElementField.NEGATIVE_ONE));
        if (docHoldsPermission != null) {
            // Copy the access rules from the resource map solr doc to the new solr doc
            // The read permission field
            copyFieldAllValue(SolrElementField.FIELD_READPERMISSION, docHoldsPermission, this);
            // The write permission field
            copyFieldAllValue(SolrElementField.FIELD_WRITEPERMISSION, docHoldsPermission, this);
            // The Change permission field
            copyFieldAllValue(SolrElementField.FIELD_CHANGEPERMISSION, docHoldsPermission, this);
            // The right holder field
            copyFieldAllValue(SolrElementField.FIELD_RIGHTSHOLDER, docHoldsPermission, this);
        }
        // the indication field (isPlaceholder_b)
        addField(new SolrElementField(INDICATION_FIELD, INDICATION_VALUE));
        addField(new SolrElementField("archived", "false"));
    }

    /**
     * Get the field name which indicates that it is a dummy doc
     * @return the field name
     */
    public static String getIndicationFieldName() {
        return INDICATION_FIELD;
    }

    /**
     * Get the value of the indication field which indicates that it is a dummy doc
     * @return the field value
     */
    public static String getIndicationFieldValue() {
        return INDICATION_VALUE;
    }

    /**
     * Copy all values of the given field name in the source solr doc to the destination solr doc
     * @param fieldName  the given field name
     * @param source  the source solr doc
     * @param dest  the destination solr doc
     */
    protected static void copyFieldAllValue(String fieldName, SolrDoc source, SolrDoc dest) {
        if (fieldName != null && !fieldName.isBlank()) {
            List<String> values = source.getAllFieldValues(fieldName);
            if (values != null && !values.isEmpty()) {
                for (String value : values) {
                    dest.addField(new SolrElementField(fieldName, value));
                }
            }
        }
    }
}
