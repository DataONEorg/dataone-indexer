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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.apache.commons.configuration.ConfigurationException;
import org.dataone.cn.indexer.IndexWorkerTest;
import org.dataone.configuration.Settings;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.carrotsearch.randomizedtesting.annotations.ThreadLeakScope;

@ThreadLeakScope(ThreadLeakScope.Scope.NONE)
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "../../index/test-context.xml" })
public class MemberNodeServiceRegistrationTypeConverterTest {
    static {
        try {
            Settings.augmentConfiguration(IndexWorkerTest.PORT_8985_PROPERTY_FILE_PATH);
        } catch (ConfigurationException e) {
            throw new RuntimeException(e);
        }
    }

    @Autowired
    private MemberNodeServiceRegistrationTypeConverter serviceTypesConverter;

    private static final String WMS_SERVICE_NAME = "WMS";
    private static final String OPOENDAP_SERVICE_NAME = "OPeNDAP";
    
    @Test
    public void testConvertWMS() {
        assertTrue("\"WMS\" should be converted to service type " + WMS_SERVICE_NAME + ".",
                WMS_SERVICE_NAME.equals(serviceTypesConverter.convert("WMS")));
        assertTrue("\"Wms\" should be converted to service type " + WMS_SERVICE_NAME + ".",
                WMS_SERVICE_NAME.equals(serviceTypesConverter.convert("Wms")));
        assertTrue("\"wms\" should be converted to service type " + WMS_SERVICE_NAME + ".",
                WMS_SERVICE_NAME.equals(serviceTypesConverter.convert("wms")));
        
        assertTrue("\"==WMS==\" should be converted to service type " + WMS_SERVICE_NAME + ".",
                WMS_SERVICE_NAME.equals(serviceTypesConverter.convert("==WMS==")));
        assertTrue("\"Open Geospatial Consortium Web Map Service (WMS)\" should be converted to service type " + WMS_SERVICE_NAME + ".",
                WMS_SERVICE_NAME.equals(serviceTypesConverter.convert("Open Geospatial Consortium Web Map Service (WMS)")));
        
        assertFalse("\"W-M-S\" should not be converted to service type " + WMS_SERVICE_NAME + ".",
                WMS_SERVICE_NAME.equals(serviceTypesConverter.convert("W-M-S")));
        assertFalse("\"OPeNDAP\" should not be converted to service type " + WMS_SERVICE_NAME + ".",
                WMS_SERVICE_NAME.equals(serviceTypesConverter.convert("OPeNDAP")));
    }

    @Test
    public void testConvertOPeNDAP() {
        assertTrue("\"OPeNDAP\" should be converted to service type " + OPOENDAP_SERVICE_NAME + ".",
                OPOENDAP_SERVICE_NAME.equals(serviceTypesConverter.convert("OPeNDAP")));
        assertTrue("\"OPENDAP\" should be converted to service type " + OPOENDAP_SERVICE_NAME + ".",
                OPOENDAP_SERVICE_NAME.equals(serviceTypesConverter.convert("OPENDAP")));
        assertTrue("\"opendap\" should be converted to service type " + OPOENDAP_SERVICE_NAME + ".",
                OPOENDAP_SERVICE_NAME.equals(serviceTypesConverter.convert("opendap")));

        assertTrue("\"ERDDAP OPeNDAP\" should be converted to service type " + OPOENDAP_SERVICE_NAME + ".",
                OPOENDAP_SERVICE_NAME.equals(serviceTypesConverter.convert("ERDDAP OPeNDAP")));

        assertFalse("\"WMS\" should not be converted to service type " + OPOENDAP_SERVICE_NAME + ".",
                OPOENDAP_SERVICE_NAME.equals(serviceTypesConverter.convert("WMS")));
        assertFalse("\"ERRDAP\" should not be converted to service type " + OPOENDAP_SERVICE_NAME + ".",
                OPOENDAP_SERVICE_NAME.equals(serviceTypesConverter.convert("ERRDAP")));
    }

}
