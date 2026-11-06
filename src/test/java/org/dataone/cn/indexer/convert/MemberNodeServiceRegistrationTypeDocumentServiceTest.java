package org.dataone.cn.indexer.convert;

import org.apache.commons.configuration.ConfigurationException;
import org.dataone.cn.indexer.IndexWorkerTest;
import org.dataone.configuration.Settings;
import org.junit.Assert;
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
public class MemberNodeServiceRegistrationTypeDocumentServiceTest {
    static {
        try {
            Settings.augmentConfiguration(IndexWorkerTest.PORT_8985_PROPERTY_FILE_PATH);
        } catch (ConfigurationException e) {
            throw new RuntimeException(e);
        }
    }

    @Autowired
    private MemberNodeServiceRegistrationTypeDocumentService serviceTypeDocService;

    public MemberNodeServiceRegistrationTypeDocumentServiceTest() {
    }

    @Test
    public void testInjection() {
        Assert.assertNotNull(serviceTypeDocService);
        Assert.assertNotNull(serviceTypeDocService.getServiceTypeDocUrl());
    }

    @Test
    public void testGetDocument() {
        Document doc = serviceTypeDocService.getMemberNodeServiceRegistrationTypeDocument();
        Assert.assertNotNull(doc);
    }

}
