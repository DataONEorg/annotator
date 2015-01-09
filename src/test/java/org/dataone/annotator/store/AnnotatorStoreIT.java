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
package org.dataone.annotator.store;

import static org.junit.Assert.*;

import java.io.InputStream;

import org.dataone.portal.TokenGenerator;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import net.minidev.json.JSONObject;
import net.minidev.json.JSONValue;

public class AnnotatorStoreIT  {

	private static String ANNOTATION_TEST_DOC = "test/annotator-sample.json";

    protected MockHttpServletRequest request = null;
    
    public AnnotatorStoreIT() {
    	request = new MockHttpServletRequest(null, null, null);
	}

	/**
	 * Establish a testing framework by initializing appropriate objects
	 */
	@Before
	public void setUp() throws Exception {
		String testToken = TokenGenerator.getJWT("uid=kepler,o=unaffiliated,dc=ecoinformatics,dc=org", "Kepler");
		request.addHeader("x-annotator-auth-token", testToken);

	}

	/**
	 * Release any objects after tests are complete
	 */
	@After
	public void tearDown() {
	}
	
	@Test
	public void testRoundTrip() {
		try {
			
			
			// read the test annotation for CRUD operations
			AnnotatorStore as = new AnnotatorStore(request);
			InputStream is = this.getClass().getResourceAsStream(ANNOTATION_TEST_DOC);
			JSONObject annotation = (JSONObject) JSONValue.parse(is);
			String id = as.create(annotation);
			
			// check the created
			JSONObject originalAnnotation = as.read(id);
			String text = "Original annotation content";
			String originalText = originalAnnotation.get("text").toString();
			assertEquals(text, originalText);
			
			// test the update
//			text = "updated content";
//			JSONObject partialAnnotation =  (JSONObject) JSONValue.parse("{'text' : '" + text + "' }");
//			as.update(id, partialAnnotation);
//			JSONObject updatedAnnotation = as.read(id);
//			String updatedText = updatedAnnotation.get("text").toString();
//			assertEquals(text, updatedText);
			
			// TODO: check delete
			as.delete(id);
			
		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());

		}
	}
	

}
