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

import org.dataone.cn.indexer.convert.FgdcDateConverter;
import org.dataone.cn.indexer.convert.IConverter;
import org.dataone.cn.indexer.convert.SolrDateConverter;
import org.dataone.cn.indexer.parser.ScienceMetadataDocumentSubprocessor;
import org.junit.BeforeClass;
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
public class SolrFieldXPathFgdcTest extends BaseSolrFieldXPathTest {

    @Autowired
    private Resource fdgc01111999SciMeta;

    @Autowired
    private Resource fdgc01111999SysMeta;

    @Autowired
    private Resource fgdcNasaSciMeta;

    @Autowired
    private Resource fgdcNasaSysMeta;

    @Autowired
    private Resource fgdcEsriSysMeta;

    @Autowired
    private Resource fgdcEsriSciMeta;

    @Autowired
    private Resource ornl_mercury_science_metadata;

    @Autowired
    private Resource ornl_mercury_system_metadata;

    @Autowired
    private ScienceMetadataDocumentSubprocessor fgdcstd00111999Subprocessor;

    @Autowired
    private ScienceMetadataDocumentSubprocessor fgdcEsri80Subprocessor;

    @Autowired
    private ScienceMetadataDocumentSubprocessor ornlMercury10Subprocessor;

    private static IConverter dateConverter = new FgdcDateConverter();
    private static IConverter solrDateConverter = new SolrDateConverter();

    // solr/rule field name (from spring context fields) mapped to actual value
    // from xml documents (system and science metadata docs)
    private static HashMap<String, String> csiroExpected = new HashMap<String, String>();
    private static HashMap<String, String> fgdcNasaExpected = new HashMap<String, String>();
    private static HashMap<String, String> esriExpected = new HashMap<String, String>();
    private static HashMap<String, String> ornlMercuryExpected = new HashMap<String, String>();

    private static String csiro_pid = "www.nbii.gov_metadata_mdata_CSIRO_csiro_d_abayadultprawns";
    private static String nasa_pid = "www.nbii.gov_metadata_mdata_NASA_nasa_d_FEDGPS1293";
    private static String esri_pid = "nikkis.180.1";
    private static String ornl_mercury_pid = "Map_ORR_Aspect_2m_1993.xml";

    @BeforeClass
    public static void setUp() throws Exception {
        // science metadata
        csiroExpected
                .put("abstract",
                        "Adult prawn species, size, sex, reproductive stage, moult stage, and parasites were measured at 20 stations in Albatross Bay, Gulf of Carpentaria. Sampling was carried out monthly between 1986 and 1992. This metadata record is sourced from 'MarLIN', the CSIRO Marine Laboratories Information Network.");
        csiroExpected.put("beginDate", dateConverter.convert("19860301"));
        csiroExpected.put("class", "Malacostraca");
        csiroExpected.put("contactOrganization", "CSIRO Division of Marine Research-Hobart");
        csiroExpected.put("eastBoundCoord", "142.0");
        csiroExpected.put("westBoundCoord", "141.5");
        csiroExpected.put("southBoundCoord", "-13.0");
        csiroExpected.put("northBoundCoord", "-12.5");
        csiroExpected.put("geohash_1", "r");
        csiroExpected.put("geohash_2", "rj");
        csiroExpected.put("geohash_3", "rjs");
        csiroExpected.put("geohash_4", "rjsz");
        csiroExpected.put("geohash_5", "rjsz3");
        csiroExpected.put("geohash_6", "rjsz3w");
        csiroExpected.put("geohash_7", "rjsz3wg");
        csiroExpected.put("geohash_8", "rjsz3wgj");
        csiroExpected.put("geohash_9", "rjsz3wgjy");
        csiroExpected.put("edition", "");
        csiroExpected.put("endDate", dateConverter.convert("19920401"));
        csiroExpected.put("gcmdKeyword", "");
        csiroExpected.put("genus", "");
        csiroExpected.put("site", "Australia, Gulf of Carpentaria");
        csiroExpected.put("presentationCat", "maps data");
        csiroExpected.put("geoform", "maps data");
        csiroExpected.put("kingdom", "Animalia");
        csiroExpected.put("order", "Decapoda");
        csiroExpected.put("phylum", "Arthropoda#Test");
        csiroExpected.put("species", "");
        csiroExpected.put("family", "");
        csiroExpected.put("scientificName", "");
        csiroExpected.put("placeKey", "Australlia#Gulf of Carpentaria#Albatross Bay");
        csiroExpected.put("origin",
                "CSIRO Marine Research (formerly CSIRO Division of Fisheries/Fisheries Research)");
        csiroExpected.put("author",
                "CSIRO Marine Research (formerly CSIRO Division of Fisheries/Fisheries Research)");
        csiroExpected.put("authorSurName",
                "CSIRO Marine Research (formerly CSIRO Division of Fisheries/Fisheries Research)");
        csiroExpected.put("authorSurNameSort",
                "CSIRO Marine Research (formerly CSIRO Division of Fisheries/Fisheries Research)");
        csiroExpected.put("investigator",
                "CSIRO Marine Research (formerly CSIRO Division of Fisheries/Fisheries Research)");
        csiroExpected.put("pubDate", dateConverter.convert("1993"));
        csiroExpected
                .put("purpose",
                        "The purpose of the dataset is to provide information about adult prawn species in Albatross Bay, Gulf of Carpentaria.");
        // csiroExpected
        // .put("project",
        // "The purpose of the dataset is to provide information about adult prawn species in Albatross Bay, Gulf of Carpentaria.");

        csiroExpected.put("title", "Albatross Bay Adult Prawn Data 1986-1992");
        csiroExpected.put("webUrl", "");

        csiroExpected
                .put("keywords",
                        "BIOMASS|LANDSAT TM|LANDSAT-5#adult prawn data#size#sex#reproductive stage#moult stage#parasites#Australlia#Gulf of Carpentaria#Albatross Bay");
        csiroExpected.put("fileID", "https://" + hostname + "/cn/v2/resolve/" + csiro_pid);

        csiroExpected.put("attributeName", "");
        csiroExpected.put("attributeLabel", "");
        csiroExpected.put("attributeDescription", "");
        csiroExpected.put("attributeUnit", "");
        csiroExpected.put("attribute", "");

        csiroExpected
                .put("text",
                        "http://www.nbii.gov/metadata/mdata/CSIRO/csiro_d_abayadultprawns.xml     CSIRO Marine Research (formerly CSIRO Division of Fisheries/Fisheries Research)  1993  Albatross Bay Adult Prawn Data 1986-1992  maps data   Australia  CSIRO Division of Marine Research      Adult prawn species, size, sex, reproductive stage, moult stage, and parasites were measured at 20 stations in Albatross Bay, Gulf of Carpentaria. Sampling was carried out monthly between 1986 and 1992. This metadata record is sourced from 'MarLIN', the CSIRO Marine Laboratories Information Network.  The purpose of the dataset is to provide information about adult prawn species in Albatross Bay, Gulf of Carpentaria.  Information was obtained from http://www.marine.csiro.au/marine/mcdd/data/CSIRODMR/CSIRODMR_datasets.html.The previous online linkage was determined to be broken in October 2010 and moved here.  The previous online linkage was:http://www.marine.csiro.au/marine/mcdd/data/CSIRODMR/Albatross_Bay_Adult_Prawn_Data_1986_1992.HTML      19860301  19920401    ground condition    Complete  None planned    Australia, Gulf of Carpentaria   141.5  142  -12.5  -13      ISO 19115 Topic Category  none    Parameter_Sensor_Source  BIOMASS|LANDSAT TM|LANDSAT-5    none  adult prawn data  size  sex  reproductive stage  moult stage  parasites  NONE    none  Australlia  Gulf of Carpentaria  Albatross Bay      none  prawns  shrimps  crustaceans       Agencies listed below  2002  Integrated Taxonomic Information System  Database   Washington, D.C.  U.S. Department of Agriculture   Department of Commerce, National Oceanic and Atmospheric Administration (NOAA),  Department of Interior (DOI), Geological Survey (USGS), Environmental Protection Agency (EPA), Department of Agriculture (USDA), Agriculture Research Service (ARS) Natural Resources Conservation Service (NRCS) Smithsonian Institution National Museum of Natural History (NMNH).  http://www.itis.usda.gov/       Kingdom  Animalia  animals   Phylum  Arthropoda  arthropods   Division  Test  arthropods   Subphylum  Crustacea  crustaceans   Class  Malacostraca   Subclass  Eumalacostraca   Superorder  Eucarida   Order  Decapoda  crabs  crayfishes  lobsters  prawns  shrimp           Release with the permission of the custodian.  None     Peter Crocos  CSIRO Division of Marine Research-Cleveland    mailing address  P.O. Box 120  Cleveland  Queensland  4163  Australia   unknown  unknown  peter.crocos@csiro.au      CSIRO Division of Marine Research  Unknown  Albatross Bay Chlorophyll Data 1986-1992  unknown   Australia  CSIRO Division of Marine Research       Stephen Blaber, David Brewer, John Salini, J. Kerr  Unknown  Albatross Bay Fish Data 1986-1988  unknown   Australia  CSIRO Division of Marine Research       Stever Blaber, CSIRO Division of Marine Research  Unknown  Albatross Bay Nearshore Fish Study 1991-1992  unknown   Australia  CSIRO Division of Marine Research       CSIRO Division of Marine Research  Unknown  Albatross Bay Nutrient Data 1992  unknown   Australia  CSIRO Division of Marine Research       Chris Jackson, CSIRO Division of Marine Research  Unknown  Albatross Bay Phytoplankton Data 1986-1992  unknown   Queensland, Australia  CSIRO Division of Marine Research       CSIRO Division of Marine Research  Unknown  Albatross Bay Prawn Larval Data  unknown   Queensland, Australia  CSIRO Division of Marine Research       CSIRO Division of Marine Research  Unknown  Albatross Bay Primary Productivity  unknown   Queensland, Australia  CSIRO Division of Marine Research       not applicable  Twenty stations were sampled.    Field  unknown    unknown  Unknown      Point     Entity - Adult Prawn in Albatross Bay, Gulf of Carpentaria, Australia; Attributes - size, sex, reproductive stage, moult stage, parasites  unknown        Tony Rees  CSIRO Division of Marine Research-Hobart    mailing address  Hobart  Australia   unknown  unknown  Tony.Rees@csiro.au    You accept all risks and responsibility for losses, damages, costs and other consequences resulting directly or indirectly from using this site and andinformation or material available from it. To the maximum permitted by law, CSIRO excludes all liability to any person arising directly or indirectly from using this site and any information or material available from it.  Please contact distributor.    19980710  20020930  20030930     Cheryl Solomon  Science Systems and Applications, Inc.   Metadata specialist   mailing and physical address  10210 Greenbelt Road, Suite 500  Lanham  Maryland  20706   301 867-2080  301-867-2149.  solomon@gcmd.nasa.gov    FGDC Biological Data Profile of the Content Standard for Digital Geospatial Metadata  FGDC-STD-001.1-1999 "
                                + csiro_pid);
        // system metadata
        csiroExpected.put("id", csiro_pid);
        csiroExpected.put("seriesId", "");
        csiroExpected.put("fileName", "");
        csiroExpected.put("mediaType", "");
        csiroExpected.put("mediaTypeProperty", "");
        csiroExpected.put("formatId", "FGDC-STD-001.1-1999");
        csiroExpected.put("formatType", "METADATA");
        csiroExpected.put("size", "9008");
        csiroExpected.put("checksum", "86bc6417ef29b6fbd279160699044e5e");
        csiroExpected.put("checksumAlgorithm", "MD5");
        csiroExpected.put("submitter", "CN=Dave Vieglais T799,O=Google,C=US,DC=cilogon,DC=org");
        csiroExpected.put("rightsHolder", "CN=Dave Vieglais T799,O=Google,C=US,DC=cilogon,DC=org");
        csiroExpected.put("replicationAllowed", "true");
        csiroExpected.put("numberReplicas", "3");
        csiroExpected.put("archived", "false");
        csiroExpected.put("preferredReplicationMN", "");
        csiroExpected.put("blockedReplicationMN", "");
        csiroExpected.put("obsoletes", "csiro_c_abayadultprawns");
        csiroExpected.put("obsoletedBy", "csiro_e_abayadultprawns");
        csiroExpected.put("dateUploaded", solrDateConverter.convert("2012-03-22T13:55:48.348202"));
        csiroExpected.put("dateModified", solrDateConverter.convert("2012-03-22T13:55:48.360604"));
        csiroExpected.put("datasource", "test_documents");
        csiroExpected.put("authoritativeMN", "test_documents");
        csiroExpected.put("replicaMN", "");
        csiroExpected.put("replicationStatus", "");
        csiroExpected.put("replicaVerifiedDate", "");
        csiroExpected.put("readPermission", "public");
        csiroExpected.put("writePermission",
                "CN=Dave Vieglais T799,O=Google,C=US,DC=cilogon,DC=org");
        csiroExpected.put("changePermission", "");
        csiroExpected.put("isPublic", "true");
        csiroExpected.put("dataUrl", "https://" + hostname + "/cn/v2/resolve/" + csiro_pid);

        /************************************************/
        /** Second test object expected data ************/
        // science metadata
        fgdcNasaExpected
                .put("abstract",
                        "The Biospheric Sciences Branch (formerly Earth Resources Branch) within the Laboratory for Terrestrial Physics at NASA's Goddard Space Flight Center and associated University investigators are involved in a research program entitled Forest Ecosystem Dynamics (FED) which is fundamentally concerned with vegetation change of forest ecosystems at local to regional spatial scales (100 to 10,000 meters) and temporal scales ranging from monthly to decadal periods (10 to 100 years). The nature and extent of the impacts of these changes, as well as the feedbacks to global climate, may be addressed through modeling the interactions of the vegetation, soil, and energy components of the boreal ecosystem. The Howland Forest research site lies within the Northern Experimental Forest of International Paper. The natural stands in this boreal-northern hardwood transitional forest consist of spruce-hemlock-fir, aspen-birch, and hemlock-hardwood mixtures. The topography of the region varies from flat to gently rolling, with a maximum elevation change of less than 68 m within 10 km. Due to the region's glacial history, soil drainage classes within a small area may vary widely, from well drained to poorly drained. Consequently, an elaborate patchwork of forest communities has developed, supporting exceptional local species diversity. This data set is in ARC/INFO export format and contains Global Positioning Systems (GPS) ground control points in and around the International Paper Experimental Forest, Howland ME.");
        fgdcNasaExpected.put("beginDate", dateConverter.convert("19931201"));
        fgdcNasaExpected.put("class", "");
        fgdcNasaExpected
                .put("contactOrganization",
                        "Forest Ecosystem Dynamics Project, Biospheric Sciences Branch, Hydrospheric and Biospheric Sciences Laboratory, Earth Sciences Division, Science and Exploration Directorate, Goddard Space Flight Center, NASA");
        fgdcNasaExpected.put("eastBoundCoord", "-68.0");
        fgdcNasaExpected.put("westBoundCoord", "-68.0");
        fgdcNasaExpected.put("southBoundCoord", "45.0");
        fgdcNasaExpected.put("northBoundCoord", "45.0");
        fgdcNasaExpected.put("geohash_1", "f");
        fgdcNasaExpected.put("geohash_2", "f2");
        fgdcNasaExpected.put("geohash_3", "f2p");
        fgdcNasaExpected.put("geohash_4", "f2p8");
        fgdcNasaExpected.put("geohash_5", "f2p8h");
        fgdcNasaExpected.put("geohash_6", "f2p8h8");
        fgdcNasaExpected.put("geohash_7", "f2p8h85");
        fgdcNasaExpected.put("geohash_8", "f2p8h85b");
        fgdcNasaExpected.put("geohash_9", "f2p8h85bj");
        fgdcNasaExpected.put("edition", "");
        fgdcNasaExpected.put("endDate", dateConverter.convert("19931231"));
        fgdcNasaExpected
                .put("gcmdKeyword",
                        "EARTH SCIENCE > HUMAN DIMENSIONS > LAND USE/LAND COVER > LAND MANAGEMENT > GROUND CONTROL POINT#SATELLITES#GPS > GLOBAL POSITIONING SYSTEM#FED > FOREST ECOSYSTEM DYNAMICS");
        fgdcNasaExpected.put("genus", "");
        fgdcNasaExpected.put("geoform", "Maps and Data");
        fgdcNasaExpected.put("kingdom", "");
        fgdcNasaExpected.put("order", "");
        fgdcNasaExpected.put("phylum", "");
        fgdcNasaExpected.put("species", "");
        fgdcNasaExpected.put("family", "");
        fgdcNasaExpected.put("scientificName", "");
        fgdcNasaExpected
                .put("site",
                        "The study site is located 56 km north of Bangor, Maine in Penobscot County (45 12'N, 68 44'W). The area is within the 7000 ha Northern Experimental Forest (NEF) owned by International Paper.");
        fgdcNasaExpected.put("presentationCat", "Maps and Data");
        fgdcNasaExpected
                .put("placeKey",
                        "CONTINENT > NORTH AMERICA#CONTINENT > NORTH AMERICA > UNITED STATES OF AMERICA > MAINE");
        fgdcNasaExpected
                .put("origin",
                        "Elizabeth M. Nel; NASA Goddard Space Flight Center, Forest Ecosystem Dynamics Project");
        fgdcNasaExpected
                .put("author",
                        "Elizabeth M. Nel; NASA Goddard Space Flight Center, Forest Ecosystem Dynamics Project");
        fgdcNasaExpected
                .put("authorSurName",
                        "Elizabeth M. Nel; NASA Goddard Space Flight Center, Forest Ecosystem Dynamics Project");
        fgdcNasaExpected
                .put("authorSurNameSort",
                        "Elizabeth M. Nel; NASA Goddard Space Flight Center, Forest Ecosystem Dynamics Project");
        fgdcNasaExpected
                .put("investigator",
                        "Elizabeth M. Nel; NASA Goddard Space Flight Center, Forest Ecosystem Dynamics Project");
        fgdcNasaExpected.put("pubDate", dateConverter.convert("1994"));
        fgdcNasaExpected
                .put("purpose",
                        "The field measurements component of the FED project was initiated to acquire data which are needed to: improve our understanding of vegetation, soil, and energy dynamics, and other biotic and abiotic processes within forested ecosystems so that the models can be parameterized, updated and modified; and to acquire in situ field observations for comparison with model results and conceptual model refinement.");
        // fgdcNasaExpected
        // .put("project",
        // "The field measurements component of the FED project was initiated to acquire data which are needed to: improve our understanding of vegetation, soil, and energy dynamics, and other biotic and abiotic processes within forested ecosystems so that the models can be parameterized, updated and modified; and to acquire in situ field observations for comparison with model results and conceptual model refinement.");

        fgdcNasaExpected
                .put("title",
                        "Global Positioning System Ground Control Points Acquired 1993 for the Forest Ecosystem Dynamics Project Spatial Data Archive");
        fgdcNasaExpected.put("webUrl", "http://fedwww.gsfc.nasa.gov/");
        fgdcNasaExpected
                .put("keywords",
                        "ENVIRONMENT#IMAGERY/BASE MAPS/EARTH COVER#PLANNING CADASTRE#EARTH SCIENCE > HUMAN DIMENSIONS > LAND USE/LAND COVER > LAND MANAGEMENT > GROUND CONTROL POINT#SATELLITES#GPS > GLOBAL POSITIONING SYSTEM#FED > FOREST ECOSYSTEM DYNAMICS#Easting#GIS#Northing#CONTINENT > NORTH AMERICA#CONTINENT > NORTH AMERICA > UNITED STATES OF AMERICA > MAINE");
        fgdcNasaExpected.put("fileID", "https://" + hostname + "/cn/v2/resolve/" + nasa_pid);

        fgdcNasaExpected.put("attributeName", "AREA#PERIMETER#GPS_1293#GPS_1293-ID#CODE#NUM_MEAS");
        fgdcNasaExpected.put("attributeLabel", "");
        fgdcNasaExpected
                .put("attributeDescription",
                        "Degenerate area of point#Degenerate perimeter of point#Internal feature number#User-assigned feature number#Site ID#Number of GPS fixes at location");
        fgdcNasaExpected.put("attributeUnit",
                "'0'#'0'#Sequential unique positive integer#Integer#Real numbers#Real numbers");
        fgdcNasaExpected
                .put("attribute",
                        "AREA  Degenerate area of point '0'#PERIMETER  Degenerate perimeter of point '0'#GPS_1293  Internal feature number Sequential unique positive integer#GPS_1293-ID  User-assigned feature number Integer#CODE  Site ID Real numbers#NUM_MEAS  Number of GPS fixes at location Real numbers");

        fgdcNasaExpected
                .put("text",
                        "http://www.nbii.gov/metadata/mdata/NASA/nasa_d_FEDGPS1293.xml     Elizabeth M. Nel; NASA Goddard Space Flight Center, Forest Ecosystem Dynamics Project  1994  Global Positioning System Ground Control Points Acquired 1993 for the Forest Ecosystem Dynamics Project Spatial Data Archive  Maps and Data  http://fedwww.gsfc.nasa.gov/     The Biospheric Sciences Branch (formerly Earth Resources Branch) within the Laboratory for Terrestrial Physics at NASA's Goddard Space Flight Center and associated University investigators are involved in a research program entitled Forest Ecosystem Dynamics (FED) which is fundamentally concerned with vegetation change of forest ecosystems at local to regional spatial scales (100 to 10,000 meters) and temporal scales ranging from monthly to decadal periods (10 to 100 years). The nature and extent of the impacts of these changes, as well as the feedbacks to global climate, may be addressed through modeling the interactions of the vegetation, soil, and energy components of the boreal ecosystem. The Howland Forest research site lies within the Northern Experimental Forest of International Paper. The natural stands in this boreal-northern hardwood transitional forest consist of spruce-hemlock-fir, aspen-birch, and hemlock-hardwood mixtures. The topography of the region varies from flat to gently rolling, with a maximum elevation change of less than 68 m within 10 km. Due to the region's glacial history, soil drainage classes within a small area may vary widely, from well drained to poorly drained. Consequently, an elaborate patchwork of forest communities has developed, supporting exceptional local species diversity. This data set is in ARC/INFO export format and contains Global Positioning Systems (GPS) ground control points in and around the International Paper Experimental Forest, Howland ME.  The field measurements component of the FED project was initiated to acquire data which are needed to: improve our understanding of vegetation, soil, and energy dynamics, and other biotic and abiotic processes within forested ecosystems so that the models can be parameterized, updated and modified; and to acquire in situ field observations for comparison with model results and conceptual model refinement.      19931201  19931231    publication date    Complete  As needed    The study site is located 56 km north of Bangor, Maine in Penobscot County (45 12'N, 68 44'W). The area is within the 7000 ha Northern Experimental Forest (NEF) owned by International Paper.   -68.0  -68.0  45  45.0      ISO Topic Category  ENVIRONMENT  IMAGERY/BASE MAPS/EARTH COVER  PLANNING CADASTRE    GCMD Science Keywords  EARTH SCIENCE > HUMAN DIMENSIONS > LAND USE/LAND COVER > LAND MANAGEMENT > GROUND CONTROL POINT  SATELLITES  GPS > GLOBAL POSITIONING SYSTEM  FED > FOREST ECOSYSTEM DYNAMICS    None  Easting  GIS  Northing    GCMD  CONTINENT > NORTH AMERICA  CONTINENT > NORTH AMERICA > UNITED STATES OF AMERICA > MAINE    None  Some coordinates are corrected to a greater positional accuracy than others.     Darrel L. Williams   TECHNICAL CONTACT   Mailing and Physical Address  NASA's Goddard Space Flight Center  Mailstop 614.0  Greenbelt  MD  21227  USA   301.614.6049  Darrel.L.Williams@nasa.gov    UNIX    Nel, E.M. and M.E. Jackson  1993  An Independent Evaluation of the Accuracy of Handheld GPS Receivers  Publication   Unpublished Report  n/a       Levine, E., K.J. Ranson, J.A. Smith, D.L. Williams, R.G. Knox, H.H. Shugart, D.L. Urban, and W.T. Lawrence  Unknown  Forest ecosystem dynamics: linking forest succession, soil process and radiation models  Publication   Ecological Modeling  65: 199-219        Information available at the FED website: http://fedwww.gsfc.nasa.gov/   Point features present.  A Trimble roving receiver placed on the top of the cab of a pick-up truck and leveled was used to collect position information at selected sites (road intersections) across the FED project study area. The field collected data was differentially corrected using base files measured by a Trimble Community Base Station. The Community Base Station is run by the Forestry Department at the University of Maine, Orono (UMO). The base station was surveyed by the Surveying Engineering Department at UMO using classical geodetic methods. Trimble software was used to produce coordinates in Universal Transverse Mercator (UTM) WGS84. Coordinates were adjusted based on field notes. All points were collected during December 1993 and differentially corrected.    GENERATE LIZEGPS_84  199501  1521    BUILD LIZEGPS_84 POINT  199501  1522    COPY LIZEGPS_84 /NET/FOREST/HOME/FED-GIS/GPS_GCP/LIZEGPS_84  199502  1524    LARA RENAME LIZEGPS_84 GPS_1293  199502  1731    LARA DOCUMENT GPS_1293 CREATE LARA  199502  1745    LARA PROJECTDEFINE COVER GPS_1293  199502  1836      Point    Point  27    String    Ring composed of chains         Universal Transverse Mercator   19   0.9996  0  0  0  0      coordinate pair   40  40   meters     World Geodetic System of 1984  6378137  298.25722210088        GPS_1293.PAT  GPS differential correction information  E. Nel    AREA  Degenerate area of point  FED    '0'  number  none      PERIMETER  Degenerate perimeter of point  FED    '0'  number  none      GPS_1293  Internal feature number  Computed    Sequential unique positive integer  number  none      GPS_1293-ID  User-assigned feature number  User-defined    Integer  number  none      CODE  Site ID  E. Nel    Real numbers  number  none      NUM_MEAS  Number of GPS fixes at location  E. Nel    Real numbers  number  none       Num_meas - Number of measurements used for differential correction.If num_meas is 180 or greater, the positional accuracy of the GPS coordinate is sub-meter.If num_meas is less than 180 but greater that 0, the positional accuracy is estimated at 10 meters based on Nels and Jackson, 1993.If num_meas is 0, the positional accuracy is estimated at 12 to 40 meters, based on published median error ranges.  The following measurements are included: Site ID, Number of GPS Fixes at Location, Degenerate Area/Perimeter of Point        Forest Ecosystem Dynamics Project, Biospheric Sciences Branch, Hydrospheric and Biospheric Sciences Laboratory, Earth Sciences Division, Science and Exploration Directorate, Goddard Space Flight Center, NASA  KENNETH JON RANSON   DATA CENTER CONTACT   Mailing and Physical Address  NASA  Goddard Space Flight Center  Mailstop 614.4  Greenbelt  MD  20771  USA   301-614-6650  301-614-6695  Kenneth.J.Ranson@nasa.gov    None     ARC/INFO  No compression applied       http://fedwww.gsfc.nasa.gov/gis_data/gps_1293.e00    FED Data: GPS ground control points and field site locations from 12/93     None     20090322     NASA (National Aeronautics and Space Administration)/GCMD (Goddard Space Flight Center)    Mailing and Physical Address  Goddard Space Flight Center  Code 610.2  Greenbelt  MD  20771  USA   301.614.6163  gsfc-gcmduso@mail.nasa.gov    FGDC Content Standard for Digital Geospatial Metadata  FGDC-STD-001-1998 www.nbii.gov_metadata_mdata_NASA_nasa_d_FEDGPS1293");

        // system metadata
        fgdcNasaExpected.put("id", nasa_pid);
        fgdcNasaExpected.put("seriesId", "");
        fgdcNasaExpected.put("fileName", "");
        fgdcNasaExpected.put("mediaType", "");
        fgdcNasaExpected.put("mediaTypeProperty", "");
        fgdcNasaExpected.put("formatId", "FGDC-STD-001.1-1999");
        fgdcNasaExpected.put("formatType", "METADATA");
        fgdcNasaExpected.put("size", "14880");
        fgdcNasaExpected.put("checksum", "c72ff66bbe7fa99e5fb399bab8cb6f85");
        fgdcNasaExpected.put("checksumAlgorithm", "MD5");
        fgdcNasaExpected.put("submitter", "CN=Dave Vieglais T799,O=Google,C=US,DC=cilogon,DC=org");
        fgdcNasaExpected.put("rightsHolder",
                "CN=Dave Vieglais T799,O=Google,C=US,DC=cilogon,DC=org");
        fgdcNasaExpected.put("replicationAllowed", "true");
        fgdcNasaExpected.put("numberReplicas", "3");
        fgdcNasaExpected.put("archived", "false");
        fgdcNasaExpected.put("preferredReplicationMN", "");
        fgdcNasaExpected.put("blockedReplicationMN", "");
        fgdcNasaExpected.put("obsoletes", "nasa_d_FEDGPS1292");
        fgdcNasaExpected.put("obsoletedBy", "nasa_d_FEDGPS1294");
        fgdcNasaExpected.put("dateUploaded",
                solrDateConverter.convert("2012-03-22T13:53:02.814057"));
        fgdcNasaExpected.put("dateModified",
                solrDateConverter.convert("2012-03-22T13:53:02.821757"));
        fgdcNasaExpected.put("datasource", "test_documents");
        fgdcNasaExpected.put("authoritativeMN", "test_documents");
        fgdcNasaExpected.put("replicaMN", "");
        fgdcNasaExpected.put("replicationStatus", "");
        fgdcNasaExpected.put("replicaVerifiedDate", "");
        fgdcNasaExpected.put("readPermission", "public");
        fgdcNasaExpected.put("writePermission",
                "CN=Dave Vieglais T799,O=Google,C=US,DC=cilogon,DC=org");
        fgdcNasaExpected.put("changePermission", "");
        fgdcNasaExpected.put("isPublic", "true");
        fgdcNasaExpected.put("dataUrl", "https://" + hostname + "/cn/v2/resolve/" + nasa_pid);

        /**
         * Third test object nikkis.180.1 an example of ESRI variant of FGDC
         * 
         */
        // science metadata
        esriExpected
                .put("abstract",
                        "Shape file created to delineate the boundary of the UNESCO Kruger to Canyons Biosphere Region.");
        esriExpected.put("beginDate", "");
        esriExpected.put("class", "");
        esriExpected.put("contactOrganization", "");
        esriExpected.put("eastBoundCoord", "32.041175");
        esriExpected.put("westBoundCoord", "29.92057");
        esriExpected.put("southBoundCoord", "-25.064201");
        esriExpected.put("northBoundCoord", "-23.726981");
        esriExpected.put("geohash_1", "k");
        esriExpected.put("geohash_2", "ke");
        esriExpected.put("geohash_3", "kew");
        esriExpected.put("geohash_4", "kewj");
        esriExpected.put("geohash_5", "kewj0");
        esriExpected.put("geohash_6", "kewj0y");
        esriExpected.put("geohash_7", "kewj0yz");
        esriExpected.put("geohash_8", "kewj0yzt");
        esriExpected.put("geohash_9", "kewj0yzt6");
        esriExpected.put("edition", "");
        esriExpected.put("endDate", "");
        esriExpected.put("gcmdKeyword", "");
        esriExpected.put("genus", "");
        esriExpected.put("site", "");
        esriExpected.put("presentationCat", "vector digital data");
        esriExpected.put("geoform", "vector digital data");
        esriExpected.put("kingdom", "");
        esriExpected.put("order", "");
        esriExpected.put("phylum", "");
        esriExpected.put("species", "");
        esriExpected.put("family", "");
        esriExpected.put("scientificName", "");
        esriExpected.put("placeKey", "k2c#lowveld");
        esriExpected.put("origin", "Debby Thomson");
        esriExpected.put("author", "Debby Thomson");
        esriExpected.put("authorSurName", "Debby Thomson");
        esriExpected.put("authorSurNameSort", "Debby Thomson");
        esriExpected.put("investigator", "Debby Thomson");
        esriExpected.put("pubDate", dateConverter.convert("March 2008"));
        esriExpected.put("purpose",
                "Delinate boundaries based on agreement with stakeholders and municipalities");

        esriExpected.put("title", "K2C_Biosphere");
        esriExpected.put("webUrl", "\\\\NSTEVENS-NB1\\D$\\GIS data\\Boundaries\\K2C_Biosphere.shp");

        esriExpected.put("keywords",
                "Ndlovu SAEON Node#SAEON, South Africa#Kruger to Canoyns#biosphere#k2c#lowveld");
        esriExpected.put("fileID", "https://" + hostname + "/cn/v2/resolve/" + esri_pid);

        esriExpected.put("attributeName", "FID#Shape#Id");
        esriExpected.put("attributeLabel", "FID#Shape#Id");
        esriExpected.put("attributeDescription", "Internal feature number.#Feature geometry.");
        esriExpected.put("attributeUnit", "");
        esriExpected.put("attribute",
                "FID FID Internal feature number.#Shape Shape Feature geometry.#Id Id");

        esriExpected
                .put("text",
                        "20100106  11110600  FALSE  20100226  11173900  20100226  11173900  {57F8DBAE-74A0-4AF4-BD76-9D78053A6627}    Microsoft Windows XP Version 5.1 (Build 2600) Service Pack 3; ESRI ArcCatalog 9.3.0.1770   en  Shape file created to delineate the boundary of the UNESCO Kruger to Canyons Biosphere Region.  Delinate boundaries based on agreement with stakeholders and municipalities     Debby Thomson  March 2008  K2C_Biosphere  K2C_Biosphere  vector digital data  \\\\NSTEVENS-NB1\\D$\\GIS data\\Boundaries\\K2C_Biosphere.shp  2     publication date    March 2008      Complete  Irregular     29.920570  32.041175  -23.726981  -25.064201    29.920570  32.041175  -25.064201  -23.726981      REQUIRED: Reference to a formally registered thesaurus or a similar authoritative source of theme keywords.  Ndlovu SAEON Node  SAEON, South Africa  Kruger to Canoyns  biosphere    k2c  lowveld    Freely accessible  REQUIRED: Restrictions and legal prerequisites for using the data set after access is granted.  Shapefile     Debby Thompson  Kruger to Canyons Biosphere region   Project manager  info@bushveldconnections.co.za    Initial map preparation: Prof Willem van der Riet and Craig Beech (GIS Business Soultions August 2000. Final map presentation: Debby Thompson, Hoedspruit Jan 2001Update of map: Debby Thompson, Hoedspruit,  March 2008    Microsoft Windows XP Version 5.1 (Build 2600) Service Pack 3; ESRI ArcCatalog 9.3.0.1770      K2C_Biosphere            29.92057  32.041175  -23.726981  -25.064201  1      29.92057  32.041175  -23.726981  -25.064201  1        Nikki Stevens              nikkis.180.1      DBF  K2C_Biosphere.dbf       nikkis.182.1          SBX  K2C_Biosphere.sbx       nikkis.183.1         PRJ  K2C_Biosphere.prj       nikkis.184.1         SHP  K2C_Biosphere.shp       nikkis.181.1        K2C_Biosphere.shp.xml    en  FGDC Content Standards for Digital Geospatial Metadata  FGDC-STD-001-1998  local time     REQUIRED: The person responsible for the metadata information.  REQUIRED: The organization responsible for the metadata information.    REQUIRED: The mailing and/or physical address for the organization or individual.  REQUIRED: The city of the address.  REQUIRED: The state or province of the address.  REQUIRED: The ZIP or other postal code of the address.   REQUIRED: The telephone number by which individuals can speak to the organization or individual.    20100226   http://www.esri.com/metadata/esriprof80.html  ESRI Metadata Profile       ISO 19115 Geographic Information - Metadata  DIS_ESRI1.0        dataset   Downloadable Data     0.003  0.003          002  file://\\\\NSTEVENS-NB1\\D$\\GIS data\\Boundaries\\K2C_Biosphere.shp  Local Area Network   0.003    Shapefile      Vector    Simple  Polygon  FALSE  1  TRUE  FALSE    G-polygon  1        GCS_WGS_1984    Decimal degrees  0.000000  0.000000    D_WGS_1984  WGS_1984  6378137.000000  298.257224        GCS_WGS_1984              1        K2C_Biosphere  Feature Class  1    FID  FID  OID  4  0  0  Internal feature number.  ESRI   Sequential unique whole numbers that are automatically generated.     Shape  Shape  Geometry  0  0  0  Feature geometry.  ESRI   Coordinates defining the features.     Id  Id  Number  6     20100226    Shape file was based rtaced from a georeferenced map. Inaccuracies will occur   20m      Dataset copied.  F:\\k2c\\K2C_Biosphere  20100106  11110600 "
                                + esri_pid);
        // system metadata
        esriExpected.put("id", esri_pid);
        esriExpected.put("seriesId", "");
        esriExpected.put("fileName", "");
        esriExpected.put("mediaType", "");
        esriExpected.put("mediaTypeProperty", "");
        esriExpected.put("formatId", "http://www.esri.com/metadata/esriprof80.dtd");
        esriExpected.put("formatType", "METADATA");
        esriExpected.put("size", "12575");
        esriExpected.put("checksum", "19021f947d54c11d1a4bad8725c827d5");
        esriExpected.put("checksumAlgorithm", "MD5");
        esriExpected.put("submitter", "uid=nikkis,o=SAEON,dc=ecoinformatics,dc=org");
        esriExpected.put("rightsHolder", "uid=nikkis,o=SAEON,dc=ecoinformatics,dc=org");
        esriExpected.put("replicationAllowed", "false");
        esriExpected.put("numberReplicas", "");
        esriExpected.put("archived", "false");
        esriExpected.put("preferredReplicationMN", "");
        esriExpected.put("blockedReplicationMN", "");
        esriExpected.put("obsoletes", "");
        esriExpected.put("obsoletedBy", "");
        esriExpected
                .put("dateUploaded", solrDateConverter.convert("2010-02-26T00:00:00.000+00:00"));
        esriExpected
                .put("dateModified", solrDateConverter.convert("2012-06-15T02:50:07.060+00:00"));
        esriExpected.put("datasource", "urn:node:SANPARKS");
        esriExpected.put("authoritativeMN", "urn:node:SANPARKS");
        esriExpected.put("replicaMN", "");
        esriExpected.put("replicationStatus", "");
        esriExpected.put("replicaVerifiedDate", "");
        esriExpected.put("readPermission", "public");
        esriExpected.put("writePermission", "");
        esriExpected.put("changePermission", "");
        esriExpected.put("isPublic", "true");
        esriExpected.put("dataUrl", "https://" + hostname + "/cn/v2/resolve/" + esri_pid);

        // ornl-mercury
        ornlMercuryExpected
                .put("abstract",
                        "This raster map the Oak Ridge Reservation identifies the aspect or the steepest down-slope direction from each cell to its neighbors.  The aspect map was created from the Digital Elevation Model (DEM) produced by Melanie Harris (Qsystems).  She created the DEM from a Digital Terrain Model (DTM) created from aerial photography taken during leaf-off in April of 1993.  The aerial photographs were flown at 7200' Above Ground Level resulting in a nominal scale of 1:14400.  The data were compiled at 1:2,400 scale (1=200') yielding the +/- 2.32m error.  The original DTM was created as part of the DOE Basemapping and Imagery project for the Oak Ridge Environmental Restoration program.");
        ornlMercuryExpected.put("beginDate", "1993-04-01T00:00:00.000Z");
        ornlMercuryExpected.put("class", "");
        ornlMercuryExpected.put("contactOrganization",
                "Environmental Sciences Division, Oak Ridge National Laboratory");
        ornlMercuryExpected.put("eastBoundCoord", "84.44");
        ornlMercuryExpected.put("westBoundCoord", "-84.19");
        ornlMercuryExpected.put("southBoundCoord", "-35.87");
        ornlMercuryExpected.put("northBoundCoord", "36.0");
        ornlMercuryExpected.put("geohash_1", "s");
        ornlMercuryExpected.put("geohash_2", "s0");
        ornlMercuryExpected.put("geohash_3", "s00");
        ornlMercuryExpected.put("geohash_4", "s000");
        ornlMercuryExpected.put("geohash_5", "s0006");
        ornlMercuryExpected.put("geohash_6", "s0006g");
        ornlMercuryExpected.put("geohash_7", "s0006gg");
        ornlMercuryExpected.put("geohash_8", "s0006gg4");
        ornlMercuryExpected.put("geohash_9", "s0006gg48");
        ornlMercuryExpected.put("edition", "");
        ornlMercuryExpected.put("endDate", "1993-04-01T00:00:00.000Z");
        ornlMercuryExpected.put("gcmdKeyword", "");
        ornlMercuryExpected.put("genus", "");
        ornlMercuryExpected.put("site", "");
        ornlMercuryExpected.put("presentationCat", "");
        ornlMercuryExpected.put("geoform", "");
        ornlMercuryExpected.put("kingdom", "");
        ornlMercuryExpected.put("order", "");
        ornlMercuryExpected.put("phylum", "");
        ornlMercuryExpected.put("species", "");
        ornlMercuryExpected.put("family", "");
        ornlMercuryExpected.put("scientificName", "");
        ornlMercuryExpected.put("placeKey", "Oak Ridge Reservation, TN, USA");
        ornlMercuryExpected.put("origin", "Henley, Marsha#Harris, Melinda H.");
        ornlMercuryExpected.put("author", "Henley, Marsha");
        ornlMercuryExpected.put("authorSurName", "Henley, Marsha");
        ornlMercuryExpected.put("authorSurNameSort", "Henley, Marsha");
        ornlMercuryExpected.put("investigator", "Henley, Marsha#Harris, Melinda H.");
        ornlMercuryExpected.put("pubDate", "");
        ornlMercuryExpected.put("purpose", "");

        ornlMercuryExpected.put("title", "Map ORR Aspect 2m 1993");
        ornlMercuryExpected
                .put("webUrl",
                        "http://daac.ornl.gov/data/bluangel_harvest/WBW/gibbs/metadata/Map_ORR_Aspect_2m_1993.xml#ftp://ftp.esd.ornl.gov/pub/orr/Maps/Topography/ORR/Aspect/Browse%20ORR%20Aspect.html");

        ornlMercuryExpected
                .put("keywords",
                        "TERRAIN ELEVATION|CAMERA|HELICOPTER#TERRAIN ELEVATION#HELICOPTER#CAMERA#Oak Ridge Reservation, TN, USA");
        ornlMercuryExpected.put("fileID", "https://" + hostname + "/cn/v2/resolve/"
                + ornl_mercury_pid);

        ornlMercuryExpected.put("attributeName", "");
        ornlMercuryExpected.put("attributeLabel", "");
        ornlMercuryExpected.put("attributeDescription", "");
        ornlMercuryExpected.put("attributeUnit", "");
        ornlMercuryExpected.put("attribute", "");

        ornlMercuryExpected
                .put("text",
                        "Henley, Marsha  Harris, Melinda H.  Map ORR Aspect 2m 1993  http://daac.ornl.gov/data/bluangel_harvest/WBW/gibbs/metadata/Map_ORR_Aspect_2m_1993.xml  ftp://ftp.esd.ornl.gov/pub/orr/Maps/Topography/ORR/Aspect/Browse%20ORR%20Aspect.html      This raster map the Oak Ridge Reservation identifies the aspect or the steepest down-slope direction from each cell to its neighbors.  The aspect map was created from the Digital Elevation Model (DEM) produced by Melanie Harris (Qsystems).  She created the DEM from a Digital Terrain Model (DTM) created from aerial photography taken during leaf-off in April of 1993.  The aerial photographs were flown at 7200' Above Ground Level resulting in a nominal scale of 1:14400.  The data were compiled at 1:2,400 scale (1=200') yielding the +/- 2.32m error.  The original DTM was created as part of the DOE Basemapping and Imagery project for the Oak Ridge Environmental Restoration program.       19930401  19930401      Final  As appropriate     -84.19  84.44  36  -35.87      Parameter_Sensor_Source  TERRAIN ELEVATION|CAMERA|HELICOPTER  Parameter  TERRAIN ELEVATION  Source  HELICOPTER  Sensor  CAMERA    Place Keywords  Oak Ridge Reservation, TN, USA       Henley, Marsha   6.1.10.8 Contact Electronic Mail: INTERNET &gt; mhenley@qsystems.net         Environmental Sciences Division, Oak Ridge National Laboratory  Gibbs, Holly K.   (865) 574-7356  6.1.10.8 Contact Electronic Mail: INTERNET &gt; mhenley@qsystems.net  gibbshk@ornl.gov        ftp://ftp.esd.ornl.gov/pub/orr/  Public         20020413 162015     Gibbs, Holly K.    Gibbs, Holly K.   (865) 574-7356  gibbshk@ornl.gov    FGDC Content Standard for Digital Geospatial Metadata    19930401  19930401   Oak Ridge Reservation, TN, USA  -84.19  84.44  36  -35.87   Aspect  Elevation  Oak Ridge Reservation  GIS  Raster  Digital Elevation Model  Topography   TERRAIN ELEVATION  CAMERA  HELICOPTER  TOPOGRAPHY  LAND SURFACE   The Oak Ridge Reservation Bioenergy and Carbon Sequestration Initiative; managed by Robin L. Graham  HELICOPTER|CAMERA|TERRAIN ELEVATION   Henley, Marsha  mhenley@qsystems.net    Harris, Melinda H.  Harrism@qsystems.net    ftp://ftp.esd.ornl.gov/pub/orr/Maps/Topography/ORR/Aspect/  Download Aspect data set     ftp://ftp.esd.ornl.gov/pub/orr/Maps/Topography/ORR/Aspect/Browse%20ORR%20Aspect.html  Browse image and metadata        Version 2.4.04a, Revision Date: 19 Mar 2002  metadata20.dtd   gibbshk@ornl.gov  Gibbs, Holly K.  (865) 574-7356   N/A   20140610  Created 20020413 161719 by 160.91.16.106  Raster  12119.4381   ftp://ftp.esd.ornl.gov/pub/orr/Maps/Topography/ORR/Aspect/Browse%20ORR%20Aspect.html  Browse image and metadata "
                                + ornl_mercury_pid);
        // system metadata
        ornlMercuryExpected.put("id", ornl_mercury_pid);
        ornlMercuryExpected.put("seriesId", "");
        ornlMercuryExpected.put("fileName", "");
        ornlMercuryExpected.put("mediaType", "");
        ornlMercuryExpected.put("mediaTypeProperty", "");
        ornlMercuryExpected.put("formatId", "http://purl.org/ornl/schema/mercury/terms/v1.0");
        ornlMercuryExpected.put("formatType", "METADATA");
        ornlMercuryExpected.put("size", "7424");
        ornlMercuryExpected.put("checksum", "8151041c5f77f89b05004272ed94d463");
        ornlMercuryExpected.put("checksumAlgorithm", "MD5");
        ornlMercuryExpected.put("submitter", "CN=mnEDORA,DC=dataone,DC=org");
        ornlMercuryExpected.put("rightsHolder", "CN=mnEDORA,DC=dataone,DC=org");
        ornlMercuryExpected.put("replicationAllowed", "");
        ornlMercuryExpected.put("numberReplicas", "");
        ornlMercuryExpected.put("archived", "false");
        ornlMercuryExpected.put("preferredReplicationMN", "");
        ornlMercuryExpected.put("blockedReplicationMN", "");
        ornlMercuryExpected.put("obsoletes", "");
        ornlMercuryExpected.put("obsoletedBy", "");
        ornlMercuryExpected.put("dateUploaded",
                solrDateConverter.convert("2014-06-11T09:33:32.000+00:00"));
        ornlMercuryExpected.put("dateModified",
                solrDateConverter.convert("2014-06-11T09:33:32.000+00:00"));
        ornlMercuryExpected.put("datasource", "urn:node:mnEDORA");
        ornlMercuryExpected.put("authoritativeMN", "urn:node:mnEDORA");
        ornlMercuryExpected.put("replicaMN", "");
        ornlMercuryExpected.put("replicationStatus", "");
        ornlMercuryExpected.put("replicaVerifiedDate", "");
        ornlMercuryExpected.put("readPermission", "public");
        ornlMercuryExpected.put("writePermission", "");
        ornlMercuryExpected.put("changePermission", "");
        ornlMercuryExpected.put("isPublic", "true");
        ornlMercuryExpected.put("dataUrl", "https://" + hostname + "/cn/v2/resolve/"
                + ornl_mercury_pid);
    }

    /**
     * Testing that the Xpath expressions used by XPathParser and associates are
     * 'mining' the expected data from the science and system metadata
     * documents.
     * 
     * @throws Exception
     */
    @Test
    public void testCsiroScienceMetadataFields() throws Exception {
        testXPathParsing(fgdcstd00111999Subprocessor, fdgc01111999SysMeta, fdgc01111999SciMeta,
                csiroExpected, csiro_pid);
    }

    @Test
    public void testFgdcNasaScienceMetadataFields() throws Exception {
        testXPathParsing(fgdcstd00111999Subprocessor, fgdcNasaSysMeta, fgdcNasaSciMeta,
                fgdcNasaExpected, nasa_pid);
    }

    @Test
    public void testEsriFgdcScienceMetadataFields() throws Exception {
        testXPathParsing(fgdcEsri80Subprocessor, fgdcEsriSysMeta, fgdcEsriSciMeta, esriExpected,
                esri_pid);
    }

    @Test
    public void testOrnlMercuryScienceMetadataFields() throws Exception {
        testXPathParsing(ornlMercury10Subprocessor, ornl_mercury_system_metadata,
                ornl_mercury_science_metadata, ornlMercuryExpected, ornl_mercury_pid);
    }
}
