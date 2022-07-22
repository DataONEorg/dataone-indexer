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

package org.dataone.cn.index;

import static org.junit.Assert.fail;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.List;

import junit.framework.Assert;

import org.apache.log4j.Logger;
import org.dataone.cn.indexer.XmlDocumentUtility;
import org.dataone.cn.indexer.parser.BaseXPathDocumentSubprocessor;
import org.dataone.cn.indexer.parser.ISolrField;
import org.dataone.cn.indexer.solrhttp.SolrElementField;
import org.dataone.service.types.v1.Identifier;
import org.dataone.service.types.v2.SystemMetadata;
import org.dataone.service.util.TypeMarshaller;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.w3c.dom.Document;

import com.carrotsearch.randomizedtesting.annotations.ThreadLeakScope;

@ThreadLeakScope(ThreadLeakScope.Scope.NONE)
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "test-context.xml" })
public class InvalidXmlCharTest {

    @Autowired
    private BaseXPathDocumentSubprocessor systemMetadata200Subprocessor;

    @Autowired
    private Resource commonBMPCharSetExample;

    @Autowired
    //private IndexTaskRepository repo;

    private static Logger logger = Logger.getLogger(InvalidXmlCharTest.class.getName());

    @BeforeClass
    public static void init() {
        //HazelcastClientFactoryTest.setUp();
    }

    @AfterClass
    public static void cleanup() throws Exception {
        //HazelcastClientFactoryTest.shutDown();
    }

    @Test
    public void testTaskWithBmpCharset() throws Exception {
        SystemMetadata sysmeta = null;
        String pid = "testMNodeTier3:2012679267486_common-bmp-doc-example-ฉันกินกระจกได้";

       // repo.deleteInBatch(repo.findByPid(pid));

        try {
            sysmeta = TypeMarshaller.unmarshalTypeFromStream(SystemMetadata.class,
                    commonBMPCharSetExample.getInputStream());
        } catch (Exception ex) {
            logger.error(ex.getMessage(), ex);
            fail("Test SystemMetadata misconfiguration - Exception " + ex);
        }
        //HazelcastClientFactory.getSystemMetadataMap().put(sysmeta.getIdentifier(), sysmeta);

        //IndexTask task = new IndexTask(sysmeta, "");
        //InputStream is = new ByteArrayInputStream(task.getSysMetadata().getBytes());
        InputStream is = null;
        testXMLParsing(is, pid);

        //repo.save(task);
        Thread.sleep(200);
        //List<IndexTask> taskList = repo.findByPid(pid);
        //Assert.assertEquals(1, taskList.size());

        //task = taskList.get(0);
        //is = new ByteArrayInputStream(task.getSysMetadata().getBytes());
        testXMLParsing(is, pid);

        Identifier id = new Identifier();
        id.setValue(pid);
        //SystemMetadata smd = HazelcastClientFactory.getSystemMetadataMap().get(id);
        //task = new IndexTask(smd, "");
        //is = new ByteArrayInputStream(task.getSysMetadata().getBytes());
        testXMLParsing(is, pid);
    }

    private void testXMLParsing(InputStream in, String pid) throws Exception {
        Document sysMeta = XmlDocumentUtility.generateXmlDocument(in);
        System.out.println(" ");
        for (ISolrField field : systemMetadata200Subprocessor.getFieldList()) {
            List<SolrElementField> fields = field.getFields(sysMeta, pid);
            if (fields.isEmpty() == false) {
                for (SolrElementField docField : fields) {
                    System.out.println("field value: " + docField.getValue());
                }
            }
        }
    }

}
