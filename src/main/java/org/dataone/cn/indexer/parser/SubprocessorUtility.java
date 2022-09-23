package org.dataone.cn.indexer.parser;

import java.io.IOException;
import java.util.List;

import javax.xml.xpath.XPathExpressionException;

import org.apache.commons.codec.EncoderException;
import org.apache.log4j.Logger;
import org.dataone.cn.indexer.solrhttp.HTTPService;
import org.dataone.cn.indexer.solrhttp.SolrDoc;
import org.dataone.cn.indexer.solrhttp.SolrElementField;
import org.dataone.configuration.Settings;
import org.springframework.beans.factory.annotation.Autowired;

public class SubprocessorUtility {

    private static Logger logger = Logger.getLogger(SubprocessorUtility.class.getName());

    @Autowired
    private HTTPService httpService = null;

    private String solrQueryUri = Settings.getConfiguration().getString("solr.query.uri");

    public SubprocessorUtility() {
    }

    public SolrDoc mergeWithIndexedDocument(SolrDoc indexDocument, List<String> fieldsToMerge)
            throws IOException, EncoderException, XPathExpressionException {

        logger.debug("about to merge indexed document with new doc to insert for pid: "
                + indexDocument.getIdentifier());
        SolrDoc solrDoc = httpService.retrieveDocumentFromSolrServer(indexDocument.getIdentifier(),
                solrQueryUri);
        if (solrDoc != null) {
            logger.debug("found existing doc to merge for pid: " + indexDocument.getIdentifier());
            for (SolrElementField field : solrDoc.getFieldList()) {
                if (fieldsToMerge.contains(field.getName())
                        && !indexDocument.hasFieldWithValue(field.getName(), field.getValue())) {
                    indexDocument.addField(field);
                    logger.debug("merging field: " + field.getName() + " with value: "
                            + field.getValue());
                }
            }
        }
        return indexDocument;
    }
}
