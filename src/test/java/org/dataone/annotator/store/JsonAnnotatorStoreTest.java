package org.dataone.annotator.store;

import static org.junit.Assert.fail;

import org.junit.Test;

public class JsonAnnotatorStoreTest {

	@Test
	public void testSearchIndex() {
		try {
			AnnotatorStore as = new JsonAnnotatorStore(null);
			String query = "uri=https://pasta.lternet.edu/package/metadata/eml/knb-lter-hfr/14/15";
			String results = as.search(query);
			System.out.println(results);
		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
		
	}
}
