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
package org.dataone.cn.indexer.convert;

import java.util.Collection;

import junit.framework.TestCase;

import org.apache.commons.configuration.ConfigurationException;
import org.dataone.cn.indexer.IndexWorkerTest;
import org.dataone.cn.indexer.parser.utility.MemberNodeServiceRegistrationTypesParser;
import org.dataone.configuration.Settings;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.w3c.dom.Document;

import com.carrotsearch.randomizedtesting.annotations.ThreadLeakScope;

@ThreadLeakScope(ThreadLeakScope.Scope.NONE)
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "../../index/test-context.xml" })
public class MemberNodeServiceRegistrationTypesParserTest extends TestCase {
    static {
        try {
            Settings.augmentConfiguration(IndexWorkerTest.PORT_8985_PROPERTY_FILE_PATH);
        } catch (ConfigurationException e) {
            throw new RuntimeException(e);
        }
    }

    @Autowired
    MemberNodeServiceRegistrationTypeDocumentService serviceTypeDocService;
    
    @Test
    public void testServiceTypes() {

        Document doc = serviceTypeDocService.getMemberNodeServiceRegistrationTypeDocument();
        assertTrue("Fetched services Document shouldn't be null.", doc != null);
        
        Collection<MemberNodeServiceRegistrationType> serviceTypes = MemberNodeServiceRegistrationTypesParser.parseServiceTypes(doc);
        
        assertTrue("Service types document should contain 2 service types.", serviceTypes.size() == 2);
        MemberNodeServiceRegistrationType serviceTypesArr[] = new MemberNodeServiceRegistrationType[2];
        serviceTypes.toArray(serviceTypesArr);
        
        MemberNodeServiceRegistrationType opendapType = serviceTypesArr[0];
        MemberNodeServiceRegistrationType wmsType = serviceTypesArr[1];
        
        assertTrue("Service types document should contain an OPeNDAP service", opendapType.getName().equals("OPeNDAP"));
        assertTrue("Service types document should contain a WMS service", wmsType.getName().equals("WMS"));
        
        String OPENDAP_PATTERN_1 = "(.*[Oo][Pp][Ee][Nn][Dd][Aa][Pp].*)";
        Collection<String> opendapPatterns = opendapType.getMatchingPatterns();
        assertTrue("OPeNDAP service type should contain match pattern: " + OPENDAP_PATTERN_1,
                opendapPatterns.contains(OPENDAP_PATTERN_1));
        
        String WMS_PATTERN_1 = "(.*[Ww][Mm][Ss].*)";
        Collection<String> wmsPatterns = wmsType.getMatchingPatterns();
        assertTrue("WMS service type should contain match pattern: " + WMS_PATTERN_1, 
                wmsPatterns.contains(WMS_PATTERN_1));
    }
}
