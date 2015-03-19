package org.dataone.annotator.matcher.esor;

import java.io.InputStream;
import java.net.URI;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClients;
import org.dataone.annotator.matcher.ConceptItem;
import org.dataone.annotator.matcher.ConceptMatcher;
import org.dataone.annotator.matcher.QueryItem;
import org.json.JSONArray;
import org.json.JSONObject;

public class EsorService implements ConceptMatcher {

	private static Log log = LogFactory.getLog(EsorService.class);

	//private static final String REST_URL = "http://127.0.0.1:9100/search";

	//temporary server for testing
	private static final String REST_URL = "http://dataonetwc.tw.rpi.edu/linkipedia/search";


	@Override
	public List<ConceptItem> getConcepts(String fullText) throws Exception {
		return getConcepts(new QueryItem(fullText));
	}

	@Override
	public List<ConceptItem> getConcepts(QueryItem queryItem) throws Exception {
		return lookupEsor(queryItem);
	}

	private static List<ConceptItem> lookupEsor(QueryItem queryItem) throws Exception  {

		HttpClient client = HttpClients.createDefault();
		String uriStr = REST_URL + "?query=" + URLEncoder.encode(queryItem.toString(), "UTF-8");
		//System.out.println(uriStr);

		HttpGet method = new HttpGet(uriStr);
		method.setHeader("Accept", "application/json");
		HttpResponse response = client.execute(method);
		int code = response.getStatusLine().getStatusCode();
		if (2 != code / 100) {
				throw new Exception("response code " + code + " for resource at " + uriStr);
			}
		InputStream body = response.getEntity().getContent();
		String jsonStr = IOUtils.toString(body, "UTF-8");
		System.out.println(jsonStr);

		JSONObject json = new JSONObject(jsonStr);
		String query = json.getString("query");
		JSONArray results = json.getJSONArray("results");

		//analysis the result and return
		ArrayList<ConceptItem> concepts = new ArrayList<ConceptItem>();
		for(int i = 0; i < results.length(); i++){
			JSONObject r = results.getJSONObject(i);
			String url = r.getString("url");
			String score = r.getString("score");
			System.out.println("url=" + url + ", score=" + score);

			//returned result may be empty
			if(url.length()>0) {
				ConceptItem c = new ConceptItem(new URI(url.substring(1, url.length() - 1)), Double.parseDouble(score));
				concepts.add(c);
			}else{
				System.out.println("NA");
			}
			
		}

		return concepts;
	}
}
