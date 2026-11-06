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

package org.dataone.cn.indexer.parser.utility;

import java.util.ArrayList;
import java.util.List;

import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.dataone.cn.indexer.convert.GeohashConverter;
import org.dataone.cn.indexer.convert.SolrLatitudeConverter;
import org.dataone.cn.indexer.convert.SolrLongitudeConverter;
import org.dataone.cn.indexer.solrhttp.SolrElementField;
import org.w3c.dom.Document;

public class SpatialBoxParsingUtility {

    private static Log logger = LogFactory.getLog(SpatialBoxParsingUtility.class.getName());

    public static final String INDEX_NORTH_PROPERTY = "northBoundCoord";
    public static final String INDEX_SOUTH_PROPERTY = "southBoundCoord";
    public static final String INDEX_EAST_PROPERTY = "eastBoundCoord";
    public static final String INDEX_WEST_PROPERTY = "westBoundCoord";

    public static final String GEOHASH_LEVEL_1_FIELD = "geohash_1";
    public static final String GEOHASH_LEVEL_2_FIELD = "geohash_2";
    public static final String GEOHASH_LEVEL_3_FIELD = "geohash_3";
    public static final String GEOHASH_LEVEL_4_FIELD = "geohash_4";
    public static final String GEOHASH_LEVEL_5_FIELD = "geohash_5";
    public static final String GEOHASH_LEVEL_6_FIELD = "geohash_6";
    public static final String GEOHASH_LEVEL_7_FIELD = "geohash_7";
    public static final String GEOHASH_LEVEL_8_FIELD = "geohash_8";
    public static final String GEOHASH_LEVEL_9_FIELD = "geohash_9";

    private static final String DC_BOX_TOKEN_DELIMITER = ";";
    private static final String DC_BOX_TOKEN_VALUE_DELIMITER = "=";

    public static final String DC_BOX_NORTH_PROPERTY = "northlimit";
    public static final String DC_BOX_SOUTH_PROPERTY = "southlimit";
    public static final String DC_BOX_EAST_PROPERTY = "eastlimit";
    public static final String DC_BOX_WEST_PROPERTY = "westlimit";

    private static final String DATA_CITE_DELIMITER = " ";

    private static final SolrLatitudeConverter latitudeConverter = new SolrLatitudeConverter(); // north,south
    private static final SolrLongitudeConverter longitudeConverter = new SolrLongitudeConverter(); // east,west
    private static final GeohashConverter geohashConverter = new GeohashConverter();

    public String extractNodeValue(Document doc, XPathExpression xPathExpression) {
        String nodeValue = null;
        try {
            nodeValue = (String) xPathExpression.evaluate(doc, XPathConstants.STRING);
            if (nodeValue != null && StringUtils.isNotEmpty(nodeValue)) {
                nodeValue = nodeValue.trim();
            }
        } catch (XPathExpressionException e) {
            logger.error("Error in XPath expression parsing for DC Spatial Box", e);
        }
        return nodeValue;
    }

    public String extractDublinCoreDirectionalValue(String nodeValue, String boxDirectionProperty) {
        String finalValue = null;

        if (nodeValue != null && StringUtils.isNotEmpty(nodeValue)) {
            nodeValue = nodeValue.trim();
            String[] tokens = StringUtils.split(nodeValue, DC_BOX_TOKEN_DELIMITER);
            for (int tokenCount = 0; tokenCount < tokens.length; tokenCount++) {
                String token = tokens[tokenCount];
                token = token.trim();
                if (token != null && StringUtils.isNotEmpty(token)
                        && StringUtils.contains(token, boxDirectionProperty)) {

                    String directionValue = StringUtils.substringAfter(token,
                            DC_BOX_TOKEN_VALUE_DELIMITER);
                    if (StringUtils.isNotEmpty(directionValue)) {
                        finalValue = directionValue.trim();
                        if (DC_BOX_NORTH_PROPERTY.equals(boxDirectionProperty)
                                || DC_BOX_SOUTH_PROPERTY.equals(boxDirectionProperty)) {
                            finalValue = latitudeConverter.convert(finalValue);
                        } else if (DC_BOX_EAST_PROPERTY.equals(boxDirectionProperty)
                                || DC_BOX_WEST_PROPERTY.equals(boxDirectionProperty)) {
                            finalValue = longitudeConverter.convert(finalValue);
                        }
                    }
                    break;
                }
            }
        }
        return finalValue;
    }

    public List<SolrElementField> parseDataCiteBoundingCoordinates(Document doc,
            XPathExpression boxExpression, XPathExpression pointExpression) {
        List<SolrElementField> fields = new ArrayList<SolrElementField>();

        String boxValue = extractNodeValue(doc, boxExpression);
        String[] tokens = StringUtils.split(boxValue, DATA_CITE_DELIMITER);
        if (boxValue != null && StringUtils.isNotEmpty(boxValue) && tokens.length == 4) {

            String directionalValue = tokens[0].trim(); // south lat
            if (directionalValue != null && !StringUtils.isEmpty(directionalValue)) {
                directionalValue = latitudeConverter.convert(directionalValue);
                fields.add(new SolrElementField(INDEX_SOUTH_PROPERTY, directionalValue));
            }

            directionalValue = tokens[1].trim(); // west long
            if (directionalValue != null && !StringUtils.isEmpty(directionalValue)) {
                directionalValue = longitudeConverter.convert(directionalValue);
                fields.add(new SolrElementField(INDEX_WEST_PROPERTY, directionalValue));
            }

            directionalValue = tokens[2].trim(); // north lat
            if (directionalValue != null && !StringUtils.isEmpty(directionalValue)) {
                directionalValue = latitudeConverter.convert(directionalValue);
                fields.add(new SolrElementField(INDEX_NORTH_PROPERTY, directionalValue));
            }

            directionalValue = tokens[3].trim(); // east long
            if (directionalValue != null && !StringUtils.isEmpty(directionalValue)) {
                directionalValue = longitudeConverter.convert(directionalValue);
                fields.add(new SolrElementField(INDEX_EAST_PROPERTY, directionalValue));
            }
        } else {
            String pointValue = extractNodeValue(doc, pointExpression);
            tokens = StringUtils.split(boxValue, DATA_CITE_DELIMITER);
            if (tokens.length == 2) {
                String directionalValue = tokens[0].trim(); // latitude
                if (directionalValue != null && !StringUtils.isEmpty(directionalValue)) {
                    directionalValue = latitudeConverter.convert(directionalValue);
                    fields.add(new SolrElementField(INDEX_NORTH_PROPERTY, directionalValue));
                    fields.add(new SolrElementField(INDEX_SOUTH_PROPERTY, directionalValue));
                }

                directionalValue = tokens[1].trim(); // longitude
                if (directionalValue != null && !StringUtils.isEmpty(directionalValue)) {
                    directionalValue = longitudeConverter.convert(directionalValue);
                    fields.add(new SolrElementField(INDEX_EAST_PROPERTY, directionalValue));
                    fields.add(new SolrElementField(INDEX_WEST_PROPERTY, directionalValue));
                }
            }
        }
        return fields;
    }

    public List<SolrElementField> parseDataCiteGeohash(Document doc, XPathExpression boxExpression,
            XPathExpression pointExpression) {
        List<SolrElementField> fields = new ArrayList<SolrElementField>();
        String boxValue = extractNodeValue(doc, boxExpression);
        String[] tokens = StringUtils.split(boxValue, DATA_CITE_DELIMITER);
        String latLong = null;
        if (boxValue != null && StringUtils.isNotEmpty(boxValue) && tokens.length == 4) {
            latLong = tokens[2].trim() + " " + tokens[0].trim() + " " + tokens[3] + " " + tokens[1];
        } else {
            String pointValue = extractNodeValue(doc, pointExpression);
            tokens = StringUtils.split(boxValue, DATA_CITE_DELIMITER);
            if (tokens.length == 2) {
                latLong = tokens[0].trim() + " " + tokens[1].trim();
            }
        }

        if (latLong != null && StringUtils.isNotEmpty(latLong)) {
            addGeohashLevelField(1, SpatialBoxParsingUtility.GEOHASH_LEVEL_1_FIELD, latLong, fields);
            addGeohashLevelField(2, SpatialBoxParsingUtility.GEOHASH_LEVEL_2_FIELD, latLong, fields);
            addGeohashLevelField(3, SpatialBoxParsingUtility.GEOHASH_LEVEL_3_FIELD, latLong, fields);
            addGeohashLevelField(4, SpatialBoxParsingUtility.GEOHASH_LEVEL_4_FIELD, latLong, fields);
            addGeohashLevelField(5, SpatialBoxParsingUtility.GEOHASH_LEVEL_5_FIELD, latLong, fields);
            addGeohashLevelField(6, SpatialBoxParsingUtility.GEOHASH_LEVEL_6_FIELD, latLong, fields);
            addGeohashLevelField(7, SpatialBoxParsingUtility.GEOHASH_LEVEL_7_FIELD, latLong, fields);
            addGeohashLevelField(8, SpatialBoxParsingUtility.GEOHASH_LEVEL_8_FIELD, latLong, fields);
            addGeohashLevelField(9, SpatialBoxParsingUtility.GEOHASH_LEVEL_9_FIELD, latLong, fields);
        }

        return fields;
    }

    public void addGeohashLevelField(int level, String indexFieldName, String latLongVal,
            List<SolrElementField> fields) {

        geohashConverter.setLength(level);
        String geohasVal = geohashConverter.convert(latLongVal);

        if (StringUtils.isNotEmpty(geohasVal)) {
            fields.add(new SolrElementField(indexFieldName, geohasVal));
        }
    }
}
