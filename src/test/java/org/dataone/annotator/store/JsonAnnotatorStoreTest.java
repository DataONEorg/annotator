package org.dataone.annotator.store;

import static org.junit.Assert.fail;

import org.junit.Test;

public class JsonAnnotatorStoreTest {

	@Test
	public void testSearchIndex() {
		try {
			AnnotatorStore as = new JsonAnnotatorStore(null);
			//String query = "uri=tao.1.6";
			String query = "uri=http://localhost:8080/metacat/d1/mn/v1/object/tao.1.6";
			String results = as.search(query);
			System.out.println("RESULTS = " + results);
		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
		
	}
}
