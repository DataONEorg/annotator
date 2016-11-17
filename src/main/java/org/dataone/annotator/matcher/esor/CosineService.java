package org.dataone.annotator.matcher.esor;

import java.io.InputStream;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClients;
import org.dataone.annotator.matcher.ConceptItem;
import org.dataone.annotator.matcher.ConceptMatcher;

public class CosineService implements ConceptMatcher {

	private static Log log = LogFactory.getLog(CosineService.class);

	//the cosine server
	private static final String REST_URL = "https://esor.tw.rpi.edu/cosine/?dataset=";

	
	public CosineService() {
	}


	@Override
	public List<ConceptItem> getConcepts(String fullText, String unit, String context) throws Exception {
		return null;
	}

	@Override
	public List<ConceptItem> getConcepts(Map<String, String> queryItems) throws Exception {
		return null;
	}

	public static String lookupCosine(String id) throws Exception  {

		HttpClient client = HttpClients.createDefault();
		String uriStr = REST_URL + id;
		log.debug("uriStr=" + uriStr);

		// use post to handle potentially long parameter values
		HttpGet method = new HttpGet(uriStr);
		method.setHeader("Accept", "*/*");
		
		HttpResponse response = client.execute(method);
		int code = response.getStatusLine().getStatusCode();
		if (2 != code / 100) {
				throw new Exception("response code " + code + " for resource at " + uriStr);
		}
		InputStream body = response.getEntity().getContent();
		String jsonStr = IOUtils.toString(body, "UTF-8");
		log.debug("jsonStr=" + jsonStr);

		return jsonStr;
		
		
	}
	
	
}
