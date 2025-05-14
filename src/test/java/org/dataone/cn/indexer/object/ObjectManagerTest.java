package org.dataone.cn.indexer.object;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;

import java.io.InputStream;
import java.io.OutputStream;
import java.security.MessageDigest;

import javax.xml.bind.DatatypeConverter;


import org.dataone.indexer.storage.Storage;
import org.dataone.service.types.v1.Identifier;
import org.dataone.service.types.v2.SystemMetadata;
import org.dataone.service.util.TypeMarshaller;
import org.junit.Before;
import org.junit.Test;

/**
 * A junit test class for the ObjecManager class.
 * @author tao
 *
 */
public class ObjectManagerTest {

    private String identifier;

    @Before
    public void setUp() throws Exception {
        identifier = "ObjectManagerTest-" + System.currentTimeMillis();
        File objectFile = new File("src/test/resources/org/dataone/cn/index/resources/d1_testdocs/"
                                    + "fgdc/nasa_d_FEDGPS1293.xml");
        try (InputStream object = new FileInputStream(objectFile)) {
            Storage.getInstance().storeObject(object, identifier);
        }
        File sysmetaFile = new File("src/test/resources/org/dataone/cn/index/resources/"
                                    + "d1_testdocs/fgdc/nasa_d_FEDGPS1293Sysmeta.xml");
        try (InputStream sysmetaStream = new FileInputStream(sysmetaFile)) {
            SystemMetadata sysmeta = TypeMarshaller
                                      .unmarshalTypeFromStream(SystemMetadata.class, sysmetaStream);
            Identifier pid = new Identifier();
            pid.setValue(identifier);
            sysmeta.setIdentifier(pid);
            try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
                TypeMarshaller.marshalTypeToOutputStream(sysmeta, output);
                try (ByteArrayInputStream input = new ByteArrayInputStream(output.toByteArray())) {
                    Storage.getInstance().storeMetadata(input, identifier);
                }
            }
        }
    }

    /**
     * Test the getObject and getSystemMetadata method
     * @throws Exception
     */
    @Test
    public void testGetObjectAndSystemMetadata() throws Exception {
        try (InputStream input = ObjectManagerFactory.getObjectManager().getObject(identifier)) {
            assertNotNull(input);
            try (OutputStream os = new ByteArrayOutputStream()) {
                MessageDigest md5 = MessageDigest.getInstance("MD5");
                // Calculate hex digests
                byte[] buffer = new byte[8192];
                int bytesRead;
                while ((bytesRead = input.read(buffer)) != -1) {
                    os.write(buffer, 0, bytesRead);
                    md5.update(buffer, 0, bytesRead);
                }
                String md5Digest = DatatypeConverter.printHexBinary(md5.digest()).toLowerCase();
                assertEquals("1755a557c13be7af44d676bb09274b0e", md5Digest);
            }
        }
        org.dataone.service.types.v1.SystemMetadata sysmeta = ObjectManagerFactory.getObjectManager()
                .getSystemMetadata(identifier);
        assertEquals(identifier, sysmeta.getIdentifier().getValue());
        assertEquals("1755a557c13be7af44d676bb09274b0e", sysmeta.getChecksum().getValue());
        assertEquals(14828, sysmeta.getSize().intValue());
    }


}
