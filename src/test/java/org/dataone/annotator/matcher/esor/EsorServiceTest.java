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
package org.dataone.annotator.matcher.esor;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.util.List;

import org.dataone.annotator.generator.oa.OAAnnotationGenerator;
import org.dataone.annotator.matcher.ConceptItem;
import org.dataone.annotator.matcher.bioportal.BioPortalService;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.hp.hpl.jena.ontology.OntClass;
import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Resource;

public class EsorServiceTest {

	
	/**
	 * constructor for the test
	 */
	public EsorServiceTest() {
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
		
		try {

			// look up the annotation recommendation from ESOR
			String text = "carbon flux";
			EsorService service = new EsorService();
			List<ConceptItem> results;
			results = service.getConcepts(text);
			String retConcept = results.get(0).getUri().toString();
			System.out.println("retConcept=" + retConcept);
			assertEquals("http://purl.dataone.org/odo/ECSO_00000011", retConcept);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			fail(e.getMessage());
		}
		
	}

}
