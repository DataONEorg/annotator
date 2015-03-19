package org.dataone.annotator.generator;

import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.List;

import org.dataone.configuration.Settings;
import org.junit.Before;
import org.junit.Test;

public class AnnotationUploaderTest {

	private List<String> identifiers;

	@Before
	public void setup() {
		Settings.getConfiguration().setProperty("D1Client.CN_URL", "https://cn-sandbox-2.test.dataone.org/cn");

		identifiers = new ArrayList<String>();
		identifiers.add("https://pasta.lternet.edu/package/metadata/eml/knb-lter-arc/20032/2");
	}
	
	//@Test
	public void testProcess() {
		try {
			AnnotationUploader uploader = new AnnotationUploader();
			uploader.process(identifiers);
			
		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
		
	}
}
