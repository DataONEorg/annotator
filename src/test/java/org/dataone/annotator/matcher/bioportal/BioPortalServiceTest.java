/**  '$RCSfile$'
 *  Copyright: 2010 Regents of the University of California and the
 *              National Center for Ecological Analysis and Synthesis
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */
package org.dataone.annotator.matcher.bioportal;

import static org.junit.Assert.assertEquals;

import org.dataone.annotator.generator.AnnotationGenerator;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.hp.hpl.jena.ontology.OntClass;
import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Resource;

public class BioPortalServiceTest {

	
	/**
	 * constructor for the test
	 */
	public BioPortalServiceTest() {
	}

	/**
	 * Establish a testing framework by initializing appropriate objects
	 */
	@Before
	public void setUp() throws Exception {
	}

	/**
	 * Release any objects after tests are complete
	 */
	@After
	public void tearDown() {
	}

	
	@Test
	public void testLookup() {
		// set up our simple model for testing: Characteristic <- Temperature
		OntModel m = ModelFactory.createOntologyModel();
		OntClass characteristicClass = m.createClass(AnnotationGenerator.oboe_core + "Characteristic");
		OntClass temperatureClass = m.createClass(AnnotationGenerator.oboe_characteristics + "Temperature");
		temperatureClass.addSuperClass(characteristicClass);
		
		// look up the annotation recommendation from BioPortal
		String text = "Air temperature";
		Resource retClass = BioPortalService.lookupAnnotationClass(characteristicClass, text, AnnotationGenerator.OBOE_SBC);
		assertEquals(AnnotationGenerator.oboe_characteristics + "Temperature", retClass.getURI());
	}

}
