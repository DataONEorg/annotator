package org.dataone.annotator.generator;

import static org.junit.Assert.fail;

import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.dataone.client.auth.CertificateManager;
import org.dataone.configuration.Settings;
import org.dataone.service.types.v1.Session;
import org.dataone.service.types.v1.Subject;
import org.junit.Before;
import org.junit.Test;

public class AnnotationUploaderTest {

	private static Log log = LogFactory.getLog(AnnotationUploaderTest.class);
	
	private List<String> identifiers;
	private Session session = null;

	@Before
	public void setup() {
		Settings.getConfiguration().setProperty("D1Client.CN_URL", "https://cn-sandbox-2.test.dataone.org/cn");
		
		identifiers = new ArrayList<String>();
		
		X509Certificate certificate = CertificateManager.getInstance().loadCertificate();
		if (certificate == null) {
			return;
		}
			
		PrivateKey key = CertificateManager.getInstance().loadKey();
		String subjectDN = CertificateManager.getInstance().getSubjectDN(certificate);
		CertificateManager.getInstance().registerCertificate(subjectDN, certificate, key );
		session = new Session();
		Subject subject = new Subject();
		subject.setValue(subjectDN);
		session.setSubject(subject);
		
	}
	
	@Test
	public void testProcess() {

		identifiers.add("https://pasta.lternet.edu/package/metadata/eml/knb-lter-arc/20032/2");

		try {
			AnnotationUploader uploader = new AnnotationUploader(session);
			uploader.createAnnotationsFor(identifiers);
			
		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
		
	}
	
}
