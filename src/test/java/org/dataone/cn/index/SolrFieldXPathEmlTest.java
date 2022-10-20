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
public class SolrFieldXPathEmlTest extends BaseSolrFieldXPathTest {

    @Autowired
    private Resource peggym1304SysObsoletedBy;

    @Autowired
    private Resource peggym1304Sci;

    @Autowired
    private Resource emlRefSciMeta;
    
    @Autowired
    private Resource serviceDescriptionEmlSysMeta;
    
    @Autowired
    private Resource serviceDescriptionEmlSciMeta;

    @Autowired
    private Resource eml220TestDocSysMeta;

    @Autowired
    private Resource eml220TestDocSciMeta;

    @Autowired
    private ScienceMetadataDocumentSubprocessor eml210Subprocessor;

    @Autowired
    private ScienceMetadataDocumentSubprocessor eml220Subprocessor;

    private String serviceEmlPid = "serviceEmlPid";
    
    private SolrDateConverter dateConverter = new SolrDateConverter();

    // solr/rule field name (from spring context fields) mapped to actual value
    // from xml documents (system and science metadata docs)
    private HashMap<String, String> eml210Expected = new HashMap<String, String>();
    private HashMap<String, String> eml220Expected = new HashMap<String, String>();
    private HashMap<String, String> emlRefExpected = new HashMap<String, String>();
    private HashMap<String, String> emlServiceExpected = new HashMap<String, String>();
    
    @Before
    public void setUp() throws Exception {
        setUpEml210();
        setUpEml220();
        setUpEmlService();
    }
    
    public void setUpEml210() throws Exception {
        // science metadata
        eml210Expected
                .put("abstract",
                        "This metadata record fred, describes a 12-34 TT-12 long-term data document can't frank.  This is a test.  If this was not a lower, an abstract \"double\" or 'single' would be present in UPPER (parenthized) this location.");
        eml210Expected
                .put("keywords",
                        "SANParks, South Africa##Augrabies Falls National Park,South Africa##Census data#EARTH SCIENCE : Oceans : Ocean Temperature : Water Temperature");
        eml210Expected.put("title", "Augrabies falls National Park census data.");
        eml210Expected.put("project", "");
        eml210Expected.put("southBoundCoord", "26.0");
        eml210Expected.put("northBoundCoord", "26.0");
        eml210Expected.put("westBoundCoord", "-120.31121");
        eml210Expected.put("eastBoundCoord", "-120.31121");
        eml210Expected.put("geohash_1", "9");
        eml210Expected.put("geohash_2", "9k");
        eml210Expected.put("geohash_3", "9kd");
        eml210Expected.put("geohash_4", "9kd7");
        eml210Expected.put("geohash_5", "9kd7y");
        eml210Expected.put("geohash_6", "9kd7ym");
        eml210Expected.put("geohash_7", "9kd7ym0");
        eml210Expected.put("geohash_8", "9kd7ym0h");
        eml210Expected.put("geohash_9", "9kd7ym0hc");
        eml210Expected.put("site", "Agulhas falls national Park");
        eml210Expected.put("beginDate", dateConverter.convert("1998"));
        eml210Expected.put("endDate", dateConverter.convert("2004-02-13"));
        eml210Expected.put("pubDate", "");
        eml210Expected.put("author", "SANParks");
        eml210Expected.put("authorGivenName", "");
        eml210Expected.put("authorSurName", "SANParks");
        eml210Expected.put("authorGivenNameSort", "");
        eml210Expected.put("authorSurNameSort", "SANParks");
        eml210Expected.put("authorLastName", "SANParks#Garcia#Freeman");
        eml210Expected.put("investigator", "SANParks#Garcia#Freeman");
        eml210Expected.put("contactOrganization", "SANParks#The Awesome Store");
        eml210Expected
                .put("genus",
                        "Antidorcas#Cercopithecus#Diceros#Equus#Giraffa#Oreotragus#Oryz#Papio#Taurotragus#Tragelaphus");
        eml210Expected
                .put("species",
                        "marsupialis#aethiops#bicornis#hartmannae#camelopardalis#oreotragus#gazella#hamadryas#oryx#strepsiceros");
        eml210Expected.put("kingdom", "");
        eml210Expected.put("order", "");
        eml210Expected.put("phylum", "");
        eml210Expected.put("family", "");
        eml210Expected.put("class", "");
        eml210Expected
                .put("scientificName",
                        "Antidorcas marsupialis#Cercopithecus aethiops#Diceros bicornis#Equus hartmannae#Giraffa camelopardalis#Oreotragus oreotragus#Oryz gazella#Papio hamadryas#Taurotragus oryx#Tragelaphus strepsiceros");
        eml210Expected.put("origin", "SANParks Freddy Garcia#Gordon Freeman#The Awesome Store");
        eml210Expected
                .put("attributeName",
                        "ID#Lat S#Long E#Date#Stratum#Transect#Species#LatS#LongE#Total#Juvenile#L/R#Species#Stratum#Date#SumOfTotal#SumOfJuvenile#Species#Date#SumOfTotal#SumOfJuvenile");
        eml210Expected.put("attributeLabel", "");
        eml210Expected
                .put("attributeDescription",
                        "The ID#Lat S#Long E#The date#Stratum#Transect#The name of species#LatS#LongE#The total#Juvenile#L/R#The name of species#Stratum#The date#Sum of the total#Sum of juvenile#The name of species#The date#The sum of total#Sum of juvenile");
        eml210Expected
                .put("attributeUnit",
                        "dimensionless#dimensionless#dimensionless#dimensionless#dimensionless#dimensionless#dimensionless#dimensionless#dimensionless#dimensionless#dimensionless#dimensionless#dimensionless#dimensionless#dimensionless");
        eml210Expected
                .put("attribute",
                        "ID  The ID dimensionless#Lat S  Lat S dimensionless#Long E  Long E dimensionless#Date  The date#Stratum  Stratum dimensionless#Transect  Transect dimensionless#Species  The name of species#LatS  LatS dimensionless#LongE  LongE dimensionless#Total  The total dimensionless#Juvenile  Juvenile dimensionless#L/R  L/R dimensionless#Species  The name of species#Stratum  Stratum dimensionless#Date  The date#SumOfTotal  Sum of the total dimensionless#SumOfJuvenile  Sum of juvenile dimensionless#Species  The name of species#Date  The date#SumOfTotal  The sum of total dimensionless#SumOfJuvenile  Sum of juvenile dimensionless");

        eml210Expected.put("fileID", "https://" + hostname + "/cn/v2/resolve/peggym.130.4");
        eml210Expected
                .put("text",
                        "Augrabies falls National Park census data.   SANParks    Garcia  Freddy   SANParks  Regional Ecologists   Private Bag x402 Skukuza, 1350 South Africa     Freeman  Gordon   SANParks  Regional Ecologists   Private Bag x402 Skukuza, 1350 South Africa    The Awesome Store  Regional Ecologists   Private Bag x402 Skukuza, 1350 South Africa    This metadata record fred, describes a 12-34 TT-12 long-term data document can't frank.  This is a test.  If this was not a lower, an abstract \"double\" or 'single' would be present in UPPER (parenthized) this location.   SANParks, South Africa  Augrabies Falls National Park,South Africa  Census data  EARTH SCIENCE : Oceans : Ocean Temperature : Water Temperature    Agulhas falls national Park   -120.311210  -120.311210  26.0  26.0       1998    2004-02-13       genus  Antidorcas   species  marsupialis  Hartmans Zebra     Genus  Cercopithecus   Species  aethiops  Vervet monkey     Genus  Diceros   Species  bicornis  Baboon     Genus  Equus   Species  hartmannae  Giraffe     Genus  Giraffa   Species  camelopardalis  Kudu     Genus  Oreotragus   Species  oreotragus  Gemsbok     Genus  Oryz   Species  gazella  Eland     Genus  Papio   Species  hamadryas     Genus  Taurotragus   Species  oryx  Black rhino     Genus  Tragelaphus   Species  strepsiceros  Klipspringer      1251095992100 peggym.130.4 ID Lat S Long E Date Stratum Transect Species LatS LongE Total Juvenile L/R SumOfTotal SumOfJuvenile The ID Lat S Long E The date Stratum Transect The name of species LatS LongE The total Juvenile L/R Sum of the total Sum of juvenile The sum of total dimensionless");

        // system metadata
        eml210Expected.put("id", "peggym.130.4");
        eml210Expected.put("seriesId", "peggym.130");
        eml210Expected.put("fileName", "fileName1.txt");
        eml210Expected.put("mediaType", "text/rdf");
        eml210Expected.put("mediaTypeProperty", "extension rdf#something cool");
        eml210Expected.put("formatId", "eml://ecoinformatics.org/eml-2.1.0");
        eml210Expected.put("formatType", "METADATA");
        eml210Expected.put("size", "36281");
        eml210Expected.put("checksum", "24426711d5385a9ffa583a13d07af2502884932f");
        eml210Expected.put("checksumAlgorithm", "SHA-1");
        eml210Expected.put("submitter", "dataone_integration_test_user");
        eml210Expected.put("rightsHolder", "dataone_integration_test_user");
        eml210Expected.put("replicationAllowed", "true");
        eml210Expected.put("numberReplicas", "");
        eml210Expected.put("archived", "false");
        eml210Expected.put("preferredReplicationMN", "");
        eml210Expected.put("blockedReplicationMN", "");
        eml210Expected.put("obsoletes", "peggym.130.3");
        eml210Expected.put("obsoletedBy", "peggym.130.5");
        eml210Expected.put("dateUploaded", dateConverter.convert("2011-08-31T15:59:50.071163"));
        eml210Expected.put("dateModified", dateConverter.convert("2011-08-31T15:59:50.072921"));
        eml210Expected.put("datasource", "test_documents");
        eml210Expected.put("authoritativeMN", "test_documents");
        eml210Expected.put("replicaMN", "");
        eml210Expected.put("replicationStatus", "");
        eml210Expected.put("replicaVerifiedDate", "");
        eml210Expected.put("readPermission", "public#dataone_test_user##dataone_public_user");
        eml210Expected.put("writePermission", "dataone_integration_test_user");
        eml210Expected.put("changePermission", "");
        eml210Expected.put("isPublic", "true");
        eml210Expected.put("dataUrl", "https://" + hostname + "/cn/v2/resolve/peggym.130.4");

        eml210Expected.put("isService", "false");
        eml210Expected.put("serviceTitle", "");
        eml210Expected.put("serviceDescription", "");
        eml210Expected.put("serviceEndpoint", "");
        eml210Expected.put("licenseDescription", "");
        
        emlRefExpected
                .put("abstract",
                        "National Oceanic and Atmospheric Administration treatment effects studies from 1989 through 1997 suggested that bivalve assemblages on beaches in Prince William Sound treated with high-pressure washing were severely injured in terms of abundance, species composition, and function. Restoration Project 040574 assessed the generality and persistence of this apparent injury to this assemblage. We found that the initial conclusions were accurate, indicating that a considerable proportion of mixed-soft beaches in treated areas of the sound remained extremely disturbed and that these beaches are functionally impaired in terms of their ability to support foraging by humans and damaged nearshore vertebrate predators such as sea otters 13 years after the spill. Large, long-lived hard-shell clams remained 66% less abundant at Treated sites than at Reference sites. We also found that standard sediment properties did not appear implicated in lagging recovery. But, based on several lines of evidence, we deduced that a major cause for the delay was the disruption of surface armoring (a stratified organization of mixed-soft shoreline sediments common in southcentral Alaska), an effect of beach washing. Based on the apparent recovery trajectory, we predict that recovery to pre-spill status will take several more decades. We also found that sedimentary components and the biota in the armored mixed-soft sediments in Prince William Sound do not respond according to traditionally described paradigms for homogeneous sediments.Citation: Lees, D. C., and W. B. Driskell.  2007.  Assessment of Bivalve Recovery on Treated Mixed-Soft Beaches in Prince William Sound, Alaska.  Exxon Valdez Oil Spill Restoration Project Final Report (Restoration Project 040574).  National Oceanic & Atmospheric Administration National Marine Fisheries Service, Office of Oil Spill Damage & Restoration, Auke Bay, Alaska.");
        emlRefExpected
                .put("keywords",
                        "armoring#sediment condition#recruitment#shoreline treatment#beach washing#bivalves#clams#Exxon Valdez#oil spill#Alaska#Prince William Sound#Hiatella arctica#Protothaca staminea#Leukoma staminea#Saxidomus gigantea#high-pressure hot water wash#injury#recovery#Exxon Valdez Oil Spill Trustee Council#EVOSTC");
        emlRefExpected
                .put("title",
                        "Assessment of Bivalve Recovery on Treated Mixed-Soft Beaches In Prince William Sound, 1989-1997");
        emlRefExpected
                .put("project",
                        "Assessment of Bivalve Recovery on Treated Mixed-Soft Beaches in Prince William Sound");
        emlRefExpected.put("southBoundCoord", "60.0653");
        emlRefExpected.put("northBoundCoord", "60.5435");
        emlRefExpected.put("westBoundCoord", "-148.047");
        emlRefExpected.put("eastBoundCoord", "-147.604");
        emlRefExpected.put("geohash_1", "b");
        emlRefExpected.put("geohash_2", "bd");
        emlRefExpected.put("geohash_3", "bdw");
        emlRefExpected.put("geohash_4", "bdwz");
        emlRefExpected.put("geohash_5", "bdwzh");
        emlRefExpected.put("geohash_6", "bdwzh4");
        emlRefExpected.put("geohash_7", "bdwzh4h");
        emlRefExpected.put("geohash_8", "bdwzh4hf");
        emlRefExpected.put("geohash_9", "bdwzh4hf8");
        emlRefExpected.put("site", "Western Prince William Sound");
        emlRefExpected.put("beginDate", dateConverter.convert("2002-08-07"));
        emlRefExpected.put("endDate", dateConverter.convert("2002-08-14"));
        emlRefExpected.put("pubDate", "");
        emlRefExpected.put("author", "Dennis Lees");
        emlRefExpected.put("authorGivenName", "Dennis");
        emlRefExpected.put("authorSurName", "Lees");
        emlRefExpected.put("authorGivenNameSort", "Dennis");
        emlRefExpected.put("authorSurNameSort", "Lees");
        emlRefExpected.put("authorLastName", "Lees");
        emlRefExpected.put("investigator", "Lees");
        emlRefExpected.put("origin", "Dennis Lees");
        emlRefExpected.put("contactOrganization", "Littoral Ecological and Environmental Services");
        emlRefExpected.put("genus", "Hiatella#Leukoma#Saxidomus");
        emlRefExpected.put("species", "arctica#staminea#gigantea");
        emlRefExpected.put("kingdom", "");
        emlRefExpected.put("order", "Bivalvia");
        emlRefExpected.put("phylum", "Mollusca");
        emlRefExpected.put("family", "");
        emlRefExpected.put("class", "Bivalvia");
        emlRefExpected
                .put("scientificName", "Hiatella arctica#Leukoma staminea#Saxidomus gigantea");
        emlRefExpected
                .put("attributeName",
                        "site#replicate#species#number sampled#sieved volume (L)#Site#Replicate#Species#Length (mm)#Annuli#Site#Replicate#Species#Number sampled#Length (mm)#Annuli#Date#Site#Treatment#Latitude (degree in decimals)#Longitude (degree in decimals)#Latitude (degrees)#Latitude (seconds)#Longitude (degrees)#Longitude (seconds)#% silt/clay#TOC %#TKN (mg/kg)#C:N ratio");
        emlRefExpected.put("attributeLabel", "");
        emlRefExpected
                .put("attributeDescription",
                        "Name of site#Number of sample taken at the site#Name of species sampled#Number of individuals sampled#Volume sieved#Name of site#Number of sample taken at the site#Name of species sampled#Length of specimen#Count of annuli (growth rings)#name of site#number of sample taken#name of species#number of individuals sampled#length of sample#number of annuli (growth rings)#date of sampling#Name of site#treated or reference site#latitude in decimal degrees#longitude in decimal degrees#latitude in degrees#latitude in seconds#longitude in degrees#longitude seconds#silt to clay ratio#total organic carbon#Total Kjeldahl nitrogen#ratio of Carbon to Nitrogen");
        emlRefExpected
                .put("attributeUnit",
                        "dimensionless#dimensionless#liter#dimensionless#millimeter#dimensionless#dimensionless#dimensionless#millimeter#dimensionless#degree#degree#degree#degree#degree#degree#dimensionless#dimensionless#miligramsPerKilogram#dimensionless");
        emlRefExpected
                .put("attribute",
                        "site  Name of site#replicate  Number of sample taken at the site dimensionless#species  Name of species sampled#number sampled  Number of individuals sampled dimensionless#sieved volume (L)  Volume sieved liter#Site  Name of site#Replicate  Number of sample taken at the site dimensionless#Species  Name of species sampled#Length (mm)  Length of specimen millimeter#Annuli  Count of annuli (growth rings) dimensionless#Site  name of site#Replicate  number of sample taken dimensionless#Species  name of species#Number sampled  number of individuals sampled dimensionless#Length (mm)  length of sample millimeter#Annuli  number of annuli (growth rings) dimensionless#Date  date of sampling#Site  Name of site#Treatment  treated or reference site#Latitude (degree in decimals)  latitude in decimal degrees degree#Longitude (degree in decimals)  longitude in decimal degrees degree#Latitude (degrees)  latitude in degrees degree#Latitude (seconds)  latitude in seconds degree#Longitude (degrees)  longitude in degrees degree#Longitude (seconds)  longitude seconds degree#% silt/clay  silt to clay ratio dimensionless#TOC %  total organic carbon dimensionless#TKN (mg/kg)  Total Kjeldahl nitrogen miligramsPerKilogram#C:N ratio  ratio of Carbon to Nitrogen dimensionless");

        emlRefExpected.put("fileID", "https://" + hostname + "/cn/v2/resolve/df35c.9.14");
        emlRefExpected.put("text", "Assessment of Bivalve Recovery on Treated Mixed-Soft Beaches In Prince William Sound, 1989-1997 Mr.  Dennis  Lees   Littoral Ecological and Environmental Services  Principal Investigator  1075 Urania Ave.  Leucadia  CA  92024  USA   760-635-7998  760-635-7999  dennislees@earthlink.net  1359152217358  William  Driskell   6536 20th Ave. NE  Seattle  WA  98115  USA   206-522-5930  bdriskell@comcast.net  Principal Investigator  Emma  Freeman   National Center for Ecological Analysis and Synthesis  Graduate Student Researcher  735 State Street  Suite 300  Santa Barbara  California  93101  USA   1.805.892.2500  http://www.nceas.ucsb.edu  Processor  National Oceanic and Atmospheric Administration treatment effects studies from 1989 through 1997 suggested that bivalve assemblages on beaches in Prince William Sound treated with high-pressure washing were severely injured in terms of abundance, species composition, and function. Restoration Project 040574 assessed the generality and persistence of this apparent injury to this assemblage. We found that the initial conclusions were accurate, indicating that a considerable proportion of mixed-soft beaches in treated areas of the sound remained extremely disturbed and that these beaches are functionally impaired in terms of their ability to support foraging by humans and damaged nearshore vertebrate predators such as sea otters 13 years after the spill. Large, long-lived hard-shell clams remained 66% less abundant at Treated sites than at Reference sites. We also found that standard sediment properties did not appear implicated in lagging recovery. But, based on several lines of evidence, we deduced that a major cause for the delay was the disruption of surface armoring (a stratified organization of mixed-soft shoreline sediments common in southcentral Alaska), an effect of beach washing. Based on the apparent recovery trajectory, we predict that recovery to pre-spill status will take several more decades. We also found that sedimentary components and the biota in the armored mixed-soft sediments in Prince William Sound do not respond according to traditionally described paradigms for homogeneous sediments.Citation: Lees, D. C., and W. B. Driskell.  2007.  Assessment of Bivalve Recovery on Treated Mixed-Soft Beaches in Prince William Sound, Alaska.  Exxon Valdez Oil Spill Restoration Project Final Report (Restoration Project 040574).  National Oceanic & Atmospheric Administration National Marine Fisheries Service, Office of Oil Spill Damage & Restoration, Auke Bay, Alaska.  armoring  sediment condition  recruitment  shoreline treatment  beach washing  bivalves  clams  Exxon Valdez  oil spill  Alaska  Prince William Sound  Hiatella arctica  Protothaca staminea  Leukoma staminea  Saxidomus gigantea  high-pressure hot water wash  injury  recovery  Exxon Valdez Oil Spill Trustee Council  EVOSTC  This material is based upon work funded by the Exxon Valdez Oil Spill Trustee Council.  Any opinions, findings, conclusions, or recommendations expressed herein are those of the author(s) and do not necessarily reflect the views or positions of the Trustee Council.Standard scientific norms for attribution and credit should be followed when using this data including to the Owners, Exxon Valdez Oil Spill Trustee Council and other sources of funding. Please let the Owner know when this data is used.This data is licensed under CC0. The person who associated a work with this deed has dedicated the work to the public domain by waiving all of his or her rights to the work worldwide under copyright law, including all related and neighboring rights, to the extent allowed by law. You can copy, modify, distribute and perform the work, even for commercial purposes, all without asking permission.  Western Prince William Sound  -148.047  -147.604  60.5435  60.0653    2002-08-07   2002-08-14     Phylum  Mollusca  Class  Bivalvia  Genus  Hiatella  Species  arctica  Nestling clam      Phylum  Mollusca  Order  Bivalvia  Genus  Leukoma  Species  staminea  Littleneck clam      Phylum  Mollusca  Order  Bivalvia  Genus  Saxidomus  Species  gigantea  Butter clam       1359152118258  Site selection  To optimize the potential for detecting treatment effects, sampling was focused on intertidal mixed-soft sediment beaches in central and southwestern PWS where the greatest quantities of oil from the spill were stranded.  Using the NOAA Shoreline Segment Summary and Alaska Department of Natural Resources GIS databases and historic spill-survey documents from 1989 and 1990, appropriate shoreline segments were selected based on exposure, sediment type, degree of oiling, and recommended treatment history.  A random selection of appropriate shoreline segments was made from this list.  Each of the selected segments was then physically viewed during an aerial reconnaissance to determine where suitable sites with an adequate stretch of beach composed of mixed-soft sediments existed at an appropriate tidal elevation.  Final selections were made randomly from this final list of confirmed workable sites.  Four sites from the previous NOAA program were included to assess the degree of consistency with the NOAA studies.  These included one reference site (Bay of Isles) and three treated sites (Northwest Bay West Arm, Shelter Bay, and Sleepy Bay).  Determining the treatment history for any particular stretch of shoreline was a somewhat difficult and complex task.  In the manner previously reported by Mearns (1996), we used NOAA's Shoreline Segment Summary database to assign substrate type, relative degree of oiling (no-, light-, moderate-, or heavy oiling), types of treatment (e.g., moderate- to high-pressure or warm- or hot-water), number of types of treatment, and number of treatment days on a segment.  Mearns (1996) concluded that, although \"...treatment varied greatly among shorelines...treatment effort was generally proportional to the amount of oil present.\" According to his data for Eleanor and Ingot Islands, 81% of the heavily oiled sites were exposed to warm or hot water and 71% were exposed to both.  In addition, 80% of the moderately oiled sites were exposed to warm or hot water.  Only about 10% of the moderately or heavily oiled segments were not treated or did not have accompanying treatment characterization.  From these data, one can conclude that most heavily or moderately oiled sites were washed with hot or warm water.  However, we are unaware of any available public records that actually record treatment for the particular beaches within a portion of a shoreline segment.  Detailed oiling reports and recommended treatments are recorded in the Shoreline Cleanup Assessment Team (SCAT) reports (available at the Alaska Resource Library and Information Services in Anchorage), which include sketches of the distribution of oil and sediments.  The oiling reports and treatment recommendations in these records formed the predominant basis for our decisions on which beaches to survey.  However, it should be clear that, except for the NOAA Treated sites, where we were able to observe treatment underway in 1989, the treatment history of both Treated and Reference sites is based only on educated conjecture.  It is likely that some of the sites were inadvertently misclassified.     Physico-chemical Sediment Analysis  Bulk sediment samples were collected at all sites for analysis of particle grain size (PGS), total organic carbon (TOC), and total Kjeldahl nitrogen (TKN).  These samples were composited from surficial sediments scooped approximately 2 cm deep at points immediately adjacent to three randomly selected sampling locations for the infaunal samples.  Thus, the single composite-sample method did not provide a measure of within site variance.  Each sample was preserved by freezing.  PGS distributions were determined using a pipette method (Plumb 1981) modified to correct for dissolved solids (i.e., salinity and the dispersant added to keep silt/clay particles from clumping). Percent weights within each phi category were used to calculate cumulative phi values for 16, 50, and 84 percent of each sample.  Two statistics were determined from these values.  Median grain size = phi50.  Median grain size in mm = 2exp(-phi50).  The equation used to calculate the sorting coefficient for each sample = (phi84-phi16)/2.  In the laboratory, the samples used for analysis of organic nutrients in the sediments were purged of inorganic carbon, dried at 70 degrees C, ground, and sieved through a 120 mesh screen.  TOC was measured on a Dohrman DC 180 Carbon Analyzer using EPA method 415.1/5310B.  TKN was measured by chromate digestion as described in EPA Method 351.4.  Quality control (QC) for TOC included analysis of standards, method blanks, and comparison of replicate analyses.  All QC analyses for TOC fell within acceptable QC limits.  QC for TKN included analyses of spiked blanks and replicate analyses of spiked samples.  All of the spiked TKN blank analyses fell within QC limits.  However, none of the RPD or REC for the replicate analyses was within QC limits.     Shoreline Exposure  Because many physical and biological variables can be correlated with the intensity of exposure to wave action, variations in exposure to wave action can be a confounding factor.  One commonly used method for estimating exposure is to measure fetch, i.e., the unobstructed distance across open water that wind or waves travel before encountering a beach at a perpendicular angle.  Accordingly, we estimated fetch for each site by measuring the distance to the nearest landfall in a directly offshore direction using a navigation chart.  Nevertheless, using fetch as a measure of exposure is a very crude and potentially inaccurate approach.  It ignores the importance of the direction from which the dominant wind or waves arrive, the seasonal differences in the potential velocity and frequency of winds from the direction of the fetch, and the mitigating effects of local topography and offshore bathymetry (subtidal reefs, etc.), all of which are poorly known in this region.  Consequently, we devised another approach that integrates a variety of exposure-related physical and biological factors to provide an index of exposure.  Using our site photos and field notes to assess the various criteria, we devised an ordinal evaluation of twelve site conditions that reflect the degree of exposure.  The factors included seven physical characteristics of the beach (shape and weathering of individual rocks, degree of imbrication or armoring of the rock population, presence of silt on coarser sediments or rocks, and the susceptibility of the site to current or wave action), and five biological characteristics (absence or level of development of epibenthic algae, animals, or an amorphous biological turf on the rocks, eelgrass or burrowing organisms in the sediments).  Each feature was scored on a scale of 1 to 5.  We then averaged factor scores for each site to provide an integrated exposure score for each site.  By this method, low exposure scores indicate protected sites whereas high scores indicate exposed sites.  Each site was scored without knowledge of its treatment classification in order to avoid biasing the score.  The exposure scores were then paired with the appropriate environmental or biological variables for each site to evaluate the importance of exposure in any observed patterns.     Biological Sampling  In the NOAA studies cited above, we used a clam-gun core to sample infauna and the associated bivalves.  It became clear when we analyzed these samples that this approach provided good information on smaller clam species and juveniles of larger clam species but did not provide adequate data on abundance and size structure of the naturally less abundant, older, larger size clams.  This shortcoming created an important gap in our understanding of the long-term dynamics of clam populations and recovery.  Consequently, we chose to use two contrasting methods to gain a fuller understanding of population and recovery dynamics.  Smaller bivalves were sampled using core samplers 10.7 cm in diameter (0.009 m2) by 15 cm deep, replicating the methods used in the NOAA study.  Five cores (total of 0.045 m2 sampled) were collected at randomly selected locations along a 30-m transect laid horizontally at each site at the lowest feasible level for completing the sampling and within the specified elevation range (0 to 0.8 m [+2.6 feet] above MLLW); the actual level varied with differing tide stage.  Each sample was field-sieved through a 1.0 mm mesh screen, washed into a double-labeled Ziploc bag, and fixed with buffered 10% formalin-seawater solution.  These samples were collected to provide data consistent with and comparable to the NOAA program and to gain an understanding of the status of smaller clam species and younger size classes of the larger, more longevous clams.  For the larger, older, less abundant and typically more dispersed bivalves, sediments were excavated to a depth of 15 cm using a shovel and hands inside a square 0.0625-m2 quadrat.  Three replicate excavations (a total of 0.1875 m2 sampled) were collected adjacent to first, third, and fifth randomly placed core samples described above.  These sediments were sieved on site through 6.35-mm (0.25-inch) mesh hardware cloth, the bivalves removed, placed in labeled bags, and frozen for shipment to the laboratory.  This approach provided useful information on abundance and size and age structure of the larger size classes.  These samples were collected to gain an understanding of the status of older size classes of the larger, more longevous clams  The two sample types provide complementary data.  The core data provide data on a wider spectrum of sizes but, because larger animals are generally rare, these data are better suited for evaluation of the smaller clams and juveniles of the larger species.  This component is lost in the sieving process for the excavation samples but, because that approach samples four times the surface area, it provides substantially better information on the larger, less abundant clams.  Processing the excavation samples with the finer mesh sieve used for the core samples would require an inordinate amount of time both in the field and in the lab.  Following receipt in the laboratory, the samples were washed on a 1-mm sieve to remove the formalin-seawater solution and then preserved with 70% isopropyl alcohol.  After identification and enumeration in the laboratory, shell length was measured with digital calipers to 0.1-mm precision.  In addition, age was estimated for four species (Protothaca, Saxidomus, and Hiatella, Macoma inquinata) by counting growth checks (annuli).  Arbitrary size criteria based on examination of the size-frequency histograms for each species were used to distinguish juveniles from adults for one set of analyses.  For Protothaca, specimens <10 mm in shell length were classified as juvenile.  For Saxidomus, Macoma inquinata, and Hiatella, shell length criteria for juveniles were <12, <15, and <6 mm, respectively.     Assessment of Bivalve Recovery on Treated Mixed-Soft Beaches in Prince William Sound  Dennis  Lees   PI   Exxon Valdez Oil Spill Trustee Council Project Number 574 df35c.9.14 site replicate species number sampled sieved volume (L) Site Replicate Species Length (mm) Annuli Number sampled Date Treatment Latitude (degree in decimals) Longitude (degree in decimals) Latitude (degrees) Latitude (seconds) Longitude (degrees) Longitude (seconds) % silt/clay TOC % TKN (mg/kg) C:N ratio Name of site Number of sample taken at the site Name of species sampled Number of individuals sampled Volume sieved Length of specimen Count of annuli (growth rings) name of site number of sample taken name of species number of individuals sampled length of sample number of annuli (growth rings) date of sampling treated or reference site latitude in decimal degrees longitude in decimal degrees latitude in degrees latitude in seconds longitude in degrees longitude seconds silt to clay ratio total organic carbon Total Kjeldahl nitrogen ratio of Carbon to Nitrogen dimensionless liter millimeter degree miligramsPerKilogram");
        emlRefExpected.put("isService", "false");
        emlRefExpected.put("serviceTitle", "");
        emlRefExpected.put("serviceDescription", "");
        emlRefExpected.put("serviceEndpoint", "");
        emlRefExpected.put("licenseDescription", "This material is based upon work funded by the Exxon Valdez Oil Spill Trustee Council.  Any opinions, findings, conclusions, or recommendations expressed herein are those of the author(s) and do not necessarily reflect the views or positions of the Trustee Council.Standard scientific norms for attribution and credit should be followed when using this data including to the Owners, Exxon Valdez Oil Spill Trustee Council and other sources of funding. Please let the Owner know when this data is used.This data is licensed under CC0. The person who associated a work with this deed has dedicated the work to the public domain by waiving all of his or her rights to the work worldwide under copyright law, including all related and neighboring rights, to the extent allowed by law. You can copy, modify, distribute and perform the work, even for commercial purposes, all without asking permission.");
    }

    public void setUpEml220() throws Exception {
        eml220Expected.put("title", "EML Annotation Example");
        eml220Expected.put("abstract", "");
        eml220Expected.put("keywords", "");

        eml220Expected.put("project", "MY PROJECT");
        eml220Expected.put("funding", "SOME_RANDOM_FUNDING_INFO");
        eml220Expected.put("funderName", "My Funder");
        eml220Expected.put("funderIdentifier", "MY_FUNDER");
        eml220Expected.put("awardNumber", "AWARD1");
        eml220Expected.put("awardTitle", "An example award title");

        eml220Expected.put("southBoundCoord", "");
        eml220Expected.put("northBoundCoord", "");
        eml220Expected.put("westBoundCoord", "");
        eml220Expected.put("eastBoundCoord", "");
        eml220Expected.put("geohash_1", "");
        eml220Expected.put("geohash_2", "");
        eml220Expected.put("geohash_3", "");
        eml220Expected.put("geohash_4", "");
        eml220Expected.put("geohash_5", "");
        eml220Expected.put("geohash_6", "");
        eml220Expected.put("geohash_7", "");
        eml220Expected.put("geohash_8", "");
        eml220Expected.put("geohash_9", "");
        eml220Expected.put("site", "");
        eml220Expected.put("beginDate", "");
        eml220Expected.put("endDate", "");
        eml220Expected.put("pubDate", "");
        eml220Expected.put("author", "EML Annotator");
        eml220Expected.put("authorGivenName", "EML");
        eml220Expected.put("authorSurName", "Annotator");
        eml220Expected.put("authorGivenNameSort", "EML");
        eml220Expected.put("authorSurNameSort", "Annotator");
        eml220Expected.put("authorLastName", "Annotator");
        eml220Expected.put("investigator", "Annotator");
        eml220Expected.put("contactOrganization", "");
        eml220Expected.put("genus","");
        eml220Expected.put("species","");
        eml220Expected.put("kingdom", "");
        eml220Expected.put("order", "");
        eml220Expected.put("phylum", "");
        eml220Expected.put("family", "");
        eml220Expected.put("class", "");
        eml220Expected.put("scientificName", "");
        eml220Expected.put("origin", "EML Annotator");
        eml220Expected
                .put("attributeName",
                        "SOME_ATTRIBUTE");
        eml220Expected.put("attributeLabel", "");
        eml220Expected
                .put("attributeDescription",
                        "SOME_ATTRIBUTE's definition");
        eml220Expected
                .put("attributeUnit",
                        "");
        eml220Expected
                .put("attribute",
                        "SOME_ATTRIBUTE  SOME_ATTRIBUTE's definition");

        eml220Expected.put("fileID", "https://" + hostname + "/cn/v2/resolve/eml-test-doc");
        eml220Expected
                .put("text",
                        "EML Annotation Example   EML  Annotator     EML  Annotator    MY PROJECT    EML  Annotator   principalInvestigator   SOME_RANDOM_FUNDING_INFO   My Funder  MY_FUNDER  AWARD1  An example award title  https://example.org/someaward eml-test-doc SOME_ATTRIBUTE SOME_ATTRIBUTE's definition");

        // system metadata
        eml220Expected.put("id", "eml-test-doc");
        eml220Expected.put("seriesId", "");
        eml220Expected.put("fileName", "");
        eml220Expected.put("mediaType", "");
        eml220Expected.put("mediaTypeProperty", "");
        eml220Expected.put("formatId", "https://eml.ecoinformatics.org/eml-2.2.0");
        eml220Expected.put("formatType", "METADATA");
        eml220Expected.put("size", "0");
        eml220Expected.put("checksum", "12345");
        eml220Expected.put("checksumAlgorithm", "MD5");
        eml220Expected.put("submitter", "dataone_integration_test_user");
        eml220Expected.put("rightsHolder", "dataone_integration_test_user");
        eml220Expected.put("replicationAllowed", "true");
        eml220Expected.put("numberReplicas", "");
        eml220Expected.put("archived", "false");
        eml220Expected.put("preferredReplicationMN", "");
        eml220Expected.put("blockedReplicationMN", "");
        eml220Expected.put("obsoletes", "");
        eml220Expected.put("obsoletedBy", "");
        eml220Expected.put("dateUploaded", dateConverter.convert("2019-07-31T15:59:47.000"));
        eml220Expected.put("dateModified", dateConverter.convert("2019-07-31T15:59:47.000"));
        eml220Expected.put("datasource", "test_documents");
        eml220Expected.put("authoritativeMN", "test_documents");
        eml220Expected.put("replicaMN", "");
        eml220Expected.put("replicationStatus", "");
        eml220Expected.put("replicaVerifiedDate", "");
        eml220Expected.put("readPermission", "public#dataone_public_user");
        eml220Expected.put("writePermission", "dataone_integration_test_user");
        eml220Expected.put("changePermission", "");
        eml220Expected.put("isPublic", "true");
        eml220Expected.put("dataUrl", "https://" + hostname + "/cn/v2/resolve/eml-test-doc");

        eml220Expected.put("isService", "false");
        eml220Expected.put("serviceTitle", "");
        eml220Expected.put("serviceDescription", "");
        eml220Expected.put("serviceEndpoint", "");
    }

    public void setUpEmlService() throws Exception {
        // science metadata
        emlServiceExpected.put("abstract", "");
        emlServiceExpected.put("keywords", "");
        emlServiceExpected.put("title", "");
        emlServiceExpected.put("project", "");
        emlServiceExpected.put("southBoundCoord", "");
        emlServiceExpected.put("northBoundCoord", "");
        emlServiceExpected.put("westBoundCoord", "");
        emlServiceExpected.put("eastBoundCoord", "");
        emlServiceExpected.put("geohash_1", "");
        emlServiceExpected.put("geohash_2", "");
        emlServiceExpected.put("geohash_3", "");
        emlServiceExpected.put("geohash_4", "");
        emlServiceExpected.put("geohash_5", "");
        emlServiceExpected.put("geohash_6", "");
        emlServiceExpected.put("geohash_7", "");
        emlServiceExpected.put("geohash_8", "");
        emlServiceExpected.put("geohash_9", "");
        emlServiceExpected.put("site", "");
        emlServiceExpected.put("beginDate", "");
        emlServiceExpected.put("endDate", "");
        emlServiceExpected.put("pubDate", "");
        emlServiceExpected.put("author", "");
        emlServiceExpected.put("authorGivenName", "");
        emlServiceExpected.put("authorSurName", "");
        emlServiceExpected.put("authorGivenNameSort", "");
        emlServiceExpected.put("authorSurNameSort", "");
        emlServiceExpected.put("authorLastName", "");
        emlServiceExpected.put("investigator", "");
        emlServiceExpected.put("contactOrganization", "");
        emlServiceExpected.put("genus", "");
        emlServiceExpected.put("species", "");
        emlServiceExpected.put("kingdom", "");
        emlServiceExpected.put("order", "");
        emlServiceExpected.put("phylum", "");
        emlServiceExpected.put("family", "");
        emlServiceExpected.put("class", "");
        emlServiceExpected.put("scientificName", "");
        emlServiceExpected.put("origin", "");
        emlServiceExpected.put("attributeName", "");
        emlServiceExpected.put("attributeLabel", "");
        emlServiceExpected.put("attributeDescription", "");
        emlServiceExpected.put("attributeUnit", "");
        emlServiceExpected.put("attribute", "");
        emlServiceExpected.put("fileID", "https://" + hostname + "/cn/v2/resolve/" + serviceEmlPid);
        emlServiceExpected.put("text", "" + serviceEmlPid);

        // service fields
        emlServiceExpected.put("isService", "true");
        emlServiceExpected.put("serviceTitle", "PACIOOS: Water Quality Processing Software, Matlab DataProcessor, Configure, Scedule scripts");
        emlServiceExpected.put("serviceDescription", "PACIOOS processes water quality data in near real-time (with only network transfer delays)          and produces 1, 3, 7, 10, 21, and 30-day plots of conductivity, salinity, pressure,          chlorophyll, and turbidity measurements for each instrument location on the islands of          Oahu, Maui, and other insular Pacific sites.:The ala-wai-canal-ns02-matlab-processing-schedule_AW02XX_001CTDXXXXR00_processing.1.m provides          configuration values that get set in the ala-wai-canal-ns02-matlab-processing-Configure.1.m          class, which is in turn used by the ala-wai-canal-ns02-matlab-processing-DataProcessor.1.m          class to process the raw data and produce EPS and JPEG figures.");
        emlServiceExpected.put("serviceEndpoint", "https://cn-sandbox-2.test.dataone.org/cn/v1/resolve/ala-wai-canal-ns02-matlab-processing-schedule_AW02XX_001CTDXXXXR00_processing.1.m"
                + "#" + "https://cn-sandbox-2.test.dataone.org/cn/v1/resolve/ala-wai-canal-ns02-matlab-processing-DataProcessor.1.m"
                + "#" + "https://cn-sandbox-2.test.dataone.org/cn/v1/resolve/ala-wai-canal-ns02-matlab-processing-Configure.1.m");
                
        // not checking system metadata
        emlServiceExpected.put("id", serviceEmlPid);
        emlServiceExpected.put("seriesId", "");
        emlServiceExpected.put("fileName", "");
        emlServiceExpected.put("mediaType", "");
        emlServiceExpected.put("mediaTypeProperty", "");
        emlServiceExpected.put("formatId", "eml://ecoinformatics.org/eml-2.1.0");
        emlServiceExpected.put("formatType", "METADATA");
        emlServiceExpected.put("size", "36281");
        emlServiceExpected.put("checksum", "24426711d5385a9ffa583a13d07af2502884932f");
        emlServiceExpected.put("checksumAlgorithm", "SHA-1");
        emlServiceExpected.put("submitter", "dataone_integration_test_user");
        emlServiceExpected.put("rightsHolder", "dataone_integration_test_user");
        emlServiceExpected.put("replicationAllowed", "true");
        emlServiceExpected.put("numberReplicas", "");
        emlServiceExpected.put("archived", "false");
        emlServiceExpected.put("preferredReplicationMN", "");
        emlServiceExpected.put("blockedReplicationMN", "");
        emlServiceExpected.put("obsoletes", "");
        emlServiceExpected.put("obsoletedBy", "");
        emlServiceExpected.put("dateUploaded", dateConverter.convert("2011-08-31T15:59:50.071163"));
        emlServiceExpected.put("dateModified", dateConverter.convert("2011-08-31T15:59:50.072921"));
        emlServiceExpected.put("datasource", "test_documents");
        emlServiceExpected.put("authoritativeMN", "test_documents");
        emlServiceExpected.put("replicaMN", "");
        emlServiceExpected.put("replicationStatus", "");
        emlServiceExpected.put("replicaVerifiedDate", "");
        emlServiceExpected.put("readPermission", "public#dataone_test_user##dataone_public_user");
        emlServiceExpected.put("writePermission", "dataone_integration_test_user");
        emlServiceExpected.put("changePermission", "");
        emlServiceExpected.put("isPublic", "true");
        emlServiceExpected.put("dataUrl", "https://" + hostname + "/cn/v2/resolve/" + serviceEmlPid);
        emlServiceExpected.put("licenseDescription", "");

    }
    
    /**
     * Testing that the Xpath expressions used by XPathParser and associates are
     * 'mining' the expected data from the science and system metadata
     * documents.
     * 
     * @throws Exception
     */
    @Test
    public void testEml210ScienceMetadataFields() throws Exception {
        testXPathParsing(eml210Subprocessor, peggym1304SysObsoletedBy, peggym1304Sci,
                eml210Expected, "peggym.130.4");
    }

    @Test
    public void testEml220ScienceMetadataFields() throws Exception {
        testXPathParsing(eml220Subprocessor, eml220TestDocSysMeta, eml220TestDocSciMeta,
                eml220Expected, "eml-test-doc");
    }

    /**
     * Example of an eml document that uses references.
     * Tests that double pipe characters are not indexed when a reference is found
     * OR and empty creator element.
     * 
     * Origin element contained the double pipe characters before fix.
     * @throws Exception
     */
    @Test
    public void testEmlRefScienceMetadataFields() throws Exception {
        testXPathParsing(eml210Subprocessor, null, emlRefSciMeta, emlRefExpected, "df35c.9.14");
    }
    
    /**
     * Example of an eml document that contains service information.
     */
    @Test
    public void testEmlServiceScienceMetadataFields() throws Exception {
        testXPathParsing(eml210Subprocessor, serviceDescriptionEmlSysMeta, serviceDescriptionEmlSciMeta, emlServiceExpected, serviceEmlPid);
    }
}
