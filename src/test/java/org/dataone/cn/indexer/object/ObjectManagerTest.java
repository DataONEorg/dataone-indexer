/**
 * This work was created by participants in the DataONE project, and is
 * jointly copyrighted by participating institutions in DataONE. For 
 * more information on DataONE, see our web site at http://dataone.org.
 *
 *   Copyright 2022
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
 */
package org.dataone.cn.indexer.object;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.nio.file.Paths;

import org.dataone.cn.index.DataONESolrJettyTestBase;
import org.dataone.service.exceptions.NotFound;
import org.dataone.service.types.v2.SystemMetadata;
import org.junit.Test;

/**
 * A junit test class for the ObjecManager class.
 * @author tao
 *
 */
public class ObjectManagerTest {
    
    /**
     * Test the getFilePath method
     * @throws Exception
     */
    @Test
    public void testgetFilePath() throws Exception {
        ObjectManager manager = ObjectManager.getInstance();
        String path = null;
        String format = "eml://ecoinformatics.org/eml-2.0.1";
        String resultPath = manager.getFilePath(path, format);
        assertTrue(resultPath == null);
        format = "image/bmp";
        resultPath = manager.getFilePath(path, format);
        assertTrue(resultPath == null);
        
        path = "";
        format = "eml://ecoinformatics.org/eml-2.0.1";
        resultPath = manager.getFilePath(path, format);
        assertTrue(resultPath == null);
        format = "image/bmp";
        resultPath = manager.getFilePath(path, format);
        assertTrue(resultPath == null);
        
        path = "/var/metacat/documents/foo.1.1";
        format = "eml://ecoinformatics.org/eml-2.0.1";
        resultPath = manager.getFilePath(path, format);
        assertTrue(resultPath.equals("//var/metacat/documents/foo.1.1"));
        
        path = "/var/metacat/documents/foo.2.1";
        format = "image/bmp";;
        resultPath = manager.getFilePath(path, format);
        assertTrue(resultPath.equals("//var/metacat/documents/foo.2.1"));
    }
    
    /**
     * Test the getSystemMetadata method
     * @throws Exception
     */
     @Test
    public void testGetSystemMetadata() throws Exception {
        //Test to get system metadata from a file
        String currentDir = Paths.get(".").toAbsolutePath().normalize().toString();
        System.out.println("current dir " + currentDir);
        String path = currentDir + "/src/test/resources/org/dataone/cn/index/resources/d1_testdocs/json-ld/hakai-deep-schema/hakai-deep-schema.jsonld";
        String id = "hakai-deep-schema.jsonld";
        SystemMetadata sysmeta = ObjectManager.getInstance().getSystemMetadata(id, path);
        assertTrue(sysmeta.getIdentifier().getValue().equals(id));
        
        //Test to get system metadata from the Mock dataone cn server.
        id = "ala-wai-canal-ns02-matlab-processing.eml.1.xml";
        path = null;
        MockMNode mockMNode = new MockMNode("http://mnode.foo");
        mockMNode.setContext(DataONESolrJettyTestBase.getContext());
        ObjectManager.setD1Node(mockMNode);
        sysmeta = ObjectManager.getInstance().getSystemMetadata(id, path);
        assertTrue(sysmeta.getIdentifier().getValue().equals(id));
        
        //Test the system metadata not found
        id = "foo.1.1";
        path = "foo1";
        try {
            sysmeta = ObjectManager.getInstance().getSystemMetadata(id, path);
            fail("We should reach here");
        } catch (NotFound e) {
            assert(true);
        }
    }

}
