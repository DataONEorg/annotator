package org.dataone.annotator.generator;

import static org.junit.Assert.fail;

import java.io.InputStream;
import java.net.URLEncoder;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import net.minidev.json.JSONArray;
import net.minidev.json.JSONObject;
import net.minidev.json.parser.JSONParser;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.dataone.annotator.generator.json.JsonAnnotatorGenerator;
import org.dataone.client.auth.CertificateManager;
import org.dataone.client.v2.itk.D1Client;
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
			uploader.process(identifiers);
			
		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
		
	}
	
	//@Test
	public void testBatchProcess() {

		try {
			
		 	// get the values from solr
	        String datasource = "urn:node:mnTestMSTMIP";
			String paramString = "?q=attribute:GPP" + "&fl=attribute,origin,datasource,id" + "&wt=json&rows=100";
			//String paramString = "?q=datasource:" + URLEncoder.encode("\"" + datasource  + "\"", "UTF-8") + "&fl=attribute,origin,datasource,id" + "&wt=json";

			log.debug("paramString=" + paramString);
	        
	        InputStream solrStream = D1Client.getCN().query(session, "solr", paramString);
		
	        JSONParser jsonParser = new JSONParser();
	        Object results = jsonParser.parse(solrStream);
	        log.debug("results:" + results);
	        JSONObject solrResults = (JSONObject) results;
	        log.debug("results SIZE:" + solrResults.size());

	        // get the first matching doc (should be only)
	        JSONArray solrDocs = (JSONArray)((JSONObject) solrResults.get("response")).get("docs");
			
	        Iterator<Object> docIter = solrDocs.iterator();
			while (docIter.hasNext()) {
				JSONObject solrDoc = (JSONObject) docIter.next();
				String identifier = solrDoc.get("id").toString();
				identifiers.add(identifier);
			}

			AnnotationUploader uploader = new AnnotationUploader(session);
			uploader.process(identifiers);
			
		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
		
	}
}
