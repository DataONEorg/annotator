package org.dataone.annotator.ontology;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class MeasurementTypeGeneratorTest {

	@Test
	public void testClassLookup() {
		String fullLabel = "oboe-characteristics:Count";
		MeasurementTypeGenerator mtg = new MeasurementTypeGenerator();
		String conceptUri = mtg.lookupConcept(fullLabel);
		assertEquals("http://ecoinformatics.org/oboe/oboe.1.1/oboe-characteristics.owl#Count", conceptUri);
		
	}
	
	@Test
	public void testLabelLookup() {
		String fullLabel = "ecso:concentration";
		MeasurementTypeGenerator mtg = new MeasurementTypeGenerator();
		String conceptUri = mtg.lookupConcept(fullLabel);
		assertEquals("http://purl.dataone.org/odo/ECSO_00000512", conceptUri);
		
	}
}
