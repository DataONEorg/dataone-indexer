package org.dataone.cn.index;

import java.io.File;
import java.io.IOException;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.dataone.cn.hazelcast.HazelcastClientFactory;
//import org.dataone.cn.index.generator.IndexTaskGenerator;
//import org.dataone.cn.index.processor.IndexTaskProcessor;
import org.dataone.cn.indexer.solrhttp.HTTPService;
import org.dataone.service.types.v2.SystemMetadata;
import org.dataone.service.util.TypeMarshaller;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.springframework.core.io.Resource;

import com.carrotsearch.randomizedtesting.annotations.ThreadLeakScope;


@ThreadLeakScope(ThreadLeakScope.Scope.NONE)
public class SolrIndexBatchAddTest extends DataONESolrJettyTestBase {

    private static Logger logger = Logger.getLogger(SolrIndexBatchAddTest.class.getName());

    //private IndexTaskProcessor processor;
    //private IndexTaskGenerator generator;

    private static final int NUM_FILES = 10;
    
    private Resource peggym1271Sys;
    private Resource peggym1281Sys;
    private Resource peggym1304Sys;
    private Resource fdgc01111999SysMeta;
    private Resource fgdcNasaSysMeta;
    private Resource fgdcEsriSysMeta;
    private Resource isotc211_noaa_SysMeta;
    private Resource ornl_mercury_system_metadata;
    private Resource isotc211_tightlyCoupledServiceSrvOnly_SysMeta;
    private Resource isotc211_nodc_2_SysMeta;
    
    @Test
    public void testBatchAddRuntime() throws Exception {
        
        /*long totalIndivTime = 0;
        long totalBatchTime = 0;
        int iterations = 100;
        
        for (int x=0; x<iterations; x++) {
            deleteAll();
            addSystemMetadata(peggym1304Sys);
            long totalIndividualAddTime = 0;
            
            for (int i=0; i<NUM_FILES; i++) {
                deleteAll();
                addSystemMetadata(peggym1304Sys);
                
                long startIndividualAdd = System.currentTimeMillis();
                processor.processIndexTaskQueue();
                long individualAddTime = System.currentTimeMillis() - startIndividualAdd;
                totalIndividualAddTime += individualAddTime;
            }
            
            deleteAll();
            addSystemMetadata(peggym1271Sys);
            addSystemMetadata(peggym1281Sys);
            addSystemMetadata(peggym1304Sys);
            addSystemMetadata(fdgc01111999SysMeta);
            addSystemMetadata(fgdcNasaSysMeta);
            addSystemMetadata(fgdcEsriSysMeta);
            addSystemMetadata(isotc211_noaa_SysMeta);
            addSystemMetadata(ornl_mercury_system_metadata);
            addSystemMetadata(isotc211_tightlyCoupledServiceSrvOnly_SysMeta);
            addSystemMetadata(isotc211_nodc_2_SysMeta);
            
            long startBatchAdd = System.currentTimeMillis();
            processor.batchProcessIndexTaskQueue();
            long batchAddTime = System.currentTimeMillis() - startBatchAdd;
            
            System.out.println("individual add time:  " + totalIndividualAddTime);
            System.out.println("batch add time:       " + batchAddTime);
            
            totalIndivTime += totalIndividualAddTime;
            totalBatchTime += batchAddTime;
        }
        
        System.out.println("avg individual add time:  " + ((float) totalIndivTime) / iterations);
        System.out.println("avg batch add time:       " + ((float) totalBatchTime) / iterations);*/
    }
    
    @Test
    public void testBatchAddCorrect() throws Exception {
        
        /*deleteAll();
        addSystemMetadata(peggym1271Sys);
        addSystemMetadata(peggym1281Sys);
        addSystemMetadata(peggym1304Sys);
        addSystemMetadata(fdgc01111999SysMeta);
        addSystemMetadata(fgdcNasaSysMeta);
        addSystemMetadata(fgdcEsriSysMeta);
        addSystemMetadata(isotc211_noaa_SysMeta);
        addSystemMetadata(ornl_mercury_system_metadata);
        addSystemMetadata(isotc211_tightlyCoupledServiceSrvOnly_SysMeta);
        addSystemMetadata(isotc211_nodc_2_SysMeta);
        
        processor.batchProcessIndexTaskQueue();
        
        assertPresentInSolrIndex("peggym.130.4");
        assertPresentInSolrIndex("peggym.127.1");
        assertPresentInSolrIndex("peggym.128.1");
        assertPresentInSolrIndex("www.nbii.gov_metadata_mdata_CSIRO_csiro_d_abayadultprawns");
        assertPresentInSolrIndex("www.nbii.gov_metadata_mdata_NASA_nasa_d_FEDGPS1293");
        assertPresentInSolrIndex("nikkis.180.1");
        assertPresentInSolrIndex("isotc211_noaa_12345");
        assertPresentInSolrIndex("Map_ORR_Aspect_2m_1993.xml");
        assertPresentInSolrIndex("isotc211_tightlyCoupledServiceSrvOnly");
        assertPresentInSolrIndex("gov.noaa.nodc:GHRSST-NEODAAS-L2P-AVHRR17_L");*/
        
    }
    
    private void deleteAll() throws IOException {
        HTTPService httpService = (HTTPService) context.getBean("httpService");
        httpService.sendSolrDelete("peggym.130.4");
        httpService.sendSolrDelete("peggym.127.1");
        httpService.sendSolrDelete("peggym.128.1");
        httpService.sendSolrDelete("www.nbii.gov_metadata_mdata_CSIRO_csiro_d_abayadultprawns");
        httpService.sendSolrDelete("www.nbii.gov_metadata_mdata_NASA_nasa_d_FEDGPS1293");
        httpService.sendSolrDelete("nikkis.180.1");
        httpService.sendSolrDelete("isotc211_noaa_12345");
        httpService.sendSolrDelete("Map_ORR_Aspect_2m_1993.xml");
        httpService.sendSolrDelete("isotc211_tightlyCoupledServiceSrvOnly");
        httpService.sendSolrDelete("gov.noaa.nodc:GHRSST-NEODAAS-L2P-AVHRR17_L");
    }

    private void addSystemMetadata(Resource systemMetadataResource) {
        SystemMetadata sysmeta = null;
        try {
            sysmeta = TypeMarshaller.unmarshalTypeFromStream(SystemMetadata.class,
                    systemMetadataResource.getInputStream());
        } catch (Exception ex) {
            logger.error(ex.getMessage(), ex);
            fail("Test SystemMetadata misconfiguration - Exception " + ex);
        }
        String path = null;
        try {
            path = StringUtils
                    .remove(systemMetadataResource.getFile().getPath(), File.separator + "SystemMetadata");
        } catch (IOException e) {
            logger.error(e.getMessage(), e);
        }
        HazelcastClientFactory.getSystemMetadataMap().put(sysmeta.getIdentifier(), sysmeta);
        HazelcastClientFactory.getObjectPathMap().putAsync(sysmeta.getIdentifier(), path);
        //generator.processSystemMetaDataUpdate(sysmeta, path);
    }

    public void setUp() throws Exception {
        super.setUp();
        configureSpringResources();
    }

    public void tearDown() throws Exception {
        super.tearDown();
    }

    @BeforeClass
    public static void init() {
        HazelcastClientFactoryTest.setUp();
    }

    @AfterClass
    public static void cleanup() throws Exception {
        HazelcastClientFactoryTest.shutDown();
    }

    private void configureSpringResources() {
        //processor = (IndexTaskProcessor) context.getBean("indexTaskProcessor");
        //generator = (IndexTaskGenerator) context.getBean("indexTaskGenerator");

        peggym1271Sys = (Resource) context.getBean("peggym1271Sys");
        peggym1281Sys = (Resource) context.getBean("peggym1281Sys");
        peggym1304Sys = (Resource) context.getBean("peggym1304Sys");
        fdgc01111999SysMeta = (Resource) context.getBean("fdgc01111999SysMeta");
        fgdcNasaSysMeta = (Resource) context.getBean("fgdcNasaSysMeta");
        fgdcEsriSysMeta = (Resource) context.getBean("fgdcEsriSysMeta");
        isotc211_noaa_SysMeta = (Resource) context.getBean("isotc211_noaa_SysMeta");
        ornl_mercury_system_metadata = (Resource) context.getBean("ornl_mercury_system_metadata");
        isotc211_tightlyCoupledServiceSrvOnly_SysMeta = (Resource) context.getBean("isotc211_tightlyCoupledServiceSrvOnly_SysMeta");
        isotc211_nodc_2_SysMeta = (Resource) context.getBean("isotc211_nodc_2_SysMeta");
    }

}
