package org.dataone.cn.indexer.parser;

import java.util.List;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.dataone.cn.indexer.parser.utility.SpatialBoxParsingUtility;
import org.dataone.cn.indexer.solrhttp.SolrElementField;
import org.w3c.dom.Document;

public class DataCiteSpatialBoxBoundingCoordinatesSolrField extends SolrField implements ISolrField {

    private static Log logger = LogFactory
            .getLog(DataCiteSpatialBoxBoundingCoordinatesSolrField.class.getName());

    protected static SpatialBoxParsingUtility boxParsingUtility = new SpatialBoxParsingUtility();

    protected String pointXPath = null;
    protected String boxXPath = null;

    protected XPathExpression pointXPathExpression = null;
    protected XPathExpression boxXPathExpression = null;

    public DataCiteSpatialBoxBoundingCoordinatesSolrField() {
    }

    public void initExpression(XPath xpathObject) {
        if (pointXPathExpression == null) {
            try {
                pointXPathExpression = xpathObject.compile(pointXPath);
            } catch (XPathExpressionException e) {
                e.printStackTrace();
            }
        }
        if (boxXPathExpression == null) {
            try {
                boxXPathExpression = xpathObject.compile(boxXPath);
            } catch (XPathExpressionException e) {
                e.printStackTrace();
            }
        }
    }

    public List<SolrElementField> getFields(Document doc, String identifier) throws Exception {
        return boxParsingUtility.parseDataCiteBoundingCoordinates(doc, boxXPathExpression,
                pointXPathExpression);
    }

    public String getPointXPath() {
        return pointXPath;
    }

    public void setPointXPath(String pointXPath) {
        this.pointXPath = pointXPath;
    }

    public String getBoxXPath() {
        return boxXPath;
    }

    public void setBoxXPath(String boxXPath) {
        this.boxXPath = boxXPath;
    }
}
