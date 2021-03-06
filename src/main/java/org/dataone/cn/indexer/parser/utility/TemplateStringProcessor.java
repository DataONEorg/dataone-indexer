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

import java.util.Map;
import java.util.regex.Matcher;

/**
 * Used by RootElement to define how leaf element data values are to be combined
 * into the final field value.
 * 
 * @author sroseboo
 *
 */
public class TemplateStringProcessor {

    public String process(String template, Map<String, String> valueMap) {
        String result = template;
        // Check if the special template value "*" has been specified. If yes, then
        // all values will be included, in the order that they are encountered.
        Boolean includeAllValues = false;
        if (template.trim().compareTo("*") == 0) {
            includeAllValues = true;
            result = "";
        }
        for (String key : valueMap.keySet()) {
            String value = valueMap.get(key);
            if (includeAllValues) {
                result = result + " " + value;
            } else if (result.contains(key)) {
                result = result.replaceAll("\\[" + key + "\\]", Matcher.quoteReplacement(value));
            }
        }
        if (result != null) {
            result = result.trim();
        }
        return result;
    }
}
