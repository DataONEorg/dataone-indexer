package org.dataone.cn.indexer.annotation;

import static org.junit.Assert.*;

import java.util.Map;
import java.util.HashSet;
import java.util.Set;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.carrotsearch.randomizedtesting.annotations.ThreadLeakScope;

@ThreadLeakScope(ThreadLeakScope.Scope.NONE)
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "../../index/test-context.xml" })

public class OntologyModelServiceTest {
	@Test
	public void testConceptNotFoundExpansion() {
		/**
		 * Test that the OntologyModelService returns just the concept you asked it to expand when
		 * we expect that no pre-loaded ontologies define superclasses for it.
		 */
		try {
			Map<String, Set<String>> concepts = OntologyModelService.getInstance().expandConcepts("https://example.org");

			// Assert on Solr fields returend
			Set<String> fields = new HashSet<String>();
			fields.add("annotation_property_uri");
			fields.add("annotation_value_uri");
			assertEquals(concepts.keySet(), fields);

			// Assert on Solr field values returned
			Set<String> values = new HashSet<String>();
			values.add("https://example.org");
			assertEquals(values, concepts.get("annotation_property_uri"));
			assertEquals(values, concepts.get("annotation_value_uri"));
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	@Test
	public void testECSOMeasurementTypeExpansion() {
		/**
		 *
		 */
		try {
			Map<String, Set<String>> concepts = OntologyModelService.getInstance().expandConcepts("http://purl.dataone.org/odo/ECSO_00000543");

			// Assert on Solr fields returend
			Set<String> fields = new HashSet<String>();
			fields.add("annotation_property_uri");
			fields.add("annotation_value_uri");
			assertEquals(concepts.keySet(), fields);

			// Assert on Solr field values returned
			Set<String> values = new HashSet<String>();
			values.add("http://purl.dataone.org/odo/ECSO_00000536");
			values.add("http://purl.dataone.org/odo/ECSO_00000543");
			values.add("http://purl.dataone.org/odo/ECSO_00001105");
			values.add("http://purl.dataone.org/odo/ECSO_00000514");
			values.add("http://ecoinformatics.org/oboe/oboe.1.2/oboe-core.owl#MeasurementType");
			values.add("http://www.w3.org/2000/01/rdf-schema#Resource");
			values.add("http://www.w3.org/2000/01/rdf-schema#Class");
			values.add("http://www.w3.org/2002/07/owl#Class");

			assertEquals(values, concepts.get("annotation_value_uri"));
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	@Test
	public void testPropertyExpansion() {
		try {
			Map<String, Set<String>> concepts = OntologyModelService.getInstance().expandConcepts("http://purl.obolibrary.org/obo/RO_0002352");

			Set<String> values = new HashSet<String>();
			values.add("http://purl.obolibrary.org/obo/RO_0002328");
			values.add("http://purl.obolibrary.org/obo/RO_0002352");
			values.add("http://purl.obolibrary.org/obo/RO_0000056");

			assertEquals(values, concepts.get("annotation_property_uri"));
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	@Test
	public void testMOSAiCExpansion() {
		try {
			Map<String, Set<String>> concepts = OntologyModelService.getInstance().expandConcepts("https://purl.dataone.org/odo/MOSAIC_00000865");

			Set<String> values = new HashSet<String>();
			values.add("https://purl.dataone.org/odo/MOSAIC_00000865");
			values.add("https://purl.dataone.org/odo/MOSAIC_00000036");
			values.add("https://purl.dataone.org/odo/MOSAIC_00012000");
			values.add("http://www.w3.org/2000/01/rdf-schema#Resource");
			values.add("http://www.w3.org/2000/01/rdf-schema#Class");
			values.add("http://www.w3.org/2002/07/owl#Class");

			assertEquals(values, concepts.get("annotation_value_uri"));
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	@Test
	public void testMOSAiCSameAsExpansion() {
		try {
			Map<String, Set<String>> concepts = OntologyModelService.getInstance().expandConcepts("https://purl.dataone.org/odo/MOSAIC_00000225");

			Set<String> values = new HashSet<String>();
			values.add("https://purl.dataone.org/odo/MOSAIC_00000022"); // Project
			values.add("https://purl.dataone.org/odo/MOSAIC_00000225"); // MOSAiC
			values.add("https://purl.dataone.org/odo/MOSAIC_00000226"); // Multidisciplinary drifting Observatory for the Study of Arctic Climate (sameAs)
			values.add("https://purl.dataone.org/odo/MOSAIC_00000023"); // MOSAiC20192020 (sameAs)
			values.add("http://www.w3.org/2002/07/owl#NamedIndividual");
			values.add("http://www.w3.org/2000/01/rdf-schema#Resource");

			assertEquals(values, concepts.get("annotation_value_uri"));
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	@Test
	public void testARCRCExpansion() {
		try {
			Map<String, Set<String>> concepts = OntologyModelService.getInstance().expandConcepts("http://purl.dataone.org/odo/ARCRC_00000051");

			Set<String> values = new HashSet<String>();
			/**
			 * Add values for ARCRC:SnowDepth and its supertypes:
			 *
			 *   SnowDepth -> Snow Key Variable Arctic Key Variable -> Arctic Report Card Component
			 */
			values.add("http://purl.dataone.org/odo/ARCRC_00000051"); // ARCRC:SnowDepth
			values.add("http://purl.dataone.org/odo/ARCRC_00000502"); // ARCRC:SnowKeyVariable
			values.add("http://purl.dataone.org/odo/ARCRC_00000040"); // ARCRC:KeyVariable
			values.add("http://purl.dataone.org/odo/ARCRC_00000500"); // ARCRC:ArcticReportCardComponent


			/**
			 * Add values for ARCRC:SnowDepth's equivalentClass of ECSO:SnowDepth and its supertypes:
			 *
			 *   SnowDepth -> Water Depth -> Depth -> linearMeasurementType -> Measurement Type
			 */
			values.add("http://purl.dataone.org/odo/ECSO_00001205"); // ECSO:SnowDepth
			values.add("http://purl.dataone.org/odo/ECSO_00000553"); // ECSO:linearMeasurementType
			values.add("http://purl.dataone.org/odo/ECSO_00001203"); // ECSO:WaterDepth
			values.add("http://purl.dataone.org/odo/ECSO_00001250"); // ECSO:Depth
			values.add("http://ecoinformatics.org/oboe/oboe.1.2/oboe-core.owl#MeasurementType");

			// Values for high level stuff
			values.add("http://www.w3.org/2000/01/rdf-schema#Resource");
			values.add("http://www.w3.org/2000/01/rdf-schema#Class");
			values.add("http://www.w3.org/2002/07/owl#Class");
			values.add("http://www.w3.org/2002/07/owl#NamedIndividual");

			assertEquals(values, concepts.get("annotation_value_uri"));
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	@Test
	public void testNamedIndividualExpansion() {
		try {
			Map<String, Set<String>> concepts = OntologyModelService.getInstance().expandConcepts("http://purl.dataone.org/odo/ARCRC_00000507");

			Set<String> values = new HashSet<String>();
			values.add("http://purl.dataone.org/odo/ARCRC_00000507"); // ARCRC:Greenland itself
			values.add("http://www.w3.org/2002/07/owl#NamedIndividual"); // the type of ARCRC:Greenland
			values.add("http://purl.dataone.org/odo/ARCRC_00000505"); // ARCRC:GeographicNamedPlace (type of ARCRC:Greenland)
			values.add("http://purl.dataone.org/odo/ARCRC_00000506"); // ARCRC:Place (supertype of GeographicNamedPlace)
			values.add("http://purl.dataone.org/odo/ARCRC_00000500"); // ARCRC:ArcticReportCardComponent
			values.add("http://purl.obolibrary.org/obo/GAZ_00001507"); // sameAs

			assertEquals(values, concepts.get("annotation_value_uri"));
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	@Test
	public void testEquivalentPropertyExpansion() {
		try {
			Map<String, Set<String>> concepts = OntologyModelService.getInstance().expandConcepts("https://purl.dataone.org/odo/MOSAIC_00002250");

			Set<String> values = new HashSet<String>();
			values.add("http://www.w3.org/ns/ssn/deployedSystem");
			values.add("https://purl.dataone.org/odo/MOSAIC_00002250");

			assertEquals(values, concepts.get("annotation_property_uri"));
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			fail(e.getMessage());
		}
	}
}
