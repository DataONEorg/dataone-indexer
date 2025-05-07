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

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.dataone.cn.indexer.parser.utility.SpatialBoxParsingUtility;
import org.dataone.cn.indexer.solrhttp.SolrElementField;
import org.w3c.dom.Document;

public class DublinCoreSpatialBoxGeohashSolrField extends SolrField implements ISolrField {

    private static Log logger = LogFactory.getLog(DublinCoreSpatialBoxGeohashSolrField.class
            .getName());

    private static SpatialBoxParsingUtility boxParsingUtility = new SpatialBoxParsingUtility();

    public DublinCoreSpatialBoxGeohashSolrField() {
    }

    public DublinCoreSpatialBoxGeohashSolrField(String xpath) {
        this.xpath = xpath;
    }

    public List<SolrElementField> getFields(Document doc, String identifier) throws Exception {
        return parseBox(doc);
    }

    private List<SolrElementField> parseBox(Document doc) {
        List<SolrElementField> fields = new ArrayList<SolrElementField>();

        String nodeValue = boxParsingUtility.extractNodeValue(doc, this.xPathExpression);

        String northValue = boxParsingUtility.extractDublinCoreDirectionalValue(nodeValue,
                SpatialBoxParsingUtility.DC_BOX_NORTH_PROPERTY);
        String southValue = boxParsingUtility.extractDublinCoreDirectionalValue(nodeValue,
                SpatialBoxParsingUtility.DC_BOX_SOUTH_PROPERTY);
        String eastValue = boxParsingUtility.extractDublinCoreDirectionalValue(nodeValue,
                SpatialBoxParsingUtility.DC_BOX_EAST_PROPERTY);
        String westValue = boxParsingUtility.extractDublinCoreDirectionalValue(nodeValue,
                SpatialBoxParsingUtility.DC_BOX_WEST_PROPERTY);

        String latLongValue = northValue + " " + southValue + " " + eastValue + " " + westValue;

        if (latLongValue != null && StringUtils.isNotEmpty(latLongValue)) {
            latLongValue = latLongValue.trim();
            boxParsingUtility.addGeohashLevelField(1,
                    SpatialBoxParsingUtility.GEOHASH_LEVEL_1_FIELD, latLongValue, fields);
            boxParsingUtility.addGeohashLevelField(2,
                    SpatialBoxParsingUtility.GEOHASH_LEVEL_2_FIELD, latLongValue, fields);
            boxParsingUtility.addGeohashLevelField(3,
                    SpatialBoxParsingUtility.GEOHASH_LEVEL_3_FIELD, latLongValue, fields);
            boxParsingUtility.addGeohashLevelField(4,
                    SpatialBoxParsingUtility.GEOHASH_LEVEL_4_FIELD, latLongValue, fields);
            boxParsingUtility.addGeohashLevelField(5,
                    SpatialBoxParsingUtility.GEOHASH_LEVEL_5_FIELD, latLongValue, fields);
            boxParsingUtility.addGeohashLevelField(6,
                    SpatialBoxParsingUtility.GEOHASH_LEVEL_6_FIELD, latLongValue, fields);
            boxParsingUtility.addGeohashLevelField(7,
                    SpatialBoxParsingUtility.GEOHASH_LEVEL_7_FIELD, latLongValue, fields);
            boxParsingUtility.addGeohashLevelField(8,
                    SpatialBoxParsingUtility.GEOHASH_LEVEL_8_FIELD, latLongValue, fields);
            boxParsingUtility.addGeohashLevelField(9,
                    SpatialBoxParsingUtility.GEOHASH_LEVEL_9_FIELD, latLongValue, fields);
        }

        return fields;
    }

}
