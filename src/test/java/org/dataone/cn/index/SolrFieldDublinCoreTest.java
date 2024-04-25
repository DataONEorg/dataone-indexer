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
public class SolrFieldDublinCoreTest extends BaseSolrFieldXPathTest {

    @Autowired
    private Resource dcx_ipumsi_SysMeta;

    @Autowired
    private Resource dcx_ipumsi_SciMeta;

    @Autowired
    private ScienceMetadataDocumentSubprocessor dublinCoreExtendedSubprocessor;

    @Autowired
    private Resource dcterms_spatial_no_namespace_SysMeta;

    @Autowired
    private Resource dcterms_spatial_no_namespace_SciMeta;
    
    private HashMap<String, String> dcxExpected = new HashMap<String, String>();
    private HashMap<String, String> dc_spatial_Expected = new HashMap<String, String>();

    private SolrDateConverter dateConverter = new SolrDateConverter();

    @Before
    public void setUp() throws Exception {
        // science metadata
        dcxExpected
                .put("abstract",
                        "IPUMS-International is an effort to inventory, preserve, harmonize, and disseminate census microdata from around the world. The project has collected the world's largest archive of publicly available census samples. The data are coded and documented consistently across countries and over time to facillitate comparative research. IPUMS-International makes these data available to qualified researchers free of charge through a web dissemination system. The IPUMS project is a collaboration of the Minnesota Population Center, National Statistical Offices, and international data archives. Major funding is provided by the U.S. National Science Foundation and the Demographic and Behavioral Sciences Branch of the National Institute of Child Health and Human Development. Additional support is provided by the University of Minnesota Office of the Vice President for Research, the Minnesota Population Center, and Sun Microsystems. Detailed metadata will be found in ipumsi_6.3_br_2000_ddic.html within the Data Package. The related metadata describes the content of the extraction of the specified sample from the IPUMS International on-line extraction system.");
        dcxExpected
                .put("keywords",
                        "Census#Technical Variables -- HOUSEHOLD#Group Quarters Variables -- HOUSEHOLD#Geography Variables -- HOUSEHOLD#Economic Variables -- HOUSEHOLD#Utilities Variables -- HOUSEHOLD#Appliances, Mechanicals, Other Amenities Variables -- HOUSEHOLD#Dwelling Characteristics Variables -- HOUSEHOLD#Constructed Household Variables -- HOUSEHOLD#Imputation Flags Variables -- HOUSEHOLD#Technical Variables -- PERSON#Constructed Family Interrelationship Variables -- PERSON#Demographic Variables -- PERSON#Fertility and Mortality Variables -- PERSON#Nativity and Birthplace Variables -- PERSON#Ethnicity and Language Variables -- PERSON#Education Variables -- PERSON#Work Variables -- PERSON#Income Variables -- PERSON#Migration Variables -- PERSON#Disability Variables -- PERSON#Other Variables -- PERSON#Imputation Flags Variables -- PERSON#Work: Occupation Variables -- PERSON#Work: Industry Variables -- PERSON");
        dcxExpected.put("title", "IPUMS-International: Brazil 2000 Census");

        dcxExpected.put("northBoundCoord", "5.273055");
        dcxExpected.put("eastBoundCoord", "-34.792631");
        dcxExpected.put("southBoundCoord", "-33.740641");
        dcxExpected.put("westBoundCoord", "-74.004097");
        
        dcxExpected.put("geohash_1", "6");
        dcxExpected.put("geohash_2", "6v");
        dcxExpected.put("geohash_3", "6v3");
        dcxExpected.put("geohash_4", "6v3r");
        dcxExpected.put("geohash_5", "6v3r4");
        dcxExpected.put("geohash_6", "6v3r40");
        dcxExpected.put("geohash_7", "6v3r40u");
        dcxExpected.put("geohash_8", "6v3r40u6");
        dcxExpected.put("geohash_9", "6v3r40u64");
        
        dcxExpected.put("site", "Brazil");

        dcxExpected.put("beginDate", dateConverter.convert("1999-09-25T04:20:00.000Z"));
        dcxExpected.put("endDate", dateConverter.convert("1999-09-25T06:40:00.000Z"));
        dcxExpected.put("pubDate", dateConverter.convert("2014-09-15"));
        dcxExpected.put("author", "Minnesota Population Center");
        dcxExpected.put("authorSurName", "Minnesota Population Center");
        dcxExpected.put("authorSurNameSort", "Minnesota Population Center");
        dcxExpected.put("investigator", "Minnesota Population Center");
        dcxExpected.put("contactOrganization", "Minnesota Population Center");
        dcxExpected.put("origin", "Minnesota Population Center");

        dcxExpected.put("fileID", "https://" + hostname
                + "/cn/v2/resolve/ipumsi_6-3_br_2000_dc.xml");
        dcxExpected
                .put("text",
                        "package  Minnesota Population Center  IPUMS-International: Brazil 2000 Census  ipumsi_6.3_br_2000_DC.xml  Census  Technical Variables -- HOUSEHOLD  Group Quarters Variables -- HOUSEHOLD  Geography Variables -- HOUSEHOLD  Economic Variables -- HOUSEHOLD  Utilities Variables -- HOUSEHOLD  Appliances, Mechanicals, Other Amenities Variables -- HOUSEHOLD  Dwelling Characteristics Variables -- HOUSEHOLD  Constructed Household Variables -- HOUSEHOLD  Imputation Flags Variables -- HOUSEHOLD  Technical Variables -- PERSON  Constructed Family Interrelationship Variables -- PERSON  Demographic Variables -- PERSON  Fertility and Mortality Variables -- PERSON  Nativity and Birthplace Variables -- PERSON  Ethnicity and Language Variables -- PERSON  Education Variables -- PERSON  Work Variables -- PERSON  Income Variables -- PERSON  Migration Variables -- PERSON  Disability Variables -- PERSON  Other Variables -- PERSON  Imputation Flags Variables -- PERSON  Work: Occupation Variables -- PERSON  Work: Industry Variables -- PERSON    2014-09-15  2014-07-01  IPUMS-International is an effort to inventory, preserve, harmonize, and disseminate census microdata from around the world. The project has collected the world's largest archive of publicly available census samples. The data are coded and documented consistently across countries and over time to facillitate comparative research. IPUMS-International makes these data available to qualified researchers free of charge through a web dissemination system. The IPUMS project is a collaboration of the Minnesota Population Center, National Statistical Offices, and international data archives. Major funding is provided by the U.S. National Science Foundation and the Demographic and Behavioral Sciences Branch of the National Institute of Child Health and Human Development. Additional support is provided by the University of Minnesota Office of the Vice President for Research, the Minnesota Population Center, and Sun Microsystems. Detailed metadata will be found in ipumsi_6.3_br_2000_ddic.html within the Data Package. The related metadata describes the content of the extraction of the specified sample from the IPUMS International on-line extraction system.  Record type; Country; Year; IPUMS sample identifier; Household serial number; Number of person records in the household; Household weight; Subsample number; Group quarters status; Number of unrelated persons; Urban-rural status; Continent and region of country; 1st subnational geographic level, world [consistent boundaries over time]; State, Brazil [Level 1; consistent boundaries over time]; State, Brazil [Level 1; inconsistent boundaries, harmonized by name]; Municipality, Brazil [Level 2; inconsistent boundaries, harmonized by name]; Region, Brazil; Mesoregion, Brazil; Municipality, Brazil -- compatible 1980-2000; Metropolitan region, Brazil; Ownership of dwelling [general version]; Ownership of dwelling [detailed version]; Land ownership; Electricity; Water supply; Sewage; Telephone availability; Trash disposal; Automobiles available; Air conditioning; Computer; Clothes washing machine; Refrigerator; Television set; Videocassette recorder; Radio in household; Number of rooms; Number of bedrooms; Number of bathrooms; Bathing facilities; Household classification; Number of families in household; Number of married couples in household; Number of mothers in household; Number of fathers in household; Head's location in household; State; Geographic region; Metropolitan region; Urban-rural status, detailed; Urban-rural status; Sector type; Number of males; Number of females; Dwelling type 1; Flag for dwelling type; Dwelling type 2; Flag for dwelling type 2; Number of rooms; Flag for number of rooms; Number of rooms serving as bedrooms; Flag for number of rooms serving as bedrooms; Ownership of dwelling; Flag for ownership of dwelling; Ownership of land; Flag for ownership of land; Water source; Flag for water supply; Piped water; Flag for piped water; Number of bathrooms; Flag for number of bathrooms; Toilet; Flag for toilet; Waste water; Flag for waste water; Destination of trash; Flag for destination of trash; Electricity; Flag for electricity; Radio; Flag for radio; Refrigerator or freezer; Flag for refrigerator or freezer; VCR (videocassette recorder); Flag for VCR; Clothes washing machine; Flag for clothes washing machine; Microwave oven; Flag for microwave oven; Telephone line installed; Flag for telephone line installed; Computer; Flag for computer; Number of televisions; Flag for number of televisions; Number of automobiles for private use; Flag for number of automobiles for private use; Number of air conditioning units; Flag for number of air conditioning units; Total number of people in the dwelling; Density of residents per room; Density of residents per bedroom; Number of people in family 1; Number of people in family 2; Number of people in family 3; Number of people in family 4; Number of people in family 5; Number of people in family 6; Number of people in family 7; Number of people in family 8; Number of people in family 9; Total income in the private dwelling; Total income in the private dwelling, in minimum salaries; Household weight; Record type [person version]; Country [person version]; Year [person version]; IPUMS sample identifier [person version]; Household serial number [person version]; Person number; Person weight; Mother's location in household; Father's location in household; Spouse's location in household; Rule for linking parent; Rule for linking spouse; Probable stepmother; Probable stepfather; Man with more than one wife linked; Woman is second or higher order wife; Family unit membership; Number of own family members in household; Number of own children in household; Number of own children under age 5 in household; Age of eldest own child in household; Age of youngest own child in household; Relationship to household head [general version]; Relationship to household head [detailed version]; Age; Age, grouped into intervals; Sex; Marital status [general version]; Marital status [detailed version]; Consensual union; Relationship to head of subfamily; Subfamily membership number; Children ever born; Children surviving; Nativity status; Country of birth; State of birth, Brazil; Religion [general version]; Religion [detailed version]; Race or color; Member of an indigenous group; School attendance; Literacy; Educational attainment, international recode [general version]; Educational attainment, international recode [detailed version]; Years of schooling; Educational attainment, Brazil; Employment status [general version]; Employment status [detailed version]; Occupation, ISCO general; Occupation, unrecoded; Industry, general recode; Industry, unrecoded; Class of worker [general version]; Class of worker [detailed version]; Hours worked per week; Hours worked per week, categorized; Hours worked in main occupation; Hours worked outside of main occupation; Total income; Earned income; Migration status, 5 years; Migration status, previous residence; Country of previous residence; Country of residence 5 years ago; Years residing in current locality; State of previous residence, Brazil; State of residence 5 years ago, Brazil; Years residing in current state, Brazil; Disability status; Blind or vision-impaired; Deaf or hearing-impaired; Disability affecting lower extremities; Mental disability; Person number; Metropolitan region; Respondent provided own information; Sex; Flag for sex; Relationship to head of household; Flag for relationship to head of household; Relationship to head of family; Flag for relationship to head of family; Family number; Flag for family number; Age; Flag for age; Age in months; Flag for age in months; Flag for age; Color or race; Flag for color or race; Religion, 2 digits; Religion, 3 digits; Flag for religion; Permanent mental problem; Flag for permanent mental problem; Ability to see; Flag for ability to see; Ability to hear; Flag for ability to hear; Ability to walk/climb stairs; Flag for ability to walk/climb stairs; Paralysis or loss of limb; Flag for paralysis or loss of limb; Always lived in this municipality; Flag for always lived in this municipality; Duration of residence in this municipality; Flag for duration of residence in this municipality; Was born in this municipality; Flag for born in this municipality; Born in this state; Flag for born in this state; Nationality; Flag for nationality; Year in which began residing in Brazil; Flag for year in which began residing in Brazil; State or country of birth; Flag for state or country of birth; Duration of residence in the state; Flag for duration of residence in the state; State or country of previous residence; Flag for state or country of previous residence; Residence on 31 July 1995; Flag for residence on 31 July 1995; Flagfor municipality of residence; State or country of residence on 31 July 1995; Flag for state or country of residence on 31 July 1995; Flag for municipality or state where work or study; Know how to read and write; Flag for know how to read and write; Attend school or daycare; Flag for attend school or daycare; Course being taken; Flag for course being taken; School year now attending; Flag for school year now attending; Highest course attended, having concluded at least one year; Flag for highest course attended; Last year of school passed; Flag for last year of school passed; Concluded course studied; Flag for concluded course studied; Flag for highest course concluded; Years of education; Live with spouse or partner; Flag for live with spouse or partner; Type of last union; Flag for type of last union; Marital status; Flag for marital status; Had remunerated work last week; Flag for had remunerated work last week; Had work but was off last week; Flag for had work but was off last week; Unpaid non-farm household or apprentice work last week; Flag for unpaid non-farm household or apprentice work last week; Unpaid farm labor last week; Flag for unpaid farm labor last week; Grew crops to feed household last week; Flag for grew crops to feed household last week; Number of jobs last week; Flag for number of jobs last week; Occupation, 1 digit; Occupation, 2 digits; Occupation, 3 digits; Occupation, 4 digits; Flag for occupation; Industry, 2 digits; Industry, 5 digits; Flag for industry; Class of worker; Flag for class of worker; Government employee or military personnel; Flag for government employee or military personnel; How many employees worked in this company; Flag for how many employees worked in this company; Contributor to the official social security; Flag for contributor to the official social security; Income in principal job; Flag for income in principal job; Gross monthly income in principal job; Flag for gross income in principal job; Total monthly income in principal job; Total income in principal job, in minimum salaries; Income in additional jobs; Flag for income in additional jobs; Gross monthly income in additional jobs; Flag for gross income in additional jobs; Total monthly income in additional jobs; Total monthly income in additional jobs, in minimum salaries; Total monthly income in all jobs; Total income in all jobs, in minimum salaries; Hours worked per week in principal job; Flag for hours worked per week in principal job; Hours worked in additional jobs; Flag for hours worked in additional jobs; Total hours worked; Efforts to find work; Flag for efforts to find work; Retired with government social security in July 2000; Flag for retired with government social security in July 2000; Earnings from retirement/pension; Flag for earnings from retirement/pension; Income from rents; Flag for income from rents; Earnings from alimony, allowance, donation; Flag for earnings from alimony, allowance, donation; Income from Federal minimum income program, school allowance, and unemployment insurance; Flag for income from federal minimum income program, school allowance, and unemployment insurance; Other income; Flag for other income; Total income; Total income, in minimum salaries; Total number of children born alive; Flag for total number of children born alive; Total number of children still alive; Age of last born child alive; Flag for age of last born child alive; Total number of children born dead; Flag for total number of children born dead; Total number of children the woman ever had; Flag for total number of children the woman ever had; Person weight;  Brazil  northlimit=5.273055; eastlimit=-34.792631; southlimit=-33.740641; westlimit=-74.004097; name=Brazil;  start=1999-09-25T14:20+10:00; end=1999-09-25T16:40+10:00; scheme=W3C-DTF;  http://international.ipums.org  ipumsi_6.3_br_2000_ddic.xml  ipumsi_6.3_br_2000_ddic_xml.html  IPUMS-International distributes integrated microdata of individuals and households only by agreement of collaborating national statistical offices and under the strictest of confidence. Before data may be distributed to an individual researcher, an electronic license agreement must be signed and approved. (see http://international.ipums.org) ipumsi_6-3_br_2000_dc.xml");

        // system metadata
        dcxExpected.put("id", "ipumsi_6-3_br_2000_dc.xml");
        dcxExpected.put("seriesId", "");
        dcxExpected.put("fileName", "");
        dcxExpected.put("mediaType", "");
        dcxExpected.put("mediaTypeProperty", "");
        dcxExpected.put("formatId", "http://ns.dataone.org/metadata/schema/onedcx/v1.0");
        dcxExpected.put("formatType", "METADATA");
        dcxExpected.put("size", "14949");
        dcxExpected.put("checksum", "e5975f877816caea2f2be2e2f6b7ddc6");
        dcxExpected.put("checksumAlgorithm", "MD5");
        dcxExpected.put("submitter", "CN=urn:node:mnTestMPC,DC=dataone,DC=org");
        dcxExpected.put("rightsHolder",
                "CN=Judy Kallestad A13391,O=University of Minnesota,C=US,DC=cilogon,DC=org");
        dcxExpected.put("replicationAllowed", "");
        dcxExpected.put("numberReplicas", "");
        dcxExpected.put("archived", "false");
        dcxExpected.put("preferredReplicationMN", "");
        dcxExpected.put("blockedReplicationMN", "");
        dcxExpected.put("obsoletes", "");
        dcxExpected.put("obsoletedBy", "");
        dcxExpected.put("dateUploaded", dateConverter.convert("2014-08-28T20:55:19.003582"));
        dcxExpected.put("dateModified", dateConverter.convert("2014-08-28T20:55:19.034555Z"));
        dcxExpected.put("datasource", "urn:node:mnTestMPC");
        dcxExpected.put("authoritativeMN", "urn:node:mnTestMPC");
        dcxExpected.put("replicaMN", "");
        dcxExpected.put("replicationStatus", "");
        dcxExpected.put("replicaVerifiedDate", "");
        dcxExpected.put("readPermission", "public");
        dcxExpected.put("writePermission", "");
        dcxExpected.put("changePermission", "");
        dcxExpected.put("isPublic", "true");
        dcxExpected.put("dataUrl", "https://" + hostname
                + "/cn/v2/resolve/ipumsi_6-3_br_2000_dc.xml");
        
        // ========================================================================
        
        // science metadata
        dc_spatial_Expected
                .put("abstract",
                        "Variability in historic faunal assemblages is believed to be related to niche      construction effects associated with the establishment and cultivation of Old World      domesticated flora and fauna in the New World. Fort Shirley, a French and Indian War period      fortification in Central Pennsylvania occupied during the mid 1750's, is an important case      study in this picture as it was occupied during the introduction of domestic livestock to      Central Pennsylvania. Published zooarchaeological analyses of historic fortifications      elsewhere in the New World highlight significant contributions of locally abundant wild fauna      to military diets. Statistical comparison of faunal assemblages from military installations      dating to the French and Indian War (1754-1763), including Fort Shirley, the American      Revolutionary War (1775-1783), and the War of 1812 (1812-1815) will test the hypothesis that      diet breadth narrowed through time and was accompanied by increasing reliance on Old World      domesticates.");
        dc_spatial_Expected
                .put("keywords",
                        "Zooarchaeology#Historic Archaeology#Diet Variability");
        dc_spatial_Expected.put("title", "Investigating Diet Variability at Early Fortifications in the American Colonies      (Contributed Poster)");

        dc_spatial_Expected.put("northBoundCoord", "47.38374632398567");
        dc_spatial_Expected.put("eastBoundCoord", "-66.76461662552896");
        dc_spatial_Expected.put("southBoundCoord", "39.30029918615029");
        dc_spatial_Expected.put("westBoundCoord", "-80.8154296875");
        
        dc_spatial_Expected.put("geohash_1", "d");
        dc_spatial_Expected.put("geohash_2", "dr");
        dc_spatial_Expected.put("geohash_3", "dre");
        dc_spatial_Expected.put("geohash_4", "drew");
        dc_spatial_Expected.put("geohash_5", "drew8");
        dc_spatial_Expected.put("geohash_6", "drew8f");
        dc_spatial_Expected.put("geohash_7", "drew8f5");
        dc_spatial_Expected.put("geohash_8", "drew8f5t");
        dc_spatial_Expected.put("geohash_9", "drew8f5tx");
        
        dc_spatial_Expected.put("site", "North America - Northeast");

        dc_spatial_Expected.put("beginDate", "");
        dc_spatial_Expected.put("endDate", "");
        dc_spatial_Expected.put("pubDate", "");
        dc_spatial_Expected.put("author", "McClure, Sarah (Penn State University)");
        dc_spatial_Expected.put("authorSurName", "McClure, Sarah (Penn State University)");
        dc_spatial_Expected.put("authorSurNameSort", "McClure, Sarah (Penn State University)");
        dc_spatial_Expected.put("investigator", "McClure, Sarah (Penn State University)#Burns, Jonathan (Penn State University)#Welker, Martin (Penn State University)");
        dc_spatial_Expected.put("contactOrganization", "McClure, Sarah (Penn State University)#Burns, Jonathan (Penn State University)#Welker, Martin (Penn State University)");
        dc_spatial_Expected.put("origin", "McClure, Sarah (Penn State University)#Burns, Jonathan (Penn State University)#Welker, Martin (Penn State University)");

        dc_spatial_Expected.put("fileID", "https://" + hostname + "/cn/v2/resolve/dcterms_spatial_no_namespace");
        dc_spatial_Expected.put("text", "Investigating Diet Variability at Early Fortifications in the American Colonies      (Contributed Poster)  McClure, Sarah (Penn State University)  Burns, Jonathan (Penn State University)  Welker, Martin (Penn State University)  Zooarchaeology  Historic Archaeology  Diet Variability  Document  http://alpha.tdar.org/document/48602/investigating-diet-variability-at-early-fortifications-in-the-american-colonies-contributed-poster  Text  Society of American Archaeology  Society of American Archaeology, San Francisco, California  conference    Variability in historic faunal assemblages is believed to be related to niche      construction effects associated with the establishment and cultivation of Old World      domesticated flora and fauna in the New World. Fort Shirley, a French and Indian War period      fortification in Central Pennsylvania occupied during the mid 1750's, is an important case      study in this picture as it was occupied during the introduction of domestic livestock to      Central Pennsylvania. Published zooarchaeological analyses of historic fortifications      elsewhere in the New World highlight significant contributions of locally abundant wild fauna      to military diets. Statistical comparison of faunal assemblages from military installations      dating to the French and Indian War (1754-1763), including Fort Shirley, the American      Revolutionary War (1775-1783), and the War of 1812 (1812-1815) will test the hypothesis that      diet breadth narrowed through time and was accompanied by increasing reliance on Old World      domesticates.  2015-04-02T21:21:52-07:00  Investigating Diet Variability at Early Fortifications in the American      Colonies (Contributed Poster). Sarah McClure, Jonathan Burns, Martin Welker. Presented at      Society of American Archaeology, San Francisco, California. 2015 ( tDAR id: 48602) ;      doi:10.6067/XCV848602  North America - Northeast  northlimit=47.38374632398567; southlimit=39.30029918615029;      westlimit=-80.8154296875; eastlimit=-66.76461662552896;  2015 dcterms_spatial_no_namespace");
        
        // system metadata
        dc_spatial_Expected.put("id", "dcterms_spatial_no_namespace");
        dc_spatial_Expected.put("seriesId", "");
        dc_spatial_Expected.put("fileName", "");
        dc_spatial_Expected.put("mediaType", "");
        dc_spatial_Expected.put("mediaTypeProperty", "");
        dc_spatial_Expected.put("formatId", "http://ns.dataone.org/metadata/schema/onedcx/v1.0");
        dc_spatial_Expected.put("formatType", "METADATA");
        dc_spatial_Expected.put("size", "14949");
        dc_spatial_Expected.put("checksum", "e5975f877816caea2f2be2e2f6b7ddc6");
        dc_spatial_Expected.put("checksumAlgorithm", "MD5");
        dc_spatial_Expected.put("submitter", "CN=urn:node:mnTestMPC,DC=dataone,DC=org");
        dc_spatial_Expected.put("rightsHolder",
                "CN=Judy Kallestad A13391,O=University of Minnesota,C=US,DC=cilogon,DC=org");
        dc_spatial_Expected.put("replicationAllowed", "");
        dc_spatial_Expected.put("numberReplicas", "");
        dc_spatial_Expected.put("archived", "false");
        dc_spatial_Expected.put("preferredReplicationMN", "");
        dc_spatial_Expected.put("blockedReplicationMN", "");
        dc_spatial_Expected.put("obsoletes", "");
        dc_spatial_Expected.put("obsoletedBy", "");
        dc_spatial_Expected.put("dateUploaded", dateConverter.convert("2014-08-28T20:55:19.003582"));
        dc_spatial_Expected.put("dateModified", dateConverter.convert("2014-08-28T20:55:19.034555Z"));
        dc_spatial_Expected.put("datasource", "urn:node:mnTestMPC");
        dc_spatial_Expected.put("authoritativeMN", "urn:node:mnTestMPC");
        dc_spatial_Expected.put("replicaMN", "");
        dc_spatial_Expected.put("replicationStatus", "");
        dc_spatial_Expected.put("replicaVerifiedDate", "");
        dc_spatial_Expected.put("readPermission", "public");
        dc_spatial_Expected.put("writePermission", "");
        dc_spatial_Expected.put("changePermission", "");
        dc_spatial_Expected.put("isPublic", "true");
        dc_spatial_Expected.put("dataUrl", "https://" + hostname
                + "/cn/v2/resolve/dcterms_spatial_no_namespace");
    }

    @Test
    public void testDublinCoreExtendedFieldParsing() throws Exception {
        testXPathParsing(dublinCoreExtendedSubprocessor, dcx_ipumsi_SysMeta, dcx_ipumsi_SciMeta,
                dcxExpected, "ipumsi_6-3_br_2000_dc.xml");
    }
    
    @Test
    public void testDublinCoreSpatialFieldParsing() throws Exception {
        testXPathParsing(dublinCoreExtendedSubprocessor, dcterms_spatial_no_namespace_SysMeta, dcterms_spatial_no_namespace_SciMeta,
                dc_spatial_Expected, "dcterms_spatial_no_namespace");
    }
}
