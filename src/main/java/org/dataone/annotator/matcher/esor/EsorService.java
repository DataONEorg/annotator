package org.dataone.annotator.matcher.esor;

import java.io.InputStream;
import java.net.URI;
import java.net.URLEncoder;
import java.util.ArrayList;
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
		//merge two results with different escape condition
		String query = parseTerm(fullText);
		String escapedSpaceQuery = escapeToSpace(query);
		String escapedCommaQuery = escapeToComma(query);

		if(escapedSpaceQuery == escapedCommaQuery){
			return lookupEsor(escapedSpaceQuery);
		}else{
			List<ConceptItem> res_escapedSpace = lookupEsor(escapedSpaceQuery);
			List<ConceptItem> res_escapedComma = lookupEsor(escapedCommaQuery);
			return mergeRes(res_escapedSpace, res_escapedComma);
		}

		//return lookupEsor(fullText);
	}

	@Override
	public List<ConceptItem> getConcepts(Map<String, String> queryItems) throws Exception {
		StringBuffer sb = new StringBuffer();
		for (String value: queryItems.values()) {
			sb.append(value);
			sb.append(" ");
		}
		//return lookupEsor(sb.toString());
		return getConcepts(sb.toString());
	}

	private static List<ConceptItem> lookupEsor(String query) throws Exception  {

		HttpClient client = HttpClients.createDefault();
		// remove quotes for now
		// see: https://github.com/DataONEorg/sem-prov-design/issues/134
		//query = query.replaceAll("\"", "");
		String uriStr = REST_URL + "?query=" + URLEncoder.encode(query, "UTF-8");
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
		//String query = json.getString("query");
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
				//System.out.println("NA");
			}

		}

		return concepts;
	}


	//escape input query
	private String parseTerm(String str) throws Exception{

		if(str.contains("(")){
			str =  str.substring(0, str.indexOf("("));
		}

		str = str.replaceAll("\\s+$", "");

		str = replaceFromSlashToSpace(replaceFromDotToSpace(replaceFromDashToSpace(str)));
		str = str.replace("%", " percent");
		str = insertSpaceBeforeCapital(str);
		str = URLEncoder.encode(str, "UTF-8").replaceAll("\\+", "%20");
		return str;
	}

	private String replaceFromDotToSpace(String str) {
		return str.replace(".", " ");
	}

	private String replaceFromSlashToSpace(String str){
		return str.replace("/", " ");
	}

	private String insertSpaceBeforeCapital(String str){
		char[] charArr = str.toCharArray();
		StringBuilder sb = new StringBuilder();
		for(int i = 0 ; i < charArr.length; i++){
			if(i>0 && charArr[i] >= 'A' && charArr[i] <= 'Z' && charArr[i-1] >= 'a'&& charArr[i-1] <= 'z')
				sb.append(" ");
			sb.append(charArr[i]);
		}
		return sb.toString();
	}

	private String replaceFromDashToSpace(String original){
		return original.replace("_", " ");
	}

	private String escapeToSpace(String original){
		return original.replace(" ", "%20");
	}

	private String escapeToComma(String original){
		return original.replace("%20", ",");
	}

	private List<ConceptItem> mergeRes(List<ConceptItem> res_escapedSpace, List<ConceptItem> res_escapedComma) {
		if(res_escapedSpace.size()==0) return res_escapedComma;
		if(res_escapedComma.size()==0) return res_escapedSpace;

		int indexS = 0;
		int indexC = 0;
		while(indexS < res_escapedSpace.size()){
			if(indexC < res_escapedComma.size() && res_escapedComma.get(indexC).getWeight() >= res_escapedSpace.get(indexS).getWeight()){
				res_escapedSpace.add(indexS, res_escapedComma.get(indexC));
				indexS++;
				indexC++;
			}else{
				indexS++;
			}
		}

		for(int i = indexC; i < res_escapedComma.size(); i++ ){
			res_escapedSpace.add(res_escapedComma.get(i));
		}

		return res_escapedSpace;
	}
}
