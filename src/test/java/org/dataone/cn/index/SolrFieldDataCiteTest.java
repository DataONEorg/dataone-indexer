package org.dataone.cn.index;

import java.util.HashMap;

import org.dataone.cn.indexer.convert.SolrDateConverter;
import org.dataone.cn.indexer.parser.ScienceMetadataDocumentSubprocessor;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.carrotsearch.randomizedtesting.annotations.ThreadLeakScope;


@ThreadLeakScope(ThreadLeakScope.Scope.NONE)
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "test-context.xml" })
public class SolrFieldDataCiteTest extends BaseSolrFieldXPathTest {

    @Autowired
    private Resource datacite_1_SysMeta;

    @Autowired
    private Resource datacite_1_SciMeta;

    private String pid1 = "dataciteScienceMetadata1";

    @Autowired
    private Resource datacite_2_SysMeta;

    @Autowired
    private Resource datacite_2_SciMeta;

    private String pid2 = "dataciteScienceMetadata2";

    @Autowired
    private ScienceMetadataDocumentSubprocessor datacite3Subprocessor;

    private HashMap<String, String> datacite1Expected = new HashMap<String, String>();

    private HashMap<String, String> datacite2Expected = new HashMap<String, String>();

    private SolrDateConverter dateConverter = new SolrDateConverter();

    @Before
    public void setUp() throws Exception {
        // science metadata
        datacite1Expected.put("author", "Peach, A.");
        datacite1Expected.put("authorLastName", "Peach");
        datacite1Expected.put("authorSurName", "Peach");
        datacite1Expected.put("authorSurNameSort", "Peach");
        datacite1Expected.put("authorGivenName", "A.");
        datacite1Expected.put("authorGivenNameSort", "A.");
        datacite1Expected
                .put("abstract",
                        "The Division has been taking records of temperatures and humidities in groups of houses at various locations in Canada over the past several years. This survey has more recently been extended to include schools. Records obtained from classrooms in six schools in Ponhook Lake, Nova Scotia from June 1, 1961-October 12, 1962 are now reported.");
        datacite1Expected.put("title",
                "Temperature and Humidity in School Classrooms, Ponhook Lake, N.S., 1961-1962");
        datacite1Expected.put("pubDate", dateConverter.convert("1963"));
        datacite1Expected.put("keywords", "Temperature#Humidity#Classrooms#Ponhook Lake (N.S.)");
        //
        datacite1Expected.put("beginDate", dateConverter.convert("1961-06-01"));
        datacite1Expected.put("endDate", dateConverter.convert("1962-10-12"));
        datacite1Expected.put("origin", "Peach, A.");
        datacite1Expected.put("investigator", "Peach, A.#Pomegranate, B.");
        datacite1Expected.put("contactOrganization", "");
        datacite1Expected.put("site", "Ponhook Lake, Nova Scotia");
        datacite1Expected.put("fileID", "https://" + hostname + "/cn/v2/resolve/" + pid1);
        datacite1Expected
                .put("text",
                        "10.5072/DataCollector_dateCollected_geoLocationBox    Peach, A.     Temperature and Humidity in School Classrooms, Ponhook Lake, N.S., 1961-1962   National Research Council Canada  1963   Temperature  Humidity  Classrooms  Ponhook Lake (N.S.)     Pomegranate, B.     1961-06-01/1962-10-12   en  report   10 p.    The Division has been taking records of temperatures and humidities in groups of houses at various locations in Canada over the past several years. This survey has more recently been extended to include schools. Records obtained from classrooms in six schools in Ponhook Lake, Nova Scotia from June 1, 1961-October 12, 1962 are now reported.     44.7167 -64.2 44.9667 -63.8  Ponhook Lake, Nova Scotia     CC0 1.0 Universal dataciteScienceMetadata1");
        
        datacite1Expected.put("northBoundCoord", "44.9667");
        datacite1Expected.put("eastBoundCoord", "-63.8");
        datacite1Expected.put("southBoundCoord", "44.7167");
        datacite1Expected.put("westBoundCoord", "-64.2");

        datacite1Expected.put("geohash_1", "d");
        datacite1Expected.put("geohash_2", "dx");
        datacite1Expected.put("geohash_3", "dxf");
        datacite1Expected.put("geohash_4", "dxfr");
        datacite1Expected.put("geohash_5", "dxfrp");
        datacite1Expected.put("geohash_6", "dxfrpe");
        datacite1Expected.put("geohash_7", "dxfrpeh");
        datacite1Expected.put("geohash_8", "dxfrpeht");
        datacite1Expected.put("geohash_9", "dxfrpehtg");

        // system metadata
        datacite1Expected.put("id", pid1);
        datacite1Expected.put("seriesId", "");
        datacite1Expected.put("fileName", "");
        datacite1Expected.put("mediaType", "");
        datacite1Expected.put("mediaTypeProperty", "");
        datacite1Expected.put("formatId", "http://datacite.org/schema/kernel-3.0");
        datacite1Expected.put("formatType", "METADATA");
        datacite1Expected.put("size", "8849");
        datacite1Expected.put("checksum", "f3985f867816caea2f2be2e2f6b7ddc6");
        datacite1Expected.put("checksumAlgorithm", "MD5");
        datacite1Expected.put("submitter", "CN=urn:node:mnTestDASH,DC=dataone,DC=org");
        datacite1Expected.put("rightsHolder",
                "CN=Tom Scientist A13461,O=University of America,C=US,DC=cilogon,DC=org");
        datacite1Expected.put("replicationAllowed", "");
        datacite1Expected.put("numberReplicas", "");
        datacite1Expected.put("archived", "false");
        datacite1Expected.put("preferredReplicationMN", "");
        datacite1Expected.put("blockedReplicationMN", "");
        datacite1Expected.put("obsoletes", "");
        datacite1Expected.put("obsoletedBy", "");
        datacite1Expected.put("dateUploaded", dateConverter.convert("2014-08-28T20:55:19.003582"));
        datacite1Expected.put("dateModified", dateConverter.convert("2014-08-28T20:55:19.034555Z"));
        datacite1Expected.put("datasource", "urn:node:mnTestDASH");
        datacite1Expected.put("authoritativeMN", "urn:node:mnTestDASH");
        datacite1Expected.put("replicaMN", "");
        datacite1Expected.put("replicationStatus", "");
        datacite1Expected.put("replicaVerifiedDate", "");
        datacite1Expected.put("readPermission", "public");
        datacite1Expected.put("writePermission", "");
        datacite1Expected.put("changePermission", "");
        datacite1Expected.put("isPublic", "true");
        datacite1Expected.put("dataUrl", "https://" + hostname + "/cn/v2/resolve/" + pid1);
        datacite1Expected.put("licenseName", "CC0 1.0 Universal");
        datacite1Expected.put("licenseUrl", "http://creativecommons.org/publicdomain/zero/1.0/");
     }

    @Test
    public void testDataCiteFieldParsing() throws Exception {
        testXPathParsing(datacite3Subprocessor, datacite_1_SysMeta, datacite_1_SciMeta,
                datacite1Expected, pid1);

        //        testXPathParsing(datacite3Subprocessor, datacite_2_SysMeta, datacite_2_SciMeta,
        //                datacite2Expected, pid2);
    }
}
