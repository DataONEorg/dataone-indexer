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

package org.dataone.cn.indexer.resourcemap;

import org.dspace.foresite.OREParserException;
import org.w3c.dom.Document;

/**
 * Provides concrete instances of ResourceMap interface.  Hides use of implementation class
 * from clients.
 * 
 * @author sroseboo
 *
 */
public class ResourceMapFactory {

    private ResourceMapFactory() {
    }

    public static ResourceMap buildResourceMap(String objectFilePath) throws OREParserException {
        return new ForesiteResourceMap(objectFilePath);
    }

    public static ResourceMap buildResourceMap(Document oreDoc) throws OREParserException {
        return new ForesiteResourceMap(oreDoc);
    }
}
