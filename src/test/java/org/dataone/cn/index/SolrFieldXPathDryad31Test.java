package org.dataone.cn.index;

import java.util.HashMap;

import org.dataone.cn.indexer.convert.IConverter;
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
public class SolrFieldXPathDryad31Test extends BaseSolrFieldXPathTest {

    @Autowired
    private Resource dryadDataPackage1;

    @Autowired
    private Resource dryadDataPackage1SysMeta;

    @Autowired
    private Resource dryadDataFile1;

    @Autowired
    private Resource dryadDataFile1SysMeta;

    @Autowired
    private ScienceMetadataDocumentSubprocessor dryad31Subprocessor;

    private IConverter dateConverter = new SolrDateConverter();

    // solr/rule field name (from spring context fields) mapped to actual value
    // from xml documents (system and science metadata docs)
    private HashMap<String, String> dryadDataPackage1Expected = new HashMap<String, String>();

    private String dryadDataPackage1Pid = "http:/dx.doi.org/10.5061/dryad.m4h77";
    private String dryadDataPackage1PidEncoded = "http%3A%2Fdx.doi.org%2F10.5061%2Fdryad.m4h77";

    private HashMap<String, String> dryadDataFile1Expected = new HashMap<String, String>();

    private String dryadDataFile1Pid = "http:/dx.doi.org/10.5061/dryad.m4h77/1";
    private String dryadDataFile1PidEncoded = "http%3A%2Fdx.doi.org%2F10.5061%2Fdryad.m4h77%2F1";

    @Before
    public void setUp() throws Exception {
        // science metadata
        dryadDataPackage1Expected
                .put("abstract",
                        "Worker policing (mutual repression of reproduction) in the eusocial Hymenoptera represents a leading example of how coercion can facilitate cooperation. The occurrence of worker policing in “primitively” eusocial species with low mating frequencies, which lack relatedness differences conducive to policing, suggests that separate factors may underlie the origin and maintenance of worker policing. We tested this hypothesis by investigating conflict over male parentage in the primitively eusocial, monandrous bumblebee, Bombus terrestris. Using observations, experiments, and microsatellite genotyping, we found that: (a) worker- but not queen-laid male eggs are nearly all eaten (by queens, reproductive, and nonreproductive workers) soon after being laid, so accounting for low observed frequencies of larval and adult worker-produced males; (b) queen- and worker-laid male eggs have equal viabilities; (c) workers discriminate between queen- and worker-laid eggs using cues on eggs and egg cells that almost certainly originate from queens. The cooccurrence in B. terrestris of these three key elements of “classical” worker policing as found in the highly eusocial, polyandrous honeybees provides novel support for the hypothesis that worker policing can originate in the absence of relatedness differences maintaining it. Worker policing in B. terrestris almost certainly arose via reproductive competition among workers, that is, as \"selfish\" policing.");
        dryadDataPackage1Expected.put("site", "United Kingdom");
        dryadDataPackage1Expected
                .put("origin",
                        "Zanette, Lorenzo Roberto Sgobaro#Miller, Sophie D. L.#Faria, Christiana M. A.#Almond, Edd J.#Huggins, Tim J.#Jordan, William C.#Bourke, Andrew F. G.");
        dryadDataPackage1Expected.put("author", "Zanette, Lorenzo Roberto Sgobaro");
        dryadDataPackage1Expected.put("authorSurName", "Zanette");
        dryadDataPackage1Expected.put("authorSurNameSort", "Zanette");
        dryadDataPackage1Expected.put("authorGivenName", "Lorenzo Roberto Sgobaro");
        dryadDataPackage1Expected.put("authorGivenNameSort", "Lorenzo Roberto Sgobaro");

        dryadDataPackage1Expected
                .put("investigator",
                        "Zanette, Lorenzo Roberto Sgobaro#Miller, Sophie D. L.#Faria, Christiana M. A.#Almond, Edd J.#Huggins, Tim J.#Jordan, William C.#Bourke, Andrew F. G.");
        dryadDataPackage1Expected.put("pubDate", dateConverter.convert("2012-05-22T19:49:50Z"));

        dryadDataPackage1Expected.put("scientificName", "Bombus terrestris");
        dryadDataPackage1Expected
                .put("title",
                        "Data from: Reproductive conflict in bumblebees and the evolution of worker policing");
        dryadDataPackage1Expected.put("keywords",
                "kin selection#inclusive fitness theory#worker reproduction#social insect");
        dryadDataPackage1Expected.put("fileID", "https://" + hostname + "/cn/v2/resolve/"
                + dryadDataPackage1PidEncoded);
        dryadDataPackage1Expected
                .put("text",
                        "package  Zanette, Lorenzo Roberto Sgobaro  Miller, Sophie D. L.  Faria, Christiana M. A.  Almond, Edd J.  Huggins, Tim J.  Jordan, William C.  Bourke, Andrew F. G.  2012-05-22T19:49:50Z  2012-05-22T19:49:50Z  Data from: Reproductive conflict in bumblebees and the evolution of worker policing  http://dx.doi.org/10.5061/dryad.m4h77  Worker policing (mutual repression of reproduction) in the eusocial Hymenoptera represents a leading example of how coercion can facilitate cooperation. The occurrence of worker policing in “primitively” eusocial species with low mating frequencies, which lack relatedness differences conducive to policing, suggests that separate factors may underlie the origin and maintenance of worker policing. We tested this hypothesis by investigating conflict over male parentage in the primitively eusocial, monandrous bumblebee, Bombus terrestris. Using observations, experiments, and microsatellite genotyping, we found that: (a) worker- but not queen-laid male eggs are nearly all eaten (by queens, reproductive, and nonreproductive workers) soon after being laid, so accounting for low observed frequencies of larval and adult worker-produced males; (b) queen- and worker-laid male eggs have equal viabilities; (c) workers discriminate between queen- and worker-laid eggs using cues on eggs and egg cells that almost certainly originate from queens. The cooccurrence in B. terrestris of these three key elements of “classical” worker policing as found in the highly eusocial, polyandrous honeybees provides novel support for the hypothesis that worker policing can originate in the absence of relatedness differences maintaining it. Worker policing in B. terrestris almost certainly arose via reproductive competition among workers, that is, as \"selfish\" policing.  kin selection  inclusive fitness theory  worker reproduction  social insect  Bombus terrestris  United Kingdom  http://dx.doi.org/10.1111/j.1558-5646.2012.01709.x  Evolution  http://dx.doi.org/10.5061/dryad.m4h77/1  http://dx.doi.org/10.5061/dryad.m4h77/2  http://dx.doi.org/10.5061/dryad.m4h77/3  http://dx.doi.org/10.5061/dryad.m4h77/4  http://dx.doi.org/10.5061/dryad.m4h77/5  http://dx.doi.org/10.5061/dryad.m4h77/6 http:/dx.doi.org/10.5061/dryad.m4h77");
        // system metadata
        dryadDataPackage1Expected.put("id", dryadDataPackage1Pid);
        dryadDataPackage1Expected.put("seriesId", "");
        dryadDataPackage1Expected.put("fileName", "");
        dryadDataPackage1Expected.put("mediaType", "");
        dryadDataPackage1Expected.put("mediaTypeProperty", "");
        dryadDataPackage1Expected.put("formatId", "http://datadryad.org/profile/v3.1");
        dryadDataPackage1Expected.put("formatType", "METADATA");
        dryadDataPackage1Expected.put("size", "3686");
        dryadDataPackage1Expected.put("checksum", "799ab8c72997ad4cbb979e6d6df42d3");
        dryadDataPackage1Expected.put("checksumAlgorithm", "MD5");
        dryadDataPackage1Expected.put("submitter", "lozanette@gmail.com");
        dryadDataPackage1Expected.put("rightsHolder", "admin@datadryad.org");
        dryadDataPackage1Expected.put("replicationAllowed", "");
        dryadDataPackage1Expected.put("numberReplicas", "");
        dryadDataPackage1Expected.put("archived", "false");
        dryadDataPackage1Expected.put("preferredReplicationMN", "");
        dryadDataPackage1Expected.put("blockedReplicationMN", "");
        dryadDataPackage1Expected.put("obsoletes", "");
        dryadDataPackage1Expected.put("obsoletedBy", "");
        dryadDataPackage1Expected
                .put("dateUploaded", dateConverter.convert("2012-05-22T19:49:50Z"));
        dryadDataPackage1Expected.put("dateModified", "2012-06-27T14:30:30.009Z");
        dryadDataPackage1Expected.put("datasource", "urn:node:DRYAD");
        dryadDataPackage1Expected.put("authoritativeMN", "urn:node:DRYAD");
        dryadDataPackage1Expected.put("replicaMN", "");
        dryadDataPackage1Expected.put("replicationStatus", "");
        dryadDataPackage1Expected.put("replicaVerifiedDate", "");
        dryadDataPackage1Expected.put("readPermission", "public");
        dryadDataPackage1Expected.put("writePermission", "");
        dryadDataPackage1Expected.put("changePermission", "");
        dryadDataPackage1Expected.put("isPublic", "true");
        dryadDataPackage1Expected.put("dataUrl", "https://" + hostname + "/cn/v2/resolve/"
                + dryadDataPackage1PidEncoded);

        //
        //
        //
        dryadDataFile1Expected.put("abstract", "");
        dryadDataFile1Expected.put("site", "United Kingdom");
        dryadDataFile1Expected
                .put("origin",
                        "Zanette, Lorenzo Roberto Sgobaro#Miller, Sophie D. L.#Faria, Christiana M. A.#Almond, Edd J.#Huggins, Tim J.#Jordan, William C.#Bourke, Andrew F. G.");
        dryadDataFile1Expected.put("author", "Zanette, Lorenzo Roberto Sgobaro");
        dryadDataFile1Expected.put("authorSurName", "Zanette");
        dryadDataFile1Expected.put("authorSurNameSort", "Zanette");
        dryadDataFile1Expected.put("authorGivenName", "Lorenzo Roberto Sgobaro");
        dryadDataFile1Expected.put("authorGivenNameSort", "Lorenzo Roberto Sgobaro");
        dryadDataFile1Expected
                .put("investigator",
                        "Zanette, Lorenzo Roberto Sgobaro#Miller, Sophie D. L.#Faria, Christiana M. A.#Almond, Edd J.#Huggins, Tim J.#Jordan, William C.#Bourke, Andrew F. G.");
        dryadDataFile1Expected.put("pubDate", dateConverter.convert("2012-05-22T19:49:50Z"));
        dryadDataFile1Expected.put("title", "Table A1");
        dryadDataFile1Expected.put("keywords",
                "kin selection#inclusive fitness theory#worker reproduction#social insect");
        dryadDataFile1Expected.put("fileID", "https://" + hostname + "/cn/v2/resolve/"
                + dryadDataFile1PidEncoded);
        dryadDataFile1Expected.put("scientificName", "Bombus terrestris");

        dryadDataFile1Expected
                .put("text",
                        "file  deposited  Zanette, Lorenzo Roberto Sgobaro  Miller, Sophie D. L.  Faria, Christiana M. A.  Almond, Edd J.  Huggins, Tim J.  Jordan, William C.  Bourke, Andrew F. G.  Table A1  http://dx.doi.org/10.5061/dryad.m4h77/1  http://creativecommons.org/publicdomain/zero/1.0/  kin selection  inclusive fitness theory  worker reproduction  social insect  Bombus terrestris  United Kingdom  2012-05-22T19:49:50Z  2012-06-27T14:30:34Z  Made available in DSpace on 2012-05-22T19:49:50Z (GMT). No. of bitstreams: 2\rTable A1.txt: 1097 bytes, checksum: 51bb09788be23c41fb1722dd53e84a05 (MD5)\rREADME.txt: 881 bytes, checksum: 676d5b3cb9ad3fc5400431a15bdba044 (MD5)  http://dx.doi.org/10.5061/dryad.m4h77 http:/dx.doi.org/10.5061/dryad.m4h77/1");
        // system metadata
        dryadDataFile1Expected.put("id", dryadDataFile1Pid);
        dryadDataFile1Expected.put("seriesId", "");
        dryadDataFile1Expected.put("fileName", "");
        dryadDataFile1Expected.put("mediaType", "");
        dryadDataFile1Expected.put("mediaTypeProperty", "");
        dryadDataFile1Expected.put("formatId", "http://datadryad.org/profile/v3.1");
        dryadDataFile1Expected.put("formatType", "METADATA");
        dryadDataFile1Expected.put("size", "2042");
        dryadDataFile1Expected.put("checksum", "3bb3b1de2a4ee6ed227d985f74471265");
        dryadDataFile1Expected.put("checksumAlgorithm", "MD5");
        dryadDataFile1Expected.put("submitter", "lozanette@gmail.com");
        dryadDataFile1Expected.put("rightsHolder", "admin@datadryad.org");
        dryadDataFile1Expected.put("replicationAllowed", "");
        dryadDataFile1Expected.put("numberReplicas", "");
        dryadDataFile1Expected.put("archived", "false");
        dryadDataFile1Expected.put("preferredReplicationMN", "");
        dryadDataFile1Expected.put("blockedReplicationMN", "");
        dryadDataFile1Expected.put("obsoletes", "");
        dryadDataFile1Expected.put("obsoletedBy", "");
        dryadDataFile1Expected.put("dateUploaded", dateConverter.convert("2012-05-22T19:49:50Z"));
        dryadDataFile1Expected.put("dateModified",
                dateConverter.convert("2012-06-27T14:30:34.473Z"));
        dryadDataFile1Expected.put("datasource", "urn:node:DRYAD");
        dryadDataFile1Expected.put("authoritativeMN", "urn:node:DRYAD");
        dryadDataFile1Expected.put("replicaMN", "");
        dryadDataFile1Expected.put("replicationStatus", "");
        dryadDataFile1Expected.put("replicaVerifiedDate", "");
        dryadDataFile1Expected.put("readPermission", "public");
        dryadDataFile1Expected.put("writePermission", "");
        dryadDataFile1Expected.put("changePermission", "");
        dryadDataFile1Expected.put("isPublic", "true");
        dryadDataFile1Expected.put("dataUrl", "https://" + hostname + "/cn/v2/resolve/"
                + dryadDataFile1PidEncoded);
    }

    /**
     * Testing that the Xpath expressions used by XPathParser and associates are
     * 'mining' the expected data from the science and system metadata
     * documents.
     * 
     * @throws Exception
     */
    @Test
    public void testDryadDataPackageMetadataFields() throws Exception {
        testXPathParsing(dryad31Subprocessor, dryadDataPackage1SysMeta, dryadDataPackage1,
                dryadDataPackage1Expected, dryadDataPackage1Pid);
    }

    @Test
    public void testDyradDataFileMetadataFields() throws Exception {
        testXPathParsing(dryad31Subprocessor, dryadDataFile1SysMeta, dryadDataFile1,
                dryadDataFile1Expected, dryadDataFile1Pid);
    }

}
