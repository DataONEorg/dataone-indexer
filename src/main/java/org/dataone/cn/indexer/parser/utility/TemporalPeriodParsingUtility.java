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

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.logging.Log;
import org.dataone.cn.indexer.convert.IConverter;
import org.dataone.cn.indexer.convert.SolrDateConverter;
import org.dataone.cn.indexer.parser.TemporalPeriodSolrField;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;
import org.w3c.dom.Document;

/**
 * Utility class for extracting values from a dcterms:temporal with xsi:type="dcterms:Period"
 * and for formatting the date values contained within to a solr consumable format.
 * Example input:
 * <pre>
 * {@code
 * <dcterms:temporal xsi:type="dcterms:Period">
 *      start=1999-09-25T14:20+10:00; end=1999-09-25T16:40+10:00; scheme=W3C-DTF;
 * </dcterms:temporal>
 * }
 * </pre>
 * 
 * Supports temporal strings following the W3C-DTF scheme, meaning the patterns specified here: 
 * <a href="https://www.w3.org/TR/NOTE-datetime">https://www.w3.org/TR/NOTE-datetime</a>
 * </br>
 * Actually supports a somewhat larger superset of those; see {@link ISODateTimeFormat#dateTimeParser}
 * 
 * @author Andrei
 */
public class TemporalPeriodParsingUtility {

    private static Log log = LogFactory.getLog(TemporalPeriodSolrField.class);
    
    public static final String START_FIELD = "start";
    public static final String END_FIELD = "end";
    public static final String SCHEME_FIELD = "scheme";
    public static final String W3C_DTF_SCHEME = "W3C-DTF";
    
    private static final Map<String,DateTimeFormatter> FORMATTERS = new HashMap<String, DateTimeFormatter>();
    
    static {
        FORMATTERS.put(W3C_DTF_SCHEME, ISODateTimeFormat.dateTimeParser());
    }
    // ^ actually supports a superset of W3C-DTF formats
    // to limit it to the stricter formats, replace the above with:
    //        W3C_DTF_SCHEME -> DateTimeFormat.forPattern("yyyy-MM-dd'T'HH:mm:ss.SSSSZZ"),
    //        W3C_DTF_SCHEME -> DateTimeFormat.forPattern("yyyy-MM-dd'T'HH:mm:ss.SSSZZ"),
    //        W3C_DTF_SCHEME -> DateTimeFormat.forPattern("yyyy-MM-dd'T'HH:mm:ss.SSZZ"),
    //        W3C_DTF_SCHEME -> DateTimeFormat.forPattern("yyyy-MM-dd'T'HH:mm:ss.SZZ"),
    //        W3C_DTF_SCHEME -> DateTimeFormat.forPattern("yyyy-MM-dd'T'HH:mm:ssZZ"),
    //        W3C_DTF_SCHEME -> DateTimeFormat.forPattern("yyyy-MM-dd'T'HH:mmZZ"),
    //        W3C_DTF_SCHEME -> DateTimeFormat.forPattern("yyyy-MM-dd"),
    //        W3C_DTF_SCHEME -> DateTimeFormat.forPattern("yyyy-MM"),
    //        W3C_DTF_SCHEME -> DateTimeFormat.forPattern("yyyy")
    
    /**
     * Returns the text from the given doc, extracted with the given xPathExpression.
     */
    public String extractTextValue(Document doc, XPathExpression xPathExpression) {
        String textValue = null;
        try {
            textValue = (String) xPathExpression.evaluate(doc, XPathConstants.STRING);
            if (textValue != null)
                textValue = textValue.trim();
        } catch (XPathExpressionException e) {
            throw new AssertionError("Unable to get temoral element string value.", e);
        }
        return textValue;
    }

    /**
     * Extracts the value for the fieldName from the text. 
     * Values in the text are expected to look like this:
     * <pre>fieldName=fieldValue;</pre>
     * and may be located anywhere in the text.
     * 
     * @param text the text to search
     * @param fieldName the name of the field whose value we want returned
     * @return the value corresponding to the given fieldName
     */
    protected String getFieldValue (String text, String fieldName) {
        Pattern matchPattern = Pattern.compile(".*" + fieldName + "=(.*?);.*");
        Matcher matcher = matchPattern.matcher(text);
        
        if (matcher.find())
            return matcher.group(1);
        
        return null;
    }
    
    /**
     * Returns the scheme value from the given text. 
     * For example:
     * <pre>scheme=W3C-DTF;</pre>
     * will return "W3C-DTF"
     * May be null if not present in text.
     */
    public String getScheme(String text) {
        return getFieldValue(text, SCHEME_FIELD);
    }
    
    /**
     * Returns a formatted start date value for the given text.
     * Format of returned date String will be in a solr consumable format. 
     * 
     * @param text the text to search for the beginDate
     * @param scheme the format beginDate's value is expected in
     * @return the date String, in Zulu time zone with Millisecond accuracy 
     *      (see http://lucene.apache.org/solr/api/org/apache/solr/schema/DateField.html)
     */
    public String getFormattedStartDate(String text, String scheme) {
        String startDateStr = getFieldValue(text, START_FIELD);
        return formatDate(startDateStr, scheme);
    }
    
    /**
     * Returns a formatted end date value for the given text.
     * Format of returned date String will be in a solr consumable format. 
     * 
     * @param text the text to search for the beginDate
     * @param scheme the format endDate's value is expected in
     * @return the date String, in Zulu time zone with Millisecond accuracy 
     *      (see http://lucene.apache.org/solr/api/org/apache/solr/schema/DateField.html)
     */
    public String getFormattedEndDate(String text, String scheme) {
        String endDateStr = getFieldValue(text, END_FIELD);
        return formatDate(endDateStr, scheme);
    }
    
    /**
     * Converts a dateString to a valid solr consumable format. The scheme
     * of the dateString may be provided to specify what format the dateString is in. 
     * Not providing a scheme will default to using the formatter that expects 
     * W3C-DTF input.
     *  
     * @param dateString the date String, format specified by scheme param
     * @param scheme the format of dateString, null defaults to W3C-DTF
     * @return the date String, in Zulu time zone with Millisecond accuracy 
     *      (see http://lucene.apache.org/solr/api/org/apache/solr/schema/DateField.html)
     */
    public String formatDate (String dateString, String scheme) {
        
        DateTime date = parseDateTime(scheme, dateString);
        if (date == null) {
            log.error("Date string could not be parsed: " + dateString);
            return null;
        }
        
        DateTimeFormatter iso8601Formatter = ISODateTimeFormat.dateTime();
        String dateIso8601 = iso8601Formatter.print(date.toDateTime(DateTimeZone.UTC));
        
        return dateIso8601;
    }

    private DateTime parseDateTime(String scheme, String dateString) {
        
        if (StringUtils.isEmpty(scheme)) {
            log.info("No scheme provided, defaulting to " + W3C_DTF_SCHEME);
            scheme = W3C_DTF_SCHEME;
        }
        
        DateTimeFormatter formatter = FORMATTERS.get(scheme);
        DateTime dateTime = null;
        try {
            dateTime = formatter.parseDateTime(dateString);
        } catch (Exception e) {
            log.error("");
        }
        
        return dateTime;
    }
}
