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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.dataone.cn.indexer.parser.utility.TemporalPeriodParsingUtility;
import org.dataone.cn.indexer.solrhttp.SolrElementField;
import org.w3c.dom.Document;

/**
 * Used to convert the text inside of a dcterms:temporal with xsi:type="dcterms:Period" 
 * into {@link SolrElementField}s for beginDate and endDate
 * Example input: 
 * <pre>
 * {@code
 * <dcterms:temporal xsi:type="dcterms:Period">
 *      start=1999-09-25T14:20+10:00; end=1999-09-25T16:40+10:00; scheme=W3C-DTF;
 * </dcterms:temporal>
 * }
 * </pre>
 * 
 * Uses {@link TemporalPeriodParsingUtility}
 * @author Andrei
 */
public class TemporalPeriodSolrField extends SolrField implements ISolrField {

    private static Log log = LogFactory.getLog(TemporalPeriodSolrField.class);
    
    public static final String BEGIN_FIELD_NAME = "beginDate";
    public static final String END_FIELD_NAME = "endDate";
    
    private static TemporalPeriodParsingUtility temporalParsingUtil = new TemporalPeriodParsingUtility();
    
    
    public TemporalPeriodSolrField() {
    }

    public TemporalPeriodSolrField(String xpath) {
        this.xpath = xpath;
    }
    
    @Override
    public List<SolrElementField> getFields(Document doc, String identifier) throws Exception {
        List<SolrElementField> fields = new ArrayList<SolrElementField>();
        
        String textValue = temporalParsingUtil.extractTextValue(doc, this.xPathExpression);
        
        String scheme = temporalParsingUtil.getScheme(textValue);
        if (scheme != null && !scheme.equalsIgnoreCase(TemporalPeriodParsingUtility.W3C_DTF_SCHEME))
            log.warn("Scheme \"" + scheme + "\" may not be supported for pid " + identifier + ". "
                    + "Currently supporting: " + TemporalPeriodParsingUtility.W3C_DTF_SCHEME);
        
        String startDate = temporalParsingUtil.getFormattedStartDate(textValue, scheme);
        String endDate = temporalParsingUtil.getFormattedEndDate(textValue, scheme);
        
        if (startDate == null && endDate == null) {
            log.error("Couldn't extract 'start' or 'end' date for pid " + identifier + ". "
                    + "Temporal pattern of type period needs to contain at least one of these. "
                    + "Value was: " + textValue);
            return fields;
        }
        
        // if period only specifies the start OR end, we set that to both
        if (startDate != null && endDate == null)
            endDate = startDate;
        if (endDate != null && startDate == null)
            startDate = endDate;
        
        SolrElementField beginField = new SolrElementField();
        beginField.setName(BEGIN_FIELD_NAME);
        beginField.setValue(startDate);
        fields.add(beginField);
        
        SolrElementField endField = new SolrElementField();
        endField.setName(END_FIELD_NAME);
        endField.setValue(endDate);
        fields.add(endField);

        return fields;
    }
}
